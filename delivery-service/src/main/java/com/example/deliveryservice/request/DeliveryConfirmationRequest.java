package com.example.deliveryservice.request;

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
public class DeliveryConfirmationRequest {

    @NotNull(message = "Order ID cannot be null")
    private Long orderId;

    private List<String> deliveryPhotos; // List of photo URLs

    private String deliveryNotes;

}


