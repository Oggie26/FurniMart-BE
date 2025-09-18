package com.example.userservice.controller;

import com.example.userservice.request.StaffRequest;
import com.example.userservice.request.StaffUpdateRequest;
import com.example.userservice.response.ApiResponse;
import com.example.userservice.response.PageResponse;
import com.example.userservice.response.StaffResponse;
import com.example.userservice.service.inteface.StaffService;
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
@RequestMapping("/api/staff")
@Tag(name = "Staff Controller")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class StaffController {
    private final StaffService staffService;

    @PostMapping
    @Operation(summary = "Create new staff member")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') or hasRole('BRANCH_MANAGER')")
    public ApiResponse<StaffResponse> createStaff(@Valid @RequestBody StaffRequest request) {
        return ApiResponse.<StaffResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Staff created successfully")
                .data(staffService.createStaff(request))
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update staff information")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BRANCH_MANAGER')")
    public ApiResponse<StaffResponse> updateStaff(@PathVariable String id, @Valid @RequestBody StaffUpdateRequest request) {
        return ApiResponse.<StaffResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Staff updated successfully")
                .data(staffService.updateStaff(id, request))
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get staff by ID")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BRANCH_MANAGER') or hasRole('STAFF')")
    public ApiResponse<StaffResponse> getStaffById(@PathVariable String id) {
        return ApiResponse.<StaffResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Staff retrieved successfully")
                .data(staffService.getStaffById(id))
                .build();
    }

    @GetMapping
    @Operation(summary = "Get all staff")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BRANCH_MANAGER')")
    public ApiResponse<List<StaffResponse>> getAllStaff() {
        return ApiResponse.<List<StaffResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Staff retrieved successfully")
                .data(staffService.getAllStaff())
                .build();
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get staff by status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BRANCH_MANAGER')")
    public ApiResponse<List<StaffResponse>> getStaffByStatus(@PathVariable String status) {
        return ApiResponse.<List<StaffResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Staff by status retrieved successfully")
                .data(staffService.getStaffByStatus(status))
                .build();
    }

    @GetMapping("/paginated")
    @Operation(summary = "Get staff with pagination")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BRANCH_MANAGER')")
    public ApiResponse<PageResponse<StaffResponse>> getStaffWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<StaffResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Staff retrieved successfully with pagination")
                .data(staffService.getStaffWithPagination(page, size))
                .build();
    }

    @GetMapping("/search")
    @Operation(summary = "Search staff by multiple criteria")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BRANCH_MANAGER')")
    public ApiResponse<PageResponse<StaffResponse>> searchStaff(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<StaffResponse> staff = staffService.searchStaff(searchTerm, page, size);

        return ApiResponse.<PageResponse<StaffResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Search completed successfully")
                .data(staff)
                .build();
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get staff by email")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BRANCH_MANAGER')")
    public ApiResponse<StaffResponse> getStaffByEmail(@PathVariable String email) {
        return ApiResponse.<StaffResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Staff retrieved successfully")
                .data(staffService.getStaffByEmail(email))
                .build();
    }

    @GetMapping("/phone/{phone}")
    @Operation(summary = "Get staff by phone")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BRANCH_MANAGER')")
    public ApiResponse<StaffResponse> getStaffByPhone(@PathVariable String phone) {
        return ApiResponse.<StaffResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Staff retrieved successfully")
                .data(staffService.getStaffByPhone(phone))
                .build();
    }

    @GetMapping("/department/{department}")
    @Operation(summary = "Get staff by department")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BRANCH_MANAGER')")
    public ApiResponse<List<StaffResponse>> getStaffByDepartment(@PathVariable String department) {
        return ApiResponse.<List<StaffResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Staff by department retrieved successfully")
                .data(staffService.getStaffByDepartment(department))
                .build();
    }

    @PatchMapping("/{id}/disable")
    @Operation(summary = "Disable staff (set INACTIVE)")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BRANCH_MANAGER')")
    public ApiResponse<Void> disableStaff(@PathVariable String id) {
        staffService.disableStaff(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Staff disabled successfully")
                .build();
    }

    @PatchMapping("/{id}/enable")
    @Operation(summary = "Enable staff (set ACTIVE)")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BRANCH_MANAGER')")
    public ApiResponse<Void> enableStaff(@PathVariable String id) {
        staffService.enableStaff(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Staff enabled successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete staff")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BRANCH_MANAGER')")
    public ApiResponse<Void> deleteStaff(@PathVariable String id) {
        staffService.deleteStaff(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Staff deleted successfully")
                .build();
    }
}
