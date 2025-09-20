package com.example.orderservice.service.inteface;

import com.example.orderservice.request.VoucherRequest;
import com.example.orderservice.response.VoucherResponse;
import com.example.orderservice.enums.VoucherType;

import java.util.List;

public interface VoucherService {
    
    VoucherResponse createVoucher(VoucherRequest request);
    
    VoucherResponse getVoucherById(Integer id);
    
    VoucherResponse getVoucherByCode(String code);
    
    List<VoucherResponse> getAllVouchers();
    
    List<VoucherResponse> getActiveVouchers();
    
    List<VoucherResponse> getVouchersByType(VoucherType type);
    
    List<VoucherResponse> getVouchersByOrderId(Long orderId);
    
    List<VoucherResponse> getApplicableVouchers(Double orderAmount);
    
    VoucherResponse updateVoucher(Integer id, VoucherRequest request);
    
    void deleteVoucher(Integer id);
    
    // Voucher operations
    VoucherResponse validateVoucher(String code, Double orderAmount);
    
    VoucherResponse applyVoucher(String code, Long orderId);
    
    Double calculateDiscount(String voucherCode, Double orderAmount);
    
    void incrementUsageCount(Integer voucherId);
    
    void expireVouchers();
}
