package com.marketplace.event;

import com.marketplace.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMQProducer {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBidPlaced(BidPlacedEvent event) {
        publish(event, "bid.placed");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAuctionWon(AuctionWonEvent event) {
        publish(event, "auction.won");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderPaid(OrderPaidEvent event) {
        publish(event, "order.paid");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderShipped(OrderShippedEvent event) {
        publish(event, "order.shipped");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderDelivered(OrderDeliveredEvent event) {
        publish(event, "order.delivered");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEscrowReleased(EscrowReleasedEvent event) {
        publish(event, "escrow.released");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDisputeOpened(DisputeOpenedEvent event) {
        publish(event, "dispute.opened");
    }

    private void publish(DomainEvent event, String routingKeySuffix) {
        String routingKey = "marketplace.event." + routingKeySuffix;
        log.info("Publishing event {} to RabbitMQ with key {}", event.getClass().getSimpleName(), routingKey);
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, routingKey, event);
        } catch (Exception e) {
            log.error("Failed to publish event to RabbitMQ", e);
            // In a real system, we might save to a 'failed_events' table here for retry
        }
    }
}
