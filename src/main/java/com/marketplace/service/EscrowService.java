package com.marketplace.service;

import com.marketplace.entity.EscrowTransaction;
import com.marketplace.entity.EscrowWallet;
import com.marketplace.entity.Order;
import com.marketplace.entity.User;
import com.marketplace.enums.EscrowStatus;
import com.marketplace.enums.OrderStatus;
import com.marketplace.repository.EscrowTransactionRepository;
import com.marketplace.repository.EscrowWalletRepository;
import com.marketplace.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.marketplace.event.OrderPaidEvent;
import com.marketplace.event.EscrowReleasedEvent;
import com.marketplace.event.DisputeOpenedEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EscrowService {

    private final EscrowWalletRepository walletRepository;
    private final EscrowTransactionRepository transactionRepository;
    private final OrderRepository orderRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    private static final BigDecimal PLATFORM_FEE_PERCENTAGE = new BigDecimal("0.05"); // 5%

    @Transactional
    public EscrowWallet getOrCreateWallet(User user) {
        return walletRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    EscrowWallet wallet = EscrowWallet.builder()
                            .user(user)
                            .balance(BigDecimal.ZERO)
                            .build();
                    return walletRepository.save(wallet);
                });
    }

    @Transactional
    public EscrowWallet getPlatformWallet() {
        return walletRepository.findByIsPlatformWalletTrue()
                .orElseGet(() -> {
                    EscrowWallet wallet = EscrowWallet.builder()
                            .isPlatformWallet(true)
                            .balance(BigDecimal.ZERO)
                            .build();
                    return walletRepository.save(wallet);
                });
    }

    /**
     * Buyer pays for an order. Funds move from Buyer Wallet -> Escrow (Platform
     * Wallet temporarily or Virtual Hold).
     * For double-entry, we effectively move funds from "Buyer Wallet" to "Escrow
     * Wallet" (transaction linked to order).
     * In this implementation, we'll use a specific "Escrow Lock" state or move to
     * Platform Wallet with a reference.
     * Let's move to Platform Wallet but tag the transaction as ESCROW_HOLD.
     */
    @Transactional
    public void processPayment(Order order, BigDecimal paymentAmount, String paymentMethod, String externalTxId) {
        // 1. Idempotency & State Validation
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException(
                    "Order is not in PENDING_PAYMENT state. Current status: " + order.getStatus());
        }

        // 2. Amount Validation
        if (paymentAmount.compareTo(order.getAmount()) != 0) {
            throw new IllegalArgumentException(
                    "Payment amount mismatch. Expected: " + order.getAmount() + ", Got: " + paymentAmount);
        }

        // 3. Check for existing successful transaction (Double submission check)
        if (transactionRepository.findByOrderId(order.getId()).isPresent()) {
            throw new IllegalStateException("Transaction already exists for this order.");
        }

        User buyer = order.getBuyer();
        EscrowWallet buyerWallet = getOrCreateWallet(buyer);
        EscrowWallet platformWallet = getPlatformWallet();

        // 4. Simulate External Payment / Top-up Logic
        log.info("Processing payment for Order {}. Amount: {}, TxId: {}", order.getId(), paymentAmount, externalTxId);

        // Credit Buyer Wallet first (Simulating deposit)
        buyerWallet.setBalance(buyerWallet.getBalance().add(paymentAmount));
        walletRepository.save(buyerWallet);

        // 5. Move Funds to Escrow (Platform Wallet)
        buyerWallet.setBalance(buyerWallet.getBalance().subtract(paymentAmount));
        platformWallet.setBalance(platformWallet.getBalance().add(paymentAmount));

        walletRepository.save(buyerWallet);
        walletRepository.save(platformWallet);

        // 6. Create Escrow Transaction Record
        EscrowTransaction escrowTx = EscrowTransaction.builder()
                .order(order)
                .debitWallet(buyerWallet)
                .creditWallet(platformWallet)
                .amount(paymentAmount)
                .status(EscrowStatus.HELD)
                .paymentMethod(paymentMethod)
                .transactionId(externalTxId)
                .heldAt(LocalDateTime.now())
                .build();

        transactionRepository.save(escrowTx);

        // 7. Update Order State
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        log.info("Payment successful. Funds held in escrow for Order {}", order.getId());

        eventPublisher.publishEvent(new OrderPaidEvent(
                order.getId(),
                buyer.getId(),
                paymentAmount,
                order.getSeller().getEmail()));
    }

    @Transactional
    public void releaseEscrow(Order order) {
        EscrowTransaction tx = transactionRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new RuntimeException("No escrow transaction found for order"));

        if (tx.getStatus() != EscrowStatus.HELD) {
            throw new RuntimeException("Escrow funds not in HELD state");
        }

        EscrowWallet platformWallet = getPlatformWallet();
        EscrowWallet sellerWallet = getOrCreateWallet(order.getSeller());

        BigDecimal totalAmount = tx.getAmount();
        BigDecimal platformFee = totalAmount.multiply(PLATFORM_FEE_PERCENTAGE);
        BigDecimal sellerAmount = totalAmount.subtract(platformFee);

        // 1. Debit Platform (Full Amount)
        platformWallet.setBalance(platformWallet.getBalance().subtract(totalAmount));

        // 2. Credit Seller (Net Amount)
        sellerWallet.setBalance(sellerWallet.getBalance().add(sellerAmount));

        // 3. Credit Platform (Fee) - technically it stays in platform wallet, but
        // accounting-wise we should track it.
        // For simplicity, we just put the fee back into platform wallet (or separate
        // Revenue Wallet).
        platformWallet.setBalance(platformWallet.getBalance().add(platformFee));

        walletRepository.save(platformWallet);
        walletRepository.save(sellerWallet);

        // 4. Update Transaction
        tx.setStatus(EscrowStatus.RELEASED);
        tx.setReleasedAt(LocalDateTime.now());
        transactionRepository.save(tx);

        log.info("Escrow released for Order {}. Seller credited: {}, Fee: {}", order.getId(), sellerAmount,
                platformFee);

        // 5. Update Order status if needed (e.g., COMPLETED)
        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);

        eventPublisher.publishEvent(new EscrowReleasedEvent(
                order.getId(),
                order.getSeller().getId(),
                sellerAmount,
                order.getSeller().getEmail()));
    }

    @Transactional
    public void refundBuyer(Order order) {
        EscrowTransaction tx = transactionRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new RuntimeException("No escrow transaction found for order"));

        if (tx.getStatus() != EscrowStatus.HELD && tx.getStatus() != EscrowStatus.DISPUTED) {
            throw new RuntimeException("Cannot refund from current state: " + tx.getStatus());
        }

        EscrowWallet platformWallet = getPlatformWallet();
        EscrowWallet buyerWallet = getOrCreateWallet(order.getBuyer());

        // Move funds back
        platformWallet.setBalance(platformWallet.getBalance().subtract(tx.getAmount()));
        buyerWallet.setBalance(buyerWallet.getBalance().add(tx.getAmount()));

        walletRepository.save(platformWallet);
        walletRepository.save(buyerWallet);

        tx.setStatus(EscrowStatus.REFUNDED);
        transactionRepository.save(tx);

        order.setStatus(OrderStatus.REFUNDED);
        orderRepository.save(order);

        log.info("Order {} refunded to buyer.", order.getId());
    }

    @Transactional
    public void disputeTransaction(Order order) {
        EscrowTransaction tx = transactionRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new RuntimeException("No escrow transaction found"));

        if (tx.getStatus() != EscrowStatus.HELD) {
            throw new RuntimeException("Transaction must be HELD to dispute");
        }

        tx.setStatus(EscrowStatus.DISPUTED);
        transactionRepository.save(tx);

        order.setStatus(OrderStatus.DISPUTED);
        orderRepository.save(order);

        log.info("Order {} marked as DISPUTED. Funds frozen.", order.getId());

        eventPublisher.publishEvent(new DisputeOpenedEvent(
                order.getId(),
                "Dispute opened",
                "admin@marketplace.com"));
    }
}
