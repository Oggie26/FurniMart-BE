package com.example.userservice.controller;

import com.example.userservice.request.StaffCreateCustomerRequest;
import com.example.userservice.request.UserRequest;
import com.example.userservice.request.UserUpdateRequest;
import com.example.userservice.response.*;
import com.example.userservice.service.inteface.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Controller")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class UserController {
        private final UserService userService;

        @PostMapping
        @Operation(summary = "Create new user (Admin only for employee accounts)")
        @ResponseStatus(HttpStatus.CREATED)
        public ApiResponse<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
                return ApiResponse.<UserResponse>builder()
                                .status(HttpStatus.CREATED.value())
                                .message("User created successfully")
                                .data(userService.createUser(request))
                                .build();
        }

        @PutMapping("/{id}")
        @Operation(summary = "Update user information (Admin only for employee accounts)")
        @PreAuthorize("hasRole('ADMIN')")
        public ApiResponse<UserResponse> updateUser(@PathVariable String id,
                        @Valid @RequestBody UserUpdateRequest request) {
                return ApiResponse.<UserResponse>builder()
                                .status(HttpStatus.OK.value())
                                .message("User updated successfully")
                                .data(userService.updateUser(id, request))
                                .build();
        }

        @GetMapping("/{id}")
        @Operation(summary = "Get user by ID")
        public ApiResponse<UserResponse> getUserById(@PathVariable String id) {
                return ApiResponse.<UserResponse>builder()
                                .status(HttpStatus.OK.value())
                                .message("User retrieved successfully")
                                .data(userService.getUserById(id))
                                .build();
        }

        @GetMapping
        @Operation(summary = "Get all users (Admin only)")
        @PreAuthorize("hasRole('ADMIN')")
        public ApiResponse<List<UserResponse>> getAllUsers() {
                return ApiResponse.<List<UserResponse>>builder()
                                .status(HttpStatus.OK.value())
                                .message("Users retrieved successfully")
                                .data(userService.getAllUsers())
                                .build();
        }

        @GetMapping("/status/{status}")
        @Operation(summary = "Get users by status")
        public ApiResponse<List<UserResponse>> getUsersByStatus(@PathVariable String status) {
                return ApiResponse.<List<UserResponse>>builder()
                                .status(HttpStatus.OK.value())
                                .message("Users by status retrieved successfully")
                                .data(userService.getUsersByStatus(status))
                                .build();
        }

        @GetMapping("/paginated")
        @Operation(summary = "Get users with pagination")
        @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
        public ApiResponse<PageResponse<UserResponse>> getUsersWithPagination(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
                return ApiResponse.<PageResponse<UserResponse>>builder()
                                .status(HttpStatus.OK.value())
                                .message("Users retrieved successfully with pagination")
                                .data(userService.getUsersWithPagination(page, size))
                                .build();
        }

        @GetMapping("/search")
        @Operation(summary = "Search users by multiple criteria")
        @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
        public ApiResponse<PageResponse<UserResponse>> searchUsers(
                        @RequestParam(required = false) String searchTerm,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                PageResponse<UserResponse> users = userService.searchUsers(searchTerm, page, size);

                return ApiResponse.<PageResponse<UserResponse>>builder()
                                .status(HttpStatus.OK.value())
                                .message("Search completed successfully")
                                .data(users)
                                .build();
        }

        @GetMapping("/email/{email:.+}")
        @Operation(summary = "Get user by email")
        public ApiResponse<UserResponse> getUserByEmail(@PathVariable String email) {
                return ApiResponse.<UserResponse>builder()
                                .status(HttpStatus.OK.value())
                                .message("User retrieved successfully")
                                .data(userService.getUserByEmail(email))
                                .build();
        }

        @GetMapping("/phone/{phone}")
        @Operation(summary = "Get user by phone")
        public ApiResponse<UserResponse> getUserByPhone(@PathVariable String phone) {
                return ApiResponse.<UserResponse>builder()
                                .status(HttpStatus.OK.value())
                                .message("User retrieved successfully")
                                .data(userService.getUserByPhone(phone))
                                .build();
        }

        @GetMapping("/profile")
        @Operation(summary = "Get current user profile")
        public ApiResponse<UserResponse> getProfile() {
                return ApiResponse.<UserResponse>builder()
                                .status(HttpStatus.OK.value())
                                .data(userService.getProfile())
                                .message("Profile retrieved successfully")
                                .build();
        }

        @GetMapping("/account/{accountId}")
        @Operation(summary = "Lấy thông tin User bằng AccountId")
        public ApiResponse<UserResponse> getUserByAccountId(@PathVariable String accountId) {
                return ApiResponse.<UserResponse>builder()
                                .status(HttpStatus.OK.value())
                                .data(userService.getUserByAccountId(accountId))
                                .message("Thành công")
                                .build();
        }

        @PutMapping("/profile")
        @Operation(summary = "Update current user profile")
        public ApiResponse<UserResponse> updateProfile(@Valid @RequestBody UserUpdateRequest request) {
                return ApiResponse.<UserResponse>builder()
                                .status(HttpStatus.OK.value())
                                .message("Profile updated successfully")
                                .data(userService.updateProfile(request))
                                .build();
        }

        @PatchMapping("/change-password")
        @Operation(summary = "Change password")
        public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePassword changePassword) {
                userService.changePassword(changePassword);
                return ApiResponse.<Void>builder()
                                .status(HttpStatus.OK.value())
                                .message("Password changed successfully")
                                .build();
        }

        @PatchMapping("/{id}/disable")
        @Operation(summary = "Disable user (set INACTIVE)")
        public ApiResponse<Void> disableUser(@PathVariable String id) {
                userService.disableUser(id);
                return ApiResponse.<Void>builder()
                                .status(HttpStatus.OK.value())
                                .message("User disabled successfully")
                                .build();
        }

        @PatchMapping("/{id}/enable")
        @Operation(summary = "Enable user (set ACTIVE)")
        public ApiResponse<Void> enableUser(@PathVariable String id) {
                userService.enableUser(id);
                return ApiResponse.<Void>builder()
                                .status(HttpStatus.OK.value())
                                .message("User enabled successfully")
                                .build();
        }

        @DeleteMapping("/{id}")
        @Operation(summary = "Soft delete user (Admin only)")
        @PreAuthorize("hasRole('ADMIN')")
        public ApiResponse<Void> deleteUser(@PathVariable String id) {
                userService.deleteUser(id);
                return ApiResponse.<Void>builder()
                                .status(HttpStatus.OK.value())
                                .message("User deleted successfully")
                                .build();
        }

        @PostMapping("/staff/create-customer")
        @Operation(summary = "Create customer account with delivery address (Staff only)")
        @ResponseStatus(HttpStatus.CREATED)
        @PreAuthorize("hasRole('ADMIN') or hasRole('BRANCH_MANAGER') or hasRole('STAFF')")
        public ApiResponse<StaffCreateCustomerResponse> createCustomerAccountForStaff(
                        @Valid @RequestBody StaffCreateCustomerRequest request) {
                return ApiResponse.<StaffCreateCustomerResponse>builder()
                                .status(HttpStatus.CREATED.value())
                                .message("Customer account created successfully")
                                .data(userService.createCustomerAccountForStaff(request))
                                .build();
        }

        @GetMapping("/count")
        @Operation(summary = "Get total users count")
        @PreAuthorize("hasRole('ADMIN')")
        public ApiResponse<Long> getTotalUsersCount() {
                return ApiResponse.<Long>builder()
                                .status(HttpStatus.OK.value())
                                .message("Total users count retrieved successfully")
                                .data(userService.getTotalUsersCount())
                                .build();
        }

        @PostMapping("/{userId}/wallet/refund")
        @Operation(summary = "Refund amount to user's wallet (creates transaction record)")
        @PreAuthorize("hasRole('ADMIN') or hasRole('BRANCH_MANAGER')")
        public ApiResponse<Void> refundToWallet(
                        @PathVariable String userId,
                        @RequestParam Double amount,
                        @RequestParam(required = false) String referenceId) {
                userService.refundToWallet(userId, amount, referenceId);
                return ApiResponse.<Void>builder()
                                .status(HttpStatus.OK.value())
                                .message("Refund to wallet completed successfully")
                                .build();
        }
}
