package com.example.deliveryservice.repository;

import com.example.deliveryservice.entity.DeliveryConfirmation;
import com.example.deliveryservice.enums.DeliveryConfirmationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryConfirmationRepository extends JpaRepository<DeliveryConfirmation, Long> {

    Optional<DeliveryConfirmation> findByOrderIdAndIsDeletedFalse(Long orderId);

    Optional<DeliveryConfirmation> findByQrCodeAndIsDeletedFalse(String qrCode);

    List<DeliveryConfirmation> findByDeliveryStaffIdAndIsDeletedFalse(String deliveryStaffId);

    List<DeliveryConfirmation> findByCustomerIdAndIsDeletedFalse(String customerId);

    List<DeliveryConfirmation> findByStatusAndIsDeletedFalse(DeliveryConfirmationStatus status);

    @Query("SELECT dc FROM DeliveryConfirmation dc WHERE dc.deliveryStaffId = :deliveryStaffId AND dc.isDeleted = false ORDER BY dc.deliveryDate DESC")
    List<DeliveryConfirmation> findByDeliveryStaffIdOrderByDeliveryDateDesc(@Param("deliveryStaffId") String deliveryStaffId);

    @Query("SELECT dc FROM DeliveryConfirmation dc WHERE dc.customerId = :customerId AND dc.isDeleted = false ORDER BY dc.deliveryDate DESC")
    List<DeliveryConfirmation> findByCustomerIdOrderByDeliveryDateDesc(@Param("customerId") String customerId);

    @Query("SELECT dc FROM DeliveryConfirmation dc WHERE dc.qrCodeScannedAt IS NOT NULL AND dc.isDeleted = false")
    List<DeliveryConfirmation> findScannedConfirmations();

    @Query("SELECT dc FROM DeliveryConfirmation dc WHERE dc.qrCodeScannedAt IS NULL AND dc.isDeleted = false")
    List<DeliveryConfirmation> findUnscannedConfirmations();

    List<DeliveryConfirmation> findByIsDeletedFalse();
}


