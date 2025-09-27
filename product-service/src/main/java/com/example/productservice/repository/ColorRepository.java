package com.example.productservice.repository;

import com.example.productservice.entity.Color;
import org.springframework.data.domain.Page;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ColorRepository extends JpaRepository<Color, String> {
    void deleteAllByProductId(String productId);
    Optional<Color> findById(String colorId);
    @Query(value = """
        SELECT * FROM colors 
        WHERE is_deleted = false 
          AND (
               LOWER(color_name) LIKE LOWER(CONCAT('%', :keyword, '%')) 
               OR LOWER(hex_code) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
        """,
            countQuery = """
        SELECT COUNT(*) FROM colors 
        WHERE is_deleted = false 
          AND (
               LOWER(color_name) LIKE LOWER(CONCAT('%', :keyword, '%')) 
               OR LOWER(hex_code) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
        """,
            nativeQuery = true)
    Page<Color> searchByKeywordNative(@Param("keyword") String keyword, Pageable pageable);


}
