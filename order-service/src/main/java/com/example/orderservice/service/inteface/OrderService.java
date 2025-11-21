package com.example.orderservice.service.inteface;

import com.example.orderservice.enums.EnumProcessOrder;
import com.example.orderservice.enums.PaymentMethod;
import com.example.orderservice.request.CancelOrderRequest;
import com.example.orderservice.response.OrderResponse;
import com.example.orderservice.response.PageResponse;
import com.example.orderservice.response.ProcessOrderResponse;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(Long cartId, Long addressId, PaymentMethod paymentMethod, String voucherCode);
    OrderResponse createPreOrder(Long cartId, Long addressId, String voucherCode);
    OrderResponse getOrderById(Long id);
    void cancelOrder(CancelOrderRequest cancelOrderRequest);
    void handlePaymentCOD(Long orderId);
    OrderResponse updateOrderStatus(Long orderId, EnumProcessOrder status);
    PageResponse<OrderResponse> searchOrderByCustomer(String request, int page, int size);
    PageResponse<OrderResponse> searchOrder(String request, int page, int size);
    PageResponse<OrderResponse> searchOrderByStoreId(String request, int page, int size, String storeId);
    PageResponse<OrderResponse> getOrdersByStatus(EnumProcessOrder status, int page, int size);
    
    /**
     * Lấy lịch sử status của đơn hàng theo orderId, sắp xếp theo thời gian (cũ nhất trước)
     */
    List<ProcessOrderResponse> getOrderStatusHistory(Long orderId);
}
