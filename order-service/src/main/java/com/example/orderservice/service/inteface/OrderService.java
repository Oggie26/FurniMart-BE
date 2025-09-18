package com.example.orderservice.service.inteface;

import com.example.orderservice.enums.EnumProcessOrder;
import com.example.orderservice.enums.PaymentMethod;
import com.example.orderservice.response.OrderResponse;

public interface OrderService {
    OrderResponse createOrder(Long cartId, Long addressId, PaymentMethod paymentMethod, String voucherCode);
    OrderResponse getOrderById(Long id);
    OrderResponse updateOrderStatus(Long orderId, EnumProcessOrder status);

}
