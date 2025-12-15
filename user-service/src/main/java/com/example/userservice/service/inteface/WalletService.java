package com.example.userservice.service.inteface;

import com.example.userservice.request.WalletRequest;
import com.example.userservice.request.WalletTransactionRequest;
import com.example.userservice.response.WalletResponse;
import com.example.userservice.response.WalletTransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface WalletService {
    
    WalletResponse createWallet(WalletRequest request);
    
    WalletResponse getWalletById(String id);
    
    WalletResponse getWalletByUserId(String userId);
    
    WalletResponse getMyWallet(); // Get current user's wallet
    
    WalletResponse getWalletByCode(String code);
    
    List<WalletResponse> getAllWallets();
    
    WalletResponse updateWallet(String id, WalletRequest request);
    
    void deleteWallet(String id);
    
    // Transaction methods
    WalletTransactionResponse createTransaction(WalletTransactionRequest request);
    
    WalletTransactionResponse getTransactionById(String id);
    
    List<WalletTransactionResponse> getTransactionsByWalletId(String walletId);
    
    Page<WalletTransactionResponse> getTransactionsByWalletId(String walletId, Pageable pageable);
    
    // Wallet operations
    WalletResponse deposit(String walletId, Double amount, String description, String referenceId);
    
    WalletResponse withdraw(String walletId, Double amount, String description, String referenceId);
    
    WalletResponse transfer(String fromWalletId, String toWalletId, Double amount, String description, String referenceId);
    
    Double getWalletBalance(String walletId);
    
    boolean hasBalance(String walletId, Double amount);
    
    // Auto-create wallet for new customer
    WalletResponse createWalletForUser(String userId);
    
    // Withdraw to VNPay bank account
    WalletTransactionResponse withdrawToVNPay(String walletId, Double amount, String bankAccountNumber, 
                                              String bankName, String accountHolderName, String description);
    
    // Deposit via VNPay payment gateway
    String depositViaVNPay(String walletId, Double amount, String ipAddress);

}
