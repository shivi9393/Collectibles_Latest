package com.marketplace.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BidPlacedEvent extends DomainEvent {
    private Long auctionId;
    private Long bidderId;
    private BigDecimal amount;
    private String bidderEmail; // Helpful for notification
}
