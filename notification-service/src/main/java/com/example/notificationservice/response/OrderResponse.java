package com.example.notificationservice.response;

import com.example.notificationservice.enums.EnumProcessOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderResponse {

    private Long id;
    private UserResponse user;
    private String storeId;
    private Double total;
    private String note;
    private Date orderDate;
    private EnumProcessOrder status;
    private String reason;
    private List<OrderDetailResponse> orderDetails;
    private List<ProcessOrderResponse> processOrders;
    private PaymentResponse payment;
}
