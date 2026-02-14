package com.marketplace.integration;

import com.marketplace.entity.*;
import com.marketplace.enums.EscrowStatus;
import com.marketplace.enums.OrderStatus;
import com.marketplace.repository.*;
import com.marketplace.service.EscrowService;
import com.marketplace.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ShippingIntegrationTest {

    @Autowired
    private OrderService orderService;
    @Autowired
    private EscrowService escrowService;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private EscrowWalletRepository walletRepository;
    @Autowired
    private EscrowTransactionRepository transactionRepository;

    private User buyer;
    private User seller;
    private Item item;
    private Order order;

    @BeforeEach
    void setUp() {
        buyer = User.builder().username("buyer").email("buyer@ship.com").passwordHash("pw").build();
        userRepository.save(buyer);

        seller = User.builder().username("seller").email("seller@ship.com").passwordHash("pw").build();
        userRepository.save(seller);

        item = Item.builder()
                .seller(seller)
                .title("Shipping Item")
                .description("Test")
                .category("Collectibles")
                .saleType(com.marketplace.enums.SaleType.FIXED_PRICE) // Added required field
                .startingBid(new BigDecimal("50.00"))
                .currentPrice(new BigDecimal("50.00")) // Buy Now Price essentially
                .status(com.marketplace.enums.ItemStatus.ACTIVE)
                .build();
        itemRepository.save(item);
    }

    @Test
    void fullShippingLifecycle() {
        // 1. Create Order (Simulate Buy Now or Auction Win)
        order = orderService.createOrder(item.getId(), "buyer@ship.com");
        assertNotNull(order.getId());
        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());

        // 2. Pay
        orderService.processPayment(order.getId(), new BigDecimal("50.00"), "CARD", "tx_ship_1");

        Order paidOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.PAID, paidOrder.getStatus());

        // check wallet balance
        EscrowWallet platformWallet = walletRepository.findByIsPlatformWalletTrue().orElseThrow();
        // Since database isn't reset between tests perfectly in simple spring boot
        // tests without @DirtiesContext,
        // we check if it has AT LEAST 50. But in @Transactional test it should roll
        // back.
        // Let's assert strictly relevant to this transaction?
        // Best to just check the transaction record exists and is HELD.
        EscrowTransaction tx = transactionRepository.findByOrderId(order.getId()).orElseThrow();
        assertEquals(EscrowStatus.HELD, tx.getStatus());

        // 3. Shim (Ship)
        orderService.shipOrder(order.getId(), "TRACK999", "FEDEX");

        Order shippedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.SHIPPED, shippedOrder.getStatus());

        // Verify deadline set
        tx = transactionRepository.findByOrderId(order.getId()).orElseThrow();
        assertNotNull(tx.getEscrowReleaseDeadline());
        assertTrue(tx.getEscrowReleaseDeadline().isAfter(LocalDateTime.now()));

        // 4. Confirm Delivery
        orderService.confirmDelivery(order.getId());

        Order deliveredOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.COMPLETED, deliveredOrder.getStatus()); // releaseEscrow sets it to COMPLETED

        tx = transactionRepository.findByOrderId(order.getId()).orElseThrow();
        assertEquals(EscrowStatus.RELEASED, tx.getStatus());

        // Verify Seller Wallet Credited (50 - 5% = 47.50)
        EscrowWallet sellerWallet = walletRepository.findByUserId(seller.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("47.50").compareTo(sellerWallet.getBalance()));
    }

    @Test
    void adminLostPackageflow() {
        // 1. Create and Pay
        order = orderService.createOrder(item.getId(), "buyer@ship.com");
        orderService.processPayment(order.getId(), new BigDecimal("50.00"), "CARD", "tx_lost_1");

        // 2. Ship
        orderService.shipOrder(order.getId(), "TRACK_LOST", "USPS");

        // 3. Mark As Lost (Refund Buyer)
        orderService.markAsLost(order.getId(), true);

        Order lostOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.REFUNDED, lostOrder.getStatus());

        EscrowTransaction tx = transactionRepository.findByOrderId(order.getId()).orElseThrow();
        assertEquals(EscrowStatus.REFUNDED, tx.getStatus());

        // Buyer Refunded
        EscrowWallet buyerWallet = walletRepository.findByUserId(buyer.getId()).orElseThrow();
        // 0 start + 50 deposit - 50 pay + 50 refund = 50.
        // Wait, processPayment adds 50 then subtracts 50. So balance is 0. Then refund
        // adds 50. So 50.
        // Actually our processPayment impl:
        // buyerWallet.setBalance(buyerWallet.getBalance().add(order.getAmount()));
        // buyerWallet.setBalance(buyerWallet.getBalance().subtract(order.getAmount()));
        // So net 0.
        // refundBuyer:
        // buyerWallet.setBalance(buyerWallet.getBalance().add(tx.getAmount()));
        assertEquals(0, new BigDecimal("50.00").compareTo(buyerWallet.getBalance()));
    }
}
