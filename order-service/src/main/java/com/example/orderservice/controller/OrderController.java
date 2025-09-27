package com.example.orderservice.controller;

import com.example.orderservice.enums.EnumProcessOrder;
import com.example.orderservice.enums.PaymentMethod;
import com.example.orderservice.response.ApiResponse;
import com.example.orderservice.response.OrderResponse;
import com.example.orderservice.response.PageResponse;
import com.example.orderservice.service.VNPayService;
import com.example.orderservice.service.inteface.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order Controller")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OrderController {

    OrderService orderService;
    VNPayService vnPayService;

    // ================== Checkout ==================
    @PostMapping("/checkout")
    public ApiResponse<Void> checkout(
            @RequestParam Long addressId,
            @RequestParam Long cartId,
            @RequestParam(required = false) String voucherCode,
            @RequestParam PaymentMethod paymentMethod,
            HttpServletRequest request
    ) throws UnsupportedEncodingException {
        String clientIp = getClientIp(request);

        if (paymentMethod == PaymentMethod.VNPAY) {
            OrderResponse orderResponse = orderService.createOrder(cartId, addressId, paymentMethod, voucherCode);
            return ApiResponse.<Void>builder()
                    .status(HttpStatus.OK.value())
                    .message("Chuyển hướng sang VNPay")
                    .redirectUrl(vnPayService.createPaymentUrl(orderResponse.getId(), orderResponse.getTotal(), clientIp))
                    .build();
        } else {
            orderService.createOrder(cartId, addressId, paymentMethod, voucherCode);
            return ApiResponse.<Void>builder()
                    .status(HttpStatus.OK.value())
                    .message("Đặt hàng thành công")
                    .build();
        }
    }

    // ================== Get Order ==================
    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getOrderById(@PathVariable Long id) {
        return ApiResponse.<OrderResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy đơn hàng thành công")
                .data(orderService.getOrderById(id))
                .build();
    }

    // ================== Search Order ==================
    @GetMapping("/search/customer")
    public ApiResponse<PageResponse<OrderResponse>> searchOrderByCustomer(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<OrderResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Tìm kiếm đơn hàng của khách hàng thành công")
                .data(orderService.searchOrderByCustomer(keyword, page, size))
                .build();
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<OrderResponse>> searchOrder(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<OrderResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Tìm kiếm đơn hàng thành công")
                .data(orderService.searchOrder(keyword, page, size))
                .build();
    }

    @GetMapping("/search/store/{storeId}")
    public ApiResponse<PageResponse<OrderResponse>> searchOrderByStore(
            @PathVariable String storeId,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<OrderResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Tìm kiếm đơn hàng theo cửa hàng thành công")
                .data(orderService.searchOrderByStoreId(keyword, page, size, storeId))
                .build();
    }

    // ================== Update Order Status ==================
    @PutMapping("/{id}/status")
    public ApiResponse<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam EnumProcessOrder status
    ) {
        return ApiResponse.<OrderResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Cập nhật trạng thái đơn hàng thành công")
                .data(orderService.updateOrderStatus(id, status))
                .build();
    }

    // ================== VNPay Callback ==================
    @GetMapping("/payment-callback")
    public ApiResponse<String> handlePaymentCallback(@RequestParam Map<String, String> params) {
        String orderId = params.get("orderId");
        String responseCode = params.get("vnp_ResponseCode");
        boolean isPaid = "00".equals(responseCode);

        if (isPaid) {
            orderService.updateOrderStatus(Long.parseLong(orderId), EnumProcessOrder.PAYMENT);
            return ApiResponse.<String>builder()
                    .status(HttpStatus.OK.value())
                    .message("Thanh toán thành công")
                    .build();
        } else {
            return ApiResponse.<String>builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("Thanh toán thất bại")
                    .build();
        }
    }

    // ================== Helper ==================
    private String getClientIp(HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        return (clientIp == null || clientIp.isEmpty()) ? request.getRemoteAddr() : clientIp;
    }
}
