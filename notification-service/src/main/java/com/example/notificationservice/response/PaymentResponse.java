package com.example.notificationservice.response;

import com.example.notificationservice.enums.PaymentMethod;
import com.example.notificationservice.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentResponse {
    private Long id;
    private String transactionCode;
    private Double total;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private Date date;
}
