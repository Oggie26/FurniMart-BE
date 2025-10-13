package com.example.userservice.controller;

import com.example.userservice.enums.EnumRole;
import com.example.userservice.request.RoleUpdateRequest;
import com.example.userservice.request.UserRequest;
import com.example.userservice.request.UserUpdateRequest;
import com.example.userservice.response.ApiResponse;
import com.example.userservice.response.PageResponse;
import com.example.userservice.response.UserResponse;
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
@RequestMapping("/api/employees")
@Tag(name = "Employee Controller")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class EmployeeController {
    private final UserService userService;

    // ========== ADMIN ONLY CRUD OPERATIONS ==========
    
    @PostMapping
    @Operation(summary = "Create new employee (Admin only)")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> createEmployee(@Valid @RequestBody UserRequest request) {
        // Ensure only employee roles can be created
        if (!isEmployeeRole(request.getRole())) {
            throw new IllegalArgumentException("Only employee roles can be created through this endpoint");
        }
        
        return ApiResponse.<UserResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Employee created successfully")
                .data(userService.createUser(request))
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update employee information (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> updateEmployee(@PathVariable String id, @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Employee updated successfully")
                .data(userService.updateUser(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete employee (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteEmployee(@PathVariable String id) {
        userService.deleteUser(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Employee deleted successfully")
                .build();
    }

    @PatchMapping("/{id}/disable")
    @Operation(summary = "Disable employee (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> disableEmployee(@PathVariable String id) {
        userService.disableUser(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Employee disabled successfully")
                .build();
    }

    @PatchMapping("/{id}/enable")
    @Operation(summary = "Enable employee (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> enableEmployee(@PathVariable String id) {
        userService.enableUser(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Employee enabled successfully")
                .build();
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Update employee role (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> updateEmployeeRole(@PathVariable String id, @Valid @RequestBody RoleUpdateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Employee role updated successfully")
                .data(userService.updateUserRole(id, request.getRole()))
                .build();
    }

    // ========== GET OPERATIONS ==========
    
    @GetMapping("/{id}")
    @Operation(summary = "Get employee by ID")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> getEmployeeById(@PathVariable String id) {
        return ApiResponse.<UserResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Employee retrieved successfully")
                .data(userService.getUserById(id))
                .build();
    }
    
    @GetMapping
    @Operation(summary = "Get all employees")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserResponse>> getAllEmployees() {
        return ApiResponse.<List<UserResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Employees retrieved successfully")
                .data(userService.getAllEmployees())
                .build();
    }

    @GetMapping("/paginated")
    @Operation(summary = "Get employees with pagination")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<UserResponse>> getEmployeesWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<UserResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Employees retrieved successfully with pagination")
                .data(userService.getEmployeesWithPagination(page, size))
                .build();
    }

    // ========== GET BY ROLE ==========
    
    @GetMapping("/role/seller")
    @Operation(summary = "Get all sellers")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserResponse>> getAllSellers() {
        return ApiResponse.<List<UserResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Sellers retrieved successfully")
                .data(userService.getEmployeesByRole(EnumRole.SELLER))
                .build();
    }

    @GetMapping("/role/manager")
    @Operation(summary = "Get all branch managers")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserResponse>> getAllManagers() {
        return ApiResponse.<List<UserResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Branch managers retrieved successfully")
                .data(userService.getEmployeesByRole(EnumRole.BRANCH_MANAGER))
                .build();
    }

    @GetMapping("/role/delivery")
    @Operation(summary = "Get all delivery staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserResponse>> getAllDeliveryStaff() {
        return ApiResponse.<List<UserResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Delivery staff retrieved successfully")
                .data(userService.getEmployeesByRole(EnumRole.DELIVERER))
                .build();
    }

    @GetMapping("/role/staff")
    @Operation(summary = "Get all general staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserResponse>> getAllStaff() {
        return ApiResponse.<List<UserResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("General staff retrieved successfully")
                .data(userService.getEmployeesByRole(EnumRole.STAFF))
                .build();
    }

    // ========== GET BY STORE ==========
    
    @GetMapping("/store/{storeId}")
    @Operation(summary = "Get all employees by store ID")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserResponse>> getEmployeesByStoreId(@PathVariable String storeId) {
        return ApiResponse.<List<UserResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Store employees retrieved successfully")
                .data(userService.getEmployeesByStoreId(storeId))
                .build();
    }

    @GetMapping("/store/{storeId}/role/seller")
    @Operation(summary = "Get sellers by store ID")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserResponse>> getSellersByStoreId(@PathVariable String storeId) {
        return ApiResponse.<List<UserResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Store sellers retrieved successfully")
                .data(userService.getEmployeesByStoreIdAndRole(storeId, EnumRole.SELLER))
                .build();
    }

    @GetMapping("/store/{storeId}/role/manager")
    @Operation(summary = "Get branch managers by store ID")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserResponse>> getManagersByStoreId(@PathVariable String storeId) {
        return ApiResponse.<List<UserResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Store branch managers retrieved successfully")
                .data(userService.getEmployeesByStoreIdAndRole(storeId, EnumRole.BRANCH_MANAGER))
                .build();
    }

    @GetMapping("/store/{storeId}/role/delivery")
    @Operation(summary = "Get delivery staff by store ID")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserResponse>> getDeliveryStaffByStoreId(@PathVariable String storeId) {
        return ApiResponse.<List<UserResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Store delivery staff retrieved successfully")
                .data(userService.getEmployeesByStoreIdAndRole(storeId, EnumRole.DELIVERER))
                .build();
    }

    @GetMapping("/store/{storeId}/role/staff")
    @Operation(summary = "Get general staff by store ID")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserResponse>> getStaffByStoreId(@PathVariable String storeId) {
        return ApiResponse.<List<UserResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Store general staff retrieved successfully")
                .data(userService.getEmployeesByStoreIdAndRole(storeId, EnumRole.STAFF))
                .build();
    }

    // ========== GET BY ROLE WITH PAGINATION ==========
    
    @GetMapping("/role/seller/paginated")
    @Operation(summary = "Get sellers with pagination")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<UserResponse>> getSellersWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<UserResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Sellers retrieved successfully with pagination")
                .data(userService.getEmployeesByRoleWithPagination(EnumRole.SELLER, page, size))
                .build();
    }

    @GetMapping("/role/manager/paginated")
    @Operation(summary = "Get branch managers with pagination")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<UserResponse>> getManagersWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<UserResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Branch managers retrieved successfully with pagination")
                .data(userService.getEmployeesByRoleWithPagination(EnumRole.BRANCH_MANAGER, page, size))
                .build();
    }

    @GetMapping("/role/delivery/paginated")
    @Operation(summary = "Get delivery staff with pagination")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<UserResponse>> getDeliveryStaffWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<UserResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Delivery staff retrieved successfully with pagination")
                .data(userService.getEmployeesByRoleWithPagination(EnumRole.DELIVERER, page, size))
                .build();
    }

    @GetMapping("/role/staff/paginated")
    @Operation(summary = "Get general staff with pagination")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<UserResponse>> getStaffWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<UserResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("General staff retrieved successfully with pagination")
                .data(userService.getEmployeesByRoleWithPagination(EnumRole.STAFF, page, size))
                .build();
    }

    // ========== UTILITY METHODS ==========
    
    private boolean isEmployeeRole(EnumRole role) {
        return role == EnumRole.SELLER || 
               role == EnumRole.BRANCH_MANAGER || 
               role == EnumRole.DELIVERER || 
               role == EnumRole.STAFF;
    }
}
