package com.example.orderservice.response;

import com.example.orderservice.enums.WarrantyActionType;
import com.example.orderservice.enums.WarrantyClaimStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarrantyClaimResponse {

    private Long id; // claimId
    private Long orderId;
    private String customerId;
    private Long addressId;
    private AddressResponse address; // Địa chỉ đầy đủ nhận bảo hành
    private LocalDateTime claimDate;
    private List<WarrantyClaimDetailResponse> items;
    // private Long warrantyId; // Removed
    // private String issueDescription; // Removed
    // private List<String> customerPhotos; // Removed
    private WarrantyClaimStatus status;
    private String adminResponse;
    private String resolutionNotes;
    private List<String> resolutionPhotos;
    private LocalDateTime resolvedDate;
    private String adminId;
    private WarrantyActionType actionType;
    private Double repairCost; // null for customers, populated for admin
    private Double refundAmount;
    private Long warrantyOrderId; // If return order was created
    private Date createdAt;
    private Date updatedAt;
}
