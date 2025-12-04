package com.example.orderservice.response;

import com.example.orderservice.enums.DeliveryConfirmationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryConfirmationResponse {

    private Long id;
    private Long orderId;
    private String deliveryStaffId;
    private String customerId;
    private LocalDateTime deliveryDate;
    private List<String> deliveryPhotos;
    private String deliveryNotes;
    private String qrCode;
    private LocalDateTime qrCodeGeneratedAt;
    private LocalDateTime qrCodeScannedAt;
    private String customerSignature;
    private DeliveryConfirmationStatus status;
    private String deliveryAddress;
    private boolean isQrCodeScanned;
    private Date createdAt;
    private Date updatedAt;
}
