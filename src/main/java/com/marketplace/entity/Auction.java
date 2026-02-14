package com.marketplace.entity;

import com.marketplace.enums.AuctionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "auctions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false, unique = true)
    private Item item;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "min_bid_increment", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal minBidIncrement = new BigDecimal("1.00");

    @Column(name = "reserve_price", precision = 10, scale = 2)
    private BigDecimal reservePrice;

    @Column(name = "highest_bid_id")
    private Long highestBidId;

    @Column(name = "bid_count")
    @Builder.Default
    private Integer bidCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AuctionStatus status = AuctionStatus.SCHEDULED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private User winner;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "auction", cascade = CascadeType.ALL)
    @OrderBy("createdAt DESC")
    @Builder.Default
    private List<Bid> bids = new ArrayList<>();

    @OneToMany(mappedBy = "auction", cascade = CascadeType.ALL)
    @Builder.Default
    private List<ProxyBid> proxyBids = new ArrayList<>();
}
