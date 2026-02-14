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
public class AuctionWonEvent extends DomainEvent {
    private Long auctionId;
    private Long winnerId;
    private BigDecimal winningAmount;
    private String winnerEmail;
    private String itemTitle;
}
