package com.example.rapiffy.repos;

import com.example.rapiffy.model.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    // Full transaction history for a user (newest first)
    List<WalletTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
}
