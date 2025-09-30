package com.example.orderservice.response;

import lombok.*;

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
    private AddressResponse address;
    private Double total;
    private String note;
    private Date orderDate;
    private List<OrderDetailResponse> orderDetails;
    private List<ProcessOrderResponse> processOrders;
    private PaymentResponse payment;
}
