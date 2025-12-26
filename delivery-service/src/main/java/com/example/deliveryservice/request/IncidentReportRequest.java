package com.example.deliveryservice.request;

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
public class IncidentReportRequest {

    @NotBlank(message = "Reason is required")
    private String reason;

    private List<String> incidentPhotos; // Optional: List of photo URLs

    @NotNull(message = "Customer refused status is required")
    private Boolean customerRefused;

    private Boolean customerContactable; // Optional: Default true if not provided
}

