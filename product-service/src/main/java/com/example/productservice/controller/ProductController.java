package com.example.productservice.controller;

import com.example.productservice.entity.Color;
import com.example.productservice.entity.Product;
import com.example.productservice.enums.ErrorCode;
import com.example.productservice.exception.AppException;
import com.example.productservice.repository.ColorRepository;
import com.example.productservice.request.ProductRequest;
import com.example.productservice.response.ApiResponse;
import com.example.productservice.response.ColorResponse;
import com.example.productservice.response.PageResponse;
import com.example.productservice.response.ProductResponse;
import com.example.productservice.service.inteface.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.awt.*;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product Controller")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ColorRepository colorRepository;

    @PostMapping()
    public ApiResponse<ProductResponse> createProduct(
            @RequestBody @Valid ProductRequest productRequest) {
        return ApiResponse.<ProductResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Tạo sản phẩm thành công")
                .data(productService.createProduct(productRequest))
                .build();

    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật sản phẩm")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<ProductResponse> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductRequest request) {
        return ApiResponse.<ProductResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Cập nhật sản phẩm thành công")
                .data(productService.updateProduct(request, id))
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xoá mềm sản phẩm (isDeleted)")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Xoá sản phẩm thành công")
                .build();
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Vô hiệu hoá / kích hoạt sản phẩm")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> disableProduct(@PathVariable String id) {
        productService.disableProduct(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Cập nhật trạng thái sản phẩm thành công")
                .build();
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả sản phẩm")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<ProductResponse>> getProducts() {
        return ApiResponse.<List<ProductResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy danh sách sản phẩm thành công")
                .data(productService.getProducts())
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết sản phẩm theo ID")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<ProductResponse> getProduct(@PathVariable String id) {
        return ApiResponse.<ProductResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy sản phẩm thành công")
                .data(productService.getProductById(id))
                .build();
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Lấy chi tiết sản phẩm theo Slug")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<ProductResponse> getProductBySlug(@RequestParam String slug) {
        return ApiResponse.<ProductResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy sản phẩm thành công")
                .data(productService.getProductBySlug(slug))
                .build();
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Lấy chi tiết sản phẩm theo Slug")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<ProductResponse>> getProductByCategoryId(@PathVariable Long categoryId) {
        return ApiResponse.<List<ProductResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy sản phẩm thành công")
                .data(productService.getProductsByCategoryId(categoryId))
                .build();
    }

    @GetMapping("/{productId}/color/{colorId}")
    @Operation(summary = "Lấy chi tiết sản phẩm theo ColorId")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<ProductResponse> getProductByColorId(@PathVariable String productId, @PathVariable String colorId) {
        return ApiResponse.<ProductResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy sản phẩm thành công")
                .data(productService.getProductByColorId(colorId,productId))
                .build();
    }

    @GetMapping("/search")
    @Operation(summary = "Tìm kiếm sản phẩm theo nhiều tiêu chí")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<PageResponse<ProductResponse>> searchProducts(
            @RequestParam(required = false) String request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<ProductResponse> products = productService.searchProduct(request, page, size);

        return ApiResponse.<PageResponse<ProductResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Tìm kiếm sản phẩm thành công")
                .data(products)
                .build();
    }

    @GetMapping("/colors/{colorId}")
    @Operation(summary = "Lấy chi tiết sản phẩm theo ColorId")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<ColorResponse> getColorById(@PathVariable String colorId) {
        Color color = colorRepository.findById(colorId)
                .orElseThrow(() -> new AppException(ErrorCode.COLOR_NOT_FOUND));
         ColorResponse colorResponse = ColorResponse.builder()
                 .id(color.getId())
                 .colorName(color.getColorName())
                 .hexCode(color.getHexCode())
                 .build();
        return ApiResponse.<ColorResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy màu thành công")
                .data(colorResponse)
                .build();
    }
}
