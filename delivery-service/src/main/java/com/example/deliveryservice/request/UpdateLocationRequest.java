package com.example.deliveryservice.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateLocationRequest {
    @NotNull(message = "Order ID không được để trống")
    private Long orderId;

    @NotBlank(message = "Driver ID không được để trống")
    private String driverId;

    @NotNull(message = "Latitude không được để trống")
    private Double lat;

    @NotNull(message = "Longitude không được để trống")
    private Double lng;
}
