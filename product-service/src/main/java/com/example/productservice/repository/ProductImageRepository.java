package com.example.productservice.repository;

import com.example.productservice.entity.ProductImage;
import com.example.productservice.entity.ProductModel3D;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, String> {
    void deleteByProductColorId(String productColorId);

}
