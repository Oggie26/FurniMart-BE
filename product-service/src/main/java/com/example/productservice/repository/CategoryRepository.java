package com.example.productservice.repository;

import com.example.productservice.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByIdAndIsDeletedFalse(Long id);
    Optional<Category> findByCategoryNameAndIsDeletedFalse(String categoryName);
    @Query(value = """
        SELECT * FROM categories 
        WHERE is_deleted = false 
          AND (LOWER(category_name) LIKE LOWER(CONCAT('%', :keyword, '%')) 
               OR LOWER(description) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """,
            countQuery = """
        SELECT COUNT(*) FROM categories 
        WHERE is_deleted = false 
          AND (LOWER(category_name) LIKE LOWER(CONCAT('%', :keyword, '%')) 
               OR LOWER(description) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """,
            nativeQuery = true)
    Page<Category> searchByKeywordNative(@Param("keyword") String keyword, PageRequest pageable);
}
