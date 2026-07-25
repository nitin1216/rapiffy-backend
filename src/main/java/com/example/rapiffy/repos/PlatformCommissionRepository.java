package com.example.rapiffy.repos;

import com.example.rapiffy.model.Category;
import com.example.rapiffy.model.PlatformCommission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlatformCommissionRepository extends JpaRepository<PlatformCommission, Long> {

    // Find commission rate for a specific category
    Optional<PlatformCommission> findByCategoryAndIsActiveTrue(Category category);
}
