package com.marketplace.repository;

import com.marketplace.entity.FraudReport;
import com.marketplace.enums.FraudReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FraudReportRepository extends JpaRepository<FraudReport, Long> {
    Page<FraudReport> findByStatus(FraudReportStatus status, Pageable pageable);

    Page<FraudReport> findAll(Pageable pageable);

    Long countByStatus(FraudReportStatus status);
}
