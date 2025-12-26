package com.example.orderservice.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintReviewRequest {

    @NotNull(message = "isStoreError is required")
    private Boolean isStoreError;

    @NotNull(message = "approved is required")
    private Boolean approved;

    private String reviewNotes; // Optional: Admin review notes

    private Boolean applyPenalty; // Optional: For Trường hợp 2 + VNPAY (penalty deduction)
}

