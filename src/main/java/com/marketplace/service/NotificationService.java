package com.marketplace.service;

import com.marketplace.entity.Notification;
import com.marketplace.entity.User;
import com.marketplace.event.*;
import com.marketplace.repository.NotificationRepository;
import com.marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final JavaMailSender javaMailSender;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void processEvent(DomainEvent event) {
        if (event instanceof BidPlacedEvent) {
            handleBidPlaced((BidPlacedEvent) event);
        } else if (event instanceof AuctionWonEvent) {
            handleAuctionWon((AuctionWonEvent) event);
        } else if (event instanceof OrderPaidEvent) {
            handleOrderPaid((OrderPaidEvent) event);
        } else if (event instanceof OrderShippedEvent) {
            handleOrderShipped((OrderShippedEvent) event);
        } else if (event instanceof OrderDeliveredEvent) {
            handleOrderDelivered((OrderDeliveredEvent) event);
        } else if (event instanceof EscrowReleasedEvent) {
            handleEscrowReleased((EscrowReleasedEvent) event);
        } else if (event instanceof DisputeOpenedEvent) {
            handleDisputeOpened((DisputeOpenedEvent) event);
        } else {
            log.warn("Unknown event type: {}", event.getClass().getSimpleName());
        }
    }

    private void handleBidPlaced(BidPlacedEvent event) {
        // Notify bidder (confirmation) - Optional, maybe too noisy
        // Notify seller? "New bid on your item"
        // For now, let's just log or maybe notify the bidder they are leading
        log.info("Processing BidPlacedEvent for auction {}", event.getAuctionId());
    }

    private void handleAuctionWon(AuctionWonEvent event) {
        String message = "Congratulations! You won the auction for " + event.getItemTitle() + " for $"
                + event.getWinningAmount();
        createNotification(event.getWinnerId(), "Auction Won", message, "AUCTION_WON", event.getAuctionId(), null);
        sendEmail(event.getWinnerEmail(), "You Won!", message);
    }

    private void handleOrderPaid(OrderPaidEvent event) {
        String message = "Your order #" + event.getOrderId() + " has been paid. Please ship the item.";
        // Notify Seller
        // We need seller ID, but event has sellerEmail. We should fetch User by email
        // or add sellerId to event.
        // Event has sellerEmail.
        User seller = userRepository.findByEmail(event.getSellerEmail()).orElse(null);
        if (seller != null) {
            createNotification(seller.getId(), "Order Paid", message, "ORDER_PAID", null, event.getOrderId());
            sendEmail(seller.getEmail(), "Order Paid", message);
        }
    }

    private void handleOrderShipped(OrderShippedEvent event) {
        String message = "Your order #" + event.getOrderId() + " has been shipped. Tracking: "
                + event.getTrackingNumber();
        User buyer = userRepository.findByEmail(event.getBuyerEmail()).orElse(null);
        if (buyer != null) {
            createNotification(buyer.getId(), "Order Shipped", message, "ORDER_SHIPPED", null, event.getOrderId());
            sendEmail(buyer.getEmail(), "Order Shipped", message);
        }
    }

    private void handleOrderDelivered(OrderDeliveredEvent event) {
        String message = "Order #" + event.getOrderId() + " was delivered. Funds will be released shortly.";
        User seller = userRepository.findByEmail(event.getSellerEmail()).orElse(null);
        if (seller != null) {
            createNotification(seller.getId(), "Order Delivered", message, "ORDER_DELIVERED", null, event.getOrderId());
            sendEmail(seller.getEmail(), "Order Delivered", message);
        }
    }

    private void handleEscrowReleased(EscrowReleasedEvent event) {
        String message = "Escrow released! $" + event.getAmountReleased() + " has been credited to your wallet.";
        createNotification(event.getSellerId(), "Funds Released", message, "ESCROW_RELEASED", null, event.getOrderId());
        sendEmail(event.getSellerEmail(), "Funds Released", message);
    }

    private void handleDisputeOpened(DisputeOpenedEvent event) {
        // Notify Admin or just log
        log.warn("Dispute opened for order {}: {}", event.getOrderId(), event.getReason());
    }

    private void createNotification(Long userId, String title, String message, String type, Long auctionId,
            Long orderId) {
        User user = userRepository.findById(userId).orElseThrow();
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .relatedAuctionId(auctionId)
                .relatedOrderId(orderId)
                .isRead(false)
                .build();
        notificationRepository.save(notification);

        // Push WebSocket
        messagingTemplate.convertAndSendToUser(user.getUsername(), "/queue/notifications", notification);
    }

    private void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            javaMailSender.send(message);
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}", to, e);
        }
    }
}
