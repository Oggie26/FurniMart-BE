package com.example.orderservice.response;

import com.example.orderservice.enums.WarrantyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarrantyResponse {

    private Long id;
    private Long orderId;
    private Long orderDetailId;
    private String productColorId;
    private String customerId;
    private LocalDateTime deliveryDate;
    private LocalDateTime warrantyStartDate;
    private LocalDateTime warrantyEndDate;
    private String status; // String representation of WarrantyStatus
    private String address;
    private String storeId;
    private Integer warrantyDurationMonths;
    private String description;
    private Integer claimCount;
    private Integer maxClaims;
    private boolean isActive;
    private boolean canClaimWarranty;
    private Date createdAt;
    private Date updatedAt;
}
