package com.example.orderservice.repository;

import com.example.orderservice.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByUserId(String userId);

    Optional<Order> findByIdAndIsDeletedFalse(Long orderId);

    Optional<Order> findByStoreId(String storeId);

    Page<Order> findByStatusAndIsDeletedFalse(com.example.orderservice.enums.EnumProcessOrder status, Pageable pageable);

    // 🔍 Search theo userId + keyword
    @Query(value = """
            SELECT * FROM orders 
            WHERE is_deleted = false 
              AND user_id = :userId
              AND (
                   LOWER(note) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(store_id) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR CAST(total AS TEXT) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """,
            countQuery = """
            SELECT COUNT(*) FROM orders 
            WHERE is_deleted = false 
              AND user_id = :userId
              AND (
                   LOWER(note) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(store_id) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR CAST(total AS TEXT) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """,
            nativeQuery = true)
    Page<Order> searchByUserIdAndKeyword(
            @Param("userId") String userId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // 🔍 Search theo storeId + keyword
    @Query("""
    SELECT o FROM Order o 
    WHERE o.isDeleted = false AND o.storeId = :storeId
      AND (
           LOWER(o.note) LIKE LOWER(CONCAT('%', :keyword, '%')) 
        OR LOWER(o.userId) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR CAST(o.total AS string) LIKE LOWER(CONCAT('%', :keyword, '%'))
      )
    ORDER BY o.createdAt DESC
""")
    Page<Order> searchByStoreIdAndKeyword(
            @Param("storeId") String storeId,
            @Param("keyword") String keyword,
            Pageable pageable
    );



    // 🔍 Search chung (không ràng buộc userId/storeId)
    @Query(value = """
            SELECT * FROM orders 
            WHERE is_deleted = false
              AND (
                   LOWER(note) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(user_id) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(store_id) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR CAST(total AS TEXT) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """,
            countQuery = """
            SELECT COUNT(*) FROM orders 
            WHERE is_deleted = false
              AND (
                   LOWER(note) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(user_id) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(store_id) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR CAST(total AS TEXT) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """,
            nativeQuery = true)
    Page<Order> searchByKeywordNative(
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
