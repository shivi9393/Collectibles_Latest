package com.marketplace.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DisputeOpenedEvent extends DomainEvent {
    private Long orderId;
    private String reason;
    private String adminEmail; // Notify admin (optional) or just log
}
