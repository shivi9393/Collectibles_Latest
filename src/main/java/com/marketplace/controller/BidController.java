package com.marketplace.controller;

import com.marketplace.entity.Bid;
import com.marketplace.repository.BidRepository;
import com.marketplace.repository.UserRepository;
import com.marketplace.service.BidService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> placeBid(
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long itemId = Long.valueOf(payload.get("itemId").toString());
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());

            BigDecimal maxProxyAmount = null;
            if (payload.containsKey("maxProxyAmount") && payload.get("maxProxyAmount") != null) {
                maxProxyAmount = new BigDecimal(payload.get("maxProxyAmount").toString());
            }

            Bid bid = bidService.placeBid(itemId, amount, maxProxyAmount, userDetails.getUsername());
            return ResponseEntity.ok(bid);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/auction/{auctionId}")
    public ResponseEntity<?> getBidsByAuction(@PathVariable Long auctionId) {
        return ResponseEntity.ok(bidRepository.findByAuctionIdOrderByAmountDesc(auctionId));
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyBids(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            com.marketplace.entity.User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return ResponseEntity.ok(bidRepository.findByBidderId(user.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
