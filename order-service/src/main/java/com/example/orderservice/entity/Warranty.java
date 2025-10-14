package com.example.orderservice.entity;

import com.example.orderservice.enums.WarrantyStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "warranties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warranty extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_detail_id", nullable = false)
    private Long orderDetailId;

    @Column(name = "product_color_id", nullable = false)
    private String productColorId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "delivery_date", nullable = false)
    private LocalDateTime deliveryDate;

    @Column(name = "warranty_start_date", nullable = false)
    private LocalDateTime warrantyStartDate;

    @Column(name = "warranty_end_date", nullable = false)
    private LocalDateTime warrantyEndDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WarrantyStatus status;

    @Column(name = "warranty_duration_months", nullable = false)
    private Integer warrantyDurationMonths;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "claim_count")
    @Builder.Default
    private Integer claimCount = 0;

    @Column(name = "max_claims")
    @Builder.Default
    private Integer maxClaims = 3;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_detail_id", insertable = false, updatable = false)
    private OrderDetail orderDetail;

    @PrePersist
    public void prePersist() {
        if (warrantyStartDate == null) {
            warrantyStartDate = deliveryDate;
        }
        if (warrantyEndDate == null) {
            warrantyEndDate = warrantyStartDate.plusMonths(warrantyDurationMonths);
        }
        if (status == null) {
            status = WarrantyStatus.ACTIVE;
        }
        if (warrantyDurationMonths == null) {
            warrantyDurationMonths = 24; // 2 years default
        }
    }

    public boolean isActive() {
        return status == WarrantyStatus.ACTIVE && 
               LocalDateTime.now().isBefore(warrantyEndDate) &&
               claimCount < maxClaims;
    }

    public boolean canClaimWarranty() {
        return isActive() && claimCount < maxClaims;
    }

    public void incrementClaimCount() {
        this.claimCount++;
        if (this.claimCount >= this.maxClaims) {
            this.status = WarrantyStatus.EXHAUSTED;
        }
    }
}
