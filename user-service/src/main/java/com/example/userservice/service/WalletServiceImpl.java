package com.example.userservice.service;

import com.example.userservice.entity.User;
import com.example.userservice.entity.Wallet;
import com.example.userservice.entity.WalletTransaction;
import com.example.userservice.enums.WalletStatus;
import com.example.userservice.enums.WalletTransactionStatus;
import com.example.userservice.enums.WalletTransactionType;
import com.example.userservice.exception.AppException;
import com.example.userservice.enums.ErrorCode;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.repository.WalletRepository;
import com.example.userservice.repository.WalletTransactionRepository;
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

        // Check if user has a deleted wallet (soft delete) - restore it instead of creating new
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
    @Transactional(readOnly = true)
    public WalletResponse getWalletByUserId(String userId) {
        Wallet wallet = walletRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElse(null);

        return mapToWalletResponse(wallet, user);
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
    public WalletResponse transfer(String fromWalletId, String toWalletId, Double amount, String description, String referenceId) {
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
    @Transactional
    public WalletResponse createWalletForUser(String userId) {
        log.info("Auto-creating wallet for user: {}", userId);

        // Check if user already has an active wallet
        if (walletRepository.existsByUserIdAndIsDeletedFalse(userId)) {
            log.warn("User {} already has a wallet, returning existing wallet", userId);
            return getWalletByUserId(userId);
        }

        // Verify user exists
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Check if user has a deleted wallet (soft delete) - restore it instead of creating new
        Optional<Wallet> deletedWallet = walletRepository.findByUserId(userId);
        if (deletedWallet.isPresent() && deletedWallet.get().getIsDeleted()) {
            Wallet wallet = deletedWallet.get();
            log.info("Found deleted wallet for user {}, restoring it instead of creating new", userId);

            // Restore wallet with default values
            wallet.setIsDeleted(false);
            // Generate new unique wallet code if needed
            String walletCode = generateWalletCode();
            wallet.setCode(walletCode);
            wallet.setBalance(BigDecimal.ZERO);
            wallet.setStatus(WalletStatus.ACTIVE);

            wallet = walletRepository.save(wallet);
            log.info("Wallet restored successfully for user {} with ID: {} and code: {}",
                    userId, wallet.getId(), walletCode);

            return mapToWalletResponse(wallet, user);
        }

        // Generate unique wallet code
        String walletCode = generateWalletCode();

        // Create wallet with default values
        Wallet wallet = Wallet.builder()
                .code(walletCode)
                .balance(BigDecimal.ZERO)
                .status(WalletStatus.ACTIVE)
                .userId(userId)
                .build();

        wallet = walletRepository.save(wallet);
        log.info("Wallet auto-created successfully for user {} with ID: {} and code: {}",
                userId, wallet.getId(), walletCode);

        return mapToWalletResponse(wallet, user);
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
}
