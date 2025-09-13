package com.example.userservice.controller;

import com.example.userservice.request.AddressRequest;
import com.example.userservice.response.AddressResponse;
import com.example.userservice.response.ApiResponse;
import com.example.userservice.response.PageResponse;
import com.example.userservice.service.inteface.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@Tag(name = "Address Controller")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    @Operation(summary = "Create a new address")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AddressResponse> createAddress(@Valid @RequestBody AddressRequest request) {
        return ApiResponse.<AddressResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Address created successfully")
                .data(addressService.createAddress(request))
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update address information")
    public ApiResponse<AddressResponse> updateAddress(@PathVariable Long id, @Valid @RequestBody AddressRequest request) {
        return ApiResponse.<AddressResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Address updated successfully")
                .data(addressService.updateAddress(id, request))
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get address by ID")
    public ApiResponse<AddressResponse> getAddressById(@PathVariable Long id) {
        return ApiResponse.<AddressResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Address retrieved successfully")
                .data(addressService.getAddressById(id))
                .build();
    }

    @GetMapping
    @Operation(summary = "Get all addresses")
    public ApiResponse<List<AddressResponse>> getAllAddresses() {
        return ApiResponse.<List<AddressResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Addresses retrieved successfully")
                .data(addressService.getAllAddresses())
                .build();
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get addresses by user ID")
    public ApiResponse<List<AddressResponse>> getAddressesByUserId(@PathVariable String userId) {
        return ApiResponse.<List<AddressResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("User addresses retrieved successfully")
                .data(addressService.getAddressesByUserId(userId))
                .build();
    }

    @GetMapping("/user/{userId}/default")
    @Operation(summary = "Get default address by user ID")
    public ApiResponse<AddressResponse> getDefaultAddressByUserId(@PathVariable String userId) {
        return ApiResponse.<AddressResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Default address retrieved successfully")
                .data(addressService.getDefaultAddressByUserId(userId))
                .build();
    }

    @GetMapping("/paginated")
    @Operation(summary = "Get addresses with pagination")
    public ApiResponse<PageResponse<AddressResponse>> getAddressesWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<AddressResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Addresses retrieved successfully with pagination")
                .data(addressService.getAddressesWithPagination(page, size))
                .build();
    }

    @GetMapping("/user/{userId}/paginated")
    @Operation(summary = "Get user addresses with pagination")
    public ApiResponse<PageResponse<AddressResponse>> getAddressesByUserWithPagination(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<AddressResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("User addresses retrieved successfully with pagination")
                .data(addressService.getAddressesByUserWithPagination(userId, page, size))
                .build();
    }

    @GetMapping("/search")
    @Operation(summary = "Search addresses by location")
    public ApiResponse<List<AddressResponse>> searchAddressByLocation(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String ward) {
        return ApiResponse.<List<AddressResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Address search completed successfully")
                .data(addressService.searchAddressByLocation(city, district, ward))
                .build();
    }

    @PatchMapping("/{id}/set-default")
    @Operation(summary = "Set address as default")
    public ApiResponse<Void> setDefaultAddress(@PathVariable Long id, @RequestParam String userId) {
        addressService.setDefaultAddress(id, userId);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Address set as default successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete address")
    public ApiResponse<Void> deleteAddress(@PathVariable Long id) {
        addressService.deleteAddress(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Address deleted successfully")
                .build();
    }
}
