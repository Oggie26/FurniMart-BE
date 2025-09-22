package com.example.inventoryservice.service;

import com.example.inventoryservice.entity.LocationItem;
import com.example.inventoryservice.entity.Zone;
import com.example.inventoryservice.enums.EnumStatus;
import com.example.inventoryservice.enums.ErrorCode;
import com.example.inventoryservice.exception.AppException;
import com.example.inventoryservice.repository.LocationItemRepository;
import com.example.inventoryservice.repository.ZoneRepository;
import com.example.inventoryservice.request.LocationItemRequest;
import com.example.inventoryservice.response.LocationItemResponse;
import com.example.inventoryservice.response.PageResponse;
import com.example.inventoryservice.service.inteface.LocationItemService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationItemServiceImpl implements LocationItemService {

    private final LocationItemRepository locationItemRepository;
    private final ZoneRepository zoneRepository;

    @Override
    @Transactional
    public LocationItemResponse createLocationItem(LocationItemRequest request) {
        Zone zone = zoneRepository.findById(request.getZoneId())
                .orElseThrow(() -> new AppException(ErrorCode.ZONE_NOT_FOUND));

        locationItemRepository.findByZoneIdAndRowLabelAndColumnNumberAndIsDeletedFalse(
                zone.getId(), request.getRowLabel(), request.getColumnNumber()
        ).ifPresent(li -> {
            throw new AppException(ErrorCode.LOCATIONITEM_EXISTS);
        });

        LocationItem locationItem = LocationItem.builder()
                .description(request.getDescription())
                .status(request.getStatus())
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

        locationItem.setRowLabel(request.getRowLabel());
        locationItem.setColumnNumber(request.getColumnNumber());
        locationItem.setDescription(request.getDescription());
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

    public LocationItemResponse toLocationItemResponse(LocationItem li) {
        return LocationItemResponse.builder()
                .id(li.getId())
                .code(li.getCode())
                .description(li.getDescription())
                .columnNumber(li.getColumnNumber())
                .rowLabel(li.getRowLabel())
                .status(li.getStatus())
                .build();
    }
}
