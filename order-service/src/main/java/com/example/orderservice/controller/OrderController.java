package com.example.orderservice.controller;

import com.example.orderservice.enums.EnumProcessOrder;
import com.example.orderservice.enums.PaymentMethod;
import com.example.orderservice.response.ApiResponse;
import com.example.orderservice.response.OrderResponse;
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
@RequestMapping("/orders")
@Tag(name = "Order Controller")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final VNPayService vnPayService;

//    @GetMapping("/history-order")
//    public ApiResponse<OrderPageResponse> getHistoryOrder(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
//        orderService.removeUnpaidVnpayOrders();
//        return ApiResponse.<OrderPageResponse>builder()
//                .status(HttpStatus.OK.value())
//                .message("Lấy danh sách mua hàng thành công")
//                .result(orderService.getOrdersByCustomer(page, size))
//                .build();
//    }

    @PostMapping("/checkout")
    public ApiResponse<Void> checkout(@RequestParam("addressId") Long addressId, @RequestParam("cartId") Long cartId, @RequestParam(required = false) String voucherCode,
                                      @RequestParam("paymentMethod") PaymentMethod paymentMethod, HttpServletRequest request) throws UnsupportedEncodingException {
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

//    @GetMapping("/payment-callback")
//    public ApiResponse<String> handlePaymentCallback(@RequestParam Map<String, String> params) throws UnsupportedEncodingException {
//        String orderId = params.get("orderId");
//        String responseCode = params.get("vnp_ResponseCode");
//        boolean isPaid = "00" .equals(responseCode);
//
//        orderService.updateOrderStatus(Long.parseLong(orderId), );
//
//        if (isPaid) {
//            return ApiResponse.<String>builder()
//                    .status(HttpStatus.OK.value())
//                    .message("Thanh toán thành công")
//                    .build();
//        } else {
//            orderService.deleteOrder(Long.parseLong(orderId));
//            return ApiResponse.<String>builder()
//                    .status(HttpStatus.BAD_REQUEST.value())
//                    .message("Thanh toán thất bại")
//                    .build();
//        }
//    }

    private String getClientIp(HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }
        return clientIp;
    }

}

