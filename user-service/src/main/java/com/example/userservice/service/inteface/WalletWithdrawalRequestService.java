package com.example.userservice.service.inteface;

import com.example.userservice.request.WalletWithdrawToVNPayRequest;
import com.example.userservice.response.WalletWithdrawalRequestResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface WalletWithdrawalRequestService {
    
    /**
     * User tạo yêu cầu rút tiền về VNPay (tự động xử lý, không cần admin duyệt)
     */
    WalletWithdrawalRequestResponse createWithdrawalRequest(WalletWithdrawToVNPayRequest request);
    
    /**
     * User xem yêu cầu rút tiền của mình
     */
    WalletWithdrawalRequestResponse getMyWithdrawalRequest(String requestId);
    
    /**
     * User xem tất cả yêu cầu rút tiền của mình
     */
    List<WalletWithdrawalRequestResponse> getMyWithdrawalRequests();
    
    /**
     * User hủy yêu cầu rút tiền (chỉ khi status = PENDING_APPROVAL hoặc PROCESSING)
     */
    WalletWithdrawalRequestResponse cancelMyWithdrawalRequest(String requestId);
    
    /**
     * Admin xem tất cả yêu cầu rút tiền (có phân trang) - chỉ để monitoring
     */
    Page<WalletWithdrawalRequestResponse> getAllWithdrawalRequests(Pageable pageable);
    
    /**
     * Admin xem yêu cầu rút tiền theo status - chỉ để monitoring
     */
    List<WalletWithdrawalRequestResponse> getWithdrawalRequestsByStatus(String status);
    
    /**
     * Admin xem chi tiết yêu cầu rút tiền - chỉ để monitoring
     */
    WalletWithdrawalRequestResponse getWithdrawalRequestById(String requestId);
}
