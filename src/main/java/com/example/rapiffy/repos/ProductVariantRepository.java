package com.example.rapiffy.repos;

import com.example.rapiffy.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByParentShopProductIdAndIsActive(Long parentShopProductId, boolean isActive);

    List<ProductVariant> findByParentShopProductId(Long parentShopProductId);

    Optional<ProductVariant> findByShopProductId(Long shopProductId);
}
