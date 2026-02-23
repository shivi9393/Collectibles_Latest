package com.marketplace.repository;

import com.marketplace.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByAdminIdOrderByCreatedAtDesc(Long adminId);

    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);

    List<AuditLog> findAllByOrderByCreatedAtDesc();

    Page<AuditLog> findAll(Pageable pageable);

    Page<AuditLog> findByEntityType(String entityType, Pageable pageable);
}
