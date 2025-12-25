package com.example.deliveryservice.repository;

import com.example.deliveryservice.entity.DriverLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DriverLocationRepository extends JpaRepository<DriverLocation, Long> {
    Optional<DriverLocation> findByOrderId(Long orderId);

    Optional<DriverLocation> findByDriverId(String driverId);
}
