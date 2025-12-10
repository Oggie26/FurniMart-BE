package com.example.orderservice.entity;

import com.example.orderservice.enums.VoucherType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vouchers", uniqueConstraints = {
        @UniqueConstraint(columnNames = "code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(unique = true, nullable = false, length = 50)
    private String code;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false, precision = 15)
    private Float amount;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer point;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoucherType type;

    @Column(nullable = false)
    private Boolean status;

//    @Column(name = "order_id")
//    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    private Order order;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "used_count")
    @Builder.Default
    private Integer usedCount = 0;

    @Column(name = "minimum_order_amount", precision = 15)
    private Float minimumOrderAmount;

    @PrePersist
    public void prePersist() {
        if (status == null) {
            status = true;
        }
        if (usedCount == null) {
            usedCount = 0;
        }
    }

    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return status && 
               now.isAfter(startDate) && 
               now.isBefore(endDate) &&
               (usageLimit == null || usedCount < usageLimit);
    }

    public boolean canBeUsedForOrder(Double orderAmount) {
        return isActive() && 
               (minimumOrderAmount == null || orderAmount >= minimumOrderAmount);
    }
}
