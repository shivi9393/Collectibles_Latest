package com.marketplace.repository;

import com.marketplace.entity.Order;
import com.marketplace.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("SELECT o FROM Order o WHERE o.buyer.id = :userId")
    List<Order> findByBuyerId(@Param("userId") Long userId);

    @Query("SELECT o FROM Order o WHERE o.seller.id = :userId")
    List<Order> findBySellerId(@Param("userId") Long userId);

    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime dateTime);
}
