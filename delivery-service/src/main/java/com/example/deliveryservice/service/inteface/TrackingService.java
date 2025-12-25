package com.example.deliveryservice.service.inteface;

import com.example.deliveryservice.entity.DriverLocation;
import com.example.deliveryservice.request.UpdateLocationRequest;

public interface TrackingService {
    DriverLocation updateLocation(UpdateLocationRequest request);
    DriverLocation getLocation(Long orderId);
}
