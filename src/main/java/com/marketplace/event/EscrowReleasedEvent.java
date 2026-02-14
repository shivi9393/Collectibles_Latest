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
public class EscrowReleasedEvent extends DomainEvent {
    private Long orderId;
    private Long sellerId;
    private BigDecimal amountReleased;
    private String sellerEmail;
}
