package com.example.inventoryservice.service.inteface;

import com.example.inventoryservice.request.ZoneRequest;
import com.example.inventoryservice.response.PageResponse;
import com.example.inventoryservice.response.ZoneResponse;

import java.util.List;

public interface ZoneService {
    ZoneResponse createZone(ZoneRequest zoneRequest);
    ZoneResponse updateZone(ZoneRequest zoneRequest, String zoneId);
    void deleteZone(String zoneId);
    void disableZone(String zoneId);
    ZoneResponse getZoneById(String zoneId);
    List<ZoneResponse> getZonesById(String warehouseId);
    PageResponse<ZoneResponse> searchZone(String request, int page, int size);

}
