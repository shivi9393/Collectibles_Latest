package com.marketplace.scheduler;

import com.marketplace.entity.Order;
import com.marketplace.enums.OrderStatus;
import com.marketplace.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentExpiryScheduler {

    private final OrderRepository orderRepository;

    @Scheduled(fixedRate = 60000) // Every minute
    @Transactional
    public void cancelUnpaidOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusHours(24); // 24 hour window
        List<Order> unpaidOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING_PAYMENT,
                deadline);

        for (Order order : unpaidOrders) {
            log.info("Cancelling unpaid order {}", order.getId());
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            // Future: Notify seller / Offer to next bidder
        }
    }
}
