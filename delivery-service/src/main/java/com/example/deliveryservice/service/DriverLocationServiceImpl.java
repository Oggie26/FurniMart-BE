package com.example.deliveryservice.service;

import com.example.deliveryservice.entity.DriverLocation;
import com.example.deliveryservice.enums.ErrorCode;
import com.example.deliveryservice.exception.AppException;
import com.example.deliveryservice.repository.DriverLocationRepository;
import com.example.deliveryservice.request.UpdateLocationRequest;
import com.example.deliveryservice.response.DriverLocationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverLocationServiceImpl implements DriverLocationService {

    private final DriverLocationRepository driverLocationRepository;

    @Override
    @Transactional
    public DriverLocationResponse updateLocation(UpdateLocationRequest request) {
        log.info("Updating location for driver: {} on order: {}", request.getDriverId(), request.getOrderId());

        // Validate request
        if (request.getOrderId() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        if (request.getDriverId() == null || request.getDriverId().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        if (request.getLat() == null || request.getLng() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // Find existing location or create new one
        DriverLocation location = driverLocationRepository.findByOrderId(request.getOrderId())
                .orElse(DriverLocation.builder()
                        .id(request.getOrderId()) // Use orderId as primary key
                        .orderId(request.getOrderId())
                        .driverId(request.getDriverId())
                        .build());

        // Update location data
        location.setLatitude(request.getLat());
        location.setLongitude(request.getLng());
        location.setUpdatedAt(LocalDateTime.now());

        // Save to database
        DriverLocation savedLocation = driverLocationRepository.save(location);

        log.info("Location updated successfully for order: {} at ({}, {})",
                request.getOrderId(), request.getLat(), request.getLng());

        return mapToResponse(savedLocation);
    }

    @Override
    public DriverLocationResponse getLocationByOrderId(Long orderId) {
        log.info("Fetching location for order: {}", orderId);

        DriverLocation location = driverLocationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.LOCATION_NOT_FOUND));

        return mapToResponse(location);
    }

    @Override
    public DriverLocationResponse getLocationByDriverId(String driverId) {
        log.info("Fetching location for driver: {}", driverId);

        DriverLocation location = driverLocationRepository.findByDriverId(driverId)
                .orElseThrow(() -> new AppException(ErrorCode.LOCATION_NOT_FOUND));

        return mapToResponse(location);
    }

    private DriverLocationResponse mapToResponse(DriverLocation location) {
        return DriverLocationResponse.builder()
                .id(location.getId())
                .driverId(location.getDriverId())
                .orderId(location.getOrderId())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .updatedAt(location.getUpdatedAt())
                .build();
    }
}
