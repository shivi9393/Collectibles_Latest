package com.marketplace.scheduler;

import com.marketplace.entity.Auction;
import com.marketplace.entity.Bid;
import com.marketplace.event.AuctionWonEvent;
import com.marketplace.entity.Item;
import com.marketplace.entity.Order;
import com.marketplace.enums.AuctionStatus;
import com.marketplace.enums.OrderStatus;
import com.marketplace.repository.AuctionRepository;
import com.marketplace.repository.BidRepository;
import com.marketplace.repository.ItemRepository;
import com.marketplace.repository.OrderRepository;
import com.marketplace.service.RedisLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionClosingScheduler {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final RedisLockService redisLockService;
    private final SimpMessagingTemplate messagingTemplate;

    @Scheduled(fixedRate = 1000) // Run every second
    public void closeExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();
        List<Auction> expiredAuctions = auctionRepository.findExpiredAuctions(now);

        for (Auction auction : expiredAuctions) {
            String lockKey = "auction_close:" + auction.getId();
            // Try to acquire lock to ensure only one instance closes the auction
            if (redisLockService.acquireLock(lockKey, 5000)) {
                try {
                    closeAuction(auction);
                } catch (Exception e) {
                    log.error("Failed to close auction {}", auction.getId(), e);
                } finally {
                    redisLockService.releaseLock(lockKey);
                }
            }
        }
    }

    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Transactional
    public void closeAuction(Auction auction) {
        log.info("Closing auction {}", auction.getId());

        // Reload to be sure
        auction = auctionRepository.findById(auction.getId()).orElseThrow();
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            return; // Already closed
        }

        auction.setStatus(AuctionStatus.CLOSED);
        auction.setClosedAt(LocalDateTime.now());

        if (auction.getHighestBidId() != null) {
            Bid winningBid = bidRepository.findById(auction.getHighestBidId()).orElseThrow();
            auction.setWinner(winningBid.getBidder());

            // Create Order
            createOrder(auction, winningBid);

            // Notify winner (via WebSocket/Notification)
            messagingTemplate.convertAndSend("/topic/auction/" + auction.getId() + "/ended", winningBid);

            // Publish Domain Event
            eventPublisher.publishEvent(new com.marketplace.event.AuctionWonEvent(
                    auction.getId(),
                    winningBid.getBidder().getId(),
                    winningBid.getAmount(),
                    winningBid.getBidder().getEmail(),
                    auction.getItem().getTitle()));

        } else {
            // No bids
            log.info("Auction {} closed with no bids", auction.getId());
            messagingTemplate.convertAndSend("/topic/auction/" + auction.getId() + "/ended", "NO_BIDS");

            // Item remains UNSOLD? Or do we update item status?
            Item item = auction.getItem();
            // item.setStatus(ItemStatus.UNSOLD); // If we had ItemStatus
            itemRepository.save(item);
        }

        auctionRepository.save(auction);
    }

    private void createOrder(Auction auction, Bid winningBid) {
        Order order = Order.builder()
                .buyer(winningBid.getBidder())
                .seller(auction.getItem().getSeller())
                .item(auction.getItem())
                .amount(winningBid.getAmount())
                .orderType(com.marketplace.enums.OrderType.AUCTION_WIN)
                .status(OrderStatus.PENDING_PAYMENT)
                .shippingInfo(null) // To be filled by buyer
                .build();

        orderRepository.save(order);
        log.info("Order created for auction {}: Order ID {}", auction.getId(), order.getId());
    }
}
