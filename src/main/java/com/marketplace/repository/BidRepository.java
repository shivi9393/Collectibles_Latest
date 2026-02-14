package com.marketplace.repository;

import com.marketplace.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findByAuctionIdOrderByAmountDesc(Long auctionId);

    List<Bid> findByBidderId(Long bidderId);

    List<Bid> findByAuctionIdAndBidderId(Long auctionId, Long bidderId);

    @Query("SELECT b FROM Bid b WHERE b.auction.id = :auctionId ORDER BY b.amount DESC LIMIT 1")
    Optional<Bid> findHighestBid(@Param("auctionId") Long auctionId);

    @Query("SELECT COUNT(DISTINCT b.bidder.id) FROM Bid b WHERE b.auction.id = :auctionId")
    Long countUniqueBidders(@Param("auctionId") Long auctionId);
}
