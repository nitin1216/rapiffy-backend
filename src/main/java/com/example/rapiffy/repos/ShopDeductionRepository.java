package com.example.rapiffy.repos;

import com.example.rapiffy.enums.ShopDeductionStatus;
import com.example.rapiffy.model.ShopDeduction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShopDeductionRepository extends JpaRepository<ShopDeduction, Long> {

    // All pending deductions for a shop — used before creating a transfer payout
    List<ShopDeduction> findByShopIdAndStatus(Long shopId, ShopDeductionStatus status);

    // All deductions for a shop (for audit/display to shop owner)
    List<ShopDeduction> findByShopIdOrderByCreatedAtDesc(Long shopId);
}
