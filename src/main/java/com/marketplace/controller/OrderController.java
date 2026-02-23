package com.marketplace.controller;

import com.marketplace.entity.Order;
import com.marketplace.repository.OrderRepository;
import com.marketplace.repository.UserRepository;
import com.marketplace.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long itemId = Long.valueOf(payload.get("itemId").toString());
            Order order = orderService.createOrder(itemId, userDetails.getUsername());
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyOrders(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            com.marketplace.entity.User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            var purchases = orderRepository.findByBuyerId(user.getId());
            var sales = orderRepository.findBySellerId(user.getId());
            return ResponseEntity.ok(Map.of("purchases", purchases, "sales", sales));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Order order = orderRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Order not found"));
            // Verify the user is buyer or seller
            com.marketplace.entity.User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            if (!order.getBuyer().getId().equals(user.getId()) && !order.getSeller().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<?> payOrder(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        String paymentMethod = (String) payload.get("paymentMethod");
        String txId = (String) payload.get("txId");

        // Validate amount presence
        if (!payload.containsKey("amount")) {
            return ResponseEntity.badRequest().body("Amount is required");
        }

        java.math.BigDecimal amount = new java.math.BigDecimal(payload.get("amount").toString());

        orderService.processPayment(id, amount, paymentMethod, txId);
        return ResponseEntity.ok("Payment successful");
    }

    @PostMapping("/{id}/ship")
    public ResponseEntity<?> shipOrder(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        orderService.shipOrder(id, payload.get("trackingNumber"), payload.get("carrier"));
        return ResponseEntity.ok("Order shipped");
    }

    @PostMapping("/{id}/deliver")
    public ResponseEntity<?> confirmDelivery(@PathVariable Long id) {
        orderService.confirmDelivery(id);
        return ResponseEntity.ok("Delivery confirmed");
    }

    @PostMapping("/{id}/dispute")
    public ResponseEntity<?> reportDispute(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        orderService.reportDispute(id, payload.get("reason"));
        return ResponseEntity.ok("Dispute reported");
    }

    @PostMapping("/{id}/admin/lost")
    public ResponseEntity<?> markAsLost(@PathVariable Long id, @RequestBody Map<String, Boolean> payload) {
        // In real app, check for Admin role here or via Security Config
        Boolean refundBuyer = payload.get("refundBuyer");
        if (refundBuyer == null) {
            return ResponseEntity.badRequest().body("refundBuyer boolean is required");
        }
        orderService.markAsLost(id, refundBuyer);
        return ResponseEntity.ok("Order marked as lost and resolved");
    }
}
