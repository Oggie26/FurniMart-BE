package com.example.deliveryservice.service;

import com.example.deliveryservice.request.UpdateLocationRequest;
import com.example.deliveryservice.response.DriverLocationResponse;

public interface DriverLocationService {
    DriverLocationResponse updateLocation(UpdateLocationRequest request);
    DriverLocationResponse getLocationByOrderId(Long orderId);
    DriverLocationResponse getLocationByDriverId(String driverId);
}
