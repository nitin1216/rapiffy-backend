package com.example.rapiffy.repos;

import com.example.rapiffy.model.SubCategory;
import com.example.rapiffy.model.MasterProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MasterProductRepository extends JpaRepository<MasterProduct, Long> {

    List<MasterProduct> findBySubCategoryInAndIsActiveTrue(Collection<SubCategory> subCategories);

    boolean existsByProductCode(String productCode);

    List<MasterProduct> findBySubCategory(SubCategory subCategory);
}
