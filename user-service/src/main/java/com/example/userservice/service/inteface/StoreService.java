package com.example.userservice.service.inteface;

import com.example.userservice.request.StoreRequest;
import com.example.userservice.request.UserStoreRequest;
import com.example.userservice.response.PageResponse;
import com.example.userservice.response.StoreResponse;
import com.example.userservice.response.UserStoreResponse;
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
    
    // Many-to-many relationship methods
    UserStoreResponse addUserToStore(UserStoreRequest request);
    
    void removeUserFromStore(String userId, String storeId);
    
    List<StoreResponse> getStoresByUserId(String userId);
    
    List<UserStoreResponse> getUsersByStoreId(String storeId);
}
