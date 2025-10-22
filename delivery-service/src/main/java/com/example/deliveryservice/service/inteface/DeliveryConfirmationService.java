package com.example.deliveryservice.service.inteface;

import com.example.deliveryservice.request.DeliveryConfirmationRequest;
import com.example.deliveryservice.request.QRCodeScanRequest;
import com.example.deliveryservice.response.DeliveryConfirmationResponse;

import java.util.List;

public interface DeliveryConfirmationService {

    DeliveryConfirmationResponse createDeliveryConfirmation(DeliveryConfirmationRequest request);

    DeliveryConfirmationResponse getDeliveryConfirmationByOrderId(Long orderId);

    DeliveryConfirmationResponse getDeliveryConfirmationByQRCode(String qrCode);

    DeliveryConfirmationResponse scanQRCode(QRCodeScanRequest request);

    List<DeliveryConfirmationResponse> getDeliveryConfirmationsByStaff(String deliveryStaffId);

    List<DeliveryConfirmationResponse> getDeliveryConfirmationsByCustomer(String customerId);

    List<DeliveryConfirmationResponse> getAllDeliveryConfirmations();

    List<DeliveryConfirmationResponse> getScannedConfirmations();

    List<DeliveryConfirmationResponse> getUnscannedConfirmations();
}


