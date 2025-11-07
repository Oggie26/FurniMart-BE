package com.example.userservice.repository;

import com.example.userservice.entity.FavoriteProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoriteProductRepository extends JpaRepository<FavoriteProduct, Long> {

    Optional<FavoriteProduct> findByUserIdAndProductIdAndIsDeletedFalse(String userId, String productId);

    List<FavoriteProduct> findByUserIdAndIsDeletedFalse(String userId);

    Page<FavoriteProduct> findByUserIdAndIsDeletedFalse(String userId, Pageable pageable);

    @Query("SELECT COUNT(f) FROM FavoriteProduct f WHERE f.userId = :userId AND f.isDeleted = false")
    Long countByUserId(@Param("userId") String userId);

    boolean existsByUserIdAndProductIdAndIsDeletedFalse(String userId, String productId);
}

