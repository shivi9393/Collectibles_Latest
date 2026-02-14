package com.marketplace.scheduler;

import com.marketplace.entity.EscrowTransaction;
import com.marketplace.enums.EscrowStatus;
import com.marketplace.repository.EscrowTransactionRepository;
import com.marketplace.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EscrowAutoReleaseScheduler {

    private final EscrowTransactionRepository transactionRepository;
    private final OrderService orderService;

    @Scheduled(fixedRate = 3600000) // Every hour
    public void autoReleaseEscrow() {
        LocalDateTime now = LocalDateTime.now();
        List<EscrowTransaction> readyForRelease = transactionRepository
                .findByStatusAndEscrowReleaseDeadlineBefore(EscrowStatus.HELD, now);

        for (EscrowTransaction tx : readyForRelease) {
            try {
                orderService.autoConfirmDelivery(tx.getOrder().getId());
            } catch (Exception e) {
                log.error("Failed to auto-release escrow for order {}", tx.getOrder().getId(), e);
            }
        }
    }
}
