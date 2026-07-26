package com.example.rapiffy.repos;

import com.example.rapiffy.model.Category;
import com.example.rapiffy.model.MasterProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MasterProductRepository extends JpaRepository<MasterProduct, Long> {

    // Get all active products for given categories (Admin's shop categories)
    List<MasterProduct> findByCategoryInAndIsActiveTrue(Collection<Category> categories);

    // Check if a product code already exists (used during CSV import to skip duplicates)
    boolean existsByProductCode(String productCode);

    // Get all products for a specific category
    List<MasterProduct> findByCategory(Category category);
}
