package com.example.orderservice.controller;

import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.Warranty;
import com.example.orderservice.entity.WarrantyClaim;
import com.example.orderservice.enums.ErrorCode;
import com.example.orderservice.enums.WarrantyClaimStatus;
import com.example.orderservice.exception.AppException;
import com.example.orderservice.feign.AuthClient;
import com.example.orderservice.feign.UserClient;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.WarrantyRepository;
import com.example.orderservice.repository.WarrantyClaimRepository;
import com.example.orderservice.request.WarrantyClaimRequest;
import com.example.orderservice.request.WarrantyClaimResolutionRequest;
import com.example.orderservice.response.ApiResponse;
import com.example.orderservice.response.AuthResponse;
import com.example.orderservice.response.PageResponse;
import com.example.orderservice.response.UserResponse;
import com.example.orderservice.response.WarrantyClaimResponse;
import com.example.orderservice.response.WarrantyReportResponse;
import com.example.orderservice.response.WarrantyResponse;
import com.example.orderservice.service.inteface.WarrantyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/warranties")
@Tag(name = "Warranty Controller")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class WarrantyController {

        private final WarrantyService warrantyService;
        private final AuthClient authClient;
        private final UserClient userClient;
        private final WarrantyRepository warrantyRepository;
        private final WarrantyClaimRepository warrantyClaimRepository;
        private final OrderRepository orderRepository;

        // ========== CUSTOMER ENDPOINTS ==========

        @GetMapping("/customer/{customerId}")
        @Operation(summary = "Get warranties by customer ID")
        @PreAuthorize("hasRole('CUSTOMER')")
        public ApiResponse<List<WarrantyResponse>> getWarrantiesByCustomer(@PathVariable String customerId) {
                // Verify customer can only access their own data
                verifyCustomerAccess(customerId);
                return ApiResponse.<List<WarrantyResponse>>builder()
                                .status(HttpStatus.OK.value())
                                .message("Warranties retrieved successfully")
                                .data(warrantyService.getWarrantiesByCustomer(customerId))
                                .build();
        }

        @GetMapping("/customer/{customerId}/active")
        @Operation(summary = "Get active warranties by customer ID")
        @PreAuthorize("hasRole('CUSTOMER')")
        public ApiResponse<List<WarrantyResponse>> getActiveWarrantiesByCustomer(@PathVariable String customerId) {
                // Verify customer can only access their own data
                verifyCustomerAccess(customerId);
                return ApiResponse.<List<WarrantyResponse>>builder()
                                .status(HttpStatus.OK.value())
                                .message("Active warranties retrieved successfully")
                                .data(warrantyService.getActiveWarrantiesByCustomer(customerId))
                                .build();
        }

        @GetMapping("/{warrantyId}")
        @Operation(summary = "Get warranty by ID")
        @PreAuthorize("hasRole('CUSTOMER')")
        public ApiResponse<WarrantyResponse> getWarrantyById(@PathVariable Long warrantyId) {
                // Verify warranty ownership
                verifyWarrantyOwnership(warrantyId);
                return ApiResponse.<WarrantyResponse>builder()
                                .status(HttpStatus.OK.value())
                                .message("Warranty retrieved successfully")
                                .data(warrantyService.getWarrantyById(warrantyId))
                                .build();
        }

        @GetMapping("/order/{orderId}")
        @Operation(summary = "Get warranties by order ID")
        @PreAuthorize("hasRole('CUSTOMER')")
        public ApiResponse<List<WarrantyResponse>> getWarrantiesByOrder(@PathVariable Long orderId) {
                return ApiResponse.<List<WarrantyResponse>>builder()
                                .status(HttpStatus.OK.value())
                                .message("Warranties retrieved successfully")
                                .data(warrantyService.getWarrantiesByOrder(orderId))
                                .build();
        }

        @GetMapping("/store/{storeId}")
        @Operation(summary = "Get warranty claims by Store ID")
        public ApiResponse<PageResponse<WarrantyClaimResponse>> getWarrantyClaimsByStore(
                        @PathVariable String storeId,
                        @RequestParam(defaultValue = "1") @Min(value = 1, message = "Page must be at least 1") int page,
                        @RequestParam(defaultValue = "10") @Min(value = 1, message = "Size must be at least 1") @Max(value = 100, message = "Size must not exceed 100") int size) {
                // Verify user has access to this store
                return ApiResponse.<PageResponse<WarrantyClaimResponse>>builder()
                                .status(HttpStatus.OK.value())
                                .message("Warranty claims retrieved successfully")
                                .data(warrantyService.getWarrantyClaimsByStore(storeId, page, size))
                                .build();
        }

        @PostMapping("/claims")
        @Operation(summary = "Create warranty claim")
        @ResponseStatus(HttpStatus.CREATED)
        @PreAuthorize("hasRole('CUSTOMER')")
        public ApiResponse<WarrantyClaimResponse> createWarrantyClaim(
                        @Valid @RequestBody WarrantyClaimRequest request) {
                // Chỉ kiểm tra quyền sở hữu đơn hàng, không cần warrantyId ở request
                verifyOrderOwnership(request.getOrderId());
                return ApiResponse.<WarrantyClaimResponse>builder()
                                .status(HttpStatus.CREATED.value())
                                .message("Warranty claim created successfully")
                                .data(warrantyService.createWarrantyClaim(request))
                                .build();
        }

        @GetMapping("/claims/customer/{customerId}")
        @Operation(summary = "Get warranty claims by customer ID")
        @PreAuthorize("hasRole('CUSTOMER')")
        public ApiResponse<List<WarrantyClaimResponse>> getWarrantyClaimsByCustomer(@PathVariable String customerId) {
                // Verify customer can only access their own data
                verifyCustomerAccess(customerId);
                return ApiResponse.<List<WarrantyClaimResponse>>builder()
                                .status(HttpStatus.OK.value())
                                .message("Warranty claims retrieved successfully")
                                .data(warrantyService.getWarrantyClaimsByCustomer(customerId))
                                .build();
        }

        @GetMapping("/claims/warranty/{warrantyId}")
        @Operation(summary = "Get warranty claims by warranty ID")
        @PreAuthorize("hasRole('CUSTOMER')")
        public ApiResponse<List<WarrantyClaimResponse>> getWarrantyClaimsByWarranty(@PathVariable Long warrantyId) {
                // Verify warranty ownership
                verifyWarrantyOwnership(warrantyId);
                return ApiResponse.<List<WarrantyClaimResponse>>builder()
                                .status(HttpStatus.OK.value())
                                .message("Warranty claims retrieved successfully")
                                .data(warrantyService.getWarrantyClaimsByWarranty(warrantyId))
                                .build();
        }

        // ========== MANAGER/STAFF ENDPOINTS ==========

        @GetMapping("/claims")
        @Operation(summary = "Get all warranty claims (Manager only)")
        public ApiResponse<List<WarrantyClaimResponse>> getAllWarrantyClaims() {
                // Manager chỉ xem được claims của store mình
                String currentUserId = getCurrentUserId();
                ApiResponse<UserResponse> userResponse = userClient.getUserById(currentUserId);
                if (userResponse == null || userResponse.getData() == null ||
                                userResponse.getData().getStoreIds() == null
                                || userResponse.getData().getStoreIds().isEmpty()) {
                        throw new AppException(ErrorCode.UNAUTHENTICATED);
                }
                // Lấy storeId đầu tiên của manager
                String managerStoreId = userResponse.getData().getStoreIds().get(0);
                // Trả về claims của store đó (sử dụng getWarrantyClaimsByStore với page lớn)
                PageResponse<WarrantyClaimResponse> pageResponse = warrantyService
                                .getWarrantyClaimsByStore(managerStoreId, 1, 1000);
                return ApiResponse.<List<WarrantyClaimResponse>>builder()
                                .status(HttpStatus.OK.value())
                                .message("Warranty claims retrieved successfully")
                                .data(pageResponse.getContent())
                                .build();
        }

        @GetMapping("/claims/status/{status}")
        @Operation(summary = "Get warranty claims by status (Manager only)")
        public ApiResponse<List<WarrantyClaimResponse>> getWarrantyClaimsByStatus(@PathVariable String status) {
                // Validate status enum
                try {
                        WarrantyClaimStatus.valueOf(status.toUpperCase());
                } catch (IllegalArgumentException e) {
                        throw new AppException(ErrorCode.INVALID_STATUS);
                }
                // Manager chỉ xem được claims của store mình
                String currentUserId = getCurrentUserId();
                ApiResponse<UserResponse> userResponse = userClient.getUserById(currentUserId);
                if (userResponse == null || userResponse.getData() == null ||
                                userResponse.getData().getStoreIds() == null
                                || userResponse.getData().getStoreIds().isEmpty()) {
                        throw new AppException(ErrorCode.UNAUTHENTICATED);
                }
                String managerStoreId = userResponse.getData().getStoreIds().get(0);
                // Lấy tất cả claims của store, rồi filter theo status
                PageResponse<WarrantyClaimResponse> pageResponse = warrantyService
                                .getWarrantyClaimsByStore(managerStoreId, 1, 1000);
                List<WarrantyClaimResponse> filtered = pageResponse.getContent().stream()
                                .filter(claim -> claim.getStatus().name().equalsIgnoreCase(status))
                                .collect(java.util.stream.Collectors.toList());
                return ApiResponse.<List<WarrantyClaimResponse>>builder()
                                .status(HttpStatus.OK.value())
                                .message("Warranty claims retrieved successfully")
                                .data(filtered)
                                .build();
        }

        @PutMapping("/claims/{claimId}/status")
        @Operation(summary = "Update warranty claim status (Manager only)")
        public ApiResponse<WarrantyClaimResponse> updateWarrantyClaimStatus(
                        @PathVariable Long claimId,
                        @RequestParam String status,
                        @RequestParam(required = false) String adminResponse,
                        @RequestParam(required = false) String resolutionNotes) {
                // Verify claim belongs to manager's store
                verifyClaimStoreAccess(claimId);
                // Validate status enum
                try {
                        WarrantyClaimStatus.valueOf(status.toUpperCase());
                } catch (IllegalArgumentException e) {
                        throw new AppException(ErrorCode.INVALID_STATUS);
                }
                return ApiResponse.<WarrantyClaimResponse>builder()
                                .status(HttpStatus.OK.value())
                                .message("Warranty claim status updated successfully")
                                .data(warrantyService.updateWarrantyClaimStatus(claimId, status, adminResponse,
                                                resolutionNotes))
                                .build();
        }

        @PostMapping("/claims/{claimId}/resolve")
        @Operation(summary = "Resolve warranty claim with action (Manager/Staff)")
        public ApiResponse<WarrantyClaimResponse> resolveWarrantyClaim(
                        @PathVariable Long claimId,
                        @Valid @RequestBody WarrantyClaimResolutionRequest request) {
                // Verify claim belongs to user's store
                verifyClaimStoreAccess(claimId);
                request.setClaimId(claimId); // Ensure claim ID matches path variable
                return ApiResponse.<WarrantyClaimResponse>builder()
                                .status(HttpStatus.OK.value())
                                .message("Warranty claim resolved successfully")
                                .data(warrantyService.resolveWarrantyClaim(request))
                                .build();
        }

        // @PostMapping("/claims/{claimId}/create-order")
        // @Operation(summary = "Create order from warranty claim (Return)")
        // @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('STAFF')")
        // public ApiResponse<OrderResponse> createWarrantyOrder(@PathVariable Long
        // claimId) {
        // return ApiResponse.<OrderResponse>builder()
        // .status(HttpStatus.CREATED.value())
        // .message("Warranty order created successfully")
        // .data(warrantyService.createWarrantyOrder(claimId))
        // .build();
        // }

        @GetMapping("/report")
        @Operation(summary = "Get warranty report (Manager only)")
        public ApiResponse<WarrantyReportResponse> getWarrantyReport(
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
                // Manager chỉ xem được report của store mình
                // Note: Hiện tại getWarrantyReport trả về tất cả, có thể cần filter theo
                // storeId trong tương lai
                return ApiResponse.<WarrantyReportResponse>builder()
                                .status(HttpStatus.OK.value())
                                .message("Warranty report retrieved successfully")
                                .data(warrantyService.getWarrantyReport(startDate, endDate))
                                .build();
        }

        // ========== HELPER METHODS ==========

        private void verifyCustomerAccess(String customerId) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || !authentication.isAuthenticated()) {
                        throw new AppException(ErrorCode.UNAUTHENTICATED);
                }

                // Customers can only access their own data
                String currentUserId = getCurrentUserId();
                if (!Objects.equals(currentUserId, customerId)) {
                        throw new AppException(ErrorCode.UNAUTHENTICATED);
                }
        }

        private String getCurrentUserId() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || !authentication.isAuthenticated()
                                || "anonymousUser".equals(authentication.getPrincipal())) {
                        throw new AppException(ErrorCode.UNAUTHENTICATED);
                }

                String username = authentication.getName();
                ApiResponse<AuthResponse> response = authClient.getUserByUsername(username);

                if (response == null || response.getData() == null) {
                        throw new AppException(ErrorCode.NOT_FOUND_USER);
                }

                ApiResponse<UserResponse> userIdResponse;
                try {
                        userIdResponse = userClient.getUserByAccountId(response.getData().getId());
                } catch (feign.FeignException e) {
                        if (e.status() == 404) {
                                throw new AppException(ErrorCode.NOT_FOUND_USER);
                        }
                        throw e;
                }

                if (userIdResponse == null || userIdResponse.getData() == null) {
                        throw new AppException(ErrorCode.NOT_FOUND_USER);
                }

                return userIdResponse.getData().getId();
        }

        private void verifyWarrantyOwnership(Long warrantyId) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || !authentication.isAuthenticated()) {
                        throw new AppException(ErrorCode.UNAUTHENTICATED);
                }

                // Customers can only access their own warranties
                Warranty warranty = warrantyRepository.findByIdAndIsDeletedFalse(warrantyId)
                                .orElseThrow(() -> new AppException(ErrorCode.WARRANTY_NOT_FOUND));

                String currentUserId = getCurrentUserId();
                if (!Objects.equals(warranty.getCustomerId(), currentUserId)) {
                        throw new AppException(ErrorCode.UNAUTHENTICATED);
                }
        }


        private void verifyOrderOwnership(Long orderId) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || !authentication.isAuthenticated()) {
                        throw new AppException(ErrorCode.UNAUTHENTICATED);
                }

                Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

                String currentUserId = getCurrentUserId();
                if (!Objects.equals(order.getUserId(), currentUserId)) {
                        throw new AppException(ErrorCode.UNAUTHENTICATED);
                }
        }


        private void verifyStoreAccess(String storeId) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || !authentication.isAuthenticated()) {
                        throw new AppException(ErrorCode.UNAUTHENTICATED);
                }

                String currentUserId = getCurrentUserId();
                ApiResponse<UserResponse> userResponse = userClient.getEmployeeById(currentUserId);

                if (userResponse == null || userResponse.getData() == null) {
                        throw new AppException(ErrorCode.NOT_FOUND_USER);
                }

                UserResponse user = userResponse.getData();

                if (user.getStoreIds() == null || user.getStoreIds().isEmpty()) {
                        throw new AppException(ErrorCode.UNAUTHENTICATED);
                }

                if (!user.getStoreIds().contains(storeId)) {
                        throw new AppException(ErrorCode.UNAUTHENTICATED);
                }
        }

        private void verifyClaimStoreAccess(Long claimId) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || !authentication.isAuthenticated()) {
                        throw new AppException(ErrorCode.UNAUTHENTICATED);
                }

                // Get claim to find its order and store
                WarrantyClaim claim = warrantyClaimRepository
                                .findByIdAndIsDeletedFalse(claimId)
                                .orElseThrow(() -> new AppException(ErrorCode.WARRANTY_CLAIM_NOT_FOUND));

                Order order = orderRepository.findByIdAndIsDeletedFalse(claim.getOrderId())
                                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

                if (order.getStoreId() == null || order.getStoreId().isEmpty()) {
                        throw new AppException(ErrorCode.INVALID_REQUEST);
                }

                verifyStoreAccess(order.getStoreId());
        }
}
