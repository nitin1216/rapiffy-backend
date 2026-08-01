package com.example.rapiffy.repos;

import com.example.rapiffy.model.Category;
import com.example.rapiffy.model.SubCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubCategoryRepository extends JpaRepository<SubCategory, Long> {

    List<SubCategory> findByCategory(Category category);

    List<SubCategory> findByCategoryAndIsActiveTrue(Category category);

    boolean existsByNameAndCategory(String name, Category category);
}
