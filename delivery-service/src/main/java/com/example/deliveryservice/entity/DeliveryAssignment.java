package com.example.deliveryservice.entity;

import com.example.deliveryservice.enums.DeliveryStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAssignment extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "store_id", nullable = false)
    private String storeId;

    @Column(name = "delivery_staff_id")
    private String deliveryStaffId;

    @Column(name = "assigned_by", nullable = false)
    private String assignedBy; // Staff or Branch Manager ID

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "estimated_delivery_date")
    private LocalDateTime estimatedDeliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "invoice_generated", nullable = false)
    @Builder.Default
    private Boolean invoiceGenerated = false;

    @Column(name = "invoice_generated_at")
    private LocalDateTime invoiceGeneratedAt;

    @Column(name = "products_prepared", nullable = false)
    @Builder.Default
    private Boolean productsPrepared = false;

    @Column(name = "products_prepared_at")
    private LocalDateTime productsPreparedAt;

    @PrePersist
    public void prePersist() {
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = DeliveryStatus.ASSIGNED;
        }
    }
}

