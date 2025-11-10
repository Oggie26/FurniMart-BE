package com.example.userservice.service;

import com.example.userservice.entity.Employee;
import com.example.userservice.entity.EmployeeStore;
import com.example.userservice.entity.Store;
<<<<<<< HEAD
import com.example.userservice.enums.EnumRole;
import com.example.userservice.entity.User;
import com.example.userservice.entity.UserStore;
=======
>>>>>>> branch/phong
import com.example.userservice.enums.ErrorCode;
import com.example.userservice.exception.AppException;
import com.example.userservice.repository.EmployeeRepository;
import com.example.userservice.repository.EmployeeStoreRepository;
import com.example.userservice.repository.StoreRepository;
import com.example.userservice.request.StoreDistance;
import com.example.userservice.request.StoreRequest;
import com.example.userservice.request.EmployeeStoreRequest;
import com.example.userservice.response.PageResponse;
import com.example.userservice.response.StoreResponse;
import com.example.userservice.response.UserResponse;
import com.example.userservice.response.EmployeeStoreResponse;
import com.example.userservice.service.inteface.StoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeStoreRepository employeeStoreRepository;

    @Override
    @Transactional
    public StoreResponse createStore(StoreRequest request) {
        Store store = Store.builder()
                .name(request.getName())
                .city(request.getCity())
                .district(request.getDistrict())
                .ward(request.getWard())
                .street(request.getStreet())
                .addressLine(request.getAddressLine())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .status(request.getStatus())
                .build();

        Store savedStore = storeRepository.save(store);
        log.info("Store created with id: {}", savedStore.getId());
        
        return mapToStoreResponse(savedStore);
    }

    @Override
    @Transactional
    public StoreResponse updateStore(String id, StoreRequest request) {
        Store store = storeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));

        store.setName(request.getName());
        store.setCity(request.getCity());
        store.setDistrict(request.getDistrict());
        store.setWard(request.getWard());
        store.setStreet(request.getStreet());
        store.setAddressLine(request.getAddressLine());
        store.setLatitude(request.getLatitude());
        store.setLongitude(request.getLongitude());
        store.setStatus(request.getStatus());

        Store updatedStore = storeRepository.save(store);
        log.info("Store updated with id: {}", updatedStore.getId());
        
        return mapToStoreResponse(updatedStore);
    }

    @Override
    public StoreResponse getStoreById(String id) {
        Store store = storeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));
        
        return mapToStoreResponse(store);
    }

    @Override
    public List<StoreResponse> getAllStores() {
        List<Store> stores = storeRepository.findByIsDeletedFalse();
        return stores.stream()
                .map(this::mapToStoreResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<StoreResponse> getStoresWithPagination(Pageable pageable) {
        Page<Store> storePage = storeRepository.findByIsDeletedFalse(pageable);
        List<StoreResponse> storeResponses = storePage.getContent().stream()
                .map(this::mapToStoreResponse)
                .collect(Collectors.toList());

        return PageResponse.<StoreResponse>builder()
                .content(storeResponses)
                .number(storePage.getNumber())
                .size(storePage.getSize())
                .totalElements(storePage.getTotalElements())
                .totalPages(storePage.getTotalPages())
                .first(storePage.isFirst())
                .last(storePage.isLast())
                .build();
    }

    @Override
    public PageResponse<StoreResponse> searchStores(String searchTerm, Pageable pageable) {
        Page<Store> storePage = storeRepository.searchStores(searchTerm, pageable);
        List<StoreResponse> storeResponses = storePage.getContent().stream()
                .map(this::mapToStoreResponse)
                .collect(Collectors.toList());

        return PageResponse.<StoreResponse>builder()
                .content(storeResponses)
                .number(storePage.getNumber())
                .size(storePage.getSize())
                .totalElements(storePage.getTotalElements())
                .totalPages(storePage.getTotalPages())
                .first(storePage.isFirst())
                .last(storePage.isLast())
                .build();
    }

    @Override
    public List<StoreResponse> getStoresByCity(String city) {
        List<Store> stores = storeRepository.findByCityAndIsDeletedFalse(city);
        return stores.stream()
                .map(this::mapToStoreResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<StoreResponse> getStoresByDistrict(String district) {
        List<Store> stores = storeRepository.findByDistrictAndIsDeletedFalse(district);
        return stores.stream()
                .map(this::mapToStoreResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteStore(String id) {
        Store store = storeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));
        
        store.setIsDeleted(true);
        storeRepository.save(store);
        log.info("Store soft deleted with id: {}", id);
    }

    @Override
    @Transactional
    public EmployeeStoreResponse addUserToStore(EmployeeStoreRequest request) {
        String employeeId = request.getEmployeeId() != null ? request.getEmployeeId() : request.getUserId();
        log.info("Attempting to add employee {} to store {}", employeeId, request.getStoreId());
        
        try {
            // Check if employee exists
            Employee employee = employeeRepository.findEmployeeById(employeeId)
                    .orElseThrow(() -> {
                        log.error("Employee not found for store assignment with id: {}", employeeId);
                        return new AppException(ErrorCode.USER_NOT_FOUND);
                    });
            
            log.info("Found employee: {} (email: {})", employee.getFullName(), employee.getAccount().getEmail());
            
            // Check if store exists
            Store store = storeRepository.findByIdAndIsDeletedFalse(request.getStoreId())
                    .orElseThrow(() -> {
                        log.error("Store not found for employee assignment with id: {}", request.getStoreId());
                        return new AppException(ErrorCode.STORE_NOT_FOUND);
                    });
            
            log.info("Found store: {} (location: {})", store.getName(), store.getAddressLine());
            
            // Check if relationship already exists
            if (employeeStoreRepository.findByEmployeeIdAndStoreIdAndIsDeletedFalse(employeeId, request.getStoreId()).isPresent()) {
                log.warn("Employee-store relationship already exists for employee {} and store {}", 
                        employeeId, request.getStoreId());
                throw new AppException(ErrorCode.USER_STORE_RELATIONSHIP_EXISTS);
            }
            
            EmployeeStore employeeStore = EmployeeStore.builder()
                    .employeeId(employeeId)
                    .storeId(request.getStoreId())
                    .build();
            
            EmployeeStore savedEmployeeStore = employeeStoreRepository.save(employeeStore);
            log.info("Successfully added employee {} to store {} with relationship id: {}", 
                    employeeId, request.getStoreId(), savedEmployeeStore.getEmployeeId() + "_" + savedEmployeeStore.getStoreId());
            
            // Verify the relationship was actually saved
            Optional<EmployeeStore> verification = employeeStoreRepository.findByEmployeeIdAndStoreIdAndIsDeletedFalse(
                    employeeId, request.getStoreId());
            if (verification.isEmpty()) {
                log.error("CRITICAL: Employee-store relationship was not persisted! Employee: {}, Store: {}", 
                        employeeId, request.getStoreId());
                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
            
            log.info("Verified: Employee-store relationship successfully persisted");
            return mapToEmployeeStoreResponse(savedEmployeeStore);
            
        } catch (AppException e) {
            log.error("Application error adding employee {} to store {}: {}", 
                    employeeId, request.getStoreId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error adding employee {} to store {}", 
                    employeeId, request.getStoreId(), e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public void removeUserFromStore(String userId, String storeId) {
        EmployeeStore employeeStore = employeeStoreRepository.findByEmployeeIdAndStoreIdAndIsDeletedFalse(userId, storeId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_STORE_RELATIONSHIP_NOT_FOUND));
        
        employeeStore.setIsDeleted(true);
        employeeStoreRepository.save(employeeStore);
        log.info("Employee {} removed from store {}", userId, storeId);
    }

    @Override
    public List<StoreResponse> getStoresByUserId(String userId) {
        // userId is actually employeeId in this context
        List<Store> stores = storeRepository.findStoresByEmployeeId(userId);
        return stores.stream()
                .map(this::mapToStoreResponse)
                .collect(Collectors.toList());
    }

    @Override
<<<<<<< HEAD
    public List<UserStoreResponse> getUsersByStoreId(String storeId) {
        List<UserStore> userStores = userStoreRepository.findByStoreIdAndIsDeletedFalse(storeId);
        return userStores.stream()
                .filter(us -> {
                    EnumRole role = us.getUser() != null && us.getUser().getAccount() != null
                            ? us.getUser().getAccount().getRole()
                            : null;
                    return role == EnumRole.MANAGER || role == EnumRole.DELIVERY || role == EnumRole.STAFF;
                })
                .map(this::mapToUserStoreResponse)
=======
    public List<EmployeeStoreResponse> getUsersByStoreId(String storeId) {
        List<EmployeeStore> employeeStores = employeeStoreRepository.findByStoreIdAndIsDeletedFalse(storeId);
        return employeeStores.stream()
                .map(this::mapToEmployeeStoreResponse)
>>>>>>> branch/phong
                .collect(Collectors.toList());
    }

    @Override
    public Store getNearestStore(double lat, double lon) {
        return storeRepository.findNearestStore(lat, lon);
    }

    @Override
    public List<Store> getNearestStores(double lat, double lon, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return storeRepository.findNearestStores(lat, lon, pageable);
    }

    public List<StoreDistance> findNearestStores(double lat, double lon, int limit) {
        List<Store> stores = storeRepository.findAllWithCoordinates();

        return stores.stream()
                .map(store -> {
                    double distance = haversine(lat, lon, store.getLatitude(), store.getLongitude());
                    StoreResponse storeResponse = mapToStoreResponse(store);
                    return new StoreDistance(storeResponse, distance);
                })
                .sorted(Comparator.comparing(StoreDistance::getDistance))
                .limit(limit)
                .toList();
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    private StoreResponse mapToStoreResponse(Store store) {
        // Get all employees assigned to this store
        List<EmployeeStore> employeeStores = employeeStoreRepository.findByStoreIdAndIsDeletedFalse(store.getId());
        List<UserResponse> employees = employeeStores.stream()
                .map(employeeStore -> {
                    Employee employee = employeeStore.getEmployee();
                    return mapEmployeeToUserResponse(employee);
                })
                .collect(Collectors.toList());
        
        return StoreResponse.builder()
                .id(store.getId())
                .name(store.getName())
                .city(store.getCity())
                .district(store.getDistrict())
                .ward(store.getWard())
                .street(store.getStreet())
                .addressLine(store.getAddressLine())
                .latitude(store.getLatitude())
                .longitude(store.getLongitude())
                .status(store.getStatus())
                .users(employees)  // Populate employees list (using users field for backward compatibility)
                .createdAt(store.getCreatedAt())
                .updatedAt(store.getUpdatedAt())
                .build();
    }

    private UserResponse mapEmployeeToUserResponse(Employee employee) {
        List<String> storeIds = employeeStoreRepository.findByEmployeeIdAndIsDeletedFalse(employee.getId())
                .stream()
                .map(EmployeeStore::getStoreId)
                .collect(Collectors.toList());

        return UserResponse.builder()
                .id(employee.getId())
                .fullName(employee.getFullName())
                .email(employee.getAccount().getEmail())
                .phone(employee.getPhone())
                .gender(employee.getGender())
                .birthday(employee.getBirthday())
                .avatar(employee.getAvatar())
                .cccd(employee.getCccd())
                .point(null) // Employees don't have points
                .role(employee.getAccount().getRole())
                .status(employee.getStatus())
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .storeIds(storeIds)
                .build();
    }

    private EmployeeStoreResponse mapToEmployeeStoreResponse(EmployeeStore employeeStore) {
        Employee employee = employeeStore.getEmployee();
        Store store = employeeStore.getStore();
        
        return EmployeeStoreResponse.builder()
                .employeeId(employeeStore.getEmployeeId())
                .storeId(employeeStore.getStoreId())
                .employee(mapEmployeeToUserResponse(employee))
                .store(mapToStoreResponse(store))
                .createdAt(employeeStore.getCreatedAt())
                .updatedAt(employeeStore.getUpdatedAt())
                .build();
    }
}
