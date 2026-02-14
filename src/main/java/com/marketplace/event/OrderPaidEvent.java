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
public class OrderPaidEvent extends DomainEvent {
    private Long orderId;
    private Long buyerId;
    private BigDecimal amount;
    private String sellerEmail; // Notify seller
}
