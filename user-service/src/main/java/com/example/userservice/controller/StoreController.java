package com.example.userservice.controller;

import com.example.userservice.request.StoreDistance;
import com.example.userservice.request.StoreRequest;
import com.example.userservice.request.EmployeeStoreRequest;
import com.example.userservice.response.*;
import com.example.userservice.service.inteface.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stores")
@Tag(name = "Store Controller")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class StoreController {
    
    private final StoreService storeService;

    @PostMapping
    @Operation(summary = "Create new store")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StoreResponse> createStore(@Valid @RequestBody StoreRequest request) {
        return ApiResponse.<StoreResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Store created successfully")
                .data(storeService.createStore(request))
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update store information")
    public ApiResponse<StoreResponse> updateStore(@PathVariable String id, @Valid @RequestBody StoreRequest request) {
        return ApiResponse.<StoreResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Store updated successfully")
                .data(storeService.updateStore(id, request))
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get store by ID")
    public ApiResponse<StoreResponse> getStoreById(@PathVariable String id) {
        return ApiResponse.<StoreResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Store retrieved successfully")
                .data(storeService.getStoreById(id))
                .build();
    }

    @GetMapping
    @Operation(summary = "Get all stores")
    public ApiResponse<List<StoreResponse>> getAllStores() {
        return ApiResponse.<List<StoreResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Stores retrieved successfully")
                .data(storeService.getAllStores())
                .build();
    }

    @GetMapping("/paginated")
    @Operation(summary = "Get stores with pagination")
    public ApiResponse<PageResponse<StoreResponse>> getStoresWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        
        Sort sort = sortDirection.equalsIgnoreCase("desc") 
            ? Sort.by(sortBy).descending() 
            : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        return ApiResponse.<PageResponse<StoreResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Stores retrieved successfully")
                .data(storeService.getStoresWithPagination(pageable))
                .build();
    }

    @GetMapping("/search")
    @Operation(summary = "Search stores")
    public ApiResponse<PageResponse<StoreResponse>> searchStores(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        
        Sort sort = sortDirection.equalsIgnoreCase("desc") 
            ? Sort.by(sortBy).descending() 
            : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        return ApiResponse.<PageResponse<StoreResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Stores search completed successfully")
                .data(storeService.searchStores(searchTerm, pageable))
                .build();
    }

    @GetMapping("/city/{city}")
    @Operation(summary = "Get stores by city")
    public ApiResponse<List<StoreResponse>> getStoresByCity(@PathVariable String city) {
        return ApiResponse.<List<StoreResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Stores retrieved successfully")
                .data(storeService.getStoresByCity(city))
                .build();
    }

    @GetMapping("/district/{district}")
    @Operation(summary = "Get stores by district")
    public ApiResponse<List<StoreResponse>> getStoresByDistrict(@PathVariable String district) {
        return ApiResponse.<List<StoreResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Stores retrieved successfully")
                .data(storeService.getStoresByDistrict(district))
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete store")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> deleteStore(@PathVariable String id) {
        storeService.deleteStore(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.NO_CONTENT.value())
                .message("Store deleted successfully")
                .build();
    }

    @PostMapping("/employees")
    @Operation(
        summary = "Add employee to store",
        description = "Assign an employee to a store. Requires employeeId and storeId in the request body."
    )
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<EmployeeStoreResponse> addEmployeeToStore(@Valid @RequestBody EmployeeStoreRequest request) {
        return ApiResponse.<EmployeeStoreResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Employee added to store successfully")
                .data(storeService.addUserToStore(request))
                .build();
    }

    @DeleteMapping("/employees/{employeeId}/stores/{storeId}")
    @Operation(
        summary = "Remove employee from store",
        description = "Remove an employee from a store. Requires employeeId and storeId as path parameters."
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> removeEmployeeFromStore(@PathVariable String employeeId, @PathVariable String storeId) {
        storeService.removeEmployeeFromStore(employeeId, storeId);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.NO_CONTENT.value())
                .message("Employee removed from store successfully")
                .build();
    }

    @GetMapping("/employees/{employeeId}")
    @Operation(
        summary = "Get stores by employee ID",
        description = "Retrieve all stores assigned to a specific employee. Returns a list of stores."
    )
    public ApiResponse<List<StoreResponse>> getStoresByEmployeeId(@PathVariable String employeeId) {
        return ApiResponse.<List<StoreResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Stores retrieved successfully")
                .data(storeService.getStoresByEmployeeId(employeeId))
                .build();
    }

    @GetMapping("/{storeId}/employees")
    @Operation(
        summary = "Get employees by store ID",
        description = "Retrieve all employees assigned to a specific store. Returns a list of employee-store relationships."
    )
    public ApiResponse<List<EmployeeStoreResponse>> getEmployeesByStoreId(@PathVariable String storeId) {
        return ApiResponse.<List<EmployeeStoreResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Employees retrieved successfully")
                .data(storeService.getEmployeesByStoreId(storeId))
                .build();
    }

    @GetMapping("/nearest/list")
    @Operation(summary = "Lấy danh sách cửa hàng gần nhất theo vị trí (lat, lon)")
    public ApiResponse<List<StoreDistance>> getNearestStores(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "5") int limit) {

        List<StoreDistance> nearestStores = storeService.findNearestStores(lat, lon, limit);

        return ApiResponse.<List<StoreDistance>>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy danh sách cửa hàng gần nhất thành công")
                .data(nearestStores)
                .build();
    }
}
