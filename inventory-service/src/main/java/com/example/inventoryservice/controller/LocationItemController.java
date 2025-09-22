package com.example.inventoryservice.controller;

import com.example.inventoryservice.request.LocationItemRequest;
import com.example.inventoryservice.response.ApiResponse;
import com.example.inventoryservice.response.LocationItemResponse;
import com.example.inventoryservice.response.PageResponse;
import com.example.inventoryservice.service.inteface.LocationItemService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/location-items")
@RequiredArgsConstructor
@Tag(name = "Location Item Controller")
public class LocationItemController {

    private final LocationItemService locationItemService;

    @PostMapping
    public ApiResponse<LocationItemResponse> createLocationItem(@RequestBody LocationItemRequest request) {
        return ApiResponse.<LocationItemResponse>builder()
                .status(200)
                .message("Tạo vị trí chứa hàng thành công")
                .data(locationItemService.createLocationItem(request))
                .build();
    }

    @PutMapping("/{locationItemId}")
    public ApiResponse<LocationItemResponse> updateLocationItem(
            @PathVariable String locationItemId,
            @RequestBody LocationItemRequest request
    ) {
        return ApiResponse.<LocationItemResponse>builder()
                .status(200)
                .message("Cập nhật vị trí chứa hàng thành công")
                .data(locationItemService.updateLocationItem(request, locationItemId))
                .build();
    }

    @DeleteMapping("/{locationItemId}")
    public ApiResponse<Void> deleteLocationItem(@PathVariable String locationItemId) {
        locationItemService.deleteLocationItem(locationItemId);
        return ApiResponse.<Void>builder()
                .status(200)
                .message("Xóa vị trí chứa hàng thành công")
                .build();
    }

    @PatchMapping("/{locationItemId}/disable")
    public ApiResponse<Void> disableLocationItem(@PathVariable String locationItemId) {
        locationItemService.disableLocationItem(locationItemId);
        return ApiResponse.<Void>builder()
                .status(200)
                .message("Cập nhật trạng thái vị trí chứa hàng thành công")
                .build();
    }

    @GetMapping("/{locationItemId}")
    public ApiResponse<LocationItemResponse> getLocationItemById(@PathVariable String locationItemId) {
        return ApiResponse.<LocationItemResponse>builder()
                .status(200)
                .message("Lấy thông tin vị trí chứa hàng thành công")
                .data(locationItemService.getLocationItemById(locationItemId))
                .build();
    }

    @GetMapping("/zone/{zoneId}")
    public ApiResponse<List<LocationItemResponse>> getLocationItemsByZoneId(@PathVariable String zoneId) {
        return ApiResponse.<List<LocationItemResponse>>builder()
                .status(200)
                .message("Lấy danh sách vị trí chứa hàng theo khu vực thành công")
                .data(locationItemService.getLocationItemsByZoneId(zoneId))
                .build();
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<LocationItemResponse>> searchLocationItems(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<LocationItemResponse>>builder()
                .status(200)
                .message("Tìm kiếm vị trí chứa hàng thành công")
                .data(locationItemService.searchLocationItem(keyword, page, size))
                .build();
    }
}
