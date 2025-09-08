package com.example.productservice.controller;

import com.example.productservice.entity.Product;
import com.example.productservice.request.ProductRequest;
import com.example.productservice.response.ApiResponse;
import com.example.productservice.response.PageResponse;
import com.example.productservice.response.ProductResponse;
import com.example.productservice.service.inteface.IProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Product Controller", description = "Quản lý sản phẩm")
public class ProductController {

    private final IProductService productService;

    @PostMapping
    @Operation(summary = "Tạo sản phẩm mới")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ApiResponse.<ProductResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Tạo sản phẩm thành công")
                .data(response)
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật sản phẩm")
    public ApiResponse<ProductResponse> updateProduct(@PathVariable String id, @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.updateProduct(request, id);
        return ApiResponse.<ProductResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Cập nhật sản phẩm thành công")
                .data(response)
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa mềm sản phẩm")
    public ApiResponse<Void> deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Xóa sản phẩm thành công")
                .build();
    }

    @PatchMapping("/disable/{id}")
    @Operation(summary = "Kích hoạt / vô hiệu hóa sản phẩm")
    public ApiResponse<Void> disableProduct(@PathVariable String id) {
        productService.disableProduct(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Cập nhật trạng thái sản phẩm thành công")
                .build();
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả sản phẩm")
    public ApiResponse<List<ProductResponse>> getProducts() {
        List<ProductResponse> list = productService.getProducts();
        return ApiResponse.<List<ProductResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy danh sách sản phẩm thành công")
                .data(list)
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin sản phẩm theo ID")
    public ApiResponse<ProductResponse> getProduct(@PathVariable String id) {
        ProductResponse response = productService.getProduct(id);
        return ApiResponse.<ProductResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy thông tin sản phẩm thành công")
                .data(response)
                .build();
    }

    @GetMapping("/search")
    @Operation(summary = "Tìm kiếm sản phẩm")
    public ApiResponse<PageResponse<ProductResponse>> searchProduct(
            @RequestParam(required = false) String request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<ProductResponse> result = productService.searchProduct(request, page, size);
        return ApiResponse.<PageResponse<ProductResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Tìm kiếm sản phẩm thành công")
                .data(result)
                .build();
    }
}
