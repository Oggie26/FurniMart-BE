package com.example.orderservice.response;

import com.example.orderservice.enums.PaymentMethod;
import com.example.orderservice.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentAdminResponse {
    private Long id;
    private String transactionCode;
    private Double total;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private Date date;
    private String userId;
    private String userName;
    private String email;
}
