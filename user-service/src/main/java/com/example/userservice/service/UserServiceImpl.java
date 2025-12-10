package com.example.userservice.service;

import com.example.userservice.entity.Account;
import com.example.userservice.entity.Address;
import com.example.userservice.entity.User;
import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import com.example.userservice.enums.ErrorCode;
import com.example.userservice.exception.AppException;
import com.example.userservice.repository.AccountRepository;
import com.example.userservice.repository.AddressRepository;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.request.DeliveryAddressRequest;
import com.example.userservice.request.StaffCreateCustomerRequest;
import com.example.userservice.request.UserRequest;
import com.example.userservice.request.UserUpdateRequest;
import com.example.userservice.response.*;
import com.example.userservice.service.inteface.EmployeeService;
import com.example.userservice.service.inteface.UserService;
import com.example.userservice.service.inteface.WalletService;
import com.example.userservice.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final AddressRepository addressRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmployeeService employeeService;
    private final WalletService walletService;
    private final WalletTransactionRepository walletTransactionRepository;

    @Override
    @Transactional
    public UserResponse createUser(UserRequest userRequest) {
        if (userRequest.getRole() != null && userRequest.getRole() != EnumRole.CUSTOMER) {
            log.error("Attempt to create non-CUSTOMER role through UserService: {}", userRequest.getRole());
            throw new AppException(ErrorCode.INVALID_ROLE);
        }
        
        userRequest.setRole(EnumRole.CUSTOMER);
        
        if (accountRepository.findByEmailAndIsDeletedFalse(userRequest.getEmail()).isPresent()) {
            throw new AppException(ErrorCode.EMAIL_EXISTS);
        }
        
        if (userRepository.findByPhoneAndIsDeletedFalse(userRequest.getPhone()).isPresent()) {
            throw new AppException(ErrorCode.PHONE_EXISTS);
        }

        Account account = Account.builder()
                .email(userRequest.getEmail())
                .password(passwordEncoder.encode(userRequest.getPassword()))
                .role(EnumRole.CUSTOMER)
                .status(userRequest.getStatus() != null ? userRequest.getStatus() : EnumStatus.ACTIVE)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();
        
        Account savedAccount = accountRepository.save(account);

        User user = User.builder()
                .fullName(userRequest.getFullName())
                .phone(userRequest.getPhone())
                .birthday(userRequest.getBirthday())
                .gender(userRequest.getGender())
                .status(userRequest.getStatus() != null ? userRequest.getStatus() : EnumStatus.ACTIVE)
                .avatar(userRequest.getAvatar())
                .point(0)
                .account(savedAccount)
                .build();

        User savedUser = userRepository.save(user);
        
        // Auto-create wallet for the new customer so staff workflow is consistent
        try {
            userRepository.flush();
            accountRepository.flush();
            walletService.createWalletForUser(savedUser.getId());
            log.info("Wallet auto-created for staff-created customer: {}", savedUser.getId());
        } catch (AppException e) {
            log.error("Failed to auto-create wallet for staff-created customer {}: ErrorCode={}, Message={}",
                    savedUser.getId(), e.getErrorCode(), e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to auto-create wallet for staff-created customer {}: {}", savedUser.getId(), e.getMessage(), e);
        }
        
        // Auto-create wallet for new customer
        // Note: Wallet creation happens in REQUIRES_NEW transaction
        // which should be able to see the flushed user
        try {
            // Flush to ensure user is persisted to database
            userRepository.flush();
            accountRepository.flush(); // Also flush account to ensure it's visible
            log.debug("User and account flushed to database: {}", savedUser.getId());
            
            // Use separate transaction (REQUIRES_NEW) to avoid affecting main transaction
            // The REQUIRES_NEW transaction should see the flushed user
            walletService.createWalletForUser(savedUser.getId());
            log.info("Wallet auto-created for new customer: {}", savedUser.getId());
        } catch (AppException e) {
            log.error("Failed to auto-create wallet for user {}: ErrorCode={}, Message={}", 
                    savedUser.getId(), e.getErrorCode(), e.getMessage(), e);
            // Don't fail user creation if wallet creation fails, but log the error
        } catch (Exception e) {
            log.error("Failed to auto-create wallet for user {}: {}", savedUser.getId(), e.getMessage(), e);
            // Don't fail user creation if wallet creation fails, but log the error
        }
        
        log.info("Created CUSTOMER user: {} with email: {}", savedUser.getId(), savedAccount.getEmail());
        return toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUser(String id, UserUpdateRequest userRequest) {
        User existingUser = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (userRequest.getPhone() != null && !userRequest.getPhone().equals(existingUser.getPhone())) {
            userRepository.findByPhoneAndIsDeletedFalse(userRequest.getPhone())
                    .ifPresent(user -> {
                        if (!user.getId().equals(id)) {
                            throw new AppException(ErrorCode.PHONE_EXISTS);
                        }
                    });
        }

        if (userRequest.getFullName() != null) {
            existingUser.setFullName(userRequest.getFullName());
        }
        if (userRequest.getPhone() != null) {
            existingUser.setPhone(userRequest.getPhone());
        }
        if (userRequest.getBirthday() != null) {
            existingUser.setBirthday(userRequest.getBirthday());
        }
        if (userRequest.getGender() != null) {
            existingUser.setGender(userRequest.getGender());
        }
        if (userRequest.getStatus() != null) {
            existingUser.setStatus(userRequest.getStatus());
        }
        if (userRequest.getAvatar() != null) {
            existingUser.setAvatar(userRequest.getAvatar());
        }
        if (userRequest.getCccd() != null) {
            existingUser.setCccd(userRequest.getCccd());
        }
        if (userRequest.getPoint() != null) {
            existingUser.setPoint(userRequest.getPoint());
        }

        User updatedUser = userRepository.save(existingUser);

        return toUserResponse(updatedUser);
    }

    @Override
    public UserResponse getUserById(String id) {
        User user = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return toUserResponse(user);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        log.info("Fetching all CUSTOMER users");
        List<User> users = userRepository.findByAccountRoleAndIsDeletedFalse(EnumRole.CUSTOMER);
        return users.stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Long getTotalUsersCount() {
        return userRepository.countByIsDeletedFalse();
    }
    
    @Override
    @Transactional
    public void refundToWallet(String userId, Double amount, String referenceId) {
        log.info("Refunding {} to wallet for user: {}, ref: {}", amount, userId, referenceId);
        
        if (amount == null || amount <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        if (referenceId == null || referenceId.isBlank()) {
            referenceId = "REFUND-" + userId + "-" + System.currentTimeMillis();
        }
        
        // Idempotency: if referenceId exists, skip double credit
        if (walletTransactionRepository.findByReferenceIdAndIsDeletedFalse(referenceId).isPresent()) {
            log.warn("Refund already processed for ref: {}", referenceId);
            return;
        }
        
        // Get user's wallet (will create if not exists)
        var walletResponse = walletService.getWalletByUserId(userId);
        
        // Deposit to wallet (this creates a transaction record)
        String description = String.format("Hoàn tiền đơn hàng - Tham chiếu: %s", referenceId);
        
        walletService.deposit(walletResponse.getId(), amount, description, referenceId);
        
        log.info("Successfully refunded {} to wallet for user: {}, ref: {}", amount, userId, referenceId);
    }

    @Override
    public List<UserResponse> getUsersByStatus(String status) {
        EnumStatus enumStatus = EnumStatus.valueOf(status.toUpperCase());
        List<User> users = userRepository.findByStatusAndIsDeletedFalse(enumStatus);
        return users.stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<UserResponse> getUsersWithPagination(int page, int size) {
        log.info("Fetching users with pagination - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> userPage = userRepository.findByIsDeletedFalse(pageable);
        
        List<UserResponse> userResponses = userPage.getContent().stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());

        return PageResponse.<UserResponse>builder()
                .content(userResponses)
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .size(userPage.getSize())
                .number(userPage.getNumber())
                .first(userPage.isFirst())
                .last(userPage.isLast())
                .build();
    }

    @Override
    public PageResponse<UserResponse> searchUsers(String searchTerm, int page, int size) {
        // Sanitize search term to prevent injection attacks
        searchTerm = com.example.userservice.util.InputSanitizer.sanitizeSearchKeyword(searchTerm);
        log.info("Searching users with term: {} - page: {}, size: {}", searchTerm, page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> userPage = userRepository.searchUsers(searchTerm, pageable);
        
        List<UserResponse> userResponses = userPage.getContent().stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());

        return PageResponse.<UserResponse>builder()
                .content(userResponses)
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .size(userPage.getSize())
                .number(userPage.getNumber())
                .first(userPage.isFirst())
                .last(userPage.isLast())
                .build();
    }

    @Override
    @Transactional
    public void deleteUser(String id) {
        log.info("Attempting to delete user with id: {}", id);
        
        try {
            User user = userRepository.findByIdAndIsDeletedFalse(id)
                    .orElseThrow(() -> {
                        log.error("User not found for deletion with id: {}", id);
                        return new AppException(ErrorCode.USER_NOT_FOUND);
                    });
            
            if (user.getAccount() == null) {
                log.error("User {} does not have an associated account", user.getId());
                throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND);
            }
            
            log.info("Found user to delete: {} (email: {})", user.getFullName(), user.getAccount().getEmail());
            
            user.setStatus(EnumStatus.DELETED);
            user.setIsDeleted(true);
            user.getAccount().setIsDeleted(true);
            
            User savedUser = userRepository.save(user);
            Account savedAccount = accountRepository.save(user.getAccount());
            
            log.info("Successfully deleted user with id: {} and account with id: {}", 
                    savedUser.getId(), savedAccount.getId());
            
        } catch (Exception e) {
            log.error("Error deleting user with id: {}", id, e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public void disableUser(String id) {

        User user = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getAccount() == null) {
            log.error("User {} does not have an associated account", user.getId());
            throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        user.setStatus(EnumStatus.INACTIVE);
        user.getAccount().setStatus(EnumStatus.INACTIVE);
        
        userRepository.save(user);
        accountRepository.save(user.getAccount());
        
    }

    @Override
    @Transactional
    public void enableUser(String id) {

        User user = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getAccount() == null) {
            log.error("User {} does not have an associated account", user.getId());
            throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        user.setStatus(EnumStatus.ACTIVE);
        user.getAccount().setStatus(EnumStatus.ACTIVE);
        
        userRepository.save(user);
        accountRepository.save(user.getAccount());
        
    }

    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        
        String email = authentication.getName();
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return user.getId();
    }

    @Override
    public UserResponse getProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        return toUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(UserUpdateRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        return updateUser(user.getId(), request);
    }

    @Override
    @Transactional
    public void changePassword(ChangePassword changePassword) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getAccount() == null) {
            log.error("User {} does not have an associated account", user.getId());
            throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        Account account = user.getAccount();
        
        if (!passwordEncoder.matches(changePassword.getOldPassword(), account.getPassword())) {
            throw new AppException(ErrorCode.INVALID_PASSWORD);
        }
        account.setPassword(passwordEncoder.encode(changePassword.getNewPassword()));
        accountRepository.save(account);
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_FOUND));
        return toUserResponse(user);
    }

    @Override
    public UserResponse getUserByPhone(String phone) {
        User user = userRepository.findByPhoneAndIsDeletedFalse(phone)
                .orElseThrow(() -> new AppException(ErrorCode.PHONE_NOT_FOUND));
        return toUserResponse(user);
    }

    @Override
    public UserResponse getUserByAccountId(String accountId) {
        User user = userRepository.findByAccountIdAndIsDeletedFalse(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND_USER));
        return toUserResponse(user);
    }

    private UserResponse toUserResponse(User user) {
        List<String> storeIds = List.of();

        return UserResponse.builder()
                .id(user.getId())
                .birthday(user.getBirthday())
                .gender(user.getGender())
                .fullName(user.getFullName())
                .avatar(user.getAvatar())
                .phone(user.getPhone())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .status(user.getStatus())
                .cccd(user.getCccd())
                .point(user.getPoint())
                .email(user.getAccount() != null ? user.getAccount().getEmail() : null)
                .role(user.getAccount() != null ? user.getAccount().getRole() : null)
                .storeIds(storeIds)
                .build();
    }
    
    @Override
    public List<UserResponse> getAllEmployees() {
        log.info("Redirecting getAllEmployees() to EmployeeService");
        return employeeService.getAllEmployees();
    }
    
    @Override
    public List<UserResponse> getEmployeesByRole(EnumRole role) {
        log.info("Redirecting getEmployeesByRole({}) to EmployeeService", role);
        return employeeService.getEmployeesByRole(role);
    }
    
    @Override
    public List<UserResponse> getEmployeesByStoreId(String storeId) {
        log.info("Redirecting getEmployeesByStoreId({}) to EmployeeService", storeId);
        return employeeService.getEmployeesByStoreId(storeId);
    }
    
    @Override
    public List<UserResponse> getEmployeesByStoreIdAndRole(String storeId, EnumRole role) {
        log.info("Redirecting getEmployeesByStoreIdAndRole({}, {}) to EmployeeService", storeId, role);
        return employeeService.getEmployeesByStoreIdAndRole(storeId, role);
    }
    
    @Override
    public PageResponse<UserResponse> getEmployeesWithPagination(int page, int size) {
        log.info("Redirecting getEmployeesWithPagination({}, {}) to EmployeeService", page, size);
        return employeeService.getEmployeesWithPagination(page, size);
    }
    
    @Override
    public PageResponse<UserResponse> getEmployeesByRoleWithPagination(EnumRole role, int page, int size) {
        log.info("Redirecting getEmployeesByRoleWithPagination({}, {}, {}) to EmployeeService", role, page, size);
        return employeeService.getEmployeesByRoleWithPagination(role, page, size);
    }
    
    
    @Override
    @Transactional
    public UserResponse updateUserRole(String userId, EnumRole newRole) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        if (user.getAccount() == null) {
            log.error("User {} does not have an associated account", user.getId());
            throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        
        if (user.getAccount().getRole() == EnumRole.CUSTOMER) {
            throw new AppException(ErrorCode.CANNOT_UPDATE_CUSTOMER_ROLE);
        }
        
        if (!isEmployeeRole(newRole)) {
            throw new AppException(ErrorCode.INVALID_ROLE);
        }
        
        user.getAccount().setRole(newRole);
        accountRepository.save(user.getAccount());
        
        return toUserResponse(user);
    }
    
    private boolean isEmployeeRole(EnumRole role) {
        return role == EnumRole.BRANCH_MANAGER || 
               role == EnumRole.DELIVERY || 
               role == EnumRole.STAFF;
    }
    
    @Override
    @Transactional
    public StaffCreateCustomerResponse createCustomerAccountForStaff(StaffCreateCustomerRequest request) {
        log.info("Staff creating customer account for email: {}", request.getEmail());
        
        // Check if email already exists
        if (accountRepository.findByEmailAndIsDeletedFalse(request.getEmail()).isPresent()) {
            throw new AppException(ErrorCode.EMAIL_EXISTS);
        }
        
        // Check if phone already exists
        if (userRepository.findByPhoneAndIsDeletedFalse(request.getPhone()).isPresent()) {
            throw new AppException(ErrorCode.PHONE_EXISTS);
        }
        
        // Generate password if not provided
        String password = request.getPassword();
        String generatedPassword = null;
        if (password == null || password.isEmpty()) {
            generatedPassword = generateRandomPassword();
            password = generatedPassword;
        }
        
        // Create account
        Account account = Account.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(password))
                .role(EnumRole.CUSTOMER)
                .status(EnumStatus.ACTIVE)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();
        
        Account savedAccount = accountRepository.save(account);
        
        // Create user
        User user = User.builder()
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .status(EnumStatus.ACTIVE)
                .point(0)
                .account(savedAccount)
                .build();
        
        User savedUser = userRepository.save(user);
        
        // Create delivery address
        DeliveryAddressRequest addressRequest = request.getDeliveryAddress();
        Address address = Address.builder()
                .name(addressRequest.getName())
                .phone(addressRequest.getPhone())
                .city(addressRequest.getCity())
                .district(addressRequest.getDistrict())
                .ward(addressRequest.getWard())
                .street(addressRequest.getStreet())
                .addressLine(addressRequest.getAddressLine())
                .isDefault(addressRequest.getIsDefault() != null ? addressRequest.getIsDefault() : true)
                .latitude(addressRequest.getLatitude())
                .longitude(addressRequest.getLongitude())
                .user(savedUser)
                .build();
        
        // If this is set as default, unset other default addresses
        if (address.getIsDefault()) {
            addressRepository.findByUserAndIsDefaultTrue(savedUser)
                    .ifPresent(existingDefault -> {
                        existingDefault.setIsDefault(false);
                        addressRepository.save(existingDefault);
                    });
        }
        
        Address savedAddress = addressRepository.save(address);
        
        log.info("Staff successfully created customer account: {} with email: {} and address ID: {}", 
                savedUser.getId(), savedAccount.getEmail(), savedAddress.getId());
        
        // Convert to response
        AddressResponse addressResponse = toAddressResponse(savedAddress);
        
        return StaffCreateCustomerResponse.builder()
                .user(toUserResponse(savedUser))
                .address(addressResponse)
                .generatedPassword(generatedPassword)
                .build();
    }
    
    private String generateRandomPassword() {
        // Generate a random 8-character password
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return password.toString();
    }
    
    private AddressResponse toAddressResponse(Address address) {
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
                .fullAddress(fullAddress.toString())
                .longitude(address.getLongitude())
                .latitude(address.getLatitude())
                .build();
    }
}

