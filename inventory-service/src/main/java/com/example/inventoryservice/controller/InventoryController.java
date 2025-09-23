//package com.example.inventoryservice.controller;
//
//import com.example.inventoryservice.response.ApiResponse;
//import com.example.inventoryservice.response.InventoryResponse;
//import com.example.inventoryservice.service.inteface.InventoryService;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/inventories")
//@RequiredArgsConstructor
//@Tag(name = "Inventory Controller")
//public class InventoryController {
//
//    private final InventoryService inventoryService;
//
//    @PostMapping
//    public ApiResponse<InventoryResponse> upsertInventory(
//            @RequestParam String productId,
//            @RequestParam String locationItemId,
//            @RequestParam int quantity,
//            @RequestParam int minQuantity,
//            @RequestParam int maxQuantity
//    ) {
//        return ApiResponse.<InventoryResponse>builder()
//                .status(200)
//                .message("Cập nhật hoặc tạo inventory thành công")
//                .data(inventoryService.upsertInventory(productId, locationItemId, quantity, minQuantity, maxQuantity))
//                .build();
//    }
//
//    @GetMapping("/product/{productId}")
//    public ApiResponse<List<InventoryResponse>> getInventoryByProduct(@PathVariable String productId) {
//        return ApiResponse.<List<InventoryResponse>>builder()
//                .status(200)
//                .message("Lấy danh sách inventory theo sản phẩm thành công")
//                .data(inventoryService.getInventoryByProduct(productId))
//                .build();
//    }
//
//    @PatchMapping("/{productId}/{locationItemId}/increase")
//    public ApiResponse<InventoryResponse> increaseStock(
//            @PathVariable String productId,
//            @PathVariable String locationItemId,
//            @RequestParam int amount
//    ) {
//        return ApiResponse.<InventoryResponse>builder()
//                .status(200)
//                .message("Tăng tồn kho thành công")
//                .data(inventoryService.increaseStock(productId, locationItemId, amount))
//                .build();
//    }
//
//    @PatchMapping("/{productId}/{locationItemId}/decrease")
//    public ApiResponse<InventoryResponse> decreaseStock(
//            @PathVariable String productId,
//            @PathVariable String locationItemId,
//            @RequestParam int amount
//    ) {
//        return ApiResponse.<InventoryResponse>builder()
//                .status(200)
//                .message("Giảm tồn kho thành công")
//                .data(inventoryService.decreaseStock(productId, locationItemId, amount))
//                .build();
//    }
//
//    @GetMapping("/{productId}/{locationItemId}/check-stock")
//    public ApiResponse<Boolean> hasSufficientStock(
//            @PathVariable String productId,
//            @PathVariable String locationItemId,
//            @RequestParam int requiredQty
//    ) {
//        return ApiResponse.<Boolean>builder()
//                .status(200)
//                .message("Kiểm tra tồn kho cục bộ thành công")
//                .data(inventoryService.hasSufficientStock(productId, locationItemId, requiredQty))
//                .build();
//    }
//
//    @GetMapping("/{productId}/check-global-stock")
//    public ApiResponse<Boolean> hasSufficientGlobalStock(
//            @PathVariable String productId,
//            @RequestParam int requiredQty
//    ) {
//        return ApiResponse.<Boolean>builder()
//                .status(200)
//                .message("Kiểm tra tồn kho toàn cục thành công")
//                .data(inventoryService.hasSufficientGlobalStock(productId, requiredQty))
//                .build();
//    }
//}
