package com.example.userservice.service.inteface;

import com.example.userservice.request.AddressRequest;
import com.example.userservice.response.AddressResponse;
import com.example.userservice.response.PageResponse;

import java.util.List;

public interface AddressService {

    AddressResponse createAddress(AddressRequest addressRequest);

    AddressResponse updateAddress(Long id, AddressRequest addressRequest);

    AddressResponse getAddressById(Long id);

    List<AddressResponse> getAllAddresses();

    List<AddressResponse> getAddressesByUserId(String userId);

    AddressResponse getDefaultAddressByUserId(String userId);

    PageResponse<AddressResponse> getAddressesWithPagination(int page, int size);

    PageResponse<AddressResponse> getAddressesByUserWithPagination(String userId, int page, int size);

    void deleteAddress(Long id);

    void setDefaultAddress(Long id, String userId);

    List<AddressResponse> searchAddressByLocation(String city, String district, String ward);
}
