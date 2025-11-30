package com.example.inventoryservice.service;

import com.example.inventoryservice.entity.InventoryItem;
import com.example.inventoryservice.entity.LocationItem;
import com.example.inventoryservice.entity.Zone;
import com.example.inventoryservice.enums.EnumStatus;
import com.example.inventoryservice.enums.ErrorCode;
import com.example.inventoryservice.exception.AppException;
import com.example.inventoryservice.feign.ProductClient;
import com.example.inventoryservice.repository.InventoryItemRepository;
import com.example.inventoryservice.repository.LocationItemRepository;
import com.example.inventoryservice.repository.ZoneRepository;
import com.example.inventoryservice.request.LocationItemRequest;
import com.example.inventoryservice.response.*;
import com.example.inventoryservice.service.inteface.LocationItemService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocationItemServiceImpl implements LocationItemService {

    private final LocationItemRepository locationItemRepository;
    private final ZoneRepository zoneRepository;
    private final ProductClient productClient;
    private final InventoryItemRepository inventoryItemRepository;

    @Override
    @Transactional
    public LocationItemResponse createLocationItem(LocationItemRequest request) {
        Zone zone = zoneRepository.findById(request.getZoneId())
                .orElseThrow(() -> new AppException(ErrorCode.ZONE_NOT_FOUND));

        locationItemRepository.findByZoneIdAndRowLabelAndColumnNumberAndIsDeletedFalse(
                        zone.getWarehouse().getId(), request.getRowLabel(), request.getColumnNumber()
                )
                .ifPresent(li -> {
                    throw new AppException(ErrorCode.LOCATIONITEM_EXISTS);
                });

        validateZoneCapacity(zone, request.getQuantity(), null);

        LocationItem locationItem = LocationItem.builder()
                .description(request.getDescription())
                .status(request.getStatus())
                .quantity(request.getQuantity())
                .rowLabel(request.getRowLabel())
                .columnNumber(request.getColumnNumber())
                .zone(zone)
                .build();

        locationItem.generateCode();
        locationItemRepository.save(locationItem);
        return toLocationItemResponse(locationItem);
    }

    @Override
    @Transactional
    public LocationItemResponse updateLocationItem(LocationItemRequest request, String locationItemId) {
        LocationItem locationItem = locationItemRepository.findById(locationItemId)
                .orElseThrow(() -> new AppException(ErrorCode.LOCATIONITEM_NOT_FOUND));

        Zone zone = zoneRepository.findById(request.getZoneId())
                .orElseThrow(() -> new AppException(ErrorCode.ZONE_NOT_FOUND));

        locationItemRepository.findByZoneIdAndRowLabelAndColumnNumberAndIsDeletedFalse(
                        zone.getId(), request.getRowLabel(), request.getColumnNumber()
                ).filter(li -> !li.getId().equals(locationItemId))
                .ifPresent(li -> {
                    throw new AppException(ErrorCode.LOCATIONITEM_EXISTS);
                });

        validateZoneCapacity(zone, request.getQuantity(), locationItemId);

        locationItem.setRowLabel(request.getRowLabel());
        locationItem.setColumnNumber(request.getColumnNumber());
        locationItem.setDescription(request.getDescription());
        locationItem.setQuantity(request.getQuantity());
        locationItem.setStatus(request.getStatus());
        locationItem.setZone(zone);

        locationItem.generateCode();
        locationItemRepository.save(locationItem);

        return toLocationItemResponse(locationItem);
    }

    @Override
    @Transactional
    public void deleteLocationItem(String locationItemId) {
        LocationItem locationItem = locationItemRepository.findById(locationItemId)
                .orElseThrow(() -> new AppException(ErrorCode.LOCATIONITEM_NOT_FOUND));
        locationItem.setIsDeleted(true);
        locationItemRepository.save(locationItem);
    }

    @Override
    @Transactional
    public void disableLocationItem(String locationItemId) {
        LocationItem locationItem = locationItemRepository.findById(locationItemId)
                .orElseThrow(() -> new AppException(ErrorCode.LOCATIONITEM_NOT_FOUND));

        if (locationItem.getStatus() == EnumStatus.ACTIVE) {
            locationItem.setStatus(EnumStatus.INACTIVE);
        } else {
            locationItem.setStatus(EnumStatus.ACTIVE);
        }
        locationItemRepository.save(locationItem);
    }

    @Override
    public LocationItemResponse getLocationItemById(String locationItemId) {
        LocationItem locationItem = locationItemRepository.findById(locationItemId)
                .orElseThrow(() -> new AppException(ErrorCode.LOCATIONITEM_NOT_FOUND));
        return toLocationItemResponse(locationItem);
    }

    @Override
    public List<LocationItemResponse> getLocationItemsByZoneId(String zoneId) {
        List<LocationItem> locationItems = locationItemRepository.findByZoneIdAndIsDeletedFalse(zoneId);

        return locationItems.stream()
                .map(this::toLocationItemResponse)
                .toList();
    }

    @Override
    public PageResponse<LocationItemResponse> searchLocationItem(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LocationItem> locationItemPage = locationItemRepository.searchByKeywordNative(keyword, pageable);

        List<LocationItemResponse> data = locationItemPage.getContent().stream()
                .map(this::toLocationItemResponse)
                .toList();

        return new PageResponse<>(
                data,
                locationItemPage.getNumber(),
                locationItemPage.getSize(),
                locationItemPage.getTotalElements(),
                locationItemPage.getTotalPages()
        );
    }

    private LocationItemResponse toLocationItemResponse(LocationItem li) {
        List<InventoryItem> list = li.getInventoryItems();

        List<InventoryItemResponse> itemResponses = list != null
                ? list.stream()
                .collect(Collectors.groupingBy(InventoryItem::getProductColorId))
                .entrySet().stream()
                .map(entry -> {
                    List<InventoryItem> items = entry.getValue();
                    int totalQuantity = items.stream().mapToInt(InventoryItem::getQuantity).sum();
                    int totalReserved = items.stream().mapToInt(InventoryItem::getReservedQuantity).sum();
                    InventoryItem first = items.getFirst();

                    return InventoryItemResponse.builder()
                            .id(first.getId())
                            .productColorId(entry.getKey())
                            .quantity(totalQuantity)
                            .reservedQuantity(totalReserved)
                            .productName(getProductName(entry.getKey()))
                            .locationItem(li)
                            .inventoryId(first.getInventory() != null ? first.getInventory().getId() : null)
                            .build();
                })
                .toList()
                : Collections.emptyList();

        return LocationItemResponse.builder()
                .id(li.getId())
                .code(li.getCode())
                .description(li.getDescription())
                .columnNumber(li.getColumnNumber())
                .rowLabel(li.getRowLabel())
                .quantity(li.getQuantity())
                .currentQuantity(inventoryItemRepository.sumQuantityByLocationItemId(li.getId()))
                .itemResponse(itemResponses)
                .status(li.getStatus())
                .build();
    }

    private void validateZoneCapacity(Zone zone, Integer newQuantity, String excludeLocationItemId) {
        int totalQuantity = locationItemRepository
                .findByZoneIdAndIsDeletedFalse(zone.getId())
                .stream()
                .filter(li -> excludeLocationItemId == null || !li.getId().equals(excludeLocationItemId))
                .mapToInt(li -> li.getQuantity() == null ? 0 : li.getQuantity())
                .sum();

        int updatedTotal = totalQuantity + (newQuantity == null ? 0 : newQuantity);

        if (zone.getQuantity() != null && updatedTotal > zone.getQuantity()) {
            throw new AppException(ErrorCode.ZONE_CAPACITY_EXCEEDED);
        }
    }

    private String getProductName(String productColorId) {
        ApiResponse<ProductColorResponse> response = productClient.getProductColor(productColorId);
        if (response.getData() == null) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return response.getData().getProduct().getName();
    }
}
