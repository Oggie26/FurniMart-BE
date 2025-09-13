package com.example.productservice.repository;

import com.example.productservice.entity.Material;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MaterialRepository extends JpaRepository<Material,Long> {
    Optional<Material> findByIdAndIsDeletedFalse(Long id);
    Optional<Material> findByMaterialNameAndIsDeletedFalse(String materialName);
    @Query(value = """
        SELECT * FROM categories 
        WHERE is_deleted = false 
          AND (LOWER(material_name) LIKE LOWER(CONCAT('%', :keyword, '%')) 
               OR LOWER(description) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """,
            countQuery = """
        SELECT COUNT(*) FROM categories 
        WHERE is_deleted = false 
          AND (LOWER(material_name) LIKE LOWER(CONCAT('%', :keyword, '%')) 
               OR LOWER(description) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """,
            nativeQuery = true)
    Page<Material> searchByKeywordNative(@Param("keyword") String keyword, PageRequest pageable);}
