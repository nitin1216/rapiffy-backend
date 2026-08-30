package com.example.rapiffy.repos;

import com.example.rapiffy.model.ProductVariantImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductVariantImageRepository extends JpaRepository<ProductVariantImage, Long> {
    List<ProductVariantImage> findByVariantIdOrderByDisplayOrderAsc(Long variantId);
}
