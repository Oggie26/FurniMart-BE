package com.example.deliveryservice.response;

import com.example.deliveryservice.enums.PaymentMethod;
import com.example.deliveryservice.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private String transactionCode;
    private Double total;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private Date date;
}

