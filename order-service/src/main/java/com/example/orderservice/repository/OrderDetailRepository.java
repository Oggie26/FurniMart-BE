package com.example.orderservice.repository;

import com.example.orderservice.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    
    List<OrderDetail> findByOrderIdAndIsDeletedFalse(Long orderId);

    @Query("""
        SELECT od.productColorId, 
               SUM(od.quantity) as totalQuantity, 
               SUM(od.quantity * od.price) as totalRevenue
        FROM OrderDetail od
        JOIN od.order o
        WHERE o.isDeleted = false 
          AND o.status IN :statuses
          AND od.isDeleted = false
        GROUP BY od.productColorId
        ORDER BY totalQuantity DESC
    """)
    List<Object[]> getTopProductsBySales(
            @Param("statuses") List<com.example.orderservice.enums.EnumProcessOrder> statuses
    );

    @Query("""
        SELECT od.productColorId, 
               SUM(od.quantity) as totalQuantity, 
               SUM(od.quantity * od.price) as totalRevenue
        FROM OrderDetail od
        JOIN od.order o
        WHERE o.isDeleted = false 
          AND o.storeId = :storeId
          AND o.status IN :statuses
          AND od.isDeleted = false
        GROUP BY od.productColorId
        ORDER BY totalQuantity DESC
    """)
    List<Object[]> getTopProductsBySalesAndStore(
            @Param("storeId") String storeId,
            @Param("statuses") List<com.example.orderservice.enums.EnumProcessOrder> statuses
    );
}
