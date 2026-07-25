package com.example.rapiffy.repos;

import com.example.rapiffy.model.PlatformConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlatformConfigRepository extends JpaRepository<PlatformConfig, Long> {
}
