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
    PageResponse<OrderResponse> getOrdersByStoreId(String storeId, EnumProcessOrder status, int page, int size);
    List<ProcessOrderResponse> getOrderStatusHistory(Long orderId);
    PageResponse<OrderResponse> getStoreOrdersWithInvoice(String storeId, int page, int size);
}
