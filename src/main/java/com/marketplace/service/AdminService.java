package com.marketplace.service;

import com.marketplace.entity.*;
import com.marketplace.enums.FraudReportStatus;
import com.marketplace.enums.ItemStatus;
import com.marketplace.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final AuctionRepository auctionRepository;
    private final FraudReportRepository fraudReportRepository;
    private final AuditLogRepository auditLogRepository;

    // ─── Dashboard Stats ─────────────────────────────────────────────

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("activeAuctions", auctionRepository.countByStatus(com.marketplace.enums.AuctionStatus.ACTIVE));
        stats.put("pendingItems", itemRepository.countByStatus(ItemStatus.PENDING_APPROVAL));
        stats.put("openFraudReports", fraudReportRepository.countByStatus(FraudReportStatus.PENDING));
        stats.put("frozenUsers", userRepository.countByIsFrozenTrue());
        return stats;
    }

    // ─── User Management ─────────────────────────────────────────────

    public Page<User> getUsers(int page, int size) {
        return userRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Transactional
    public void banUser(Long userId, User admin, String reason) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        target.setIsFrozen(true);
        userRepository.save(target);

        createAuditLog(admin, "BAN_USER", "User", userId, "Banned user: " + target.getEmail() + ". Reason: " + reason);
        log.info("Admin {} banned user {}", admin.getEmail(), target.getEmail());
    }

    @Transactional
    public void unbanUser(Long userId, User admin) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        target.setIsFrozen(false);
        userRepository.save(target);

        createAuditLog(admin, "UNBAN_USER", "User", userId, "Unbanned user: " + target.getEmail());
        log.info("Admin {} unbanned user {}", admin.getEmail(), target.getEmail());
    }

    // ─── Listing Approval ────────────────────────────────────────────

    public Page<Item> getPendingItems(int page, int size) {
        return itemRepository.findByStatus(ItemStatus.PENDING_APPROVAL,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt")));
    }

    @Transactional
    public void approveItem(Long itemId, User admin) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        if (item.getStatus() != ItemStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Item is not pending approval. Current: " + item.getStatus());
        }
        item.setStatus(ItemStatus.ACTIVE);
        itemRepository.save(item);

        createAuditLog(admin, "APPROVE_ITEM", "Item", itemId, "Approved item: " + item.getTitle());
        log.info("Admin {} approved item {}", admin.getEmail(), itemId);
    }

    @Transactional
    public void rejectItem(Long itemId, User admin, String reason) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        if (item.getStatus() != ItemStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Item is not pending approval. Current: " + item.getStatus());
        }
        item.setStatus(ItemStatus.REJECTED);
        itemRepository.save(item);

        createAuditLog(admin, "REJECT_ITEM", "Item", itemId,
                "Rejected item: " + item.getTitle() + ". Reason: " + reason);
        log.info("Admin {} rejected item {}", admin.getEmail(), itemId);
    }

    // ─── Fraud Reports ───────────────────────────────────────────────

    public Page<FraudReport> getFraudReports(FraudReportStatus status, int page, int size) {
        if (status != null) {
            return fraudReportRepository.findByStatus(status,
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        }
        return fraudReportRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Transactional
    public void resolveFraudReport(Long reportId, User admin, FraudReportStatus newStatus, String resolutionNotes) {
        FraudReport report = fraudReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Fraud report not found"));

        report.setStatus(newStatus);
        report.setResolvedBy(admin);
        report.setResolutionNotes(resolutionNotes);
        fraudReportRepository.save(report);

        createAuditLog(admin, "RESOLVE_FRAUD_REPORT", "FraudReport", reportId,
                "Status changed to " + newStatus + ". Notes: " + resolutionNotes);
        log.info("Admin {} resolved fraud report {} as {}", admin.getEmail(), reportId, newStatus);
    }

    // ─── Audit Logs ──────────────────────────────────────────────────

    public Page<AuditLog> getAuditLogs(String entityType, int page, int size) {
        if (entityType != null && !entityType.isBlank()) {
            return auditLogRepository.findByEntityType(entityType,
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        }
        return auditLogRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    // ─── Helper ──────────────────────────────────────────────────────

    private void createAuditLog(User admin, String action, String entityType, Long entityId, String details) {
        AuditLog auditLog = AuditLog.builder()
                .admin(admin)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build();
        auditLogRepository.save(auditLog);
    }
}
