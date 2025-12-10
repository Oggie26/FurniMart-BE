package com.example.inventoryservice.service;

import com.example.inventoryservice.entity.Warehouse;
import com.example.inventoryservice.enums.ErrorCode;
import com.example.inventoryservice.enums.WarehouseStatus;
import com.example.inventoryservice.exception.AppException;
import com.example.inventoryservice.feign.StoreClient;
import com.example.inventoryservice.repository.WarehouseRepository;
import com.example.inventoryservice.request.WarehouseRequest;
import com.example.inventoryservice.response.PageResponse;
import com.example.inventoryservice.response.StoreResponse;
import com.example.inventoryservice.response.WarehouseResponse;
import com.example.inventoryservice.service.inteface.WarehouseService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final StoreClient storeClient;

    @Override
    @Transactional
    public WarehouseResponse createWarehouse(WarehouseRequest warehouseRequest, String storeId) {

        warehouseRepository.findByWarehouseNameAndIsDeletedFalse(warehouseRequest.getWarehouseName())
                .ifPresent(u -> { throw new AppException(ErrorCode.WAREHOUSE_EXISTS);
                });

        if( getStore(storeId) == null){
            throw new AppException(ErrorCode.STORE_NOT_FOUND);
        }

        warehouseRepository.findByStoreIdAndIsDeletedFalse(storeId)
                .ifPresent(w -> { throw new AppException(ErrorCode.STORE_ALREADY_HAS_WAREHOUSE); });

        Warehouse warehouse = Warehouse.builder()
                .warehouseName(warehouseRequest.getWarehouseName())
                .storeId(storeId)
                .status(warehouseRequest.getStatus())
                .capacity(warehouseRequest.getCapacity())
                .build();

        warehouseRepository.save(warehouse);
        return toWarehouseResponse(warehouse);
    }

    @Override
    @Transactional
    public WarehouseResponse updateWarehouse(String storeId, String warehouseId, WarehouseRequest warehouseRequest) {
        Warehouse warehouse = warehouseRepository.findByIdAndIsDeletedFalse(warehouseId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));

        warehouseRepository.findByWarehouseNameAndIsDeletedFalse(warehouseRequest.getWarehouseName())
                .filter(u -> !u.getId().equals(warehouseId))
                .ifPresent(u -> {
                    throw new AppException(ErrorCode.WAREHOUSE_EXISTS);
                });

        if (getStore(storeId) == null) {
            throw new AppException(ErrorCode.STORE_NOT_FOUND);
        }

        warehouseRepository.findByStoreIdAndIsDeletedFalse(storeId)
                .filter(w -> !w.getId().equals(warehouseId))
                .ifPresent(w -> {
                    throw new AppException(ErrorCode.STORE_ALREADY_HAS_WAREHOUSE);
                });

        warehouse.setWarehouseName(warehouseRequest.getWarehouseName());
        warehouse.setStatus(warehouseRequest.getStatus());
        warehouse.setCapacity(warehouseRequest.getCapacity());
        warehouse.setStoreId(storeId);

        warehouseRepository.save(warehouse);
        return toWarehouseResponse(warehouse);
    }


    @Override
    @Transactional
    public void deleteWarehouse(String warehouseId) {
        Warehouse warehouse = warehouseRepository.findByIdAndIsDeletedFalse(warehouseId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));

        warehouse.setIsDeleted(true);
        warehouse.setStatus(WarehouseStatus.DELETED);

        warehouseRepository.save(warehouse);
    }

    @Override
    @Transactional
    public void disableWarehouse(String warehouseId) {
        Warehouse warehouse = warehouseRepository.findByIdAndIsDeletedFalse(warehouseId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));

        if (warehouse.getStatus().equals(WarehouseStatus.INACTIVE)) {
            warehouse.setStatus(WarehouseStatus.ACTIVE);
        } else {
            warehouse.setStatus(WarehouseStatus.INACTIVE);
        }

        warehouseRepository.save(warehouse);
    }

    @Override
    public List<WarehouseResponse> getWarehouses() {
        List<Warehouse> warehouses = warehouseRepository.findAll()
                .stream()
                .filter(w -> !w.getIsDeleted())
                .toList();

        List<WarehouseResponse> responses = new ArrayList<>();
        for (Warehouse warehouse : warehouses) {
            responses.add(toWarehouseResponse(warehouse));
        }
        return responses;
    }

    @Override
    public WarehouseResponse getWarehouseById(String warehouseId) {
        Warehouse warehouse = warehouseRepository.findByIdAndIsDeletedFalse(warehouseId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        return toWarehouseResponse(warehouse);
    }

    @Override
    public WarehouseResponse getWarehouseByStoreId(String storeId) {
        Warehouse warehouse = warehouseRepository.findByStoreId(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        return toWarehouseResponse(warehouse);
    }

    @Override
    public PageResponse<WarehouseResponse> searchWarehouse(String request, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Warehouse> warehousesPage = warehouseRepository.searchByKeywordNative(request, pageable);

        List<WarehouseResponse> data = warehousesPage.getContent().stream()
                .map(this::toWarehouseResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                data,
                warehousesPage.getNumber(),
                warehousesPage.getSize(),
                warehousesPage.getTotalElements(),
                warehousesPage.getTotalPages()
        );
    }

    public WarehouseResponse toWarehouseResponse(Warehouse warehouse) {
        return WarehouseResponse.builder()
                .id(warehouse.getId())
                .warehouseName(warehouse.getWarehouseName())
                .capacity(warehouse.getCapacity())
                .zone(warehouse.getZones())
                .status(warehouse.getStatus())
                .zone(warehouse.getZones())
                .build();
    }

    private StoreResponse getStore(String storeId) {
        try {
            return storeClient.getStoreById(storeId).getData();
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_WAREHOUSE_STOREID);
        }
    }

}
