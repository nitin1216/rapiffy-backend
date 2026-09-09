package com.example.rapiffy.repos;

import com.example.rapiffy.model.CancellationItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CancellationItemRepository extends JpaRepository<CancellationItem, Long> {
}
