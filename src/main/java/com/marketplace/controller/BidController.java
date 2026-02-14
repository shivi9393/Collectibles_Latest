package com.marketplace.controller;

import com.marketplace.entity.Bid;
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
}
