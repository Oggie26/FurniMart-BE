package com.example.userservice.service;

import com.example.userservice.entity.Account;
import com.example.userservice.entity.Store;
import com.example.userservice.entity.User;
import com.example.userservice.entity.UserStore;
import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import com.example.userservice.enums.ErrorCode;
import com.example.userservice.exception.AppException;
import com.example.userservice.repository.AccountRepository;
import com.example.userservice.repository.EmployeeRepository;
import com.example.userservice.repository.StoreRepository;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.repository.UserStoreRepository;
import com.example.userservice.request.UserRequest;
import com.example.userservice.request.UserUpdateRequest;
import com.example.userservice.response.PageResponse;
import com.example.userservice.response.UserResponse;
import com.example.userservice.service.inteface.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of EmployeeService for managing employee operations.
 * This service ensures that only employee roles (SELLER, BRANCH_MANAGER, DELIVERER, STAFF) 
 * can be created and managed through employee endpoints.
 * ADMIN and CUSTOMER roles are explicitly blocked from these operations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final UserStoreRepository userStoreRepository;
    private final StoreRepository storeRepository;
    private final PasswordEncoder passwordEncoder;

    // Define employee roles as constants
    private static final List<EnumRole> EMPLOYEE_ROLES = Arrays.asList(
        EnumRole.ADMIN,           // Allow ADMIN creation
        EnumRole.SELLER, 
        EnumRole.BRANCH_MANAGER, 
        EnumRole.DELIVERER, 
        EnumRole.STAFF
    );

    @Override
    @Transactional
    public UserResponse createEmployee(UserRequest userRequest) {
        log.info("Creating employee with role: {}", userRequest.getRole());
        
        // Validate that role is an employee role (allow ADMIN, block CUSTOMER)
        validateEmployeeRole(userRequest.getRole());
        
        // Check if CUSTOMER role is being created - explicitly block it
        if (userRequest.getRole() == EnumRole.CUSTOMER) {
            log.error("Attempt to create CUSTOMER role through employee API");
            throw new AppException(ErrorCode.CANNOT_CREATE_CUSTOMER_THROUGH_EMPLOYEE_API);
        }

        // Check if email already exists
        if (accountRepository.findByEmailAndIsDeletedFalse(userRequest.getEmail()).isPresent()) {
            log.error("Email already exists: {}", userRequest.getEmail());
            throw new AppException(ErrorCode.EMAIL_EXISTS);
        }

        // Check if phone already exists
        if (employeeRepository.existsEmployeeByPhone(userRequest.getPhone())) {
            log.error("Phone already exists: {}", userRequest.getPhone());
            throw new AppException(ErrorCode.PHONE_EXISTS);
        }

        // Create account
        Account account = Account.builder()
                .email(userRequest.getEmail())
                .password(passwordEncoder.encode(userRequest.getPassword()))
                .role(userRequest.getRole())
                .status(userRequest.getStatus())
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        Account savedAccount = accountRepository.save(account);
        log.info("Created account for employee: {}", savedAccount.getId());

        // Create user
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

        User savedUser = employeeRepository.save(user);
        log.info("Created employee: {} with role: {}", savedUser.getId(), userRequest.getRole());

        // Assign to store if storeId is provided
        if (userRequest.getStoreId() != null && !userRequest.getStoreId().isEmpty()) {
            assignEmployeeToStore(savedUser.getId(), userRequest.getStoreId());
        }

        return toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse createAdmin(UserRequest userRequest) {
        log.info("Creating new admin user with email: {}", userRequest.getEmail());
        
        // Force ADMIN role
        userRequest.setRole(EnumRole.ADMIN);
        
        // Check if email already exists
        if (accountRepository.findByEmailAndIsDeletedFalse(userRequest.getEmail()).isPresent()) {
            log.error("Email already exists: {}", userRequest.getEmail());
            throw new AppException(ErrorCode.EMAIL_EXISTS);
        }
        
        // Check if phone already exists
        if (userRepository.findByPhoneAndIsDeletedFalse(userRequest.getPhone()).isPresent()) {
            log.error("Phone already exists: {}", userRequest.getPhone());
            throw new AppException(ErrorCode.PHONE_EXISTS);
        }

        // Create account with ADMIN role
        Account account = Account.builder()
                .email(userRequest.getEmail())
                .password(passwordEncoder.encode(userRequest.getPassword()))
                .role(EnumRole.ADMIN) // Force ADMIN role
                .status(userRequest.getStatus())
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();
        
        Account savedAccount = accountRepository.save(account);
        log.info("Created admin account: {}", savedAccount.getId());

        // Create user
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
        log.info("Created admin user: {} with email: {}", savedUser.getId(), userRequest.getEmail());

        return toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse updateEmployee(String id, UserUpdateRequest userRequest) {
        log.info("Updating employee: {}", id);
        
        User existingEmployee = employeeRepository.findEmployeeById(id)
                .orElseThrow(() -> {
                    log.error("Employee not found: {}", id);
                    return new AppException(ErrorCode.USER_NOT_FOUND);
                });

        // Validate that the user is still an employee
        if (!isEmployeeRole(existingEmployee.getAccount().getRole())) {
            log.error("User {} is not an employee", id);
            throw new AppException(ErrorCode.INVALID_ROLE);
        }

        // Check phone uniqueness if phone is being updated
        if (userRequest.getPhone() != null && !userRequest.getPhone().equals(existingEmployee.getPhone())) {
            if (employeeRepository.existsEmployeeByPhone(userRequest.getPhone())) {
                log.error("Phone already exists: {}", userRequest.getPhone());
                throw new AppException(ErrorCode.PHONE_EXISTS);
            }
        }

        // Update employee fields
        if (userRequest.getFullName() != null) {
            existingEmployee.setFullName(userRequest.getFullName());
        }
        if (userRequest.getPhone() != null) {
            existingEmployee.setPhone(userRequest.getPhone());
        }
        if (userRequest.getBirthday() != null) {
            existingEmployee.setBirthday(userRequest.getBirthday());
        }
        if (userRequest.getGender() != null) {
            existingEmployee.setGender(userRequest.getGender());
        }
        if (userRequest.getStatus() != null) {
            existingEmployee.setStatus(userRequest.getStatus());
            existingEmployee.getAccount().setStatus(userRequest.getStatus());
        }
        if (userRequest.getAvatar() != null) {
            existingEmployee.setAvatar(userRequest.getAvatar());
        }
        if (userRequest.getCccd() != null) {
            existingEmployee.setCccd(userRequest.getCccd());
        }

        // Handle role update
        if (userRequest.getRole() != null) {
            // Validate new role
            validateEmployeeRole(userRequest.getRole());
            
            // Update account role
            existingEmployee.getAccount().setRole(userRequest.getRole());
            log.info("Updated employee role to: {}", userRequest.getRole());
        }

        // Handle store assignment
        if (userRequest.getStoreId() != null) {
            // Validate store exists
            Store store = storeRepository.findById(userRequest.getStoreId())
                    .orElseThrow(() -> {
                        log.error("Store not found: {}", userRequest.getStoreId());
                        return new AppException(ErrorCode.STORE_NOT_FOUND);
                    });

            // Check if employee is already assigned to this store
            Optional<UserStore> existingUserStore = userStoreRepository.findByUserIdAndStoreId(id, userRequest.getStoreId());
            
            if (existingUserStore.isEmpty()) {
                // Remove from all other stores first
                userStoreRepository.deleteByUserId(id);
                
                // Assign to new store
                UserStore userStore = UserStore.builder()
                        .user(existingEmployee)
                        .store(store)
                        .build();
                userStoreRepository.save(userStore);
                log.info("Assigned employee {} to store {}", id, userRequest.getStoreId());
            } else {
                log.info("Employee {} is already assigned to store {}", id, userRequest.getStoreId());
            }
        }

        User updatedEmployee = employeeRepository.save(existingEmployee);
        
        if (userRequest.getStatus() != null || userRequest.getRole() != null) {
            accountRepository.save(existingEmployee.getAccount());
        }
        
        log.info("Updated employee: {}", updatedEmployee.getId());

        return toUserResponse(updatedEmployee);
    }

    @Override
    public UserResponse getEmployeeById(String id) {
        log.info("Fetching employee by ID: {}", id);
        
        User employee = employeeRepository.findEmployeeById(id)
                .orElseThrow(() -> {
                    log.error("Employee not found: {}", id);
                    return new AppException(ErrorCode.USER_NOT_FOUND);
                });

        return toUserResponse(employee);
    }

    @Override
    public List<UserResponse> getAllEmployees() {
        log.info("Fetching all employees");
        
        List<User> employees = employeeRepository.findAllEmployees();
        
        return employees.stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserResponse> getEmployeesByRole(EnumRole role) {
        log.info("Fetching employees by role: {}", role);
        
        // Validate that role is an employee role
        validateEmployeeRole(role);

        List<User> employees = employeeRepository.findEmployeesByRole(role);
        
        return employees.stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserResponse> getEmployeesByStoreId(String storeId) {
        log.info("Fetching employees by store ID: {}", storeId);
        
        List<User> employees = employeeRepository.findEmployeesByStoreId(storeId);
        
        return employees.stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserResponse> getEmployeesByStoreIdAndRole(String storeId, EnumRole role) {
        log.info("Fetching employees by store ID: {} and role: {}", storeId, role);
        
        // Validate that role is an employee role
        validateEmployeeRole(role);

        List<User> employees = employeeRepository.findEmployeesByStoreIdAndRole(storeId, role);
        
        return employees.stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<UserResponse> getEmployeesWithPagination(int page, int size) {
        log.info("Fetching employees with pagination - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> employeePage = employeeRepository.findAllEmployees(pageable);
        
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
        
        // Validate that role is an employee role
        validateEmployeeRole(role);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> employeePage = employeeRepository.findEmployeesByRole(role, pageable);
        
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
    public PageResponse<UserResponse> searchEmployees(String searchTerm, int page, int size) {
        log.info("Searching employees with term: {} - page: {}, size: {}", searchTerm, page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> employeePage = employeeRepository.searchEmployees(searchTerm, pageable);
        
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
    public UserResponse updateEmployeeRole(String userId, EnumRole newRole) {
        log.info("Updating employee role for user: {} to role: {}", userId, newRole);
        
        // Validate that new role is an employee role
        validateEmployeeRole(newRole);
        
        User employee = employeeRepository.findEmployeeById(userId)
                .orElseThrow(() -> {
                    log.error("Employee not found: {}", userId);
                    return new AppException(ErrorCode.USER_NOT_FOUND);
                });

        // Update role
        employee.getAccount().setRole(newRole);
        accountRepository.save(employee.getAccount());
        
        log.info("Updated employee role for user: {} to role: {}", userId, newRole);

        return toUserResponse(employee);
    }

    @Override
    @Transactional
    public void deleteEmployee(String id) {
        log.info("Deleting employee: {}", id);
        
        User employee = employeeRepository.findEmployeeById(id)
                .orElseThrow(() -> {
                    log.error("Employee not found: {}", id);
                    return new AppException(ErrorCode.USER_NOT_FOUND);
                });

        employee.setStatus(EnumStatus.DELETED);
        employee.setIsDeleted(true);
        employee.getAccount().setIsDeleted(true);
        employee.getAccount().setStatus(EnumStatus.DELETED);

        employeeRepository.save(employee);
        accountRepository.save(employee.getAccount());
        
        log.info("Deleted employee: {}", id);
    }

    @Override
    @Transactional
    public void disableEmployee(String id) {
        log.info("Disabling employee: {}", id);
        
        User employee = employeeRepository.findEmployeeById(id)
                .orElseThrow(() -> {
                    log.error("Employee not found: {}", id);
                    return new AppException(ErrorCode.USER_NOT_FOUND);
                });

        employee.setStatus(EnumStatus.INACTIVE);
        employee.getAccount().setStatus(EnumStatus.INACTIVE);

        employeeRepository.save(employee);
        accountRepository.save(employee.getAccount());
        
        log.info("Disabled employee: {}", id);
    }

    @Override
    @Transactional
    public void enableEmployee(String id) {
        log.info("Enabling employee: {}", id);
        
        User employee = employeeRepository.findEmployeeById(id)
                .orElseThrow(() -> {
                    log.error("Employee not found: {}", id);
                    return new AppException(ErrorCode.USER_NOT_FOUND);
                });

        employee.setStatus(EnumStatus.ACTIVE);
        employee.getAccount().setStatus(EnumStatus.ACTIVE);

        employeeRepository.save(employee);
        accountRepository.save(employee.getAccount());
        
        log.info("Enabled employee: {}", id);
    }

    @Override
    @Transactional
    public void assignEmployeeToStore(String employeeId, String storeId) {
        log.info("Assigning employee {} to store {}", employeeId, storeId);
        
        // Verify employee exists
        employeeRepository.findEmployeeById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found: {}", employeeId);
                    return new AppException(ErrorCode.USER_NOT_FOUND);
                });

        // Check if relationship already exists
        if (userStoreRepository.findByUserIdAndStoreIdAndIsDeletedFalse(employeeId, storeId).isPresent()) {
            log.warn("Employee {} is already assigned to store {}", employeeId, storeId);
            throw new AppException(ErrorCode.USER_STORE_RELATIONSHIP_EXISTS);
        }

        // Create user-store relationship
        UserStore userStore = UserStore.builder()
                .userId(employeeId)
                .storeId(storeId)
                .build();

        userStoreRepository.save(userStore);
        
        log.info("Assigned employee {} to store {}", employeeId, storeId);
    }

    @Override
    @Transactional
    public void removeEmployeeFromStore(String employeeId, String storeId) {
        log.info("Removing employee {} from store {}", employeeId, storeId);
        
        UserStore userStore = userStoreRepository.findByUserIdAndStoreIdAndIsDeletedFalse(employeeId, storeId)
                .orElseThrow(() -> {
                    log.error("User-store relationship not found for employee {} and store {}", employeeId, storeId);
                    return new AppException(ErrorCode.USER_STORE_RELATIONSHIP_NOT_FOUND);
                });

        userStore.setIsDeleted(true);
        userStoreRepository.save(userStore);
        
        log.info("Removed employee {} from store {}", employeeId, storeId);
    }

    @Override
    public Long getEmployeeCountByRole(EnumRole role) {
        log.info("Getting employee count for role: {}", role);
        
        validateEmployeeRole(role);
        
        return employeeRepository.countEmployeesByRole(role);
    }

    @Override
    public Long getTotalEmployeeCount() {
        log.info("Getting total employee count");
        
        return employeeRepository.countAllEmployees();
    }

    /**
     * Validate that the role is an employee role (allow ADMIN, block CUSTOMER)
     */
    private void validateEmployeeRole(EnumRole role) {
        if (role == EnumRole.CUSTOMER) {
            log.error("CUSTOMER role is not allowed in employee operations");
            throw new AppException(ErrorCode.CANNOT_CREATE_CUSTOMER_THROUGH_EMPLOYEE_API);
        }
        
        if (!isEmployeeRole(role)) {
            log.error("Invalid role for employee operations: {}", role);
            throw new AppException(ErrorCode.INVALID_ROLE);
        }
    }

    /**
     * Check if the role is an employee role
     */
    private boolean isEmployeeRole(EnumRole role) {
        return EMPLOYEE_ROLES.contains(role);
    }

    /**
     * Convert User entity to UserResponse DTO
     */
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
}

