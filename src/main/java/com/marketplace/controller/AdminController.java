package com.marketplace.controller;

import com.marketplace.entity.User;
import com.marketplace.enums.FraudReportStatus;
import com.marketplace.repository.UserRepository;
import com.marketplace.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin management APIs")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;
    private final UserRepository userRepository;

    // ─── Dashboard ───────────────────────────────────────────────────

    @GetMapping("/dashboard")
    @Operation(summary = "Admin dashboard stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    // ─── User Management ─────────────────────────────────────────────

    @GetMapping("/users")
    @Operation(summary = "List all users (paginated)")
    public ResponseEntity<?> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getUsers(page, size));
    }

    @PutMapping("/users/{id}/ban")
    @Operation(summary = "Ban (freeze) a user")
    public ResponseEntity<Void> banUser(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        User admin = getAdminUser(userDetails);
        String reason = body.getOrDefault("reason", "No reason provided");
        adminService.banUser(id, admin, reason);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{id}/unban")
    @Operation(summary = "Unban (unfreeze) a user")
    public ResponseEntity<Void> unbanUser(
            @PathVariable Long id,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        User admin = getAdminUser(userDetails);
        adminService.unbanUser(id, admin);
        return ResponseEntity.ok().build();
    }

    // ─── Listing Approval ────────────────────────────────────────────

    @GetMapping("/items/pending")
    @Operation(summary = "List items pending approval")
    public ResponseEntity<?> getPendingItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getPendingItems(page, size));
    }

    @PutMapping("/items/{id}/approve")
    @Operation(summary = "Approve an item listing")
    public ResponseEntity<Void> approveItem(
            @PathVariable Long id,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        User admin = getAdminUser(userDetails);
        adminService.approveItem(id, admin);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/items/{id}/reject")
    @Operation(summary = "Reject an item listing")
    public ResponseEntity<Void> rejectItem(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        User admin = getAdminUser(userDetails);
        String reason = body.getOrDefault("reason", "No reason provided");
        adminService.rejectItem(id, admin, reason);
        return ResponseEntity.ok().build();
    }

    // ─── Fraud Reports ───────────────────────────────────────────────

    @GetMapping("/fraud-reports")
    @Operation(summary = "List fraud reports")
    public ResponseEntity<?> getFraudReports(
            @RequestParam(required = false) FraudReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getFraudReports(status, page, size));
    }

    @PutMapping("/fraud-reports/{id}/resolve")
    @Operation(summary = "Resolve a fraud report")
    public ResponseEntity<Void> resolveFraudReport(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        User admin = getAdminUser(userDetails);
        FraudReportStatus newStatus = FraudReportStatus.valueOf(body.getOrDefault("status", "RESOLVED"));
        String notes = body.getOrDefault("notes", "");
        adminService.resolveFraudReport(id, admin, newStatus, notes);
        return ResponseEntity.ok().build();
    }

    // ─── Audit Logs ──────────────────────────────────────────────────

    @GetMapping("/audit-logs")
    @Operation(summary = "View audit logs")
    public ResponseEntity<?> getAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAuditLogs(entityType, page, size));
    }

    // ─── Helper ──────────────────────────────────────────────────────

    private User getAdminUser(org.springframework.security.core.userdetails.UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Admin user not found"));
    }
}
