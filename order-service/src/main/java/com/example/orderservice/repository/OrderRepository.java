package com.example.orderservice.repository;

import com.example.orderservice.entity.Order;
import com.example.orderservice.enums.EnumProcessOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByUserId(String userId);

    Optional<Order> findByIdAndIsDeletedFalse(Long orderId);

    Optional<Order> findByStoreId(String storeId);

    Page<Order> findByStatusAndIsDeletedFalse(com.example.orderservice.enums.EnumProcessOrder status, Pageable pageable);

    // 🔍 Lấy orders theo storeId và status (optional), sắp xếp theo thời gian tạo (mới nhất trước)
    @Query("""
        SELECT o FROM Order o
        WHERE o.isDeleted = false AND o.storeId = :storeId AND o.status = :status
        ORDER BY o.createdAt DESC
    """)
    Page<Order> findByStoreIdAndStatusAndIsDeletedFalse(
            @Param("storeId") String storeId,
            @Param("status") com.example.orderservice.enums.EnumProcessOrder status,
            Pageable pageable
    );

    // 🔍 Lấy tất cả orders của store (không filter status), sắp xếp theo thời gian tạo (mới nhất trước)
    @Query("""
        SELECT o FROM Order o
        WHERE o.isDeleted = false AND o.storeId = :storeId
        ORDER BY o.createdAt DESC
    """)
    Page<Order> findByStoreIdAndIsDeletedFalse(@Param("storeId") String storeId, Pageable pageable);

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

    // 🔍 Lấy orders của store đã được tạo hóa đơn (có pdfFilePath)
    // Orders có pdfFilePath IS NOT NULL AND pdfFilePath != '' được coi là đã tạo hóa đơn
    @Query("""
        SELECT o FROM Order o
        WHERE o.isDeleted = false 
          AND o.storeId = :storeId 
          AND o.pdfFilePath IS NOT NULL 
          AND o.pdfFilePath != ''
        ORDER BY o.createdAt DESC
    """)
    Page<Order> findByStoreIdWithInvoice(
            @Param("storeId") String storeId,
            Pageable pageable
    );

    // Dashboard queries
    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.isDeleted = false AND o.status IN :statuses")
    Double getTotalRevenueByStatuses(@Param("statuses") List<com.example.orderservice.enums.EnumProcessOrder> statuses);

    @Query("""
        SELECT COALESCE(SUM(o.total), 0) 
        FROM Order o 
        WHERE o.isDeleted = false 
          AND o.storeId = :storeId 
          AND o.status IN :statuses
    """)
    Double getTotalRevenueByStoreAndStatuses(
            @Param("storeId") String storeId,
            @Param("statuses") List<com.example.orderservice.enums.EnumProcessOrder> statuses
    );

    @Query("""
    SELECT COUNT(o) 
    FROM Order o 
    WHERE o.isDeleted = false
      AND o.storeId = :storeId
      AND o.status = :status
""")
    Long countOrdersByStoreAndStatus(
            @Param("storeId") String storeId,
            @Param("status") EnumProcessOrder status
    );

    @Query("""
        SELECT DATE(o.orderDate) as date, 
               COALESCE(SUM(o.total), 0) as revenue, 
               COUNT(o) as orderCount
        FROM Order o 
        WHERE o.isDeleted = false 
          AND o.status IN :statuses
          AND o.orderDate >= :startDate 
          AND o.orderDate <= :endDate
        GROUP BY DATE(o.orderDate)
        ORDER BY DATE(o.orderDate) ASC
    """)
    List<Object[]> getRevenueChartData(
            @Param("statuses") List<com.example.orderservice.enums.EnumProcessOrder> statuses,
            @Param("startDate") java.util.Date startDate,
            @Param("endDate") java.util.Date endDate
    );

    @Query("""
        SELECT COUNT(o) 
        FROM Order o 
        WHERE o.isDeleted = false 
          AND o.storeId = :storeId
    """)
    Long countByStoreId(@Param("storeId") String storeId);

    @Query("""
        SELECT COUNT(o) 
        FROM Order o 
        WHERE o.isDeleted = false 
          AND o.status = :status
    """)
    Long countByStatus(@Param("status") com.example.orderservice.enums.EnumProcessOrder status);

    @Query("""
        SELECT COUNT(o) 
        FROM Order o 
        WHERE o.isDeleted = false 
          AND o.storeId = :storeId
          AND o.orderDate >= :startDate 
          AND o.orderDate < :endDate
    """)
    Long countByStoreIdAndDateRange(
            @Param("storeId") String storeId,
            @Param("startDate") java.util.Date startDate,
            @Param("endDate") java.util.Date endDate
    );

    @Query("""
        SELECT COUNT(o) 
        FROM Order o 
        WHERE o.isDeleted = false 
          AND o.storeId = :storeId
          AND o.status = :status
          AND o.orderDate >= :startDate 
          AND o.orderDate < :endDate
    """)
    Long countOrdersByStoreAndStatusAndDateRange(
            @Param("storeId") String storeId,
            @Param("status") com.example.orderservice.enums.EnumProcessOrder status,
            @Param("startDate") java.util.Date startDate,
            @Param("endDate") java.util.Date endDate
    );

    @Query("""
        SELECT COALESCE(SUM(o.total), 0) 
        FROM Order o 
        WHERE o.isDeleted = false 
          AND o.storeId = :storeId 
          AND o.status IN :statuses
          AND o.orderDate >= :startDate 
          AND o.orderDate < :endDate
    """)
    Double getTotalRevenueByStoreAndStatusesAndDateRange(
            @Param("storeId") String storeId,
            @Param("statuses") List<com.example.orderservice.enums.EnumProcessOrder> statuses,
            @Param("startDate") java.util.Date startDate,
            @Param("endDate") java.util.Date endDate
    );

    @Query("""
        SELECT COUNT(DISTINCT o.userId) 
        FROM Order o 
        WHERE o.isDeleted = false 
          AND o.storeId = :storeId
          AND o.orderDate >= :startDate 
          AND o.orderDate < :endDate
          AND o.userId NOT IN (
              SELECT DISTINCT o2.userId 
              FROM Order o2 
              WHERE o2.isDeleted = false 
                AND o2.storeId = :storeId
                AND o2.orderDate < :startDate
          )
    """)
    Long countNewCustomersByStoreAndDateRange(
            @Param("storeId") String storeId,
            @Param("startDate") java.util.Date startDate,
            @Param("endDate") java.util.Date endDate
    );

    @Query("""
        SELECT DATE(o.orderDate) as date, 
        COALESCE(SUM(o.total), 0) as revenue, 
        COUNT(o) as orderCount
        FROM Order o 
        WHERE o.isDeleted = false 
        AND o.storeId = :storeId
        AND o.status IN :statuses
        AND o.orderDate >= :startDate 
        AND o.orderDate <= :endDate
        GROUP BY DATE(o.orderDate)
        ORDER BY DATE(o.orderDate) ASC
    """)
    List<Object[]> getRevenueChartDataByStore(
            @Param("storeId") String storeId,
            @Param("statuses") List<com.example.orderservice.enums.EnumProcessOrder> statuses,
            @Param("startDate") java.util.Date startDate,
            @Param("endDate") java.util.Date endDate
    );

    // Staff Dashboard
    @Query("""
        SELECT COALESCE(SUM(o.total), 0) 
        FROM Order o 
        WHERE o.isDeleted = false 
          AND o.createdBy = :createdBy 
          AND o.status IN :statuses
          AND o.orderDate >= :startDate 
          AND o.orderDate <= :endDate
    """)
    Double getTotalRevenueByCreatedByAndDateRange(
            @Param("createdBy") String createdBy,
            @Param("statuses") List<com.example.orderservice.enums.EnumProcessOrder> statuses,
            @Param("startDate") java.util.Date startDate,
            @Param("endDate") java.util.Date endDate
    );

    @Query("""
        SELECT COUNT(o) 
        FROM Order o 
        WHERE o.isDeleted = false 
          AND o.createdBy = :createdBy
          AND o.orderDate >= :startDate 
          AND o.orderDate <= :endDate
    """)
    Long countByCreatedByAndDateRange(
            @Param("createdBy") String createdBy,
            @Param("startDate") java.util.Date startDate,
            @Param("endDate") java.util.Date endDate
    );

    Page<Order> findByCreatedByOrderByCreatedAtDesc(String createdBy, Pageable pageable);
}
