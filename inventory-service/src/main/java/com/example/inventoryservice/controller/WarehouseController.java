package com.example.inventoryservice.controller;

import com.example.inventoryservice.request.WarehouseRequest;
import com.example.inventoryservice.response.ApiResponse;
import com.example.inventoryservice.response.PageResponse;
import com.example.inventoryservice.response.WarehouseResponse;
import com.example.inventoryservice.service.inteface.IWarehouseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouses")
@Tag(name = "Warehouse Controller")
@RequiredArgsConstructor
public class WarehouseController {

    private final IWarehouseService warehouseService;

    @PostMapping("/{storeId}")
    public ApiResponse<WarehouseResponse> createWarehouse(
            @RequestBody WarehouseRequest request,
            @PathVariable String storeId
    ) {
        return ApiResponse.<WarehouseResponse>builder()
                .status(200)
                .message("Tạo kho thành công")
                .data(warehouseService.createWarehouse(request, storeId))
                .build();
    }

    @PutMapping("/{storeId}/{warehouseId}")
    public ApiResponse<WarehouseResponse> updateWarehouse(
            @PathVariable String storeId,
            @PathVariable String warehouseId,
            @RequestBody WarehouseRequest request
    ) {
        return ApiResponse.<WarehouseResponse>builder()
                .status(200)
                .message("Cập nhật kho thành công")
                .data(warehouseService.updateWarehouse(storeId, warehouseId, request))
                .build();
    }

    @DeleteMapping("/{warehouseId}")
    public ApiResponse<Void> deleteWarehouse(@PathVariable String warehouseId) {
        warehouseService.deleteWarehouse(warehouseId);
        return ApiResponse.<Void>builder()
                .status(200)
                .message("Xóa kho thành công")
                .build();
    }

    @PatchMapping("/{warehouseId}/disable")
    public ApiResponse<Void> disableWarehouse(@PathVariable String warehouseId) {
        warehouseService.disableWarehouse(warehouseId);
        return ApiResponse.<Void>builder()
                .status(200)
                .message("Cập nhật trạng thái kho thành công")
                .build();
    }

    @GetMapping
    public ApiResponse<List<WarehouseResponse>> getWarehouses() {
        return ApiResponse.<List<WarehouseResponse>>builder()
                .status(200)
                .message("Lấy danh sách kho thành công")
                .data(warehouseService.getWarehouses())
                .build();
    }

    @GetMapping("/{warehouseId}")
    public ApiResponse<WarehouseResponse> getWarehouseById(@PathVariable String warehouseId) {
        return ApiResponse.<WarehouseResponse>builder()
                .status(200)
                .message("Lấy thông tin kho thành công")
                .data(warehouseService.getWarehouseById(warehouseId))
                .build();
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<WarehouseResponse>> searchWarehouses(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<WarehouseResponse>>builder()
                .status(200)
                .message("Tìm kiếm kho thành công")
                .data(warehouseService.searchWarehouse(keyword, page, size))
                .build();
    }
}
