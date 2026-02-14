package com.marketplace.repository;

import com.marketplace.entity.EscrowTransaction;
import com.marketplace.enums.EscrowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EscrowTransactionRepository extends JpaRepository<EscrowTransaction, Long> {
    Optional<EscrowTransaction> findByOrderId(Long orderId);

    // Find transactions ready for auto-release
    List<EscrowTransaction> findByStatusAndEscrowReleaseDeadlineBefore(EscrowStatus status, LocalDateTime now);
}
