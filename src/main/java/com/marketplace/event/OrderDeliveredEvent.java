package com.marketplace.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDeliveredEvent extends DomainEvent {
    private Long orderId;
    private String sellerEmail; // Notify seller that funds will release
}
