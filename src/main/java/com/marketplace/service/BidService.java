package com.marketplace.service;

import com.marketplace.entity.*;
import com.marketplace.repository.*;
import com.marketplace.enums.AuctionStatus;
import com.marketplace.enums.BidStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.marketplace.event.BidPlacedEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BidService {

    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final ProxyBidRepository proxyBidRepository;
    private final RedisLockService redisLockService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher; // Injected ApplicationEventPublisher

    private static final long LOCK_TIMEOUT_MS = 5000;
    private static final long LOCK_WAIT_MS = 3000;

    @Transactional
    public Bid placeBid(Long itemId, BigDecimal amount, BigDecimal maxProxyAmount, String userEmail) {
        Auction auction = auctionRepository.findByItemId(itemId)
                .orElseThrow(() -> new RuntimeException("Auction not found for this item"));

        String lockKey = "auction:" + auction.getId();
        boolean locked = redisLockService.acquireLockWithRetry(lockKey, LOCK_WAIT_MS, LOCK_TIMEOUT_MS);

        if (!locked) {
            throw new RuntimeException("System busy, please try again.");
        }

        try {
            // Refresh auction entity to get latest state
            auction = auctionRepository.findById(auction.getId()).orElseThrow();
            return processBid(auction, amount, maxProxyAmount, userEmail);
        } finally {
            redisLockService.releaseLock(lockKey);
        }
    }

    private Bid processBid(Auction auction, BigDecimal amount, BigDecimal maxProxyAmount, String userEmail) {
        User bidder = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Item item = auction.getItem();
        LocalDateTime now = LocalDateTime.now();

        // 1. Validation
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            // Allow bidding if SCHEDULED and time passed (auto-start logic conceptually)
            // But realistically, we should expect ACTIVE.
            // For robustness, if start time passed and not closed, treat as active.
            if (auction.getStatus() == AuctionStatus.SCHEDULED && now.isAfter(auction.getStartTime())) {
                auction.setStatus(AuctionStatus.ACTIVE);
            } else {
                throw new RuntimeException("Auction is not active");
            }
        }

        if (now.isAfter(auction.getEndTime())) {
            throw new RuntimeException("Auction has ended");
        }

        if (item.getSeller().getId().equals(bidder.getId())) {
            throw new RuntimeException("You cannot bid on your own item");
        }

        BigDecimal currentPrice = item.getCurrentPrice() != null ? item.getCurrentPrice() : auction.getReservePrice();
        if (currentPrice == null)
            currentPrice = BigDecimal.ZERO;

        BigDecimal minIncrement = auction.getMinBidIncrement();
        BigDecimal minBidAmount = currentPrice.add(minIncrement);

        // First bid might be equal to starting price (reserve) if 0 bids
        if (auction.getBidCount() == 0 && auction.getReservePrice() != null) {
            minBidAmount = auction.getReservePrice();
        }

        // Check incoming bid amount
        if (amount.compareTo(minBidAmount) < 0) {
            throw new RuntimeException("Bid must be at least " + minBidAmount);
        }

        // Determine effective max amount for this user
        BigDecimal effectiveMax = maxProxyAmount != null && maxProxyAmount.compareTo(amount) > 0 ? maxProxyAmount
                : amount;

        // 2. Handle Proxy Bids Competition
        // Fetch existing active proxy bid for *other* users
        List<ProxyBid> activeProxyBids = proxyBidRepository.findByAuctionIdAndIsActiveTrue(auction.getId());

        // Filter out current bidder's existing proxy if any (update it later)
        ProxyBid highestOpponentProxy = activeProxyBids.stream()
                .filter(pb -> !pb.getBidder().getId().equals(bidder.getId()))
                .max(Comparator.comparing(ProxyBid::getMaxAmount))
                .orElse(null);

        // Save/Update current user's proxy bid
        updateOrCreateProxyBid(auction, bidder, effectiveMax);

        Bid newBid = null;

        if (highestOpponentProxy == null) {
            // No competition from proxy. Just place the bid.
            // If the user placed a proxy bid significantly higher than current price, we
            // only bid the minimum necessary (amount)
            // Actually, the 'amount' passed is the *visible* bid they want to place NOW.
            newBid = createBid(auction, bidder, amount, false, false);
            updateAuctionAndItem(auction, item, newBid);
        } else {
            // Competition!
            // Case A: Incoming (EffectiveMax) > Opponent (Max)
            if (effectiveMax.compareTo(highestOpponentProxy.getMaxAmount()) > 0) {
                // Opponent bids up to their max
                // Check if we need to generate an intermediate bid for the opponent to show
                // they were outbid at their max
                // Typically we just jump to OpponentMax + Increment for the Winner.

                // But strictly, we should record the opponent's max outbid.
                // For simplicity: Winner takes it at OpponentMax + Increment.
                BigDecimal priceToWin = highestOpponentProxy.getMaxAmount().add(minIncrement);

                if (priceToWin.compareTo(effectiveMax) > 0) {
                    priceToWin = effectiveMax; // Cap at our max (shouldn't happen given if check)
                }

                // Create a bid for the opponent at their max (optional, shows "Auto-outbid")
                // createBid(auction, highestOpponentProxy.getBidder(),
                // highestOpponentProxy.getMaxAmount(), true, true);

                // Create winning bid for current user
                newBid = createBid(auction, bidder, priceToWin, true, maxProxyAmount != null);
                updateAuctionAndItem(auction, item, newBid);

                // Deactivate opponent proxy
                // highestOpponentProxy.setIsActive(false); // It's beaten
                // proxyBidRepository.save(highestOpponentProxy);

            } else {
                // Case B: Opponent (Max) >= Incoming (EffectiveMax)
                // Opponent wins.
                // Current user places their max bid (amount)
                createBid(auction, bidder, amount, false, false); // The bid that fails

                // Opponent auto-bids to beat it
                BigDecimal priceToBeat = amount.add(minIncrement);
                if (priceToBeat.compareTo(highestOpponentProxy.getMaxAmount()) > 0) {
                    priceToBeat = highestOpponentProxy.getMaxAmount();
                }

                // If priceToBeat matches existing price, do nothing?
                // No, we must ensure the price goes up.

                Bid autoBid = createBid(auction, highestOpponentProxy.getBidder(), priceToBeat, true, true);
                updateAuctionAndItem(auction, item, autoBid);

                // Current user is NOT the winner.
                // We return the *attempted* bid? Or the current highest?
                // Usually return the confirmed bid for the user, but throw/notify they are
                // outbid.
                // For this API, let's return the latest state.
                return autoBid; // The winning bid (opponent's)
            }
        }

        return newBid;
    }

    private void updateOrCreateProxyBid(Auction auction, User bidder, BigDecimal maxAmount) {
        Optional<ProxyBid> existing = proxyBidRepository.findByAuctionIdAndBidderId(auction.getId(), bidder.getId());
        if (existing.isPresent()) {
            ProxyBid pb = existing.get();
            if (maxAmount.compareTo(pb.getMaxAmount()) > 0) {
                pb.setMaxAmount(maxAmount);
                pb.setIsActive(true);
                proxyBidRepository.save(pb);
            }
        } else {
            ProxyBid pb = ProxyBid.builder()
                    .auction(auction)
                    .bidder(bidder)
                    .maxAmount(maxAmount)
                    .currentAmount(BigDecimal.ZERO)
                    .isActive(true)
                    .build();
            proxyBidRepository.save(pb);
        }
    }

    private Bid createBid(Auction auction, User bidder, BigDecimal amount, boolean isAuto, boolean isProxy) {
        Bid bid = Bid.builder()
                .auction(auction)
                .bidder(bidder)
                .amount(amount)
                .status(BidStatus.ACTIVE)
                .isAutoBid(isAuto)
                .isProxyBid(isProxy)
                .build();
        return bidRepository.save(bid);
    }

    private void updateAuctionAndItem(Auction auction, Item item, Bid highestBid) {
        auction.setHighestBidId(highestBid.getId());
        auction.setBidCount(auction.getBidCount() + 1);
        auction.setWinner(highestBid.getBidder()); // Temporarily set winner
        auctionRepository.save(auction);

        item.setCurrentPrice(highestBid.getAmount());
        itemRepository.save(item);

        // Broadcast update via WebSocket
        messagingTemplate.convertAndSend("/topic/auction/" + auction.getId(), highestBid);
    }
}
