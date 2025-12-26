package com.example.userservice.service;

import com.example.userservice.entity.User;
import com.example.userservice.entity.Wallet;
import com.example.userservice.entity.WalletTransaction;
import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.WalletStatus;
import com.example.userservice.enums.WalletTransactionStatus;
import com.example.userservice.enums.WalletTransactionType;
import com.example.userservice.exception.AppException;
import com.example.userservice.enums.ErrorCode;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.repository.WalletRepository;
import com.example.userservice.repository.WalletTransactionRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.example.userservice.request.WalletRequest;
import com.example.userservice.request.WalletTransactionRequest;
import com.example.userservice.response.WalletResponse;
import com.example.userservice.response.WalletTransactionResponse;
import com.example.userservice.service.inteface.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final VNPayWithdrawalService vnPayWithdrawalService;
    private final VNPayDepositService vnPayDepositService;

    @Override
    public WalletResponse createWallet(WalletRequest request) {
        log.info("Creating wallet with code: {}", request.getCode());

        // Check if wallet code already exists (not deleted)
        if (walletRepository.existsByCodeAndIsDeletedFalse(request.getCode())) {
            throw new AppException(ErrorCode.WALLET_CODE_EXISTS);
        }

        // Check if user already has an active wallet
        if (walletRepository.existsByUserIdAndIsDeletedFalse(request.getUserId())) {
            throw new AppException(ErrorCode.USER_ALREADY_HAS_WALLET);
        }

        // Verify user exists
        User user = userRepository.findByIdAndIsDeletedFalse(request.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Check if user has a deleted wallet (soft delete) - restore it instead of
        // creating new
        Optional<Wallet> deletedWallet = walletRepository.findByUserId(request.getUserId());
        if (deletedWallet.isPresent() && deletedWallet.get().getIsDeleted()) {
            Wallet wallet = deletedWallet.get();
            log.info("Found deleted wallet for user {}, restoring it instead of creating new", request.getUserId());

            // Restore wallet
            wallet.setIsDeleted(false);
            wallet.setCode(request.getCode());
            wallet.setBalance(request.getBalance());
            wallet.setStatus(request.getStatus());

            wallet = walletRepository.save(wallet);
            log.info("Wallet restored successfully with ID: {}", wallet.getId());

            return mapToWalletResponse(wallet, user);
        }

        // Create new wallet
        Wallet wallet = Wallet.builder()
                .code(request.getCode())
                .balance(request.getBalance())
                .status(request.getStatus())
                .userId(request.getUserId())
                .build();

        wallet = walletRepository.save(wallet);
        log.info("Wallet created successfully with ID: {}", wallet.getId());

        return mapToWalletResponse(wallet, user);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getWalletById(String id) {
        Wallet wallet = walletRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        User user = userRepository.findByIdAndIsDeletedFalse(wallet.getUserId())
                .orElse(null);

        return mapToWalletResponse(wallet, user);
    }

    @Override
    @Transactional
    public WalletResponse getWalletByUserId(String userId) {
        Wallet wallet = walletRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseGet(() -> initializeWalletForUser(userId));

        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElse(null);

        return mapToWalletResponse(wallet, user);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getMyWallet() {
        String currentUserId = getCurrentUserId();

        // Get current user to check role
        User user = userRepository.findByIdAndIsDeletedFalse(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Only CUSTOMER role can have wallet
        if (user.getAccount() == null || user.getAccount().getRole() != EnumRole.CUSTOMER) {
            log.warn("User {} with role {} attempted to access wallet. Only CUSTOMER can have wallet.",
                    currentUserId, user.getAccount() != null ? user.getAccount().getRole() : "NULL");
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        // Wallet must exist - no lazy creation
        Wallet wallet = walletRepository.findByUserIdAndIsDeletedFalse(currentUserId)
                .orElseThrow(() -> {
                    log.error(
                            "Wallet not found for CUSTOMER user {}. Wallet should have been created during registration.",
                            currentUserId);
                    return new AppException(ErrorCode.WALLET_NOT_FOUND);
                });

        return mapToWalletResponse(wallet, user);
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String email = authentication.getName(); // This returns the email
        // Find the user by email to get the actual user ID
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return user.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getWalletByCode(String code) {
        Wallet wallet = walletRepository.findByCodeAndIsDeletedFalse(code)
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        User user = userRepository.findByIdAndIsDeletedFalse(wallet.getUserId())
                .orElse(null);

        return mapToWalletResponse(wallet, user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletResponse> getAllWallets() {
        List<Wallet> wallets = walletRepository.findAll()
                .stream()
                .filter(wallet -> !wallet.getIsDeleted())
                .collect(Collectors.toList());

        return wallets.stream()
                .map(wallet -> {
                    User user = userRepository.findByIdAndIsDeletedFalse(wallet.getUserId()).orElse(null);
                    return mapToWalletResponse(wallet, user);
                })
                .collect(Collectors.toList());
    }

    @Override
    public WalletResponse updateWallet(String id, WalletRequest request) {
        log.info("Updating wallet with ID: {}", id);

        Wallet wallet = walletRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        // Check if new code already exists (if different from current)
        if (!wallet.getCode().equals(request.getCode()) &&
                walletRepository.existsByCodeAndIsDeletedFalse(request.getCode())) {
            throw new AppException(ErrorCode.WALLET_CODE_EXISTS);
        }

        wallet.setCode(request.getCode());
        wallet.setBalance(request.getBalance());
        wallet.setStatus(request.getStatus());

        wallet = walletRepository.save(wallet);
        User user = userRepository.findByIdAndIsDeletedFalse(wallet.getUserId()).orElse(null);

        log.info("Wallet updated successfully with ID: {}", wallet.getId());
        return mapToWalletResponse(wallet, user);
    }

    @Override
    public void deleteWallet(String id) {
        log.info("Deleting wallet with ID: {}", id);

        Wallet wallet = walletRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        wallet.setIsDeleted(true);
        walletRepository.save(wallet);

        log.info("Wallet deleted successfully with ID: {}", id);
    }

    @Override
    public WalletTransactionResponse createTransaction(WalletTransactionRequest request) {
        log.info("Creating wallet transaction with code: {}", request.getCode());

        // Check if transaction code already exists
        if (transactionRepository.existsByCodeAndIsDeletedFalse(request.getCode())) {
            throw new AppException(ErrorCode.TRANSACTION_CODE_EXISTS);
        }

        Wallet wallet = walletRepository.findByIdAndIsDeletedFalse(request.getWalletId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        // Check wallet status
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new AppException(ErrorCode.WALLET_NOT_ACTIVE);
        }

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter;

        // Calculate balance after transaction based on type
        if (isDebitTransaction(request.getType())) {
            if (balanceBefore.compareTo(request.getAmount()) < 0) {
                throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);
            }
            balanceAfter = balanceBefore.subtract(request.getAmount());
        } else {
            balanceAfter = balanceBefore.add(request.getAmount());
        }

        // Create transaction
        WalletTransaction transaction = WalletTransaction.builder()
                .code(request.getCode())
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .amount(request.getAmount())
                .status(WalletTransactionStatus.COMPLETED)
                .type(request.getType())
                .description(request.getDescription())
                .referenceId(request.getReferenceId())
                .walletId(request.getWalletId())
                .build();

        transaction = transactionRepository.save(transaction);

        // Update wallet balance
        wallet.setBalance(balanceAfter);
        walletRepository.save(wallet);

        log.info("Wallet transaction created successfully with ID: {}", transaction.getId());
        return mapToTransactionResponse(transaction, wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletTransactionResponse getTransactionById(String id) {
        WalletTransaction transaction = transactionRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));

        Wallet wallet = walletRepository.findByIdAndIsDeletedFalse(transaction.getWalletId())
                .orElse(null);

        return mapToTransactionResponse(transaction, wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> getTransactionsByWalletId(String walletId) {
        List<WalletTransaction> transactions = transactionRepository
                .findByWalletIdAndIsDeletedFalseOrderByCreatedAtDesc(walletId);

        Wallet wallet = walletRepository.findByIdAndIsDeletedFalse(walletId).orElse(null);

        return transactions.stream()
                .map(transaction -> mapToTransactionResponse(transaction, wallet))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> getTransactionsByWalletId(String walletId, Pageable pageable) {
        Page<WalletTransaction> transactions = transactionRepository
                .findByWalletIdAndIsDeletedFalseOrderByCreatedAtDesc(walletId, pageable);

        Wallet wallet = walletRepository.findByIdAndIsDeletedFalse(walletId).orElse(null);

        return transactions.map(transaction -> mapToTransactionResponse(transaction, wallet));
    }

    @Override
    public WalletResponse deposit(String walletId, Double amount, String description, String referenceId) {
        log.info("Depositing {} to wallet ID: {}", amount, walletId);

        WalletTransactionRequest request = WalletTransactionRequest.builder()
                .code(generateTransactionCode())
                .amount(BigDecimal.valueOf(amount))
                .type(WalletTransactionType.DEPOSIT)
                .description(description)
                .referenceId(referenceId)
                .walletId(walletId)
                .build();

        createTransaction(request);

        return getWalletById(walletId);
    }

    @Override
    public WalletResponse withdraw(String walletId, Double amount, String description, String referenceId) {
        log.info("Withdrawing {} from wallet ID: {}", amount, walletId);

        WalletTransactionRequest request = WalletTransactionRequest.builder()
                .code(generateTransactionCode())
                .amount(BigDecimal.valueOf(amount))
                .type(WalletTransactionType.WITHDRAWAL)
                .description(description)
                .referenceId(referenceId)
                .walletId(walletId)
                .build();

        createTransaction(request);

        return getWalletById(walletId);
    }

    @Override
    public WalletResponse transfer(String fromWalletId, String toWalletId, Double amount, String description,
            String referenceId) {
        log.info("Transferring {} from wallet {} to wallet {}", amount, fromWalletId, toWalletId);

        // Withdraw from source wallet
        WalletTransactionRequest withdrawRequest = WalletTransactionRequest.builder()
                .code(generateTransactionCode())
                .amount(BigDecimal.valueOf(amount))
                .type(WalletTransactionType.TRANSFER_OUT)
                .description(description)
                .referenceId(referenceId)
                .walletId(fromWalletId)
                .build();

        createTransaction(withdrawRequest);

        // Deposit to destination wallet
        WalletTransactionRequest depositRequest = WalletTransactionRequest.builder()
                .code(generateTransactionCode())
                .amount(BigDecimal.valueOf(amount))
                .type(WalletTransactionType.TRANSFER_IN)
                .description(description)
                .referenceId(referenceId)
                .walletId(toWalletId)
                .build();

        createTransaction(depositRequest);

        return getWalletById(fromWalletId);
    }

    @Override
    public WalletResponse refund(String walletId, Double amount, String description, String referenceId) {
        log.info("Refunding {} to wallet ID: {}", amount, walletId);

        WalletTransactionRequest request = WalletTransactionRequest.builder()
                .code(generateTransactionCode())
                .amount(BigDecimal.valueOf(amount))
                .type(WalletTransactionType.REFUND)
                .description(description)
                .referenceId(referenceId)
                .walletId(walletId)
                .build();

        createTransaction(request);

        return getWalletById(walletId);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getWalletBalance(String walletId) {
        Wallet wallet = walletRepository.findByIdAndIsDeletedFalse(walletId)
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
        return wallet.getBalance().doubleValue();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasBalance(String walletId, Double amount) {
        Double balance = getWalletBalance(walletId);
        return balance >= amount;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WalletResponse createWalletForUser(String userId) {
        log.info("Auto-creating wallet for user: {}", userId);

        // First verify user exists
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> {
                    log.error("User {} not found when creating wallet. This may be a transaction visibility issue.",
                            userId);
                    return new AppException(ErrorCode.USER_NOT_FOUND);
                });

        // Only CUSTOMER role can have wallet
        if (user.getAccount() == null || user.getAccount().getRole() != EnumRole.CUSTOMER) {
            log.warn("Attempted to create wallet for user {} with role {}. Only CUSTOMER can have wallet.",
                    userId, user.getAccount() != null ? user.getAccount().getRole() : "NULL");
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        // Check if user already has an active wallet
        if (walletRepository.existsByUserIdAndIsDeletedFalse(userId)) {
            log.warn("User {} already has a wallet, returning existing wallet", userId);
            return getWalletByUserId(userId);
        }

        Wallet wallet = initializeWalletForUser(userId);
        log.info("Wallet successfully created for CUSTOMER user: {}", userId);
        return mapToWalletResponse(wallet, user);
    }

    /**
     * Create wallet directly in the same transaction (for register endpoint).
     * This method runs in the same transaction as the caller, no REQUIRES_NEW.
     * Used when creating wallet during user registration to ensure atomicity.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public WalletResponse createWalletDirectlyInTransaction(String userId, User user) {
        log.info("Creating wallet directly in transaction for user: {}", userId);

        // Verify user is CUSTOMER
        if (user == null) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        if (user.getAccount() == null || user.getAccount().getRole() != EnumRole.CUSTOMER) {
            log.warn("Attempted to create wallet for user {} with role {}. Only CUSTOMER can have wallet.",
                    userId, user.getAccount() != null ? user.getAccount().getRole() : "NULL");
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        // Check if user already has an active wallet
        if (walletRepository.existsByUserIdAndIsDeletedFalse(userId)) {
            log.warn("User {} already has a wallet, returning existing wallet", userId);
            Wallet existingWallet = walletRepository.findByUserIdAndIsDeletedFalse(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
            return mapToWalletResponse(existingWallet, user);
        }

        // Create wallet directly in the same transaction
        Wallet wallet = initializeWalletForUser(userId);
        log.info("Wallet successfully created directly in transaction for CUSTOMER user: {}", userId);
        return mapToWalletResponse(wallet, user);
    }

    private Wallet initializeWalletForUser(String userId) {
        log.info("Initializing wallet for user {} (lazy creation)", userId);

        userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Optional<Wallet> existingWallet = walletRepository.findByUserId(userId);
        if (existingWallet.isPresent()) {
            Wallet wallet = existingWallet.get();
            if (Boolean.TRUE.equals(wallet.getIsDeleted())) {
                wallet.setIsDeleted(false);
            }
            // Only generate code if it's truly missing (shouldn't happen for existing
            // wallets)
            // Don't regenerate code for existing wallets to maintain consistency
            if (wallet.getCode() == null || wallet.getCode().isBlank()) {
                log.warn("Wallet {} for user {} has blank code, generating new one", wallet.getId(), userId);
                wallet.setCode(generateWalletCode());
            }
            if (wallet.getBalance() == null) {
                wallet.setBalance(BigDecimal.ZERO);
            }
            wallet.setStatus(WalletStatus.ACTIVE);
            Wallet restored = walletRepository.save(wallet);
            log.info("Restored wallet {} for user {} with code: {}", restored.getId(), userId, restored.getCode());
            return restored;
        }

        Wallet wallet = Wallet.builder()
                .code(generateWalletCode())
                .balance(BigDecimal.ZERO)
                .status(WalletStatus.ACTIVE)
                .userId(userId)
                .build();

        Wallet created = walletRepository.save(wallet);
        log.info("Wallet auto-created successfully for user {} with ID: {} and code: {}",
                userId, created.getId(), created.getCode());
        return created;
    }

    private String generateWalletCode() {
        String code;
        int attempts = 0;
        int maxAttempts = 10;

        do {
            // Generate code: WLT-{UUID first 8 chars}
            code = "WLT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            attempts++;

            if (attempts >= maxAttempts) {
                log.error("Failed to generate unique wallet code after {} attempts", maxAttempts);
                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
        } while (walletRepository.existsByCodeAndIsDeletedFalse(code));

        return code;
    }

    private boolean isDebitTransaction(WalletTransactionType type) {
        return type == WalletTransactionType.WITHDRAWAL ||
                type == WalletTransactionType.TRANSFER_OUT ||
                type == WalletTransactionType.PAYMENT ||
                type == WalletTransactionType.PENALTY;
    }

    private String generateTransactionCode() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private WalletResponse mapToWalletResponse(Wallet wallet, User user) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .code(wallet.getCode())
                .balance(wallet.getBalance())
                .status(wallet.getStatus())
                .userId(wallet.getUserId())
                .userFullName(user != null ? user.getFullName() : null)
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public WalletTransactionResponse withdrawToVNPay(String walletId, Double amount, String bankAccountNumber,
            String bankName, String accountHolderName, String description) {
        log.info("Withdrawing {} VND from wallet {} to VNPay bank account: {}", amount, walletId, bankAccountNumber);

        // Get wallet
        Wallet wallet = walletRepository.findByIdAndIsDeletedFalse(walletId)
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        // Check wallet status
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new AppException(ErrorCode.WALLET_NOT_ACTIVE);
        }

        // Check balance
        BigDecimal balanceBefore = wallet.getBalance();
        if (balanceBefore.compareTo(BigDecimal.valueOf(amount)) < 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        // Generate transaction code and reference ID
        String transactionCode = generateTransactionCode();
        String referenceId = "VNPAY-WD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Create transaction with PENDING status first
        BigDecimal amountDecimal = BigDecimal.valueOf(amount);
        BigDecimal balanceAfter = balanceBefore.subtract(amountDecimal);

        WalletTransaction transaction = WalletTransaction.builder()
                .code(transactionCode)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .amount(amountDecimal)
                .status(WalletTransactionStatus.PENDING)
                .type(WalletTransactionType.WITHDRAWAL)
                .description(description != null ? description
                        : String.format("Rút tiền về VNPay - TK: %s - %s - Chủ TK: %s",
                                bankAccountNumber, bankName, accountHolderName))
                .referenceId(referenceId)
                .walletId(walletId)
                .build();

        transaction = transactionRepository.save(transaction);

        // Update wallet balance (temporarily hold the amount)
        wallet.setBalance(balanceAfter);
        walletRepository.save(wallet);

        log.info("Transaction created with PENDING status: {}", transactionCode);

        // Call VNPay API to process withdrawal (async)
        // Transaction status will be updated via webhook callback
        try {
            // Process withdrawal asynchronously
            // If VNPay has direct API, it will be called immediately
            // Otherwise, it will be queued for processing
            boolean requestAccepted = vnPayWithdrawalService.processWithdrawal(
                    amount, bankAccountNumber, bankName, accountHolderName, referenceId);

            if (requestAccepted) {
                // Request accepted by VNPay (may still be processing)
                // Status will be updated via webhook callback
                log.info("VNPay withdrawal request accepted. Transaction {} is processing. Reference: {}",
                        transactionCode, referenceId);
                // Keep status as PENDING, will be updated by webhook
            } else {
                // Request rejected immediately
                wallet.setBalance(balanceBefore);
                walletRepository.save(wallet);
                transaction.setStatus(WalletTransactionStatus.FAILED);
                transactionRepository.save(transaction);
                log.error("VNPay withdrawal request rejected. Transaction {} failed, amount refunded to wallet.",
                        transactionCode);
                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            // Rollback: refund to wallet
            wallet.setBalance(balanceBefore);
            walletRepository.save(wallet);
            transaction.setStatus(WalletTransactionStatus.FAILED);
            transactionRepository.save(transaction);
            log.error("Error processing VNPay withdrawal: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        return mapToTransactionResponse(transaction, wallet);
    }

    @Override
    @Transactional
    public String depositViaVNPay(String walletId, Double amount, String ipAddress) {
        log.info("Creating VNPay deposit request for wallet {} with amount {}", walletId, amount);

        // Validate amount
        if (amount == null || amount <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // Get wallet
        Wallet wallet = walletRepository.findByIdAndIsDeletedFalse(walletId)
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        // Check wallet status
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new AppException(ErrorCode.WALLET_NOT_ACTIVE);
        }

        // Generate transaction code and reference ID
        String transactionCode = generateTransactionCode();
        String referenceId = "VNPAY-DEP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Create transaction with PENDING status
        BigDecimal amountDecimal = BigDecimal.valueOf(amount);
        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore; // Balance không thay đổi cho đến khi payment thành công

        WalletTransaction transaction = WalletTransaction.builder()
                .code(transactionCode)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter) // Sẽ được update khi callback thành công
                .amount(amountDecimal)
                .status(WalletTransactionStatus.PENDING)
                .type(WalletTransactionType.DEPOSIT)
                .description("Nạp tiền qua VNPay - Đang chờ thanh toán")
                .referenceId(referenceId)
                .walletId(walletId)
                .build();

        transaction = transactionRepository.save(transaction);
        log.info("Deposit transaction created with PENDING status: {}", transactionCode);

        // Generate VNPay payment URL
        try {
            String paymentUrl = vnPayDepositService.createDepositPaymentUrl(
                    transaction.getId(), // Dùng transaction ID làm vnp_TxnRef
                    amount,
                    ipAddress);
            log.info("VNPay payment URL created for transaction: {}", transactionCode);
            return paymentUrl;
        } catch (Exception e) {
            log.error("Error creating VNPay payment URL: {}", e.getMessage(), e);
            // Update transaction status to FAILED
            transaction.setStatus(WalletTransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private WalletTransactionResponse mapToTransactionResponse(WalletTransaction transaction, Wallet wallet) {
        return WalletTransactionResponse.builder()
                .id(transaction.getId())
                .code(transaction.getCode())
                .balanceBefore(transaction.getBalanceBefore())
                .balanceAfter(transaction.getBalanceAfter())
                .amount(transaction.getAmount())
                .status(transaction.getStatus())
                .type(transaction.getType())
                .description(transaction.getDescription())
                .referenceId(transaction.getReferenceId())
                .walletId(transaction.getWalletId())
                .walletCode(wallet != null ? wallet.getCode() : null)
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public void validateWalletAccess(String walletId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Check for Admin/Staff roles
        boolean isStaffOrAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"));

        if (isStaffOrAdmin) {
            return;
        }

        Wallet wallet = walletRepository.findByIdAndIsDeletedFalse(walletId)
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        String email = auth.getName();
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!wallet.getUserId().equals(user.getId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> getPaymentTransactions(Pageable pageable) {
        Page<WalletTransaction> transactions = transactionRepository.findByTypeAndIsDeletedFalseOrderByCreatedAtDesc(
                WalletTransactionType.PAYMENT, pageable);

        return transactions.map(transaction -> {
            Wallet wallet = walletRepository.findByIdAndIsDeletedFalse(transaction.getWalletId()).orElse(null);
            return mapToTransactionResponse(transaction, wallet);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> getEscrowTransactions(Pageable pageable) {
        List<WalletTransactionType> escrowTypes = List.of(
                WalletTransactionType.ESCROW_DEPOSIT,
                WalletTransactionType.ESCROW_WITHDRAWAL);

        Page<WalletTransaction> transactions = transactionRepository.findByTypeInAndIsDeletedFalseOrderByCreatedAtDesc(
                escrowTypes, pageable);

        return transactions.map(transaction -> {
            Wallet wallet = walletRepository.findByIdAndIsDeletedFalse(transaction.getWalletId()).orElse(null);
            return mapToTransactionResponse(transaction, wallet);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> getAllTransactions() {
        List<WalletTransaction> transactions = transactionRepository.findAll();

        return transactions.stream()
                .filter(transaction -> !Boolean.TRUE.equals(transaction.getIsDeleted()))
                .sorted((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()))
                .map(transaction -> {
                    Wallet wallet = walletRepository.findByIdAndIsDeletedFalse(transaction.getWalletId()).orElse(null);
                    return mapToTransactionResponse(transaction, wallet);
                })
                .collect(Collectors.toList());
    }
}
