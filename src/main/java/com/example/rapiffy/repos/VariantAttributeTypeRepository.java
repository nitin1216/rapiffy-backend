package com.example.rapiffy.repos;

import com.example.rapiffy.model.VariantAttributeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VariantAttributeTypeRepository extends JpaRepository<VariantAttributeType, Long> {

    List<VariantAttributeType> findByMasterProductIdOrderByDisplayOrder(Long masterProductId);

    List<VariantAttributeType> findByShopProductIdOrderByDisplayOrder(Long shopProductId);
}
