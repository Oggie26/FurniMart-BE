package com.example.productservice.repository;

import com.example.productservice.entity.Product;
import com.example.productservice.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    Optional<Rating> findByIdAndIsDeletedFalse (Long ratingId);
    List<Rating> findByProduct(Product product);
}
