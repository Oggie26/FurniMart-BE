package com.example.orderservice.controller;

import com.example.orderservice.enums.EnumProcessOrder;
import com.example.orderservice.enums.ErrorCode;
import com.example.orderservice.enums.PaymentMethod;
import com.example.orderservice.exception.AppException;
import com.example.orderservice.feign.InventoryClient;
import com.example.orderservice.response.*;
import com.example.orderservice.service.VNPayService;
import com.example.orderservice.service.ManagerWorkflowService;
import com.example.orderservice.service.inteface.AssignOrderService;
import com.example.orderservice.service.inteface.CartService;
import com.example.orderservice.service.inteface.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order Controller")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final VNPayService vnPayService;
    private final CartService cartService;
    private final AssignOrderService assignOrderService;
    private final InventoryClient inventoryClient;
    private final ManagerWorkflowService managerWorkflowService;

    @PostMapping("/checkout")
    public ApiResponse<Void> checkout(
            @RequestParam Long addressId,
            @RequestParam Long cartId,
            @RequestParam(required = false) String voucherCode,
            @RequestParam PaymentMethod paymentMethod,
            HttpServletRequest request
    ) throws UnsupportedEncodingException {
        String clientIp = getClientIp(request);

        CartResponse cartResponse = cartService.getCartById(cartId);
        List<CartItemResponse> cartItems = cartResponse.getItems();

        for (CartItemResponse item : cartItems) {
            ResponseEntity<ApiResponse<Boolean>> response =
                    inventoryClient.hasSufficientGlobalStock(item.getProductColorId(), item.getQuantity());

            boolean available = response.getBody() != null ? response.getBody().getData() : false;

            if (!available) {
                throw new AppException(ErrorCode.OUT_OF_STOCK);
            }
        }

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

    @PostMapping("/pre-order")
    public ApiResponse<OrderResponse> createPreOrder(
            @RequestParam Long addressId,
            @RequestParam Long cartId,
            @RequestParam(required = false) String voucherCode
    ) {
        OrderResponse orderResponse = orderService.createPreOrder(cartId, addressId, voucherCode);
        return ApiResponse.<OrderResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Tạo đơn hàng trước thành công")
                .data(orderResponse)
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getOrderById(@PathVariable Long id) {
        return ApiResponse.<OrderResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy đơn hàng thành công")
                .data(orderService.getOrderById(id))
                .build();
    }

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

    @PostMapping("/{orderId}/manager-decision")
    public ApiResponse<String> managerAcceptOrRejectOrder(
            @PathVariable Long orderId,
            @RequestParam(required = false) String storeId,
            @RequestParam(required = false) String reason,
            @RequestParam EnumProcessOrder status
    ) {
        assignOrderService.acceptRejectOrderByManager(orderId, storeId, reason, status);

        String message;
        if (status == EnumProcessOrder.MANAGER_ACCEPT) {
            message = "Quản lý đã chấp nhận đơn hàng #" + orderId;
        } else if (status == EnumProcessOrder.MANAGER_REJECT) {
            message = "Quản lý đã từ chối đơn hàng #" + orderId
                    + (storeId != null ? " và gán lại cho cửa hàng khác" : "");
        } else {
            message = "Trạng thái không hợp lệ";
        }

        return ApiResponse.<String>builder()
                .status(HttpStatus.OK.value())
                .message(message)
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

        @PutMapping("/status/{id}")
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

    @PostMapping("/{orderId}/assign-store")
    public ApiResponse<String> assignOrderToStore(
            @PathVariable Long orderId
    ) {
        assignOrderService.assignOrderToStore(orderId);

        return ApiResponse.<String>builder()
                .status(HttpStatus.OK.value())
                .message("Đã gán đơn hàng cho cửa hàng thành công" + orderId)
                .build();
    }

    @GetMapping("/pre-orders")
    public ApiResponse<PageResponse<OrderResponse>> getPreOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<OrderResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy danh sách đơn hàng trước thành công")
                .data(orderService.getOrdersByStatus(EnumProcessOrder.PRE_ORDER, page, size))
                .build();
    }

    @PostMapping("/{orderId}/approve-pre-order")
    public ApiResponse<OrderResponse> approvePreOrder(@PathVariable Long orderId) {
        OrderResponse orderResponse = orderService.updateOrderStatus(orderId, EnumProcessOrder.PENDING);
        return ApiResponse.<OrderResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Phê duyệt đơn hàng trước thành công")
                .data(orderResponse)
                .build();
    }

    @PostMapping("/{orderId}/reject-pre-order")
    public ApiResponse<OrderResponse> rejectPreOrder(
            @PathVariable Long orderId,
            @RequestParam String reason
    ) {
        OrderResponse orderResponse = orderService.updateOrderStatus(orderId, EnumProcessOrder.CANCELLED);
        return ApiResponse.<OrderResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Từ chối đơn hàng trước thành công")
                .data(orderResponse)
                .build();
    }

    // Manager Workflow APIs
    @GetMapping("/{orderId}/manager/details")
    public ApiResponse<OrderResponse> getOrderDetailsForManager(@PathVariable Long orderId) {
        OrderResponse orderResponse = orderService.getOrderById(orderId);
        return ApiResponse.<OrderResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy thông tin đơn hàng thành công")
                .data(orderResponse)
                .build();
    }

    @PostMapping("/{orderId}/manager/create-import-export")
    public ApiResponse<String> createImportExportOrder(
            @PathVariable Long orderId,
            @RequestParam String warehouseId,
            @RequestParam String storeId
    ) {
        managerWorkflowService.createImportExportOrder(orderId, warehouseId, storeId);
        return ApiResponse.<String>builder()
                .status(HttpStatus.OK.value())
                .message("Tạo phiếu xuất nhập kho thành công")
                .data("Import-export order created successfully")
                .build();
    }

    @PostMapping("/{orderId}/manager/create-sales-receipt")
    public ApiResponse<String> createSalesReceipt(@PathVariable Long orderId) {
        managerWorkflowService.createSalesReceipt(orderId);
        return ApiResponse.<String>builder()
                .status(HttpStatus.OK.value())
                .message("Tạo hóa đơn bán hàng thành công")
                .data("Sales receipt created successfully")
                .build();
    }

    @PostMapping("/{orderId}/manager/assign-delivery")
    public ApiResponse<String> assignDelivery(
            @PathVariable Long orderId,
            @RequestParam String deliveryId
    ) {
        managerWorkflowService.assignDelivery(orderId, deliveryId);
        return ApiResponse.<String>builder()
                .status(HttpStatus.OK.value())
                .message("Gán đơn vận chuyển thành công")
                .data("Delivery assigned successfully")
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        return (clientIp == null || clientIp.isEmpty()) ? request.getRemoteAddr() : clientIp;
    }

}
