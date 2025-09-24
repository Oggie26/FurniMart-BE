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

    @Query(value = """
            SELECT * FROM orders 
            WHERE is_deleted = false 
              AND user_id = :userId
              AND (
                    LOWER(order_code) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(status) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(address) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(note) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """,
            countQuery = """

                    SELECT COUNT(*) FROM orders 
            WHERE is_deleted = false 
              AND user_id = :userId
              AND (
                    LOWER(order_code) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(status) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(address) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(note) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """,
            nativeQuery = true)
    Page<Order> searchByUserIdAndKeyword(
            @Param("userId") String userId,
            @Param("keyword") String keyword,
            Pageable pageable
    );


    // search riêng theo storeId + keyword
    @Query(value = """
            SELECT * FROM orders 
            WHERE is_deleted = false 
              AND store_id = :storeId
              AND (
                    LOWER(order_code) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(status) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(address) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(note) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """,
            countQuery = """
            SELECT COUNT(*) FROM orders 
            WHERE is_deleted = false 
              AND store_id = :storeId
              AND (
                    LOWER(order_code) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(status) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(address) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(note) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """,
            nativeQuery = true)
    Page<Order> searchByStoreIdAndKeyword(
            @Param("storeId") String storeId,
            @Param("keyword") String keyword,
            Pageable pageable
    );


    // search chung (không ràng buộc userId/storeId)
    @Query(value = """
            SELECT * FROM orders 
            WHERE is_deleted = false
              AND (
                    LOWER(order_code) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(status) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(address) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(note) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """,
            countQuery = """
            SELECT COUNT(*) FROM orders 
            WHERE is_deleted = false
              AND (
                    LOWER(order_code) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(status) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(address) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(note) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """,
            nativeQuery = true)
    Page<Order> searchByKeywordNative(
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
