package com.example.orderservice.service.inteface;

import com.example.orderservice.enums.EnumProcessOrder;

public interface AssignOrderService {
    void assignOrderToStore(Long orderId);
    void acceptRejectOrderByManager(Long orderId , String storeId , String reason, EnumProcessOrder status);

}
