package com.example.rapiffy.repos;

import com.example.rapiffy.model.VariantAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VariantAttributeValueRepository extends JpaRepository<VariantAttributeValue, Long> {

    List<VariantAttributeValue> findByProductVariantId(Long productVariantId);

    List<VariantAttributeValue> findByMasterProductVariantId(Long masterProductVariantId);
}
