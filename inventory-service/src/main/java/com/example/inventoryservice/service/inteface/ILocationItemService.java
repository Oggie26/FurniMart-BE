package com.example.inventoryservice.service.inteface;

import com.example.inventoryservice.request.LocationItemRequest;
import com.example.inventoryservice.response.LocationItemResponse;
import com.example.inventoryservice.response.PageResponse;

import java.util.List;

public interface ILocationItemService {
    LocationItemResponse createLocationItem(LocationItemRequest locationItemRequest);
    LocationItemResponse updateLocationItem(LocationItemRequest locationItemRequest, String locationItemId);
    void deleteLocationItem(String locationItemId);
    void disableLocationItem(String locationItemId);
    LocationItemResponse getLocationItemById(String locationItemId);
    List<LocationItemResponse> getLocationItemsByZoneId(String zoneId);
    PageResponse<LocationItemResponse> searchLocationItem(String request, int page, int size);
}
