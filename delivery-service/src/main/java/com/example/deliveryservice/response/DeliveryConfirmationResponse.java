package com.example.deliveryservice.response;

import com.example.deliveryservice.enums.DeliveryConfirmationStatus;
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
    private Double deliveryLatitude;
    private Double deliveryLongitude;
    private String deliveryAddress;
    private boolean isQrCodeScanned;
    private Date createdAt;
    private Date updatedAt;
}


