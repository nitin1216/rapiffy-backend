package com.example.rapiffy.repos;

import com.example.rapiffy.model.CategoryImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryImageRepository extends JpaRepository<CategoryImage, Long> {
    List<CategoryImage> findByCategoryId(Long categoryId);
}
