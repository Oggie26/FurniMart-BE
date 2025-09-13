package com.example.userservice.service;

import com.example.userservice.entity.Address;
import com.example.userservice.entity.User;
import com.example.userservice.enums.ErrorCode;
import com.example.userservice.exception.AppException;
import com.example.userservice.repository.AddressRepository;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.request.AddressRequest;
import com.example.userservice.response.AddressResponse;
import com.example.userservice.response.PageResponse;
import com.example.userservice.service.inteface.AddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AddressResponse createAddress(AddressRequest addressRequest) {
        log.info("Creating new address for user ID: {}", addressRequest.getUserId());
        
        User user = userRepository.findByIdAndIsDeletedFalse(addressRequest.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // If this is set as default, unset other default addresses for this user
        if (addressRequest.getIsDefault()) {
            addressRepository.findByUserAndIsDefaultTrue(user)
                    .ifPresent(existingDefault -> {
                        existingDefault.setIsDefault(false);
                        addressRepository.save(existingDefault);
                    });
        }

        Address address = Address.builder()
                .name(addressRequest.getName())
                .phone(addressRequest.getPhone())
                .city(addressRequest.getCity())
                .district(addressRequest.getDistrict())
                .ward(addressRequest.getWard())
                .street(addressRequest.getStreet())
                .addressLine(addressRequest.getAddressLine())
                .isDefault(addressRequest.getIsDefault())
                .user(user)
                .build();

        Address savedAddress = addressRepository.save(address);
        log.info("Address created successfully with ID: {}", savedAddress.getId());
        
        return toAddressResponse(savedAddress);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Long id, AddressRequest addressRequest) {
        log.info("Updating address with ID: {}", id);
        
        Address existingAddress = addressRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));

        User user = userRepository.findByIdAndIsDeletedFalse(addressRequest.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // If this is set as default, unset other default addresses for this user
        if (addressRequest.getIsDefault() && !existingAddress.getIsDefault()) {
            addressRepository.findByUserAndIsDefaultTrue(user)
                    .ifPresent(existingDefault -> {
                        existingDefault.setIsDefault(false);
                        addressRepository.save(existingDefault);
                    });
        }

        existingAddress.setName(addressRequest.getName());
        existingAddress.setPhone(addressRequest.getPhone());
        existingAddress.setCity(addressRequest.getCity());
        existingAddress.setDistrict(addressRequest.getDistrict());
        existingAddress.setWard(addressRequest.getWard());
        existingAddress.setStreet(addressRequest.getStreet());
        existingAddress.setAddressLine(addressRequest.getAddressLine());
        existingAddress.setIsDefault(addressRequest.getIsDefault());
        existingAddress.setUser(user);

        Address updatedAddress = addressRepository.save(existingAddress);
        log.info("Address updated successfully with ID: {}", updatedAddress.getId());
        
        return toAddressResponse(updatedAddress);
    }

    @Override
    public AddressResponse getAddressById(Long id) {
        log.info("Fetching address with ID: {}", id);
        
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));
        
        return toAddressResponse(address);
    }

    @Override
    public List<AddressResponse> getAllAddresses() {
        log.info("Fetching all addresses");
        
        List<Address> addresses = addressRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        return addresses.stream()
                .map(this::toAddressResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AddressResponse> getAddressesByUserId(String userId) {
        log.info("Fetching addresses for user ID: {}", userId);
        
        List<Address> addresses = addressRepository.findAllByUserId(userId);
        return addresses.stream()
                .map(this::toAddressResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AddressResponse getDefaultAddressByUserId(String userId) {
        log.info("Fetching default address for user ID: {}", userId);
        
        Address address = addressRepository.findDefaultAddressByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));
        
        return toAddressResponse(address);
    }

    @Override
    public PageResponse<AddressResponse> getAddressesWithPagination(int page, int size) {
        log.info("Fetching addresses with pagination - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Address> addressPage = addressRepository.findAll(pageable);
        
        List<AddressResponse> addressResponses = addressPage.getContent().stream()
                .map(this::toAddressResponse)
                .collect(Collectors.toList());

        return PageResponse.<AddressResponse>builder()
                .content(addressResponses)
                .totalElements(addressPage.getTotalElements())
                .totalPages(addressPage.getTotalPages())
                .size(addressPage.getSize())
                .number(addressPage.getNumber())
                .first(addressPage.isFirst())
                .last(addressPage.isLast())
                .build();
    }

    @Override
    public PageResponse<AddressResponse> getAddressesByUserWithPagination(String userId, int page, int size) {
        log.info("Fetching addresses for user ID: {} with pagination - page: {}, size: {}", userId, page, size);
        
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Address> addressPage = addressRepository.findByUser(user, pageable);
        
        List<AddressResponse> addressResponses = addressPage.getContent().stream()
                .map(this::toAddressResponse)
                .collect(Collectors.toList());

        return PageResponse.<AddressResponse>builder()
                .content(addressResponses)
                .totalElements(addressPage.getTotalElements())
                .totalPages(addressPage.getTotalPages())
                .size(addressPage.getSize())
                .number(addressPage.getNumber())
                .first(addressPage.isFirst())
                .last(addressPage.isLast())
                .build();
    }

    @Override
    @Transactional
    public void deleteAddress(Long id) {
        log.info("Deleting address with ID: {}", id);
        
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));

        addressRepository.delete(address);
        
        log.info("Address deleted successfully with ID: {}", id);
    }

    @Override
    @Transactional
    public void setDefaultAddress(Long id, String userId) {
        log.info("Setting address ID: {} as default for user ID: {}", id, userId);
        
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));

        // Verify the address belongs to the user
        if (!address.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // Unset current default address
        addressRepository.findByUserAndIsDefaultTrue(user)
                .ifPresent(existingDefault -> {
                    existingDefault.setIsDefault(false);
                    addressRepository.save(existingDefault);
                });

        // Set new default address
        address.setIsDefault(true);
        addressRepository.save(address);
        
        log.info("Address set as default successfully - ID: {}", id);
    }

    @Override
    public List<AddressResponse> searchAddressByLocation(String city, String district, String ward) {
        log.info("Searching addresses by location - city: {}, district: {}, ward: {}", city, district, ward);
        
        List<Address> addresses = addressRepository.findByLocation(city, district, ward);
        return addresses.stream()
                .map(this::toAddressResponse)
                .collect(Collectors.toList());
    }

    private AddressResponse toAddressResponse(Address address) {
        String fullAddress = buildFullAddress(address);
        
        return AddressResponse.builder()
                .id(address.getId())
                .name(address.getName())
                .phone(address.getPhone())
                .city(address.getCity())
                .district(address.getDistrict())
                .ward(address.getWard())
                .street(address.getStreet())
                .addressLine(address.getAddressLine())
                .isDefault(address.getIsDefault())
                .userId(address.getUser().getId())
                .userName(address.getUser().getFullName())
                .fullAddress(fullAddress)
                .build();
    }

    private String buildFullAddress(Address address) {
        StringBuilder fullAddress = new StringBuilder();
        
        if (address.getStreet() != null && !address.getStreet().isEmpty()) {
            fullAddress.append(address.getStreet()).append(", ");
        }
        if (address.getWard() != null && !address.getWard().isEmpty()) {
            fullAddress.append(address.getWard()).append(", ");
        }
        if (address.getDistrict() != null && !address.getDistrict().isEmpty()) {
            fullAddress.append(address.getDistrict()).append(", ");
        }
        if (address.getCity() != null && !address.getCity().isEmpty()) {
            fullAddress.append(address.getCity());
        }
        
        if (address.getAddressLine() != null && !address.getAddressLine().isEmpty()) {
            if (fullAddress.length() > 0) {
                fullAddress.append(" - ");
            }
            fullAddress.append(address.getAddressLine());
        }
        
        return fullAddress.toString();
    }
}
