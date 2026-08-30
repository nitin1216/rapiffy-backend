package com.example.rapiffy.repos;

import com.example.rapiffy.model.MasterProductVariantImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MasterProductVariantImageRepository extends JpaRepository<MasterProductVariantImage, Long> {
    List<MasterProductVariantImage> findByVariantIdOrderByDisplayOrderAsc(Long variantId);
}
