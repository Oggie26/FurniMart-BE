package com.example.orderservice.controller;

import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.Voucher;
import com.example.orderservice.enums.EnumProcessOrder;
import com.example.orderservice.enums.ErrorCode;
import com.example.orderservice.enums.PaymentMethod;
import com.example.orderservice.exception.AppException;
import com.example.orderservice.feign.InventoryClient;
import com.example.orderservice.repository.VoucherRepository;
import com.example.orderservice.request.CancelOrderRequest;
import com.example.orderservice.request.StaffCreateOrderRequest;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.response.*;
import com.example.orderservice.service.VNPayService;
import com.example.orderservice.service.ManagerWorkflowService;
import com.example.orderservice.service.inteface.AssignOrderService;
import com.example.orderservice.service.inteface.CartService;
import com.example.orderservice.service.inteface.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
        private final OrderRepository orderRepository;
        private final AssignOrderService assignOrderService;
        private final InventoryClient inventoryClient;
        private final ManagerWorkflowService managerWorkflowService;
        private final VoucherRepository voucherRepository;

        @PostMapping("/checkout")
        public ApiResponse<Void> checkout(
                @RequestParam Long addressId,
                @RequestParam Long cartId,
                @RequestParam(required = false) String voucherCode,
                @RequestParam PaymentMethod paymentMethod,
                HttpServletRequest request) throws Exception {

                String clientIp = getClientIp(request);

                CartResponse cartResponse = cartService.getCartById(cartId);
                List<CartItemResponse> cartItems = cartResponse.getItems();

                for (CartItemResponse item : cartItems) {
                        ApiResponse<Boolean> response = inventoryClient
                                .hasSufficientGlobalStock(item.getProductColorId(), item.getQuantity());

                        if (!response.getData()) {
                                throw new AppException(ErrorCode.OUT_OF_STOCK);
                        }
                }

                Voucher voucher = null;
                if (voucherCode != null && !voucherCode.isBlank()) {
                        voucher = voucherRepository.findByCodeAndIsDeletedFalse(voucherCode).orElse(null);
                }

                Double voucherValue = (voucher != null) ? voucher.getAmount().doubleValue() : 0.0;

                OrderResponse orderResponse = null;
                Order order = null;

                try {
                        orderResponse = orderService.createOrder(cartId, addressId, paymentMethod, voucherCode);

                        cartService.clearCart();

                        order = orderRepository.findByIdAndIsDeletedFalse(orderResponse.getId())
                                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

                        double newTotal = order.getTotal() - voucherValue;
                        order.setTotal(newTotal);

                        double depositAmount = 0;

                        if (paymentMethod == PaymentMethod.VNPAY) {
                                orderRepository.save(order);
                                return ApiResponse.<Void>builder()
                                        .status(HttpStatus.OK.value())
                                        .message("Chuyển hướng sang VNPay")
                                        .redirectUrl(vnPayService.createPaymentUrl(order.getId(), newTotal,
                                                clientIp))
                                        .build();
                        } else {
                                depositAmount = newTotal * 0.1;
                                order.setDepositPrice(depositAmount);
                                orderRepository.save(order);

                                return ApiResponse.<Void>builder()
                                        .status(HttpStatus.OK.value())
                                        .message("Đặt hàng thành công")
                                        .redirectUrl(vnPayService.createPaymentUrl(order.getId(), depositAmount,
                                                clientIp))
                                        .build();
                        }
                } catch (Exception e) {
                        if (orderResponse != null) {
                                log.error("❌ Checkout failed for order {}: {}", orderResponse.getId(), e.getMessage());
                                try {
                                        orderService.updateOrderStatus(orderResponse.getId(),
                                                EnumProcessOrder.CANCELLED);
                                        log.info("✅ Order {} đã được cancel do checkout fail", orderResponse.getId());
                                } catch (Exception cancelEx) {
                                        log.error("Failed to cancel order {}: {}", orderResponse.getId(),
                                                cancelEx.getMessage());
                                }
                        }
                        throw e;
                }
        }

        @PostMapping("/mobile/checkout")
        public ApiResponse<Void> checkoutMobile(
                @RequestParam Long addressId,
                @RequestParam Long cartId,
                @RequestParam(required = false) String voucherCode,
                @RequestParam PaymentMethod paymentMethod,
                HttpServletRequest request) throws Exception {
                String clientIp = getClientIp(request);

                CartResponse cartResponse = cartService.getCartById(cartId);
                List<CartItemResponse> cartItems = cartResponse.getItems();

                for (CartItemResponse item : cartItems) {
                        ApiResponse<Boolean> response = inventoryClient
                                .hasSufficientGlobalStock(item.getProductColorId(), item.getQuantity());

                        boolean available = response.getData();

                        if (!available) {
                                throw new AppException(ErrorCode.OUT_OF_STOCK);
                        }
                }

                Voucher voucher = null;
                if (voucherCode != null && !voucherCode.isBlank()) {
                        voucher = voucherRepository.findByCodeAndIsDeletedFalse(voucherCode).orElse(null);
                }

                Double voucherValue = (voucher != null) ? voucher.getAmount().doubleValue() : 0.0;

                OrderResponse orderResponse = null;
                Order order = null;

                try {
                        orderResponse = orderService.createOrder(cartId, addressId, paymentMethod, voucherCode);

                        cartService.clearCart();

                        order = orderRepository.findByIdAndIsDeletedFalse(orderResponse.getId())
                                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

                        double newTotal = order.getTotal() - voucherValue;
                        order.setTotal(newTotal);

                        double depositAmount = 0;

                        if (paymentMethod == PaymentMethod.VNPAY) {
                                orderRepository.save(order);
                                return ApiResponse.<Void>builder()
                                        .status(HttpStatus.OK.value())
                                        .message("Chuyển hướng sang VNPay")
                                        .redirectUrl(vnPayService.createPaymentUrl(order.getId(), newTotal,
                                                clientIp))
                                        .build();
                        } else {
                                depositAmount = newTotal * 0.1;
                                order.setDepositPrice(depositAmount);
                                orderRepository.save(order);

                                return ApiResponse.<Void>builder()
                                        .status(HttpStatus.OK.value())
                                        .message("Đặt hàng thành công")
                                        .redirectUrl(vnPayService.createPaymentUrl(order.getId(), depositAmount,
                                                clientIp))
                                        .build();
                        }
                } catch (Exception e) {
                        if (orderResponse != null) {
                                log.error("❌ Checkout failed for order {}: {}", orderResponse.getId(), e.getMessage());
                                try {
                                        orderService.updateOrderStatus(orderResponse.getId(),
                                                EnumProcessOrder.CANCELLED);
                                        log.info("✅ Order {} đã được cancel do checkout fail", orderResponse.getId());
                                } catch (Exception cancelEx) {
                                        log.error("Failed to cancel order {}: {}", orderResponse.getId(),
                                                cancelEx.getMessage());
                                }
                        }
                        throw e;
                }
        }

        @PostMapping("/pre-order")
        public ApiResponse<OrderResponse> createPreOrder(
                @RequestParam Long addressId,
                @RequestParam Long cartId,
                @RequestParam(required = false) String voucherCode) {
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

        @GetMapping("/{id}/status-history")
        public ApiResponse<List<ProcessOrderResponse>> getOrderStatusHistory(@PathVariable Long id) {
                return ApiResponse.<List<ProcessOrderResponse>>builder()
                        .status(HttpStatus.OK.value())
                        .message("Lấy lịch sử trạng thái đơn hàng thành công")
                        .data(orderService.getOrderStatusHistory(id))
                        .build();
        }

        @GetMapping("/search/customer")
        public ApiResponse<PageResponse<OrderResponse>> searchOrderByCustomer(
                @RequestParam(defaultValue = "") String keyword,
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "150") int size) {
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
                @RequestParam EnumProcessOrder status) {
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
                @RequestParam(defaultValue = "100") int size) {
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
                @RequestParam(defaultValue = "100") int size) {
                return ApiResponse.<PageResponse<OrderResponse>>builder()
                        .status(HttpStatus.OK.value())
                        .message("Tìm kiếm đơn hàng theo cửa hàng thành công")
                        .data(orderService.searchOrderByStoreId(keyword, page, size, storeId))
                        .build();
        }

        @GetMapping("/store/{storeId}")
        @io.swagger.v3.oas.annotations.Operation(summary = "Lấy danh sách đơn hàng của cửa hàng", description = "Lấy danh sách các đơn hàng đã được ASSIGN_ORDER_STORE cho cửa hàng. Có thể lọc theo status (optional).")
        public ApiResponse<PageResponse<OrderResponse>> getOrdersByStore(
                @PathVariable String storeId,
                @RequestParam(required = false) EnumProcessOrder status,
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "100") int size) {
                return ApiResponse.<PageResponse<OrderResponse>>builder()
                        .status(HttpStatus.OK.value())
                        .message("Lấy danh sách đơn hàng của cửa hàng thành công")
                        .data(orderService.getOrdersByStoreId(storeId, status, page, size))
                        .build();
        }

        @GetMapping("/store/{storeId}/invoices")
        @io.swagger.v3.oas.annotations.Operation(summary = "Lấy danh sách đơn hàng đã tạo hóa đơn của cửa hàng", description = "Lấy danh sách các đơn hàng của cửa hàng đã được tạo hóa đơn (có pdfFilePath). "
                +
                "Response bao gồm thông tin về file PDF: pdfFilePath (đường dẫn file) và hasPdfFile (true/false - file có tồn tại hay không).")
        public ApiResponse<PageResponse<OrderResponse>> getStoreOrdersWithInvoice(
                @PathVariable String storeId,
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "100") int size) {
                return ApiResponse.<PageResponse<OrderResponse>>builder()
                        .status(HttpStatus.OK.value())
                        .message("Lấy danh sách đơn hàng đã tạo hóa đơn của cửa hàng thành công")
                        .data(orderService.getStoreOrdersWithInvoice(storeId, page, size))
                        .build();
        }

        @PutMapping("/status/{id}")
        public ApiResponse<OrderResponse> updateOrderStatus(
                @PathVariable Long id,
                @RequestParam EnumProcessOrder status) {
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
                @PathVariable Long orderId) {
                assignOrderService.assignOrderToStore(orderId);

                return ApiResponse.<String>builder()
                        .status(HttpStatus.OK.value())
                        .message("Đã gán đơn hàng cho cửa hàng thành công" + orderId)
                        .build();
        }

        @GetMapping("/pre-orders")
        public ApiResponse<PageResponse<OrderResponse>> getPreOrders(
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "10") int size) {
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
                @RequestParam String reason) {
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
                @RequestParam String storeId) {
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
                @RequestParam String deliveryId) {
                managerWorkflowService.assignDelivery(orderId, deliveryId);
                return ApiResponse.<String>builder()
                        .status(HttpStatus.OK.value())
                        .message("Gán đơn vận chuyển thành công")
                        .data("Delivery assigned successfully")
                        .build();
        }

        @PostMapping("/staff/create")
        @io.swagger.v3.oas.annotations.Operation(summary = "Create order for customer (Staff only)")
        @PreAuthorize("hasRole('ADMIN') or hasRole('BRANCH_MANAGER') or hasRole('STAFF')")
        public ApiResponse<OrderResponse> createOrderForStaff(
                @Valid @RequestBody StaffCreateOrderRequest request) {
                OrderResponse orderResponse = orderService.createOrderForStaff(request);
                return ApiResponse.<OrderResponse>builder()
                        .status(HttpStatus.CREATED.value())
                        .message("Order created successfully for customer")
                        .data(orderResponse)
                        .build();
        }

        @PostMapping("/{orderId}/confirm-cod")
        public ApiResponse<Void> confirmCodPayment(@PathVariable Long orderId) {

                boolean success = orderService.handleConfirmPayment(orderId);

                if (!success) {
                        throw new AppException(ErrorCode.INVALID_PAYMENT_METHOD);
                }

                return ApiResponse.<Void>builder()
                        .status(HttpStatus.OK.value())
                        .message("Xác nhận thanh toán COD thành công")
                        .data(null)
                        .build();
        }

        @PostMapping("/{orderId}/return-request")
        public ApiResponse<OrderResponse> requestReturn(
                @PathVariable Long orderId,
                @RequestParam String reason,
                @RequestParam(required = false) String note) {
                return ApiResponse.<OrderResponse>builder()
                        .status(HttpStatus.OK.value())
                        .message("Yêu cầu trả hàng thành công")
                        .data(orderService.requestReturn(orderId, reason, note))
                        .build();
        }

        @PutMapping("/{orderId}/return-accept")
        @PreAuthorize("hasRole('ADMIN') or hasRole('BRANCH_MANAGER')") // Adjust roles as needed
        public ApiResponse<OrderResponse> acceptReturn(@PathVariable Long orderId) {
                return ApiResponse.<OrderResponse>builder()
                        .status(HttpStatus.OK.value())
                        .message("Chấp nhận trả hàng thành công")
                        .data(orderService.acceptReturn(orderId))
                        .build();
        }

        @PutMapping("/{orderId}/return-reject")
        @PreAuthorize("hasRole('ADMIN') or hasRole('BRANCH_MANAGER')")
        public ApiResponse<OrderResponse> rejectReturn(
                @PathVariable Long orderId,
                @RequestParam String reason) {
                return ApiResponse.<OrderResponse>builder()
                        .status(HttpStatus.OK.value())
                        .message("Từ chối trả hàng thành công")
                        .data(orderService.rejectReturn(orderId, reason))
                        .build();
        }

        @PutMapping("/{orderId}/return-confirm")
        @PreAuthorize("hasRole('ADMIN') or hasRole('BRANCH_MANAGER')")
        public ApiResponse<OrderResponse> confirmReturn(@PathVariable Long orderId) {
                return ApiResponse.<OrderResponse>builder()
                        .status(HttpStatus.OK.value())
                        .message("Xác nhận đã nhận hàng trả và hoàn tiền thành công")
                        .data(orderService.confirmReturn(orderId))
                        .build();
        }

        @Operation(summary = "Hủy đơn hàng")
        @PostMapping("/cancel")
        public ApiResponse<Void> cancelOrder(@Valid @RequestBody CancelOrderRequest cancelOrderRequest) {
                orderService.cancelOrder(cancelOrderRequest);

                return ApiResponse.<Void>builder()
                        .status(200)
                        .message("Hủy đơn hàng thành công cho orderId: " + cancelOrderRequest.getOrderId())
                        .data(null)
                        .build();
        }

        private String getClientIp(HttpServletRequest request) {
                String clientIp = request.getHeader("X-Forwarded-For");
                return (clientIp == null || clientIp.isEmpty()) ? request.getRemoteAddr() : clientIp;
        }

}
