package com.marketplace.entity;

import com.marketplace.enums.ItemStatus;
import com.marketplace.enums.SaleType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(name = "year_era", length = 50)
    private String yearEra;

    @Column(name = "condition_description", columnDefinition = "TEXT")
    private String conditionDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_type", nullable = false)
    private SaleType saleType;

    @Column(name = "fixed_price", precision = 10, scale = 2)
    private BigDecimal fixedPrice;

    @Column(name = "starting_bid", precision = 10, scale = 2)
    private BigDecimal startingBid;

    @Column(name = "current_price", precision = 10, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "buy_now_price", precision = 10, scale = 2)
    private BigDecimal buyNowPrice;

    @Column
    @Builder.Default
    private Integer quantity = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ItemStatus status = ItemStatus.DRAFT;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "watch_count")
    @Builder.Default
    private Integer watchCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItemImage> images = new ArrayList<>();

    @OneToOne(mappedBy = "item", cascade = CascadeType.ALL)
    private Auction auction;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Certificate> certificates = new ArrayList<>();

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Watchlist> watchers = new ArrayList<>();
}
