package com.example.orderservice.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarrantyClaimDetailResponse {
    private Long id;
    private Long warrantyId;
    private Integer quantity;
    private String issueDescription;
    private List<String> customerPhotos;
    private String productColorId; // Helper
}
