package com.example.rapiffy.repos;

import com.example.rapiffy.enums.CategoryType;
import com.example.rapiffy.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByCategoryTypeIn(List<CategoryType> types);

    Optional<Category> findByCategoryType(CategoryType type);
}
