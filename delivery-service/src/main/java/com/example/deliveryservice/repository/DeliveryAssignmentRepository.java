package com.example.deliveryservice.repository;

import com.example.deliveryservice.entity.DeliveryAssignment;
import com.example.deliveryservice.enums.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, Long> {

    Optional<DeliveryAssignment> findByOrderIdAndIsDeletedFalse(Long orderId);

    List<DeliveryAssignment> findByStoreIdAndIsDeletedFalse(String storeId);

    List<DeliveryAssignment> findByDeliveryStaffIdAndIsDeletedFalse(String deliveryStaffId);

    List<DeliveryAssignment> findByStatusAndIsDeletedFalse(DeliveryStatus status);

    List<DeliveryAssignment> findByStoreIdAndStatusAndIsDeletedFalse(String storeId, DeliveryStatus status);

    List<DeliveryAssignment> findByDeliveryStaffIdAndStatusAndIsDeletedFalse(String deliveryStaffId,
            DeliveryStatus status);

    List<DeliveryAssignment> findByStatusInAndIsDeletedFalse(List<DeliveryStatus> statuses);
}
