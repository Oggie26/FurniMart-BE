package com.example.userservice.service.inteface;

import com.example.userservice.response.AccountDetailResponse;

import java.util.List;

public interface AccountService {
    /**
     * Get all accounts with details from Account, User, and Employee tables
     * Only accessible by ADMIN role
     * 
     * @return List of AccountDetailResponse containing information from all three tables
     */
    List<AccountDetailResponse> getAllAccounts();
}


