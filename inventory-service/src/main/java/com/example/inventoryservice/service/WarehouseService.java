//package com.example.inventoryservice.service;
//
//import com.example.inventoryservice.entity.Warehouse;
//import com.example.inventoryservice.enums.ErrorCode;
//import com.example.inventoryservice.enums.WarehouseStatus;
//import com.example.inventoryservice.exception.AppException;
//import com.example.inventoryservice.feign.UserClient;
//import com.example.inventoryservice.repository.WarehouseRepository;
//import com.example.inventoryservice.request.WarehouseRequest;
//import com.example.inventoryservice.response.*;
//import com.example.inventoryservice.service.inteface.IWarehouseService;
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Service;
//
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class WarehouseService implements IWarehouseService {
//
//    private final WarehouseRepository warehouseRepository;
//    private final UserClient userClient;
//
//    @Override
//    @Transactional
//    public WarehouseResponse createWarehouse(WarehouseRequest warehouseRequest) {
//        if (warehouseRepository.findByWarehouseNameAndIsDeletedFalse(warehouseRequest.getWarehouseName()).isPresent()){
//            throw new AppException(ErrorCode.WAREHOUSE_EXISTS);
//        }
//        if (warehouseRepository.findByUserIdAndIsDeletedFalse(warehouseRequest.getUserId()).isPresent()){
//            throw new AppException(ErrorCode.WAREHOUSE_EXISTS);
//        }
//
//        ApiResponse<UserResponse> response = userClient.getByUserId(warehouseRequest.getUserId());
//
//        Warehouse warehouse = Warehouse.builder()
//                .warehouseName(warehouseRequest.getWarehouseName())
//                .address(warehouseRequest.getAddress())
//                .latitude(warehouseRequest.getLatitude())
//                .longitude(warehouseRequest.getLongitude())
//                .userId(response.getData().getId())
//                .status(warehouseRequest.getStatus())
//                .capacity(warehouseRequest.getCapacity())
//                .build();
//
//        warehouseRepository.save(warehouse);
//        return toWarehouseResponse(warehouse);
//    }
//
//    @Override
//    @Transactional
//    public WarehouseResponse updateWarehouse(String warehouseId ,WarehouseRequest warehouseRequest) {
//        Warehouse warehouse = warehouseRepository.findByIdAndIsDeletedFalse(warehouseId)
//                        .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
//
//        warehouseRepository.findByWarehouseNameAndIsDeletedFalse(warehouseRequest.getWarehouseName())
//                .filter(u -> !u.getId().equals(warehouseId))
//                .ifPresent(u -> {
//                    throw new AppException(ErrorCode.WAREHOUSE_EXISTS);
//                });
//
//        warehouseRepository.findByUserIdAndIsDeletedFalse(warehouseRequest.getUserId())
//                .filter(u -> !u.getId().equals(warehouseId))
//                .ifPresent(u -> {
//                    throw new AppException(ErrorCode.USER_ALREADY_ASSIGNED_TO_WAREHOUSE);
//                });
//
//        ApiResponse<UserResponse> response = userClient.getByUserId(warehouseRequest.getUserId());
//
//        warehouse.setWarehouseName(warehouseRequest.getWarehouseName());
//        warehouse.setStatus(warehouseRequest.getStatus());
//        warehouse.setAddress(warehouseRequest.getAddress());
//        warehouse.setLatitude(warehouseRequest.getLatitude());
//        warehouse.setLongitude(warehouseRequest.getLongitude());
//        warehouse.setUserId(response.getData().getId());
//        warehouse.setCapacity(warehouseRequest.getCapacity());
//        warehouseRepository.save(warehouse);
//        return toWarehouseResponse(warehouse);
//    }
//
//    @Override
//    @Transactional
//    public void deleteWarehouse(String warehouseId) {
//        Warehouse warehouse = warehouseRepository.findByIdAndIsDeletedFalse(warehouseId)
//                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
//        warehouse.setIsDeleted(true);
//        warehouse.setStatus(WarehouseStatus.DELETED);
//        warehouseRepository.save(warehouse);
//    }
//
//    @Override
//    @Transactional
//    public void disableWarehouse(String warehouseId) {
//        Warehouse warehouse = warehouseRepository.findByIdAndIsDeletedFalse(warehouseId)
//                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
//        if (warehouse.getStatus().equals(WarehouseStatus.INACTIVE)){
//            warehouse.setStatus(WarehouseStatus.ACTIVE);
//        }else{
//            warehouse.setStatus(WarehouseStatus.INACTIVE);
//        }
//        warehouseRepository.save(warehouse);
//    }
//
//    @Override
//    public List<WarehouseResponse> getWarehouses() {
//        List<Warehouse> warehouses = warehouseRepository.findAll()
//                .stream()
//                .filter(warehouse -> !warehouse.getIsDeleted())
//                .toList();
//
//        List<WarehouseResponse> responses = new ArrayList<>();
//        for (Warehouse warehouse : warehouses) {
//            responses.add(toWarehouseResponse(warehouse));
//        }
//        return responses;
//    }
//
//    @Override
//    public WarehouseResponse getWarehouseById(String warehouseId) {
//        Warehouse warehouse = warehouseRepository.findByIdAndIsDeletedFalse(warehouseId)
//                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
//        return toWarehouseResponse(warehouse);
//    }
//
//    @Override
//    public PageResponse<WarehouseResponse> searchWarehouse(String request, int page, int size) {
//        Pageable pageable = PageRequest.of(page, size);
//        Page<Warehouse> warehousesPage = warehouseRepository.searchByKeywordNative(request, pageable);
//
//        List<WarehouseResponse> data = warehousesPage.getContent().stream()
//                .map(this::toWarehouseResponse)
//                .collect(Collectors.toList());
//
//        return new PageResponse<>(
//                data,
//                warehousesPage.getNumber(),
//                warehousesPage.getSize(),
//                warehousesPage.getTotalElements(),
//                warehousesPage.getTotalPages()
//        );
//    }
//
//    public WarehouseResponse toWarehouseResponse(Warehouse warehouse) {
//        return WarehouseResponse.builder()
//                .id(warehouse.getId())
//                .warehouseName(warehouse.getWarehouseName())
//                .address(warehouse.getAddress())
//                .latitude(warehouse.getLatitude())
//                .capacity(warehouse.getCapacity())
//                .longitude(warehouse.getLongitude())
//                .status(warehouse.getStatus())
//                .userId(warehouse.getUserId())
//                .build();
//    }
//}
