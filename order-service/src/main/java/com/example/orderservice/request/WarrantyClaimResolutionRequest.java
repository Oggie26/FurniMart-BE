package com.example.orderservice.request;

import com.example.orderservice.enums.WarrantyActionType;
import jakarta.validation.constraints.DecimalMin;
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
    @DecimalMin(value = "0.01", message = "Repair cost must be greater than 0", inclusive = false)
    private Double repairCost;

    // For RETURN action
    @DecimalMin(value = "0.01", message = "Refund amount must be greater than 0", inclusive = false)
    private Double refundAmount;
}
