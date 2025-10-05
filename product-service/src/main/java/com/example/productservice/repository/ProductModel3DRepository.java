package com.example.productservice.repository;

import com.example.productservice.entity.ProductModel3D;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductModel3DRepository extends JpaRepository<ProductModel3D, String> {
    ProductModel3D deleteByProductColorId(String productColorId);
}
