package com.example.userservice.controller;

import com.example.userservice.response.AccountDetailResponse;
import com.example.userservice.response.ApiResponse;
import com.example.userservice.service.inteface.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Account Controller")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    @Operation(
        summary = "Get all accounts with details",
        description = "Retrieve all accounts with information from Account, User, and Employee tables. " +
                     "Only accessible by ADMIN role. " +
                     "Password and other sensitive information are excluded from the response."
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<AccountDetailResponse>> getAllAccounts() {
        return ApiResponse.<List<AccountDetailResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Accounts retrieved successfully")
                .data(accountService.getAllAccounts())
                .build();
    }
}


