package com.marketplace.repository;

import com.marketplace.entity.Item;
import com.marketplace.entity.User;
import com.marketplace.enums.ItemStatus;
import com.marketplace.enums.SaleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
        List<Item> findByStatus(ItemStatus status);

        org.springframework.data.domain.Page<Item> findByStatus(ItemStatus status,
                        org.springframework.data.domain.Pageable pageable);

        Long countByStatus(ItemStatus status);

        List<Item> findBySellerId(Long sellerId);

        List<Item> findBySeller(User seller);

        List<Item> findByCategory(String category);

        List<Item> findBySaleType(SaleType saleType);

        @Query("SELECT i FROM Item i WHERE i.status = :status AND " +
                        "(:keyword IS NULL OR LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(i.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND "
                        +
                        "(:category IS NULL OR i.category = :category) AND " +
                        "(:minPrice IS NULL OR i.currentPrice >= :minPrice) AND " +
                        "(:maxPrice IS NULL OR i.currentPrice <= :maxPrice) AND " +
                        "(:saleType IS NULL OR i.saleType = :saleType) AND " +
                        "(:verifiedOnly = false OR i.isVerified = true)")
        org.springframework.data.domain.Page<Item> searchItems(@Param("status") ItemStatus status,
                        @Param("keyword") String keyword,
                        @Param("category") String category,
                        @Param("minPrice") BigDecimal minPrice,
                        @Param("maxPrice") BigDecimal maxPrice,
                        @Param("saleType") SaleType saleType,
                        @Param("verifiedOnly") Boolean verifiedOnly,
                        org.springframework.data.domain.Pageable pageable);

        @Query("SELECT DISTINCT i.category FROM Item i WHERE i.status = 'ACTIVE' ORDER BY i.category")
        List<String> findDistinctCategories();
}
