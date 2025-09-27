package com.example.orderservice.controller;

import com.example.orderservice.response.ApiResponse;
import com.example.orderservice.response.CartResponse;
import com.example.orderservice.service.inteface.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carts")
@Tag(name = "Cart Controller")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/add")
    @Operation(summary = "Thêm sản phẩm vào giỏ hàng")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> addProductToCart(
            @RequestParam String productId,
            @RequestParam Integer quantity,
            @RequestParam  String colorId) {
        cartService.addProductToCart(productId, quantity, colorId);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Thêm sản phẩm vào giỏ hàng thành công")
                .build();
    }

    @DeleteMapping("/remove")
    @Operation(summary = "Xoá sản phẩm khỏi giỏ hàng")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> removeProductFromCart(
            @RequestParam List<String> productIds) {
        cartService.removeProductFromCart(productIds);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Xoá sản phẩm khỏi giỏ hàng thành công")
                .build();
    }

    @GetMapping
    @Operation(summary = "Lấy giỏ hàng của người dùng hiện tại")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<CartResponse> getCart() {
        return ApiResponse.<CartResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy giỏ hàng thành công")
                .data(cartService.getCart())
                .build();
    }

    @PatchMapping("/update")
    @Operation(summary = "Cập nhật số lượng sản phẩm trong giỏ hàng")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> updateProductQuantity(
            @RequestParam String  productId,
            @RequestParam Integer quantity,
            @RequestParam String  colorId) {
        cartService.updateProductQuantityInCart(productId,colorId, quantity );
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Cập nhật số lượng sản phẩm thành công")
                .build();
    }

    @DeleteMapping("/remove/{productId}/color/{colorId}")
    @Operation(summary = "Xoá 1 sản phẩm khỏi giỏ hàng")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> deleteProductFromCart(@PathVariable String productId, @PathVariable String colorId) {
        cartService.deleteProductFromCart(productId,colorId);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Xoá sản phẩm khỏi giỏ hàng thành công")
                .build();
    }
}
