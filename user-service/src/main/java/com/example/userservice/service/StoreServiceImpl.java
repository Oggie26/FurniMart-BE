package com.example.userservice.service;

import com.example.userservice.entity.Store;
import com.example.userservice.entity.User;
import com.example.userservice.entity.UserStore;
import com.example.userservice.enums.ErrorCode;
import com.example.userservice.exception.AppException;
import com.example.userservice.repository.StoreRepository;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.repository.UserStoreRepository;
import com.example.userservice.request.StoreDistance;
import com.example.userservice.request.StoreRequest;
import com.example.userservice.request.UserStoreRequest;
import com.example.userservice.response.PageResponse;
import com.example.userservice.response.StoreResponse;
import com.example.userservice.response.UserResponse;
import com.example.userservice.response.UserStoreResponse;
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
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final UserStoreRepository userStoreRepository;

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
    public UserStoreResponse addUserToStore(UserStoreRequest request) {
        // Check if user exists
        User user = userRepository.findByIdAndIsDeletedFalse(request.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        // Check if store exists
        Store store = storeRepository.findByIdAndIsDeletedFalse(request.getStoreId())
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));
        
        // Check if relationship already exists
        if (userStoreRepository.findByUserIdAndStoreIdAndIsDeletedFalse(request.getUserId(), request.getStoreId()).isPresent()) {
            throw new AppException(ErrorCode.USER_STORE_RELATIONSHIP_EXISTS);
        }
        
        UserStore userStore = UserStore.builder()
                .userId(request.getUserId())
                .storeId(request.getStoreId())
                .user(user)
                .store(store)
                .build();
        
        UserStore savedUserStore = userStoreRepository.save(userStore);
        log.info("User {} added to store {}", request.getUserId(), request.getStoreId());
        
        return mapToUserStoreResponse(savedUserStore);
    }

    @Override
    @Transactional
    public void removeUserFromStore(String userId, String storeId) {
        UserStore userStore = userStoreRepository.findByUserIdAndStoreIdAndIsDeletedFalse(userId, storeId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_STORE_RELATIONSHIP_NOT_FOUND));
        
        userStore.setIsDeleted(true);
        userStoreRepository.save(userStore);
        log.info("User {} removed from store {}", userId, storeId);
    }

    @Override
    public List<StoreResponse> getStoresByUserId(String userId) {
        List<Store> stores = storeRepository.findStoresByUserId(userId);
        return stores.stream()
                .map(this::mapToStoreResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserStoreResponse> getUsersByStoreId(String storeId) {
        List<UserStore> userStores = userStoreRepository.findByStoreIdAndIsDeletedFalse(storeId);
        return userStores.stream()
                .map(this::mapToUserStoreResponse)
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
                .createdAt(store.getCreatedAt())
                .updatedAt(store.getUpdatedAt())
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getAccount().getEmail())
                .phone(user.getPhone())
                .gender(user.getGender())
                .birthday(user.getBirthday())
                .avatar(user.getAvatar())
                .cccd(user.getCccd())
                .point(user.getPoint())
                .role(user.getAccount().getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private UserStoreResponse mapToUserStoreResponse(UserStore userStore) {
        return UserStoreResponse.builder()
                .userId(userStore.getUserId())
                .storeId(userStore.getStoreId())
                .user(mapToUserResponse(userStore.getUser()))
                .store(mapToStoreResponse(userStore.getStore()))
                .createdAt(userStore.getCreatedAt())
                .updatedAt(userStore.getUpdatedAt())
                .build();
    }
}
