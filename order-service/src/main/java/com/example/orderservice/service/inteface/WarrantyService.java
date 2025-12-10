package com.example.orderservice.service.inteface;

import com.example.orderservice.request.WarrantyClaimRequest;
import com.example.orderservice.request.WarrantyClaimResolutionRequest;
import com.example.orderservice.response.OrderResponse;
import com.example.orderservice.response.WarrantyClaimResponse;
import com.example.orderservice.response.WarrantyReportResponse;
import com.example.orderservice.response.WarrantyResponse;

import java.time.LocalDateTime;

import java.util.List;

public interface WarrantyService {

    // Create warranty for order items when order is delivered
    void createWarrantiesForOrder(Long orderId);

    // Get warranties by customer
    List<WarrantyResponse> getWarrantiesByCustomer(String customerId);

    // Get active warranties by customer
    List<WarrantyResponse> getActiveWarrantiesByCustomer(String customerId);

    // Get warranty by ID
    WarrantyResponse getWarrantyById(Long warrantyId);

    // Get warranties by order
    List<WarrantyResponse> getWarrantiesByOrder(Long orderId);

    // Create warranty claim
    WarrantyClaimResponse createWarrantyClaim(WarrantyClaimRequest request);

    // Get warranty claims by customer
    List<WarrantyClaimResponse> getWarrantyClaimsByCustomer(String customerId);

    // Get warranty claims by warranty
    List<WarrantyClaimResponse> getWarrantyClaimsByWarranty(Long warrantyId);

    // Update warranty claim status (Admin only)
    WarrantyClaimResponse updateWarrantyClaimStatus(Long claimId, String status, String adminResponse,
            String resolutionNotes);

    // Get all warranty claims (Admin only)
    List<WarrantyClaimResponse> getAllWarrantyClaims();

    // Get warranty claims by status (Admin only)
    List<WarrantyClaimResponse> getWarrantyClaimsByStatus(String status);

    // Expire warranties (scheduled task)
    void expireWarranties();

    // Resolve warranty claim with action
    WarrantyClaimResponse resolveWarrantyClaim(WarrantyClaimResolutionRequest request);

    // Create order from resolved warranty claim
    OrderResponse createWarrantyOrder(Long claimId);

    // Get warranty statistics report
    WarrantyReportResponse getWarrantyReport(LocalDateTime startDate, LocalDateTime endDate);
}
