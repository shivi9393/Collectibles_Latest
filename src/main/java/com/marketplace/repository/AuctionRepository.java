package com.marketplace.repository;

import com.marketplace.entity.Auction;
import com.marketplace.enums.AuctionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {
    Optional<Auction> findByItemId(Long itemId);

    List<Auction> findByStatus(AuctionStatus status);

    @Query("SELECT a FROM Auction a WHERE a.status = 'ACTIVE' AND a.endTime < :now")
    List<Auction> findExpiredAuctions(@Param("now") LocalDateTime now);

    @Query("SELECT a FROM Auction a WHERE a.status = 'ACTIVE' ORDER BY a.endTime ASC")
    List<Auction> findActiveAuctionsEndingSoon();
}
