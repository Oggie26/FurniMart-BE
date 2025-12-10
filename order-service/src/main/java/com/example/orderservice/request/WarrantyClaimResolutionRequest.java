package com.example.orderservice.request;

import com.example.orderservice.enums.WarrantyActionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarrantyClaimResolutionRequest {
    @NotNull(message = "Claim ID cannot be null")
    private Long claimId;

    @NotNull(message = "Action type cannot be null")
    private WarrantyActionType actionType;

    private String adminResponse;
    private String resolutionNotes;

    // For REPAIR action
    private Double repairCost;

    // For RETURN action
    private Double refundAmount;
}
