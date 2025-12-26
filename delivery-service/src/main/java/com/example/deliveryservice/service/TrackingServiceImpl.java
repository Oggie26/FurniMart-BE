package com.example.deliveryservice.service;

import com.example.deliveryservice.entity.DriverLocation;
import com.example.deliveryservice.repository.DriverLocationRepository;
import com.example.deliveryservice.request.UpdateLocationRequest;
import com.example.deliveryservice.service.inteface.TrackingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TrackingServiceImpl implements TrackingService {

    private final DriverLocationRepository driverLocationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public DriverLocation updateLocation(UpdateLocationRequest request) {

        if (request.getLat() == null || request.getLng() == null) {
            throw new IllegalArgumentException("Invalid location data");
        }

        DriverLocation location = DriverLocation.builder()
                .orderId(request.getOrderId())
                .driverId(request.getDriverId())
                .latitude(request.getLat())
                .longitude(request.getLng())
                .updatedAt(LocalDateTime.now())
                .build();

        DriverLocation saved = driverLocationRepository.save(location);

        messagingTemplate.convertAndSend(
                "/topic/tracking/" + saved.getOrderId(),
                saved
        );

        return saved;
    }

    @Override
    public DriverLocation getLocation(Long orderId) {
        return driverLocationRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Location not found for order " + orderId)
                );
    }
}
