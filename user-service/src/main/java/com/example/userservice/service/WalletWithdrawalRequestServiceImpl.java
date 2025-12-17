package com.example.userservice.service;

import com.example.userservice.entity.User;
import com.example.userservice.entity.Wallet;
import com.example.userservice.entity.WalletTransaction;
import com.example.userservice.entity.WalletWithdrawalRequest;
import com.example.userservice.enums.ErrorCode;
import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.WalletStatus;
import com.example.userservice.enums.WithdrawalRequestStatus;
import com.example.userservice.exception.AppException;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.repository.WalletRepository;
import com.example.userservice.repository.WalletTransactionRepository;
import com.example.userservice.repository.WalletWithdrawalRequestRepository;
import com.example.userservice.request.WalletWithdrawToVNPayRequest;
import com.example.userservice.response.WalletTransactionResponse;
import com.example.userservice.response.WalletWithdrawalRequestResponse;
import com.example.userservice.service.inteface.WalletService;
import com.example.userservice.service.inteface.WalletWithdrawalRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WalletWithdrawalRequestServiceImpl implements WalletWithdrawalRequestService {

    private final WalletWithdrawalRequestRepository withdrawalRequestRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final WalletTransactionRepository transactionRepository;
    private final WalletService walletService;

    @Override
    public WalletWithdrawalRequestResponse createWithdrawalRequest(WalletWithdrawToVNPayRequest request) {
        log.info("Creating withdrawal request for wallet: {}", request.getWalletId());

        // Get current user
        String currentUserId = getCurrentUserId();
        
        // Get wallet
        Wallet wallet = walletRepository.findByIdAndIsDeletedFalse(request.getWalletId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        // Verify wallet belongs to current user
        if (!wallet.getUserId().equals(currentUserId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        // Check wallet status
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new AppException(ErrorCode.WALLET_NOT_ACTIVE);
        }

        // Check balance
        BigDecimal balance = wallet.getBalance();
        if (balance.compareTo(BigDecimal.valueOf(request.getAmount())) < 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        // Generate unique code
        String code = generateWithdrawalRequestCode();

        // Create withdrawal request with PROCESSING status (no approval needed)
        WalletWithdrawalRequest withdrawalRequest = WalletWithdrawalRequest.builder()
                .code(code)
                .walletId(request.getWalletId())
                .userId(currentUserId)
                .amount(BigDecimal.valueOf(request.getAmount()))
                .bankAccountNumber(request.getBankAccountNumber())
                .bankName(request.getBankName())
                .accountHolderName(request.getAccountHolderName())
                .description(request.getDescription())
                .status(WithdrawalRequestStatus.PROCESSING)
                .build();

        withdrawalRequest = withdrawalRequestRepository.save(withdrawalRequest);
        log.info("Withdrawal request created: {}", code);

        // Immediately call withdrawToVNPay() to process withdrawal
        try {
            WalletTransactionResponse transactionResponse = walletService.withdrawToVNPay(
                    request.getWalletId(),
                    request.getAmount(),
                    request.getBankAccountNumber(),
                    request.getBankName(),
                    request.getAccountHolderName(),
                    request.getDescription() != null ? request.getDescription() : 
                        "Rút tiền về VNPay - " + code
            );

            // Update request with transaction info
            withdrawalRequest.setTransactionId(transactionResponse.getId());
            withdrawalRequest.setReferenceId(transactionResponse.getReferenceId());
            withdrawalRequest = withdrawalRequestRepository.save(withdrawalRequest);

            log.info("Withdrawal request processing. Request: {}, Transaction: {}", 
                    code, transactionResponse.getCode());

        } catch (Exception e) {
            log.error("Error processing withdrawal: {}", e.getMessage(), e);
            // Update status to FAILED
            withdrawalRequest.setStatus(WithdrawalRequestStatus.FAILED);
            withdrawalRequest = withdrawalRequestRepository.save(withdrawalRequest);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        User user = userRepository.findByIdAndIsDeletedFalse(currentUserId).orElse(null);
        Wallet walletUpdated = walletRepository.findByIdAndIsDeletedFalse(request.getWalletId()).orElse(null);
        return mapToResponse(withdrawalRequest, walletUpdated, user);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletWithdrawalRequestResponse getMyWithdrawalRequest(String requestId) {
        String currentUserId = getCurrentUserId();
        
        WalletWithdrawalRequest request = withdrawalRequestRepository.findByIdAndIsDeletedFalse(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.WITHDRAWAL_REQUEST_NOT_FOUND));

        // Verify request belongs to current user
        if (!request.getUserId().equals(currentUserId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        Wallet wallet = walletRepository.findByIdAndIsDeletedFalse(request.getWalletId()).orElse(null);
        User user = userRepository.findByIdAndIsDeletedFalse(currentUserId).orElse(null);
        return mapToResponse(request, wallet, user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletWithdrawalRequestResponse> getMyWithdrawalRequests() {
        String currentUserId = getCurrentUserId();
        
        List<WalletWithdrawalRequest> requests = withdrawalRequestRepository
                .findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(currentUserId);

        return requests.stream()
                .map(request -> {
                    Wallet wallet = walletRepository.findByIdAndIsDeletedFalse(request.getWalletId()).orElse(null);
                    User user = userRepository.findByIdAndIsDeletedFalse(currentUserId).orElse(null);
                    return mapToResponse(request, wallet, user);
                })
                .collect(Collectors.toList());
    }

    @Override
    public WalletWithdrawalRequestResponse cancelMyWithdrawalRequest(String requestId) {
        String currentUserId = getCurrentUserId();
        
        WalletWithdrawalRequest request = withdrawalRequestRepository.findByIdAndIsDeletedFalse(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.WITHDRAWAL_REQUEST_NOT_FOUND));

        // Verify request belongs to current user
        if (!request.getUserId().equals(currentUserId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        // Only allow cancel if status is PENDING_APPROVAL or PROCESSING (before VNPay completes)
        if (request.getStatus() != WithdrawalRequestStatus.PENDING_APPROVAL 
                && request.getStatus() != WithdrawalRequestStatus.PROCESSING) {
            throw new AppException(ErrorCode.WITHDRAWAL_REQUEST_CANNOT_CANCEL);
        }

        request.setStatus(WithdrawalRequestStatus.CANCELLED);
        request = withdrawalRequestRepository.save(request);
        log.info("Withdrawal request cancelled: {}", request.getCode());

        Wallet wallet = walletRepository.findByIdAndIsDeletedFalse(request.getWalletId()).orElse(null);
        User user = userRepository.findByIdAndIsDeletedFalse(currentUserId).orElse(null);
        return mapToResponse(request, wallet, user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WalletWithdrawalRequestResponse> getAllWithdrawalRequests(Pageable pageable) {
        Page<WalletWithdrawalRequest> requests = withdrawalRequestRepository.findAll(pageable);
        
        return requests.map(request -> {
            Wallet wallet = walletRepository.findByIdAndIsDeletedFalse(request.getWalletId()).orElse(null);
            User user = userRepository.findByIdAndIsDeletedFalse(request.getUserId()).orElse(null);
            return mapToResponse(request, wallet, user);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletWithdrawalRequestResponse> getWithdrawalRequestsByStatus(String status) {
        try {
            WithdrawalRequestStatus requestStatus = WithdrawalRequestStatus.valueOf(status.toUpperCase());
            List<WalletWithdrawalRequest> requests = withdrawalRequestRepository
                    .findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(requestStatus);

            return requests.stream()
                    .map(request -> {
                        Wallet wallet = walletRepository.findByIdAndIsDeletedFalse(request.getWalletId()).orElse(null);
                        User user = userRepository.findByIdAndIsDeletedFalse(request.getUserId()).orElse(null);
                        return mapToResponse(request, wallet, user);
                    })
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public WalletWithdrawalRequestResponse getWithdrawalRequestById(String requestId) {
        WalletWithdrawalRequest request = withdrawalRequestRepository.findByIdAndIsDeletedFalse(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.WITHDRAWAL_REQUEST_NOT_FOUND));

        Wallet wallet = walletRepository.findByIdAndIsDeletedFalse(request.getWalletId()).orElse(null);
        User user = userRepository.findByIdAndIsDeletedFalse(request.getUserId()).orElse(null);
        return mapToResponse(request, wallet, user);
    }


    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        
        String email = authentication.getName();
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return user.getId();
    }

    private String generateWithdrawalRequestCode() {
        String code;
        int attempts = 0;
        int maxAttempts = 10;

        do {
            code = "WDR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            attempts++;

            if (attempts >= maxAttempts) {
                log.error("Failed to generate unique withdrawal request code after {} attempts", maxAttempts);
                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
        } while (withdrawalRequestRepository.existsByCodeAndIsDeletedFalse(code));

        return code;
    }

    private WalletWithdrawalRequestResponse mapToResponse(WalletWithdrawalRequest request, Wallet wallet, User user) {
        return WalletWithdrawalRequestResponse.builder()
                .id(request.getId())
                .code(request.getCode())
                .walletId(request.getWalletId())
                .walletCode(wallet != null ? wallet.getCode() : null)
                .userId(request.getUserId())
                .userFullName(user != null ? user.getFullName() : null)
                .amount(request.getAmount())
                .bankAccountNumber(request.getBankAccountNumber())
                .bankName(request.getBankName())
                .accountHolderName(request.getAccountHolderName())
                .description(request.getDescription())
                .status(request.getStatus())
                .rejectionReason(request.getRejectionReason())
                .approvedBy(request.getApprovedBy())
                .approvedAt(request.getApprovedAt())
                .transactionId(request.getTransactionId())
                .referenceId(request.getReferenceId())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}
