package com.example.deliveryservice.response;

import com.example.deliveryservice.enums.EnumProcessOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private String storeId;
    private AddressResponse address;
    private Double total;
    private String note;
    private Date orderDate;
    private String reason;
    private List<OrderDetailResponse> orderDetails;
    private PaymentResponse payment;
    private String qrCode;
    private Double depositPrice;
    private Date qrCodeGeneratedAt;
    private String pdfFilePath;
}


