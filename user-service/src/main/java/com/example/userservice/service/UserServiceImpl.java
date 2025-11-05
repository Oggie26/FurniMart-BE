package com.example.userservice.service;

import com.example.userservice.entity.Account;
import com.example.userservice.entity.User;
import com.example.userservice.entity.UserStore;
import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import com.example.userservice.enums.ErrorCode;
import com.example.userservice.exception.AppException;
import com.example.userservice.repository.AccountRepository;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.repository.UserStoreRepository;
import com.example.userservice.request.UserRequest;
import com.example.userservice.request.UserUpdateRequest;
import com.example.userservice.response.*;
import com.example.userservice.service.inteface.UserService;
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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final UserStoreRepository userStoreRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse createUser(UserRequest userRequest) {
        // NOTE: For employee creation (SELLER, BRANCH_MANAGER, DELIVERER, STAFF),
        // it is recommended to use EmployeeService.createEmployee() for better validation
        // and to ensure ADMIN roles cannot be created through employee endpoints.
        // This method is primarily used for ADMIN and CUSTOMER creation.
        
        if (accountRepository.findByEmailAndIsDeletedFalse(userRequest.getEmail()).isPresent()) {
            throw new AppException(ErrorCode.EMAIL_EXISTS);
        }
        
        if (userRepository.findByPhoneAndIsDeletedFalse(userRequest.getPhone()).isPresent()) {
            throw new AppException(ErrorCode.PHONE_EXISTS);
        }

        Account account = Account.builder()
                .email(userRequest.getEmail())
                .password(passwordEncoder.encode(userRequest.getPassword()))
                .role(userRequest.getRole())
                .status(userRequest.getStatus())
                .build();
        
        Account savedAccount = accountRepository.save(account);

        User user = User.builder()
                .fullName(userRequest.getFullName())
                .phone(userRequest.getPhone())
                .birthday(userRequest.getBirthday())
                .gender(userRequest.getGender())
                .status(userRequest.getStatus())
                .avatar(userRequest.getAvatar())
                .point(0)
                .account(savedAccount)
                .build();

        User savedUser = userRepository.save(user);
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
        List<User> users = userRepository.findByIsDeletedFalse();
        return users.stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
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
        // Load storeIds for the user
        List<String> storeIds = userStoreRepository.findByUserIdAndIsDeletedFalse(user.getId())
                .stream()
                .map(UserStore::getStoreId)
                .collect(Collectors.toList());

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
        List<EnumRole> employeeRoles = Arrays.asList(EnumRole.SELLER, EnumRole.BRANCH_MANAGER, EnumRole.DELIVERER, EnumRole.STAFF);
        List<User> employees = userRepository.findEmployeesByRoles(employeeRoles);
        return employees.stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<UserResponse> getEmployeesByRole(EnumRole role) {
        // Only allow employee roles
        if (!isEmployeeRole(role)) {
            throw new AppException(ErrorCode.INVALID_ROLE);
        }
        
        List<User> employees = userRepository.findEmployeesByRole(role);
        return employees.stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<UserResponse> getEmployeesByStoreId(String storeId) {
        List<EnumRole> employeeRoles = Arrays.asList(EnumRole.SELLER, EnumRole.BRANCH_MANAGER, EnumRole.DELIVERER, EnumRole.STAFF);
        List<User> employees = userRepository.findEmployeesByStoreIdAndRoles(storeId, employeeRoles);
        return employees.stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<UserResponse> getEmployeesByStoreIdAndRole(String storeId, EnumRole role) {
        // Only allow employee roles
        if (!isEmployeeRole(role)) {
            throw new AppException(ErrorCode.INVALID_ROLE);
        }
        
        List<User> employees = userRepository.findEmployeesByStoreIdAndRole(storeId, role);
        return employees.stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public PageResponse<UserResponse> getEmployeesWithPagination(int page, int size) {
        log.info("Fetching employees with pagination - page: {}, size: {}", page, size);
        
        List<EnumRole> employeeRoles = Arrays.asList(EnumRole.SELLER, EnumRole.BRANCH_MANAGER, EnumRole.DELIVERER, EnumRole.STAFF);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> employeePage = userRepository.findEmployeesByRoles(employeeRoles, pageable);
        
        List<UserResponse> employeeResponses = employeePage.getContent().stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());

        return PageResponse.<UserResponse>builder()
                .content(employeeResponses)
                .totalElements(employeePage.getTotalElements())
                .totalPages(employeePage.getTotalPages())
                .size(employeePage.getSize())
                .number(employeePage.getNumber())
                .first(employeePage.isFirst())
                .last(employeePage.isLast())
                .build();
    }
    
    @Override
    public PageResponse<UserResponse> getEmployeesByRoleWithPagination(EnumRole role, int page, int size) {
        log.info("Fetching employees by role {} with pagination - page: {}, size: {}", role, page, size);
        
        // Only allow employee roles
        if (!isEmployeeRole(role)) {
            throw new AppException(ErrorCode.INVALID_ROLE);
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> employeePage = userRepository.findEmployeesByRole(role, pageable);
        
        List<UserResponse> employeeResponses = employeePage.getContent().stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());

        return PageResponse.<UserResponse>builder()
                .content(employeeResponses)
                .totalElements(employeePage.getTotalElements())
                .totalPages(employeePage.getTotalPages())
                .size(employeePage.getSize())
                .number(employeePage.getNumber())
                .first(employeePage.isFirst())
                .last(employeePage.isLast())
                .build();
    }
    
    @Override
    @Transactional
    public UserResponse updateUserRole(String userId, EnumRole newRole) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        // Prevent updating customer roles
        if (user.getAccount().getRole() == EnumRole.CUSTOMER) {
            throw new AppException(ErrorCode.CANNOT_UPDATE_CUSTOMER_ROLE);
        }
        
        // Only allow updating to employee roles
        if (!isEmployeeRole(newRole)) {
            throw new AppException(ErrorCode.INVALID_ROLE);
        }
        
        user.getAccount().setRole(newRole);
        accountRepository.save(user.getAccount());
        
        return toUserResponse(user);
    }
    
    private boolean isEmployeeRole(EnumRole role) {
        return role == EnumRole.SELLER || 
               role == EnumRole.BRANCH_MANAGER || 
               role == EnumRole.DELIVERER || 
               role == EnumRole.STAFF;
    }
}

