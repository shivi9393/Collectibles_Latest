package com.marketplace.service;

import com.marketplace.entity.*;
import com.marketplace.enums.EscrowStatus;
import com.marketplace.enums.OrderStatus;
import com.marketplace.repository.EscrowTransactionRepository;
import com.marketplace.repository.EscrowWalletRepository;
import com.marketplace.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EscrowServiceTest {

    @Mock
    private EscrowWalletRepository walletRepository;
    @Mock
    private EscrowTransactionRepository transactionRepository;
    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private EscrowService escrowService;

    private User buyer;
    private User seller;
    private Order order;
    private EscrowWallet buyerWallet;
    private EscrowWallet sellerWallet;
    private EscrowWallet platformWallet;

    @BeforeEach
    void setUp() {
        buyer = User.builder().id(1L).email("buyer@test.com").build();
        seller = User.builder().id(2L).email("seller@test.com").build();

        order = Order.builder()
                .id(1L)
                .buyer(buyer)
                .seller(seller)
                .amount(new BigDecimal("100.00"))
                .status(OrderStatus.PENDING_PAYMENT)
                .build();

        buyerWallet = EscrowWallet.builder().id(1L).user(buyer).balance(BigDecimal.ZERO).build();
        sellerWallet = EscrowWallet.builder().id(2L).user(seller).balance(BigDecimal.ZERO).build();
        platformWallet = EscrowWallet.builder().id(3L).isPlatformWallet(true).balance(BigDecimal.ZERO).build();
    }

    @Test
    void processPayment_Success() {
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(buyerWallet));
        when(walletRepository.findByIsPlatformWalletTrue()).thenReturn(Optional.of(platformWallet));
        when(transactionRepository.save(any(EscrowTransaction.class))).thenAnswer(i -> i.getArgument(0));

        // Correct amount passed
        escrowService.processPayment(order, new BigDecimal("100.00"), "CREDIT_CARD", "tx_123");

        assertEquals(0, new BigDecimal("100.00").compareTo(platformWallet.getBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(buyerWallet.getBalance()));
        assertEquals(OrderStatus.PAID, order.getStatus());

        verify(transactionRepository).save(any(EscrowTransaction.class));
    }

    @Test
    void processPayment_Fails_AmountMismatch() {
        assertThrows(IllegalArgumentException.class,
                () -> escrowService.processPayment(order, new BigDecimal("50.00"), "CREDIT_CARD", "tx_123"));
    }

    @Test
    void processPayment_Fails_WrongStatus() {
        order.setStatus(OrderStatus.PAID);
        assertThrows(IllegalStateException.class,
                () -> escrowService.processPayment(order, new BigDecimal("100.00"), "CREDIT_CARD", "tx_123"));
    }

    @Test
    void releaseEscrow_Success() {
        // Setup initial state: Funds in Platform Wallet, Transaction HELD
        platformWallet.setBalance(new BigDecimal("100.00"));

        EscrowTransaction tx = EscrowTransaction.builder()
                .order(order)
                .amount(new BigDecimal("100.00"))
                .status(EscrowStatus.HELD)
                .build();

        when(transactionRepository.findByOrderId(1L)).thenReturn(Optional.of(tx));
        when(walletRepository.findByIsPlatformWalletTrue()).thenReturn(Optional.of(platformWallet));
        when(walletRepository.findByUserId(2L)).thenReturn(Optional.of(sellerWallet));

        escrowService.releaseEscrow(order);

        // Seller gets 95 (100 - 5% fee)
        assertEquals(0, new BigDecimal("95.00").compareTo(sellerWallet.getBalance()));
        // Platform keeps 5 as fee (starts 100, pays 95, keeps 5)
        assertEquals(0, new BigDecimal("5.00").compareTo(platformWallet.getBalance()));
        assertEquals(EscrowStatus.RELEASED, tx.getStatus());
    }

    @Test
    void refundBuyer_Success() {
        platformWallet.setBalance(new BigDecimal("100.00"));

        EscrowTransaction tx = EscrowTransaction.builder()
                .order(order)
                .amount(new BigDecimal("100.00"))
                .status(EscrowStatus.HELD)
                .build();

        when(transactionRepository.findByOrderId(1L)).thenReturn(Optional.of(tx));
        when(walletRepository.findByIsPlatformWalletTrue()).thenReturn(Optional.of(platformWallet));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(buyerWallet));

        escrowService.refundBuyer(order);

        assertEquals(0, new BigDecimal("100.00").compareTo(buyerWallet.getBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(platformWallet.getBalance()));
        assertEquals(EscrowStatus.REFUNDED, tx.getStatus());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }
}
