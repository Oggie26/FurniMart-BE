package com.example.deliveryservice.controller;

import com.example.deliveryservice.request.UpdateLocationRequest;
import com.example.deliveryservice.response.ApiResponse;
import com.example.deliveryservice.response.DriverLocationResponse;
import com.example.deliveryservice.service.DriverLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/driver-location")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Driver Location Controller", description = "APIs for real-time driver location tracking")
public class DriverLocationController {

    private final DriverLocationService driverLocationService;

    @PostMapping("/update")
    @Operation(summary = "Update driver location", description = "Update the current GPS location of a delivery driver for real-time tracking. "
            +
            "This endpoint should be called periodically (e.g., every 3-5 seconds) by the mobile app.")
    public ApiResponse<DriverLocationResponse> updateLocation(
            @Valid @RequestBody UpdateLocationRequest request) {
        log.info("📍 Received location update for driver: {} on order: {}",
                request.getDriverId(), request.getOrderId());

        DriverLocationResponse response = driverLocationService.updateLocation(request);

        return ApiResponse.<DriverLocationResponse>builder()
                .status(200)
                .message("Cập nhật vị trí thành công")
                .data(response)
                .build();
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get driver location by order ID", description = "Retrieve the current GPS location of the driver assigned to a specific order. "
            +
            "Used for real-time tracking on customer's mobile app or admin dashboard.")
    public ApiResponse<DriverLocationResponse> getLocationByOrderId(
            @Parameter(description = "Order ID", required = true, example = "12345") @PathVariable Long orderId) {
        log.info("🔍 Fetching location for order: {}", orderId);

        DriverLocationResponse response = driverLocationService.getLocationByOrderId(orderId);

        return ApiResponse.<DriverLocationResponse>builder()
                .status(200)
                .message("Lấy vị trí shipper thành công")
                .data(response)
                .build();
    }

    @GetMapping("/driver/{driverId}")
    @Operation(summary = "Get driver location by driver ID", description = "Retrieve the current GPS location of a specific driver. "
            +
            "Useful for fleet management and monitoring driver status.")
    public ApiResponse<DriverLocationResponse> getLocationByDriverId(
            @Parameter(description = "Driver ID (Employee ID)", required = true, example = "DRV001") @PathVariable String driverId) {
        log.info("🔍 Fetching location for driver: {}", driverId);

        DriverLocationResponse response = driverLocationService.getLocationByDriverId(driverId);

        return ApiResponse.<DriverLocationResponse>builder()
                .status(200)
                .message("Lấy vị trí shipper thành công")
                .data(response)
                .build();
    }
}
