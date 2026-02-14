package com.marketplace.service;

import com.marketplace.entity.*;
import com.marketplace.enums.AuctionStatus;
import com.marketplace.enums.BidStatus;
import com.marketplace.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BidServiceTest {

    @Mock
    private BidRepository bidRepository;
    @Mock
    private AuctionRepository auctionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private ProxyBidRepository proxyBidRepository;
    @Mock
    private RedisLockService redisLockService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private BidService bidService;

    private User bidder;
    private User seller;
    private Item item;
    private Auction auction;

    @BeforeEach
    void setUp() {
        bidder = User.builder().id(1L).email("bidder@test.com").build();
        seller = User.builder().id(2L).email("seller@test.com").build();

        item = Item.builder()
                .id(1L)
                .seller(seller)
                .currentPrice(new BigDecimal("100.00"))
                .build();

        auction = Auction.builder()
                .id(1L)
                .item(item)
                .status(AuctionStatus.ACTIVE)
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(1))
                .minBidIncrement(new BigDecimal("10.00"))
                .bidCount(1)
                .build();
    }

    @Test
    void placeBid_Successful() {
        when(userRepository.findByEmail("bidder@test.com")).thenReturn(Optional.of(bidder));
        when(auctionRepository.findByItemId(1L)).thenReturn(Optional.of(auction));
        when(redisLockService.acquireLockWithRetry(anyString(), anyLong(), anyLong())).thenReturn(true);
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction));
        when(bidRepository.save(any(Bid.class))).thenAnswer(i -> {
            Bid b = i.getArgument(0);
            b.setId(100L);
            return b;
        });

        Bid result = bidService.placeBid(1L, new BigDecimal("120.00"), null, "bidder@test.com");

        assertNotNull(result);
        assertEquals(new BigDecimal("120.00"), result.getAmount());
        verify(bidRepository).save(any(Bid.class));
        verify(auctionRepository).save(auction);
        verify(itemRepository).save(item);
    }

    @Test
    void placeBid_WithProxyCompetition() {
        // Setup existing proxy from another user
        User otherUser = User.builder().id(3L).email("other@test.com").build();
        ProxyBid otherProxy = ProxyBid.builder()
                .bidder(otherUser)
                .maxAmount(new BigDecimal("150.00")) // Higher than our bid
                .isActive(true)
                .build();

        when(userRepository.findByEmail("bidder@test.com")).thenReturn(Optional.of(bidder));
        when(auctionRepository.findByItemId(1L)).thenReturn(Optional.of(auction));
        when(redisLockService.acquireLockWithRetry(anyString(), anyLong(), anyLong())).thenReturn(true);
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction));
        when(proxyBidRepository.findByAuctionIdAndIsActiveTrue(1L)).thenReturn(List.of(otherProxy));

        // We place 120, Other has 150. Other should win at 130 (120 + 10).
        // Our bid of 120 is placed (and beaten).
        // Other bids 130.

        when(bidRepository.save(any(Bid.class))).thenAnswer(i -> {
            Bid b = i.getArgument(0);
            b.setId(System.nanoTime()); // Random ID
            return b;
        });

        Bid result = bidService.placeBid(1L, new BigDecimal("120.00"), null, "bidder@test.com");

        // The returned bid should be the winning one (Auto-bid from other user)
        assertEquals(new BigDecimal("130.00"), result.getAmount());
        assertEquals(otherUser.getId(), result.getBidder().getId());
        assertTrue(result.getIsAutoBid());
    }
}
