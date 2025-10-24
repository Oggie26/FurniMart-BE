package com.example.userservice.controller;

import com.example.userservice.enums.EnumRole;
import com.example.userservice.request.RoleUpdateRequest;
import com.example.userservice.request.UserRequest;
import com.example.userservice.request.UserUpdateRequest;
import com.example.userservice.response.ApiResponse;
import com.example.userservice.response.PageResponse;
import com.example.userservice.response.UserResponse;
import com.example.userservice.service.inteface.EmployeeService;
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
    private final EmployeeService employeeService;

    // ========== ADMIN ONLY CRUD OPERATIONS ==========
    
    @PostMapping
    @Operation(summary = "Create new employee (Admin only) - Can create SELLER, BRANCH_MANAGER, DELIVERER, STAFF roles ONLY")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> createEmployee(@Valid @RequestBody UserRequest request) {
        // Validation is now handled in EmployeeService - will throw exception if ADMIN or CUSTOMER role
        return ApiResponse.<UserResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Employee created successfully")
                .data(employeeService.createEmployee(request))
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update employee information (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> updateEmployee(@PathVariable String id, @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Employee updated successfully")
                .data(employeeService.updateEmployee(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete employee (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteEmployee(@PathVariable String id) {
        employeeService.deleteEmployee(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Employee deleted successfully")
                .build();
    }

    @PatchMapping("/{id}/disable")
    @Operation(summary = "Disable employee (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> disableEmployee(@PathVariable String id) {
        employeeService.disableEmployee(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Employee disabled successfully")
                .build();
    }

    @PatchMapping("/{id}/enable")
    @Operation(summary = "Enable employee (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> enableEmployee(@PathVariable String id) {
        employeeService.enableEmployee(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Employee enabled successfully")
                .build();
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Update employee role (Admin only) - Can only update to employee roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> updateEmployeeRole(@PathVariable String id, @Valid @RequestBody RoleUpdateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Employee role updated successfully")
                .data(employeeService.updateEmployeeRole(id, request.getRole()))
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
                .data(employeeService.getEmployeeById(id))
                .build();
    }
    
    @GetMapping
    @Operation(summary = "Get all employees")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserResponse>> getAllEmployees() {
        return ApiResponse.<List<UserResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Employees retrieved successfully")
                .data(employeeService.getAllEmployees())
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
                .data(employeeService.getEmployeesWithPagination(page, size))
                .build();
    }
    
    @GetMapping("/search")
    @Operation(summary = "Search employees by name, email, or phone")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<UserResponse>> searchEmployees(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<UserResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Employees search completed successfully")
                .data(employeeService.searchEmployees(searchTerm, page, size))
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
                .data(employeeService.getEmployeesByRole(EnumRole.SELLER))
                .build();
    }

    @GetMapping("/role/manager")
    @Operation(summary = "Get all branch managers")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserResponse>> getAllManagers() {
        return ApiResponse.<List<UserResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Branch managers retrieved successfully")
                .data(employeeService.getEmployeesByRole(EnumRole.BRANCH_MANAGER))
                .build();
    }

    @GetMapping("/role/delivery")
    @Operation(summary = "Get all delivery staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserResponse>> getAllDeliveryStaff() {
        return ApiResponse.<List<UserResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Delivery staff retrieved successfully")
                .data(employeeService.getEmployeesByRole(EnumRole.DELIVERER))
                .build();
    }

    @GetMapping("/role/staff")
    @Operation(summary = "Get all general staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserResponse>> getAllStaff() {
        return ApiResponse.<List<UserResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("General staff retrieved successfully")
                .data(employeeService.getEmployeesByRole(EnumRole.STAFF))
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
                .data(employeeService.getEmployeesByStoreId(storeId))
                .build();
    }

    @GetMapping("/store/{storeId}/role/seller")
    @Operation(summary = "Get sellers by store ID")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserResponse>> getSellersByStoreId(@PathVariable String storeId) {
        return ApiResponse.<List<UserResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Store sellers retrieved successfully")
                .data(employeeService.getEmployeesByStoreIdAndRole(storeId, EnumRole.SELLER))
                .build();
    }

    @GetMapping("/store/{storeId}/role/manager")
    @Operation(summary = "Get branch managers by store ID")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserResponse>> getManagersByStoreId(@PathVariable String storeId) {
        return ApiResponse.<List<UserResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Store branch managers retrieved successfully")
                .data(employeeService.getEmployeesByStoreIdAndRole(storeId, EnumRole.BRANCH_MANAGER))
                .build();
    }

    @GetMapping("/store/{storeId}/role/delivery")
    @Operation(summary = "Get delivery staff by store ID")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserResponse>> getDeliveryStaffByStoreId(@PathVariable String storeId) {
        return ApiResponse.<List<UserResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Store delivery staff retrieved successfully")
                .data(employeeService.getEmployeesByStoreIdAndRole(storeId, EnumRole.DELIVERER))
                .build();
    }

    @GetMapping("/store/{storeId}/role/staff")
    @Operation(summary = "Get general staff by store ID")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserResponse>> getStaffByStoreId(@PathVariable String storeId) {
        return ApiResponse.<List<UserResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Store general staff retrieved successfully")
                .data(employeeService.getEmployeesByStoreIdAndRole(storeId, EnumRole.STAFF))
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
                .data(employeeService.getEmployeesByRoleWithPagination(EnumRole.SELLER, page, size))
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
                .data(employeeService.getEmployeesByRoleWithPagination(EnumRole.BRANCH_MANAGER, page, size))
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
                .data(employeeService.getEmployeesByRoleWithPagination(EnumRole.DELIVERER, page, size))
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
                .data(employeeService.getEmployeesByRoleWithPagination(EnumRole.STAFF, page, size))
                .build();
    }

    // ========== EMPLOYEE STATISTICS ==========
    
    @GetMapping("/count")
    @Operation(summary = "Get total employee count")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Long> getTotalEmployeeCount() {
        return ApiResponse.<Long>builder()
                .status(HttpStatus.OK.value())
                .message("Total employee count retrieved successfully")
                .data(employeeService.getTotalEmployeeCount())
                .build();
    }

    @GetMapping("/count/role/{role}")
    @Operation(summary = "Get employee count by role")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Long> getEmployeeCountByRole(@PathVariable EnumRole role) {
        return ApiResponse.<Long>builder()
                .status(HttpStatus.OK.value())
                .message("Employee count for role " + role + " retrieved successfully")
                .data(employeeService.getEmployeeCountByRole(role))
                .build();
    }

    // ========== STORE ASSIGNMENT ==========
    
    @PostMapping("/{employeeId}/store/{storeId}")
    @Operation(summary = "Assign employee to store")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> assignEmployeeToStore(
            @PathVariable String employeeId,
            @PathVariable String storeId) {
        employeeService.assignEmployeeToStore(employeeId, storeId);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Employee assigned to store successfully")
                .build();
    }

    @DeleteMapping("/{employeeId}/store/{storeId}")
    @Operation(summary = "Remove employee from store")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> removeEmployeeFromStore(
            @PathVariable String employeeId,
            @PathVariable String storeId) {
        employeeService.removeEmployeeFromStore(employeeId, storeId);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Employee removed from store successfully")
                .build();
    }
}
