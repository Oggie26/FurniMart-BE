package com.example.deliveryservice.entity;

import com.example.deliveryservice.enums.DeliveryConfirmationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_confirmations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryConfirmation extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "delivery_staff_id", nullable = false)
    private String deliveryStaffId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "delivery_date", nullable = false)
    private LocalDateTime deliveryDate;

    @Column(name = "delivery_photos", columnDefinition = "TEXT")
    private String deliveryPhotos; // JSON array of photo URLs

    @Column(name = "delivery_notes", columnDefinition = "TEXT")
    private String deliveryNotes;

    @Column(name = "qr_code", nullable = false, unique = true)
    private String qrCode;

    @Column(name = "qr_code_generated_at", nullable = false)
    private LocalDateTime qrCodeGeneratedAt;

    @Column(name = "qr_code_scanned_at")
    private LocalDateTime qrCodeScannedAt;

    @Column(name = "customer_signature", columnDefinition = "TEXT")
    private String customerSignature; // Base64 encoded signature

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryConfirmationStatus status;

    @Column(name = "delivery_latitude")
    private Double deliveryLatitude;

    @Column(name = "delivery_longitude")
    private Double deliveryLongitude;

    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    @PrePersist
    public void prePersist() {
        if (deliveryDate == null) {
            deliveryDate = LocalDateTime.now();
        }
        if (qrCodeGeneratedAt == null) {
            qrCodeGeneratedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = DeliveryConfirmationStatus.DELIVERED;
        }
    }
}


