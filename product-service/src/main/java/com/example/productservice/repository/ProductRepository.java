package com.example.productservice.repository;

import com.example.productservice.entity.Product;

import feign.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product,String> {
    Optional<Product> findByIdAndIsDeletedFalse(String id);
    List<Product> findByCategoryIdAndIsDeletedFalse(Long categoryId);
    Optional<Product> findBySlugAndIsDeletedFalse(String slug);
    Optional<Product> findByCodeAndIsDeletedFalse(String code);
    Optional<Product> findByNameAndIsDeletedFalse(String name);
        @Query(value = """
        SELECT p.* 
        FROM product p
        JOIN category c ON p.category_id = c.id
        WHERE p.is_deleted = false
        AND (
            :keyword IS NULL
            OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        """,
                countQuery = """
        SELECT COUNT(*) 
        FROM product p
        JOIN category c ON p.category_id = c.id
        WHERE p.is_deleted = false
        AND (
            :keyword IS NULL
            OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        """,
                nativeQuery = true)
        Page<Product> searchByKeywordNative(@Param("keyword") String keyword, Pageable pageable);
    }

