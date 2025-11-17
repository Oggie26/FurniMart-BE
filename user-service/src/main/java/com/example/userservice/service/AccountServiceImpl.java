package com.example.userservice.service;

import com.example.userservice.entity.Account;
import com.example.userservice.entity.Employee;
import com.example.userservice.entity.User;
import com.example.userservice.enums.EnumRole;
import com.example.userservice.repository.AccountRepository;
import com.example.userservice.repository.EmployeeStoreRepository;
import com.example.userservice.response.AccountDetailResponse;
import com.example.userservice.service.inteface.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final EmployeeStoreRepository employeeStoreRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AccountDetailResponse> getAllAccounts() {
        log.info("Fetching all accounts with details from Account, User, and Employee tables");
        
        // Use custom query with JOIN FETCH to avoid N+1 query problem
        // This loads Account, User, and Employee in a single query
        List<Account> accounts = accountRepository.findAllWithUserAndEmployee();
        
        log.info("Found {} accounts", accounts.size());
        
        return accounts.stream()
                .map(this::toAccountDetailResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert Account entity to AccountDetailResponse
     * Combines information from Account, User (if CUSTOMER), and Employee (if non-CUSTOMER)
     * Excludes sensitive information like password
     */
    private AccountDetailResponse toAccountDetailResponse(Account account) {
        AccountDetailResponse.AccountDetailResponseBuilder builder = AccountDetailResponse.builder()
                // Account information (excluding password)
                .accountId(account.getId())
                .email(account.getEmail())
                .role(account.getRole())
                .status(account.getStatus())
                .enabled(account.isEnabled())
                .accountNonExpired(account.isAccountNonExpired())
                .accountNonLocked(account.isAccountNonLocked())
                .credentialsNonExpired(account.isCredentialsNonExpired())
                .accountCreatedAt(account.getCreatedAt())
                .accountUpdatedAt(account.getUpdatedAt());
        
        // Determine account type and populate accordingly
        if (account.getRole() == EnumRole.CUSTOMER) {
            // For CUSTOMER role, get information from User table
            User user = account.getUser();
            if (user != null && !user.getIsDeleted()) {
                builder
                        .accountType("CUSTOMER")
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .phone(user.getPhone())
                        .gender(user.getGender())
                        .birthday(user.getBirthday())
                        .avatar(user.getAvatar())
                        .cccd(user.getCccd())
                        .point(user.getPoint());
            } else {
                // User record not found or deleted
                builder.accountType("CUSTOMER");
                log.warn("User record not found for CUSTOMER account: {}", account.getId());
            }
        } else {
            // For non-CUSTOMER roles (ADMIN, BRANCH_MANAGER, STAFF, DELIVERY), get information from Employee table
            Employee employee = account.getEmployee();
            if (employee != null && !employee.getIsDeleted()) {
                // Get store IDs for this employee
                List<String> storeIds = employeeStoreRepository.findByEmployeeIdAndIsDeletedFalse(employee.getId())
                        .stream()
                        .map(es -> es.getStoreId())
                        .collect(Collectors.toList());
                
                builder
                        .accountType("EMPLOYEE")
                        .id(employee.getId())
                        .fullName(employee.getFullName())
                        .phone(employee.getPhone())
                        .gender(employee.getGender())
                        .birthday(employee.getBirthday())
                        .avatar(employee.getAvatar())
                        .cccd(employee.getCccd())
                        .employeeCode(employee.getCode())
                        .department(employee.getDepartment())
                        .position(employee.getPosition())
                        .salary(employee.getSalary())
                        .storeIds(storeIds);
            } else {
                // Employee record not found or deleted
                builder.accountType("EMPLOYEE");
                log.warn("Employee record not found for account with role {}: {}", account.getRole(), account.getId());
            }
        }
        
        return builder.build();
    }
}

