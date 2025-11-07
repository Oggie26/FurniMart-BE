package com.example.userservice.controller;

import com.example.userservice.request.FavoriteProductRequest;
import com.example.userservice.response.ApiResponse;
import com.example.userservice.response.FavoriteProductResponse;
import com.example.userservice.response.PageResponse;
import com.example.userservice.service.FavoriteProductServiceImpl;
import com.example.userservice.service.UserServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
@Tag(name = "Favorite Products Controller")
@SecurityRequirement(name = "bearerAuth")
public class FavoriteProductController {

    private final FavoriteProductServiceImpl favoriteProductService;
    private final UserServiceImpl userService;

    @PostMapping
    @Operation(summary = "Thêm sản phẩm yêu thích", description = "API thêm sản phẩm vào danh sách yêu thích")
    public ResponseEntity<ApiResponse<FavoriteProductResponse>> addFavoriteProduct(
            @RequestBody @Valid FavoriteProductRequest request) {
        String userId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.<FavoriteProductResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Thêm sản phẩm yêu thích thành công")
                .data(favoriteProductService.addFavoriteProduct(userId, request))
                .timestamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Xóa sản phẩm yêu thích", description = "API xóa sản phẩm khỏi danh sách yêu thích")
    public ResponseEntity<ApiResponse<String>> removeFavoriteProduct(@PathVariable String productId) {
        String userId = userService.getCurrentUserId();
        favoriteProductService.removeFavoriteProduct(userId, productId);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .status(HttpStatus.OK.value())
                .message("Xóa sản phẩm yêu thích thành công")
                .data("Removed successfully")
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách sản phẩm yêu thích", description = "API lấy tất cả sản phẩm yêu thích của người dùng")
    public ResponseEntity<ApiResponse<List<FavoriteProductResponse>>> getFavoriteProducts() {
        String userId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.<List<FavoriteProductResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy danh sách sản phẩm yêu thích thành công")
                .data(favoriteProductService.getFavoriteProducts(userId))
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/pagination")
    @Operation(summary = "Lấy danh sách sản phẩm yêu thích có phân trang", description = "API lấy sản phẩm yêu thích với phân trang")
    public ResponseEntity<ApiResponse<PageResponse<FavoriteProductResponse>>> getFavoriteProductsWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String userId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.<PageResponse<FavoriteProductResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy danh sách sản phẩm yêu thích thành công")
                .data(favoriteProductService.getFavoriteProductsWithPagination(userId, page, size))
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/{productId}/check")
    @Operation(summary = "Kiểm tra sản phẩm có trong yêu thích", description = "API kiểm tra sản phẩm có trong danh sách yêu thích không")
    public ResponseEntity<ApiResponse<Boolean>> isFavoriteProduct(@PathVariable String productId) {
        String userId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                .status(HttpStatus.OK.value())
                .message("Kiểm tra thành công")
                .data(favoriteProductService.isFavoriteProduct(userId, productId))
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/count")
    @Operation(summary = "Đếm số lượng sản phẩm yêu thích", description = "API đếm tổng số sản phẩm yêu thích")
    public ResponseEntity<ApiResponse<Long>> getFavoriteProductCount() {
        String userId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.<Long>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy số lượng sản phẩm yêu thích thành công")
                .data(favoriteProductService.getFavoriteProductCount(userId))
                .timestamp(LocalDateTime.now())
                .build());
    }
}

