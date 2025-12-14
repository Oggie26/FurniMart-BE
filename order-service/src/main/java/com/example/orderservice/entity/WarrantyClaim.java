package com.example.orderservice.entity;

import com.example.orderservice.enums.WarrantyActionType;
import com.example.orderservice.enums.WarrantyClaimStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "warranty_claims")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarrantyClaim extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @OneToMany(mappedBy = "warrantyClaim", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<WarrantyClaimDetail> claimDetails;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "address_id", nullable = false)
    private Long addressId;

    @Column(name = "claim_date", nullable = false)
    private LocalDateTime claimDate;

    // issueDescription and customerPhotos moved to WarrantyClaimDetail
    // keeping general note if needed, or remove.
    // For backward compatibility or general note:
    @Column(name = "general_note", columnDefinition = "TEXT")
    private String generalNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WarrantyClaimStatus status;

    @Column(name = "admin_response", columnDefinition = "TEXT")
    private String adminResponse;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "resolution_photos", columnDefinition = "TEXT")
    private String resolutionPhotos; // JSON array of photo URLs

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type")
    private WarrantyActionType actionType;

    @Column(name = "repair_cost")
    private Double repairCost; // Only visible to Admin/Manager

    @Column(name = "exchange_product_color_id")
    private String exchangeProductColorId; // For exchange orders

    @Column(name = "refund_amount")
    private Double refundAmount; // For return orders

    @Column(name = "resolved_date")
    private LocalDateTime resolvedDate;

    @Column(name = "admin_id")
    private String adminId;

    // Removed single warranty link


    @PrePersist
    public void prePersist() {
        if (claimDate == null) {
            claimDate = LocalDateTime.now();
        }
        if (status == null) {
            status = WarrantyClaimStatus.PENDING;
        }
    }
}
