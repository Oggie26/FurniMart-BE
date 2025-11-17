package com.example.userservice.response;

import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Response DTO for Account details including User or Employee information
 * This DTO combines information from Account, User, and Employee tables
 * without exposing sensitive data like passwords
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDetailResponse {
    // Account information
    private String accountId;
    private String email;
    @Enumerated(EnumType.STRING)
    private EnumRole role;
    @Enumerated(EnumType.STRING)
    private EnumStatus status;
    private Boolean enabled;
    private Boolean accountNonExpired;
    private Boolean accountNonLocked;
    private Boolean credentialsNonExpired;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date accountCreatedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date accountUpdatedAt;
    
    // Common information (from User or Employee)
    private String id; // User ID or Employee ID
    private String fullName;
    private String phone;
    private Boolean gender;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date birthday;
    private String avatar;
    private String cccd;
    
    // User-specific information (only for CUSTOMER role)
    private Integer point;
    
    // Employee-specific information (only for non-CUSTOMER roles)
    private String employeeCode;
    private String department;
    private String position;
    private BigDecimal salary;
    private List<String> storeIds; // Store IDs assigned to employee
    
    // Type indicator
    private String accountType; // "CUSTOMER" or "EMPLOYEE"
}


