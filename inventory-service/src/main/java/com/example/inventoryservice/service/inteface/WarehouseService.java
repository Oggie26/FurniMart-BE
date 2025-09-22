package com.example.inventoryservice.service.inteface;

import com.example.inventoryservice.request.WarehouseRequest;
import com.example.inventoryservice.response.PageResponse;
import com.example.inventoryservice.response.WarehouseResponse;

import java.util.List;

public interface WarehouseService {
    WarehouseResponse createWarehouse(WarehouseRequest warehouseRequest, String storeId);
    WarehouseResponse updateWarehouse(String storeId,String warehouseId,WarehouseRequest warehouseRequest);
    void deleteWarehouse(String warehouseId);
    void disableWarehouse(String warehouseId);
    List<WarehouseResponse> getWarehouses();
    WarehouseResponse getWarehouseById(String warehouseId);
    PageResponse<WarehouseResponse> searchWarehouse(String request, int page, int size);

}
