package com.example.orderservice.response;

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
    
    private Long id;
    private Long warrantyId;
    private String customerId;
    private LocalDateTime claimDate;
    private String issueDescription;
    private List<String> customerPhotos;
    private WarrantyClaimStatus status;
    private String adminResponse;
    private String resolutionNotes;
    private List<String> resolutionPhotos;
    private LocalDateTime resolvedDate;
    private String adminId;
    private Date createdAt;
    private Date updatedAt;
}
