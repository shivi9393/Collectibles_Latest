package com.marketplace.service;

import com.marketplace.entity.EscrowTransaction;
import com.marketplace.entity.Order;
import com.marketplace.entity.ShippingInfo;
import com.marketplace.enums.OrderStatus;
import com.marketplace.repository.EscrowTransactionRepository;
import com.marketplace.repository.OrderRepository;
import com.marketplace.repository.ShippingInfoRepository;
import com.marketplace.entity.Item;
import com.marketplace.entity.User;
import com.marketplace.enums.ItemStatus;
import com.marketplace.repository.ItemRepository;
import com.marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.marketplace.event.OrderShippedEvent;
import com.marketplace.event.OrderDeliveredEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final EscrowService escrowService;
    private final ShippingInfoRepository shippingInfoRepository;
    private final EscrowTransactionRepository escrowTransactionRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Transactional
    public Order createOrder(Long itemId, String userEmail) {
        User buyer = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        if (!item.getStatus().equals(ItemStatus.ACTIVE)) {
            throw new RuntimeException("Item is not available for purchase");
        }

        if (item.getSeller().getId().equals(buyer.getId())) {
            throw new RuntimeException("You cannot buy your own item");
        }

        // Mark Item as SOLD
        item.setStatus(ItemStatus.SOLD);
        itemRepository.save(item);

        // Create Order
        Order order = Order.builder()
                .buyer(buyer)
                .seller(item.getSeller())
                .item(item)
                .amount(item.getCurrentPrice())
                .shippingAddress("123 Test St, Test City, TS 99999") // TODO: Fetch from User Profile
                .status(OrderStatus.PENDING_PAYMENT)
                .orderType(com.marketplace.enums.OrderType.BUY_NOW)
                .createdAt(LocalDateTime.now())
                .build();

        return orderRepository.save(order);
    }

    @Transactional
    public void processPayment(Long orderId, BigDecimal amount, String paymentMethod, String externalTxId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        escrowService.processPayment(order, amount, paymentMethod, externalTxId);
    }

    @Transactional
    public void shipOrder(Long orderId, String trackingNumber, String carrier) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() == OrderStatus.DISPUTED) {
            throw new IllegalStateException("Cannot ship a DISPUTED order.");
        }

        if (order.getStatus() != OrderStatus.PAID) {
            throw new RuntimeException("Order must be PAID before shipping");
        }

        ShippingInfo shippingInfo = ShippingInfo.builder()
                .order(order)
                .trackingNumber(trackingNumber)
                .carrier(carrier)
                .shippingAddress(order.getShippingAddress())
                .shippedAt(LocalDateTime.now())
                .build();

        shippingInfoRepository.save(shippingInfo);

        order.setStatus(OrderStatus.SHIPPED);
        orderRepository.save(order);

        // Update Escrow Release Deadline (e.g. 7 days from now)
        EscrowTransaction tx = escrowTransactionRepository.findByOrderId(orderId).orElseThrow();
        tx.setEscrowReleaseDeadline(LocalDateTime.now().plusDays(7));
        escrowTransactionRepository.save(tx);

        log.info("Order {} shipped. Auto-release set for {}", orderId, tx.getEscrowReleaseDeadline());

        eventPublisher.publishEvent(new OrderShippedEvent(
                order.getId(),
                trackingNumber,
                carrier,
                order.getBuyer().getEmail()));
    }

    @Transactional
    public void confirmDelivery(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new IllegalStateException("Order must be SHIPPED to confirm delivery. Current: " + order.getStatus());
        }

        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);

        // Trigger Escrow Release
        escrowService.releaseEscrow(order);

        eventPublisher.publishEvent(new OrderDeliveredEvent(
                order.getId(),
                order.getSeller().getEmail()));
    }

    @Transactional
    public void autoConfirmDelivery(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Strict checks for auto-confirmation
        if (order.getStatus() == OrderStatus.DISPUTED) {
            log.warn("Skipping auto-confirm for DISPUTED order {}", orderId);
            return;
        }

        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.REFUNDED) {
            log.warn("Order {} already finalized. Skipping auto-confirm.", orderId);
            return;
        }

        log.info("AUDIT: System Auto-Confirming Delivery for Order {}", orderId);

        // We set to DELIVERED, then release logic sets to COMPLETED
        if (order.getStatus() != OrderStatus.DELIVERED) {
            order.setStatus(OrderStatus.DELIVERED);
            orderRepository.save(order);
        }

        escrowService.releaseEscrow(order);

        eventPublisher.publishEvent(new OrderDeliveredEvent(
                order.getId(),
                order.getSeller().getEmail()));
    }

    @Transactional
    public void reportDispute(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        escrowService.disputeTransaction(order);
        // Logic to save FraudReport or Dispute entity (simplifying here)
        log.warn("Dispute reported for order {}: {}", orderId, reason);
    }

    @Transactional
    // Admin Override
    public void markAsLost(Long orderId, boolean refundBuyer) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Idempotency: Can only mark lost if NOT already final
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.REFUNDED
                || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot mark as lost. Order is already final: " + order.getStatus());
        }

        log.info("Admin marking Order {} as LOST. Refund Buyer? {}", orderId, refundBuyer);

        if (refundBuyer) {
            escrowService.refundBuyer(order); // Sets status to REFUNDED
        } else {
            // Force release to seller
            escrowService.releaseEscrow(order); // Sets status to COMPLETED
        }
    }
}
