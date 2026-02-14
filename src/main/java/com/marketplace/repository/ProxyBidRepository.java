package com.marketplace.repository;

import com.marketplace.entity.ProxyBid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProxyBidRepository extends JpaRepository<ProxyBid, Long> {
    Optional<ProxyBid> findByAuctionIdAndBidderIdAndIsActiveTrue(Long auctionId, Long bidderId);

    Optional<ProxyBid> findByAuctionIdAndBidderId(Long auctionId, Long bidderId);

    List<ProxyBid> findByAuctionIdAndIsActiveTrue(Long auctionId);
}
