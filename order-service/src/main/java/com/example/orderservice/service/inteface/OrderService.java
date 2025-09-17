package com.example.orderservice.service.inteface;

import com.example.orderservice.request.OrderRequest;
import com.example.orderservice.response.OrderResponse;
import com.example.orderservice.response.PageResponse;

public interface OrderService {
    OrderResponse createOrder(OrderRequest orderRequest);
//    PageResponse<OrderResponse> searchOrdersByCustomer(String request, int page, int size);
//    PageResponse<OrderResponse> searchOrdersByManager(String request, String storeId, int page, int size);
    OrderResponse getOrderById(Long id);
    OrderResponse updateOrderStatus(Long orderId, String status);

}
