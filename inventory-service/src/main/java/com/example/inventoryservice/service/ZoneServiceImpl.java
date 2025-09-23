package com.example.inventoryservice.service;

import com.example.inventoryservice.entity.Warehouse;
import com.example.inventoryservice.entity.Zone;
import com.example.inventoryservice.enums.ErrorCode;
import com.example.inventoryservice.enums.ZoneStatus;
import com.example.inventoryservice.exception.AppException;
import com.example.inventoryservice.repository.WarehouseRepository;
import com.example.inventoryservice.repository.ZoneRepository;
import com.example.inventoryservice.request.ZoneRequest;
import com.example.inventoryservice.response.PageResponse;
import com.example.inventoryservice.response.ZoneResponse;
import com.example.inventoryservice.service.inteface.ZoneService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ZoneServiceImpl implements ZoneService {

    private final ZoneRepository zoneRepository;
    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional
    public ZoneResponse createZone(ZoneRequest zoneRequest) {
        Warehouse warehouse = warehouseRepository.findById(zoneRequest.getWarehouseId())
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));

        if (zoneRepository.findByZoneCodeAndIsDeletedFalse(zoneRequest.getZoneCode()).isPresent()) {
            throw new AppException(ErrorCode.ZONE_CODE_EXISTS);
        }

        if (zoneRepository.findByZoneNameAndIsDeletedFalse(zoneRequest.getZoneName()).isPresent()) {
            throw new AppException(ErrorCode.ZONE_NAME_EXISTS);
        }

        List<Zone> activeZones = zoneRepository.findByWarehouseIdAndIsDeletedFalse(warehouse.getId());

        int totalQuantity = activeZones.stream()
                .mapToInt(Zone::getQuantity)
                .sum();

        if (warehouse.getCapacity() > 0 && totalQuantity + zoneRequest.getQuantity() > warehouse.getCapacity()) {
            throw new AppException(ErrorCode.WAREHOUSE_FULL);
        }

        Zone zone = Zone.builder()
                .zoneCode(zoneRequest.getZoneCode())
                .zoneName(zoneRequest.getZoneName())
                .status(zoneRequest.getStatus())
                .description(zoneRequest.getDescription())
                .warehouse(warehouse)
                .quantity(zoneRequest.getQuantity() != null ? zoneRequest.getQuantity() : 0)
                .build();

        zoneRepository.save(zone);
        return toZoneResponse(zone);
    }

    @Override
    @Transactional
    public ZoneResponse updateZone(ZoneRequest zoneRequest, String zoneId) {
        Warehouse warehouse = warehouseRepository.findById(zoneRequest.getWarehouseId())
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));

        zoneRepository.findByZoneCodeAndIsDeletedFalse(zoneRequest.getZoneCode())
                .filter(u -> !u.getId().equals(zoneId))
                .ifPresent(u -> {
                    throw new AppException(ErrorCode.ZONE_CODE_EXISTS);
                });

        zoneRepository.findByZoneNameAndIsDeletedFalse(zoneRequest.getZoneName())
                .filter(u -> !u.getId().equals(zoneId))
                .ifPresent(u -> {
                    throw new AppException(ErrorCode.ZONE_NAME_EXISTS);
                });

        Zone zone = zoneRepository.findByIdAndIsDeletedFalse(zoneId)
                .orElseThrow(() -> new AppException(ErrorCode.ZONE_NOT_FOUND));

        List<Zone> activeZones = zoneRepository.findByWarehouseIdAndIsDeletedFalse(warehouse.getId());
        int totalQuantity = activeZones.stream()
                .mapToInt(Zone::getQuantity)
                .sum();
        totalQuantity -= zone.getQuantity();

        if (warehouse.getCapacity() > 0  && totalQuantity + zoneRequest.getQuantity() > warehouse.getCapacity()) {
            throw new AppException(ErrorCode.WAREHOUSE_FULL);
        }

        zone.setZoneCode(zoneRequest.getZoneCode());
        zone.setZoneName(zoneRequest.getZoneName());
        zone.setDescription(zoneRequest.getDescription());
        zone.setStatus(zoneRequest.getStatus());
        zone.setQuantity(zoneRequest.getQuantity());
        zone.setWarehouse(warehouse);

        zoneRepository.save(zone);
        return toZoneResponse(zone);
    }


    @Override
    @Transactional
    public void deleteZone(String zoneId) {
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new AppException(ErrorCode.ZONE_NOT_FOUND));

        if (zone.getIsDeleted()) {
            throw new AppException(ErrorCode.ZONE_ALREADY_DELETED);
        }

        Warehouse warehouse = zone.getWarehouse();
        warehouse.setCapacity(warehouse.getCapacity() + zone.getQuantity());
        warehouseRepository.save(warehouse);

        zone.setIsDeleted(true);
        zoneRepository.save(zone);
    }

    @Override
    @Transactional
    public void disableZone(String zoneId) {
        Zone zone = zoneRepository.findByIdAndIsDeletedFalse(zoneId)
                .orElseThrow(() -> new AppException(ErrorCode.ZONE_NOT_FOUND));
        if(zone.getStatus().equals(ZoneStatus.ACTIVE)) {
            zone.setStatus(ZoneStatus.INACTIVE);
        }else{
            zone.setStatus(ZoneStatus.ACTIVE);
        }
        zoneRepository.save(zone);
    }

    @Override
    public ZoneResponse getZoneById(String zoneId) {
        Zone zone = zoneRepository.findByIdAndIsDeletedFalse(zoneId)
                .orElseThrow(() -> new AppException(ErrorCode.ZONE_NOT_FOUND));
        return toZoneResponse(zone);
    }

    @Override
    public List<ZoneResponse> getZonesById(String warehouseId) {
        Warehouse warehouse = warehouseRepository.findByIdAndIsDeletedFalse(warehouseId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));

        List<Zone> zones = zoneRepository.findByWarehouseIdAndIsDeletedFalse(warehouseId);

        return zones.stream()
                .map(this::toZoneResponse)
                .toList();
    }



    @Override
    public PageResponse<ZoneResponse> searchZone(String request, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Zone> zonePage = zoneRepository.searchByKeywordNative(request, pageable);

        List<ZoneResponse> data = zonePage.getContent().stream()
                .map(this::toZoneResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                data,
                zonePage.getNumber(),
                zonePage.getSize(),
                zonePage.getTotalElements(),
                zonePage.getTotalPages()
        );
    }

    public ZoneResponse toZoneResponse(Zone zone) {
        return ZoneResponse.builder()
                .id(zone.getId())
                .description(zone.getDescription())
                .zoneName(zone.getZoneName())
                .zoneCode(zone.getZoneCode())
                .status(zone.getStatus())
                .quantity(zone.getQuantity())
                .build();
    }
}
