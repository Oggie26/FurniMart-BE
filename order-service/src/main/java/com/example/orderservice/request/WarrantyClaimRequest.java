package com.example.orderservice.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarrantyClaimRequest {
    
    @NotNull(message = "Warranty ID cannot be null")
    private Long warrantyId;
    
    @NotBlank(message = "Issue description cannot be blank")
    private String issueDescription;
    
    private Long addressId; // Optional: if null, will use address from original order
    
    private List<String> customerPhotos; // List of photo URLs
}
