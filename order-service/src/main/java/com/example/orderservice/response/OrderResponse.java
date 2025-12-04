package com.example.orderservice.response;

import com.example.orderservice.enums.EnumProcessOrder;
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
    private EnumProcessOrder status;
    private String reason;
    private List<OrderDetailResponse> orderDetails;
    private List<ProcessOrderResponse> processOrders;
    private PaymentResponse payment;
    private String qrCode;
    private Double depositPrice;
    private Date qrCodeGeneratedAt;
    private DeliveryConfirmationResponse deliveryConfirmationResponse;
    private String pdfFilePath;
    private Boolean hasPdfFile; // Ghi chú: true nếu file PDF tồn tại, false nếu không tồn tại hoặc chưa có
}
