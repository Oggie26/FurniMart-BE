package com.example.productservice.controller;

import com.example.productservice.request.ProductColorRequest;
import com.example.productservice.response.ApiResponse;
import com.example.productservice.response.PageResponse;
import com.example.productservice.response.ProductColorResponse;
import com.example.productservice.service.inteface.ProductColorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-colors")
@Tag(name = "Product Color Controller")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class ProductColorController {

    private final ProductColorService productColorService;

    @PostMapping
    @Operation(summary = "Tạo mới ProductColor")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductColorResponse> addProductColor(
            @RequestBody @Valid ProductColorRequest request) {
        return ApiResponse.<ProductColorResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Tạo ProductColor thành công")
                .data(productColorService.addProductColor(request))
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật ProductColor theo ID")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<ProductColorResponse> updateProductColor(
            @PathVariable String id,
            @Valid @RequestBody ProductColorRequest request) {
        return ApiResponse.<ProductColorResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Cập nhật ProductColor thành công")
                .data(productColorService.updateProductColor(request, id))
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xoá ProductColor (xóa cứng)")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> deleteProductColor(@PathVariable String id) {
        productColorService.deleteProductColor(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Xoá ProductColor thành công")
                .build();
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Vô hiệu hoá / kích hoạt ProductColor")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> disableProductColor(@PathVariable String id) {
        productColorService.disableProductColor(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Cập nhật trạng thái ProductColor thành công")
                .build();
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả ProductColor")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<ProductColorResponse>> getProductColors() {
        return ApiResponse.<List<ProductColorResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy danh sách ProductColor thành công")
                .data(productColorService.getProductColors())
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết ProductColor theo ID")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<ProductColorResponse> getProductColor(@PathVariable String id) {
        return ApiResponse.<ProductColorResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy ProductColor thành công")
                .data(productColorService.getProductColor(id))
                .build();
    }

//    // ✅ Tìm kiếm ProductColor có phân trang
//    @GetMapping("/search")
//    @Operation(summary = "Tìm kiếm ProductColor theo nhiều tiêu chí")
//    @ResponseStatus(HttpStatus.OK)
//    public ApiResponse<PageResponse<ProductColorResponse>> searchProductColors(
//            @RequestParam(required = false) String request,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//
//        PageResponse<ProductColorResponse> response = productColorService.searchProductColorResponse(request, page, size);
//        return ApiResponse.<PageResponse<ProductColorResponse>>builder()
//                .status(HttpStatus.OK.value())
//                .message("Tìm kiếm ProductColor thành công")
//                .data(response)
//                .build();
//    }
}
