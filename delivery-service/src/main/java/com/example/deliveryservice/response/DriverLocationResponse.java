package com.example.deliveryservice.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverLocationResponse {
    private Long id;
    private String driverId;
    private Long orderId;
    private Double latitude;
    private Double longitude;
    private LocalDateTime updatedAt;
}
