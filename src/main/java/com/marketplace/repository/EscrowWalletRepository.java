package com.marketplace.repository;

import com.marketplace.entity.EscrowWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EscrowWalletRepository extends JpaRepository<EscrowWallet, Long> {
    Optional<EscrowWallet> findByUserId(Long userId);

    Optional<EscrowWallet> findByIsPlatformWalletTrue();
}
