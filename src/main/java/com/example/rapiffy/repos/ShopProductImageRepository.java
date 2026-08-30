package com.example.rapiffy.repos;

import com.example.rapiffy.model.ShopProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShopProductImageRepository extends JpaRepository<ShopProductImage, Long> {
    List<ShopProductImage> findByShopProductIdOrderByDisplayOrderAsc(Long shopProductId);
}
