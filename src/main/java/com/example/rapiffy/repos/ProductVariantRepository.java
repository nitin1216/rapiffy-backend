package com.example.rapiffy.repos;

import com.example.rapiffy.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByParentShopProduct_IdAndIsActive(Long parentShopProductId, boolean isActive);

    List<ProductVariant> findByParentShopProduct_Id(Long parentShopProductId);

    Optional<ProductVariant> findByShopProductId(Long shopProductId);

    /**
     * Returns active variants where ALL provided attribute key=value pairs match.
     * filterCount must equal the number of entries in the attributes map.
     */
    @Query("SELECT v FROM ProductVariant v WHERE v.parentShopProduct.id = :parentId AND v.isActive = true AND (SELECT COUNT(av) FROM VariantAttributeValue av WHERE av.productVariant = v AND CONCAT(av.attributeType.attributeName, '=', av.attributeValue) IN :filters) = :filterCount")
    List<ProductVariant> findMatchingVariants(
        @Param("parentId") Long parentId,
        @Param("filters") List<String> filters,
        @Param("filterCount") long filterCount
    );
}
