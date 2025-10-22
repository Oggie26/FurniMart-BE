package com.example.deliveryservice.response;

import lombok.Data;
import java.util.Date;

@Data
public class OrderResponse {
    private Long id;
    private String userId;
    private String qrCode;
    private Date qrCodeGeneratedAt;
}


