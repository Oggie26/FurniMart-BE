package com.example.orderservice.response;

import com.example.orderservice.enums.EnumProcessOrder;
import com.example.orderservice.enums.OrderType;
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
    private Date deadline;
    private Boolean hasPdfFile;
    private OrderType orderType;
    private Long warrantyClaimId;
    private String complaintReason;
    private Date complaintDate;
    private Boolean isStoreError;
    private Boolean customerRefused;
    private Boolean customerContactable;
    private List<String> complaintEvidencePhotos;
}
