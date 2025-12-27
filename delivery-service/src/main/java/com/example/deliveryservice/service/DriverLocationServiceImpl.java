package com.example.deliveryservice.service;

import com.example.deliveryservice.entity.DriverLocation;
import com.example.deliveryservice.enums.ErrorCode;
import com.example.deliveryservice.exception.AppException;
import com.example.deliveryservice.repository.DriverLocationRepository;
import com.example.deliveryservice.request.UpdateLocationRequest;
import com.example.deliveryservice.response.DriverLocationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverLocationServiceImpl implements DriverLocationService {

        private final DriverLocationRepository driverLocationRepository;
        private final SimpMessagingTemplate messagingTemplate;

        @Override
        @Transactional
        public DriverLocationResponse updateLocation(UpdateLocationRequest request) {

                if (request.getOrderId() == null ||
                                request.getDriverId() == null || request.getDriverId().isBlank() ||
                                request.getLat() == null || request.getLng() == null) {
                        throw new AppException(ErrorCode.INVALID_REQUEST);
                }

                DriverLocation location = driverLocationRepository
                                .findByOrderId(request.getOrderId())
                                .orElse(DriverLocation.builder()
                                                .orderId(request.getOrderId())
                                                .driverId(request.getDriverId())
                                                .build());

                LocalDateTime now = LocalDateTime.now();
                boolean shouldBroadcast = true;

                if (location.getUpdatedAt() != null) {
                        boolean timeThresholdMet = location.getUpdatedAt().plusSeconds(2).isBefore(now);

                        double distance = calculateDistanceInMeters(
                                        location.getLatitude(), location.getLongitude(),
                                        request.getLat(), request.getLng());
                        boolean distanceThresholdMet = distance > 5;

                        shouldBroadcast = timeThresholdMet && distanceThresholdMet;
                }

                location.setLatitude(request.getLat());
                location.setLongitude(request.getLng());
                location.setUpdatedAt(now);

                DriverLocation savedLocation = driverLocationRepository.save(location);

                if (shouldBroadcast) {
                        messagingTemplate.convertAndSend(
                                        "/topic/tracking/" + savedLocation.getOrderId(),
                                        mapToResponse(savedLocation));
                }

                return mapToResponse(savedLocation);
        }

        private double calculateDistanceInMeters(Double lat1, Double lng1, Double lat2, Double lng2) {
                if (lat1 == null || lng1 == null || lat2 == null || lng2 == null)
                        return 0;
                double earthRadius = 6371000; // meters
                double dLat = Math.toRadians(lat2 - lat1);
                double dLng = Math.toRadians(lng2 - lng1);
                double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                                                Math.sin(dLng / 2) * Math.sin(dLng / 2);
                double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
                return earthRadius * c;
        }

        @Override
        public DriverLocationResponse getLocationByOrderId(Long orderId) {
                DriverLocation location = driverLocationRepository.findByOrderId(orderId)
                                .orElseThrow(() -> new AppException(ErrorCode.LOCATION_NOT_FOUND));
                return mapToResponse(location);
        }

        @Override
        public DriverLocationResponse getLocationByDriverId(String driverId) {
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
