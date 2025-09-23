package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory,String> {
    Optional<Inventory> findByIdAndIsDeletedFalse(String id);
    List<Inventory> findAllByProductId(String productId);
    Optional<Inventory> findByProductIdAndLocationItemId(String productId, String locationItemId);
    @Query("SELECT SUM(i.quantity) FROM Inventory i WHERE i.productId = :productId")
    Integer sumQuantityByProductId(@Param("productId") String productId);

    Optional<Inventory> findByLocationItem_IdAndProductId(String locationItemId, String productId);
    @Query(value = """
            SELECT * FROM inventory i
            WHERE i.status = 'ACTIVE'
              AND (
                   LOWER(i.product_id) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(i.id) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """,
            countQuery = """
            SELECT COUNT(*) FROM inventory i
            WHERE i.status = 'ACTIVE'
              AND (
                   LOWER(i.product_id) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(i.id) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """,
            nativeQuery = true)
    Page<Inventory> searchByKeywordNative(@Param("keyword") String keyword, Pageable pageable);

}
