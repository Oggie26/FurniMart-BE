package com.example.orderservice.service.inteface;

import com.example.orderservice.enums.EnumProcessOrder;
import com.example.orderservice.enums.PaymentMethod;
import com.example.orderservice.request.StaffCreateOrderRequest;
import com.example.orderservice.request.CancelOrderRequest;
import com.example.orderservice.response.OrderResponse;
import com.example.orderservice.response.PageResponse;
import com.example.orderservice.response.ProcessOrderResponse;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(Long cartId, Long addressId, PaymentMethod paymentMethod, String voucherCode);
    OrderResponse createPreOrder(Long cartId, Long addressId, String voucherCode);
    OrderResponse createOrderForStaff(StaffCreateOrderRequest request);
    OrderResponse getOrderById(Long id);
    void cancelOrder(CancelOrderRequest cancelOrderRequest);
    void handlePaymentCOD(Long orderId);
    OrderResponse updateOrderStatus(Long orderId, EnumProcessOrder status);
    PageResponse<OrderResponse> searchOrderByCustomer(String request, int page, int size);
    PageResponse<OrderResponse> searchOrder(String request, int page, int size);
    PageResponse<OrderResponse> searchOrderByStoreId(String request, int page, int size, String storeId);
    PageResponse<OrderResponse> getOrdersByStatus(EnumProcessOrder status, int page, int size);
    
    /**
     * Lấy danh sách orders của một cửa hàng (đã được ASSIGN_ORDER_STORE)
     * Có thể lọc theo status (optional)
     * @param storeId ID của cửa hàng
     * @param status Status để lọc (optional, null nếu muốn lấy tất cả)
     * @param page Số trang (bắt đầu từ 0)
     * @param size Số lượng items mỗi trang
     * @return PageResponse chứa danh sách orders
     */
    PageResponse<OrderResponse> getOrdersByStoreId(String storeId, EnumProcessOrder status, int page, int size);

    /**
     * Lấy lịch sử status của đơn hàng theo orderId, sắp xếp theo thời gian (cũ nhất trước)
     */
    List<ProcessOrderResponse> getOrderStatusHistory(Long orderId);

    /**
     * Lấy danh sách orders của một cửa hàng đã được tạo hóa đơn (có pdfFilePath)
     * @param storeId ID của cửa hàng
     * @param page Số trang (bắt đầu từ 0)
     * @param size Số lượng items mỗi trang
     * @return PageResponse chứa danh sách orders với thông tin về PDF file
     */
    PageResponse<OrderResponse> getStoreOrdersWithInvoice(String storeId, int page, int size);
}
