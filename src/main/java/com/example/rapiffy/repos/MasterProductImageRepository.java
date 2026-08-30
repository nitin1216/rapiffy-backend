package com.example.rapiffy.repos;

import com.example.rapiffy.model.MasterProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MasterProductImageRepository extends JpaRepository<MasterProductImage, Long> {
    List<MasterProductImage> findByMasterProductIdOrderByDisplayOrderAsc(Long masterProductId);
}
