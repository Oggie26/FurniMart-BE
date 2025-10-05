package com.example.productservice.repository;

import com.example.productservice.entity.ProductColor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductColorRepository extends JpaRepository<ProductColor, String> {
    Optional<ProductColor> findByIdAndIsDeletedFalse(String id);
    List<ProductColor> findByProductIdAndIsDeletedFalse(String id);
    boolean existsByProductIdAndColorId(String productId, String colorId);
}
