package com.marketplace.integration;

import com.marketplace.entity.*;
import com.marketplace.enums.*;
import com.marketplace.event.BidPlacedEvent;
import com.marketplace.event.OrderShippedEvent;
import com.marketplace.repository.*;
import com.marketplace.service.BidService;
import com.marketplace.service.OrderService;
import com.marketplace.config.RabbitMQConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
public class NotificationIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private BidService bidService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private EscrowWalletRepository walletRepository;

    @Autowired
    private EscrowTransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    private User buyer;
    private User seller;
    private Item item;
    private Order order;

    @BeforeEach
    void setUp() {
        // Clean up
        transactionRepository.deleteAll();
        orderRepository.deleteAll();
        itemRepository.deleteAll();
        userRepository.deleteAll();
        walletRepository.deleteAll();

        // Create Users
        buyer = User.builder()
                .username("buyer")
                .email("buyer@example.com")
                .passwordHash(passwordEncoder.encode("password"))
                .role(UserRole.BUYER)
                .build();
        userRepository.save(buyer);

        seller = User.builder()
                .username("seller")
                .email("seller@example.com")
                .passwordHash(passwordEncoder.encode("password"))
                .role(UserRole.SELLER)
                .build();
        userRepository.save(seller);

        // Create Item
        item = Item.builder()
                .seller(seller)
                .title("Test Item")
                .description("Test Desc")
                .startingBid(BigDecimal.TEN)
                .category("Test") // Added category
                .saleType(com.marketplace.enums.SaleType.FIXED_PRICE) // Added saleType
                .currentPrice(BigDecimal.TEN) // Added currentPrice
                .status(ItemStatus.ACTIVE) // Added status
                .build();
        itemRepository.save(item);
    }

    @Test
    void testOrderShippedEvent() {
        // Setup: Create PAID order
        transactionTemplate.execute(status -> {
            Order o = Order.builder()
                    .buyer(buyer)
                    .seller(seller)
                    .item(item)
                    .amount(BigDecimal.TEN)
                    .status(OrderStatus.PAID)
                    .orderType(OrderType.BUY_NOW)
                    .shippingAddress("123 Test St") // Added shippingAddress
                    .build();
            order = orderRepository.save(o);

            // Create Wallets
            EscrowWallet buyerWallet = EscrowWallet.builder().user(buyer).balance(BigDecimal.ZERO).build();
            walletRepository.save(buyerWallet);

            EscrowWallet platformWallet = EscrowWallet.builder().isPlatformWallet(true).balance(BigDecimal.ZERO)
                    .build();
            walletRepository.save(platformWallet);

            // Need EscrowTransaction for shipOrder to work (it updates deadline)
            EscrowTransaction tx = EscrowTransaction.builder()
                    .order(o)
                    .amount(BigDecimal.TEN)
                    .status(EscrowStatus.HELD)
                    .debitWallet(buyerWallet)
                    .creditWallet(platformWallet)
                    .build();
            transactionRepository.save(tx);
            return null;
        });

        // Act: Ship Order (in a new transaction)
        // orderService.shipOrder is transactional. When it commits, event should fire.
        orderService.shipOrder(order.getId(), "TRACK123", "UPS");

        // Assert: Verify RabbitTemplate called
        // Since event listener is async/after-commit, we stick to verify with timeout?
        // Or standard verify if it runs in same thread but after commit.
        // Default Spring ApplicationEventMulticaster is synchronous, but Transactional
        // outcomes might handle distinct boundaries.
        // Usually safe to just verify.

        verify(rabbitTemplate, timeout(1000).times(1))
                .convertAndSend(eq(RabbitMQConfig.EXCHANGE_NAME), eq("marketplace.event.order.shipped"),
                        any(OrderShippedEvent.class));
    }
}
