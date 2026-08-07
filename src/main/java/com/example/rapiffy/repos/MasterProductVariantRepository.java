package com.example.rapiffy.repos;

import com.example.rapiffy.model.MasterProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MasterProductVariantRepository extends JpaRepository<MasterProductVariant, Long> {

    List<MasterProductVariant> findByParentMasterProductId(Long parentMasterProductId);

    List<MasterProductVariant> findByParentMasterProductIdAndIsActive(Long parentMasterProductId, boolean isActive);
}
