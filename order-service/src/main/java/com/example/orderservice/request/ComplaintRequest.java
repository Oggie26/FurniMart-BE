package com.example.orderservice.request;

import com.example.orderservice.enums.ComplaintErrorType;
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
public class ComplaintRequest {

    @NotBlank(message = "Complaint reason is required")
    private String complaintReason;

    @NotNull(message = "Error type is required")
    private ComplaintErrorType errorType;

    @NotNull(message = "Customer refused status is required")
    private Boolean customerRefused;

    private List<String> complaintEvidencePhotos; // Optional: List of photo URLs as evidence
}

