package com.example.orderservice.service.inteface;

import com.example.orderservice.enums.EnumProcessOrder;
import com.example.orderservice.enums.PaymentMethod;
import com.example.orderservice.response.OrderResponse;
import com.example.orderservice.response.PageResponse;

public interface OrderService {
    OrderResponse createOrder(Long cartId, Long addressId, PaymentMethod paymentMethod, String voucherCode);
    OrderResponse createPreOrder(Long cartId, Long addressId, String voucherCode);
    OrderResponse getOrderById(Long id);
    OrderResponse updateOrderStatus(Long orderId, EnumProcessOrder status);
    PageResponse<OrderResponse> searchOrderByCustomer(String request, int page, int size);
    PageResponse<OrderResponse> searchOrder(String request, int page, int size);
    PageResponse<OrderResponse> searchOrderByStoreId(String request, int page, int size, String storeId);
    PageResponse<OrderResponse> getOrdersByStatus(EnumProcessOrder status, int page, int size);
}
