package com.example.userservice.repository;

import com.example.userservice.entity.Store;
import com.example.userservice.enums.EnumStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
public interface StoreRepository extends JpaRepository<Store, String> {
    Optional<Store> findByIdAndIsDeletedFalse(String id);

    List<Store> findByStatusAndIsDeletedFalse(EnumStatus status);

    Page<Store> findByIsDeletedFalse(Pageable pageable);

    List<Store> findByIsDeletedFalse();

    List<Store> findByCityAndIsDeletedFalse(String city);
    
    List<Store> findByDistrictAndIsDeletedFalse(String district);
    
    @Query("SELECT s FROM Store s WHERE s.isDeleted = false AND " +
           "(LOWER(s.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(s.city) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(s.district) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(s.ward) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(s.addressLine) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Store> searchStores(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT s FROM Store s JOIN s.employeeStores es WHERE es.employeeId = :employeeId AND s.isDeleted = false")
    List<Store> findStoresByEmployeeId(@Param("employeeId") String employeeId);
    
    @Query("SELECT s FROM Store s JOIN s.employeeStores es WHERE es.employeeId = :userId AND s.isDeleted = false")
    List<Store> findStoresByUserId(@Param("userId") String userId);

        @Query(value = """
        SELECT s.*
        FROM stores s
        ORDER BY ST_Distance(
            ST_SetSRID(ST_MakePoint(s.longitude, s.latitude), 4326)::geography,
            ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography
        )
        LIMIT 1
        """, nativeQuery = true)
        Store findNearestStore(@Param("lat") double lat, @Param("lon") double lon);

    @Query(value = """
    SELECT s.*
    FROM stores s
    ORDER BY ST_Distance(
        ST_SetSRID(ST_MakePoint(s.longitude, s.latitude), 4326)::geography,
        ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography
    )
    """, nativeQuery = true)
    List<Store> findNearestStores(@Param("lat") double lat,
                                  @Param("lon") double lon,
                                  Pageable pageable);


    @Query("SELECT s FROM Store s WHERE s.latitude IS NOT NULL AND s.longitude IS NOT NULL")
    List<Store> findAllWithCoordinates();




}
