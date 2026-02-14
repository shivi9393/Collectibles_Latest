package com.marketplace.service;

import com.marketplace.entity.*;
import com.marketplace.enums.EscrowStatus;
import com.marketplace.enums.OrderStatus;
import com.marketplace.repository.EscrowTransactionRepository;
import com.marketplace.repository.OrderRepository;
import com.marketplace.repository.ShippingInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private EscrowService escrowService;
    @Mock
    private ShippingInfoRepository shippingInfoRepository;
    @Mock
    private EscrowTransactionRepository escrowTransactionRepository;

    @InjectMocks
    private OrderService orderService;

    private Order order;
    private EscrowTransaction escrowTx;

    @BeforeEach
    void setUp() {
        order = Order.builder()
                .id(1L)
                .status(OrderStatus.PENDING_PAYMENT)
                .build();

        escrowTx = EscrowTransaction.builder()
                .id(100L)
                .order(order)
                .status(EscrowStatus.HELD)
                .build();
    }

    @Test
    void processPayment_CallsEscrowService() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        BigDecimal amount = new BigDecimal("100.00");
        orderService.processPayment(1L, amount, "CARD", "tx_123");

        verify(escrowService).processPayment(order, amount, "CARD", "tx_123");
    }

    @Test
    void shipOrder_UpdatesStatusAndSetsDeadline() {
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(escrowTransactionRepository.findByOrderId(1L)).thenReturn(Optional.of(escrowTx));

        orderService.shipOrder(1L, "TRACK123", "UPS");

        verify(shippingInfoRepository).save(any(ShippingInfo.class));
        assertEquals(OrderStatus.SHIPPED, order.getStatus());
        verify(escrowTransactionRepository).save(escrowTx);
        assertNotNull(escrowTx.getEscrowReleaseDeadline()); // Deadline set
    }

    @Test
    void confirmDelivery_ReleaseEscrow() {
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.confirmDelivery(1L);

        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        verify(escrowService).releaseEscrow(order);
    }
}
