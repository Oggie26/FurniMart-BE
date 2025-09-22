package com.example.inventoryservice.controller;

import com.example.inventoryservice.request.ZoneRequest;
import com.example.inventoryservice.response.ApiResponse;
import com.example.inventoryservice.response.PageResponse;
import com.example.inventoryservice.response.ZoneResponse;
import com.example.inventoryservice.service.inteface.ZoneService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/zones")
@RequiredArgsConstructor
@Tag(name = "Zone Controller")
public class ZoneController {

    private final ZoneService zoneService;

    @PostMapping
    public ApiResponse<ZoneResponse> createZone(@RequestBody ZoneRequest request) {
        return ApiResponse.<ZoneResponse>builder()
                .status(200)
                .message("Tạo khu vực thành công")
                .data(zoneService.createZone(request))
                .build();
    }

    @PutMapping("/{zoneId}")
    public ApiResponse<ZoneResponse> updateZone(
            @PathVariable String zoneId,
            @RequestBody ZoneRequest request
    ) {
        return ApiResponse.<ZoneResponse>builder()
                .status(200)
                .message("Cập nhật khu vực thành công")
                .data(zoneService.updateZone(request, zoneId))
                .build();
    }

    @DeleteMapping("/{zoneId}")
    public ApiResponse<Void> deleteZone(@PathVariable String zoneId) {
        zoneService.deleteZone(zoneId);
        return ApiResponse.<Void>builder()
                .status(200)
                .message("Xóa khu vực thành công")
                .build();
    }

    @PatchMapping("/{zoneId}/disable")
    public ApiResponse<Void> disableZone(@PathVariable String zoneId) {
        zoneService.disableZone(zoneId);
        return ApiResponse.<Void>builder()
                .status(200)
                .message("Cập nhật trạng thái khu vực thành công")
                .build();
    }

    @GetMapping("/{zoneId}")
    public ApiResponse<ZoneResponse> getZoneById(@PathVariable String zoneId) {
        return ApiResponse.<ZoneResponse>builder()
                .status(200)
                .message("Lấy thông tin khu vực thành công")
                .data(zoneService.getZoneById(zoneId))
                .build();
    }

    @GetMapping("/warehouse/{warehouseId}")
    public ApiResponse<List<ZoneResponse>> getZonesByWarehouseId(@PathVariable String warehouseId) {
        return ApiResponse.<List<ZoneResponse>>builder()
                .status(200)
                .message("Lấy danh sách khu vực thành công")
                .data(zoneService.getZonesById(warehouseId))
                .build();
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<ZoneResponse>> searchZones(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<ZoneResponse>>builder()
                .status(200)
                .message("Tìm kiếm khu vực thành công")
                .data(zoneService.searchZone(keyword, page, size))
                .build();
    }
}
