package com.example.orderservice.service.inteface;

import com.example.orderservice.request.DeliveryConfirmationRequest;
import com.example.orderservice.request.QRCodeScanRequest;
import com.example.orderservice.response.DeliveryConfirmationResponse;

import java.util.List;

public interface DeliveryConfirmationService {
    
    // Create delivery confirmation with QR code
    DeliveryConfirmationResponse createDeliveryConfirmation(DeliveryConfirmationRequest request);
    
    // Get delivery confirmation by order ID
    DeliveryConfirmationResponse getDeliveryConfirmationByOrderId(Long orderId);
    
    // Get delivery confirmation by QR code
    DeliveryConfirmationResponse getDeliveryConfirmationByQRCode(String qrCode);
    
    // Scan QR code to confirm delivery
    DeliveryConfirmationResponse scanQRCode(QRCodeScanRequest request);
    
    // Get delivery confirmations by delivery staff
    List<DeliveryConfirmationResponse> getDeliveryConfirmationsByStaff(String deliveryStaffId);
    
    // Get delivery confirmations by customer
    List<DeliveryConfirmationResponse> getDeliveryConfirmationsByCustomer(String customerId);
    
    // Get all delivery confirmations (Admin only)
    List<DeliveryConfirmationResponse> getAllDeliveryConfirmations();
    
    // Get scanned confirmations
    List<DeliveryConfirmationResponse> getScannedConfirmations();
    
    // Get unscanned confirmations
    List<DeliveryConfirmationResponse> getUnscannedConfirmations();
    
    // Generate QR code for order
    String generateQRCodeForOrder(Long orderId);
}
