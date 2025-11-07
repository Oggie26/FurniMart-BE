package com.example.userservice.service.inteface;

import com.example.userservice.entity.Store;
import com.example.userservice.request.StoreDistance;
import com.example.userservice.request.StoreRequest;
import com.example.userservice.request.EmployeeStoreRequest;
import com.example.userservice.response.PageResponse;
import com.example.userservice.response.StoreResponse;
import com.example.userservice.response.EmployeeStoreResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StoreService {
    
    StoreResponse createStore(StoreRequest request);
    
    StoreResponse updateStore(String id, StoreRequest request);
    
    StoreResponse getStoreById(String id);
    
    List<StoreResponse> getAllStores();
    
    PageResponse<StoreResponse> getStoresWithPagination(Pageable pageable);
    
    PageResponse<StoreResponse> searchStores(String searchTerm, Pageable pageable);
    
    List<StoreResponse> getStoresByCity(String city);
    
    List<StoreResponse> getStoresByDistrict(String district);
    
    void deleteStore(String id);
    
    EmployeeStoreResponse addUserToStore(EmployeeStoreRequest request);
    
    void removeUserFromStore(String userId, String storeId);
    
    List<StoreResponse> getStoresByUserId(String userId);
    
    List<EmployeeStoreResponse> getUsersByStoreId(String storeId);

    Store getNearestStore(double lat, double lon);

    List<Store> getNearestStores(double lat, double lon, int limit);

    List<StoreDistance> findNearestStores(double lat, double lon, int limit);
}
