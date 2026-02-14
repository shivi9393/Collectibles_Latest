package com.marketplace.consumer;

import com.marketplace.config.RabbitMQConfig;
import com.marketplace.event.DomainEvent;
import com.marketplace.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void receive(DomainEvent event) {
        log.info("Received event: {} with ID: {}", event.getClass().getSimpleName(), event.getEventId());
        try {
            notificationService.processEvent(event);
        } catch (Exception e) {
            log.error("Error processing event {}", event.getEventId(), e);
            // In a real system, send to DLQ
        }
    }
}
