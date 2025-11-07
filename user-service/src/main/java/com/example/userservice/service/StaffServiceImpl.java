package com.example.userservice.service;

import com.example.userservice.entity.Account;
import com.example.userservice.entity.Employee;
import com.example.userservice.entity.EmployeeStore;
import com.example.userservice.entity.User; // Only used in deprecated toStaffResponse(User) method
import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import com.example.userservice.enums.ErrorCode;
import com.example.userservice.exception.AppException;
import com.example.userservice.repository.AccountRepository;
import com.example.userservice.repository.EmployeeRepository;
import com.example.userservice.repository.EmployeeStoreRepository;
import com.example.userservice.repository.StoreRepository;
import com.example.userservice.request.StaffRequest;
import com.example.userservice.request.StaffUpdateRequest;
import com.example.userservice.response.PageResponse;
import com.example.userservice.response.StaffResponse;
import com.example.userservice.service.inteface.StaffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {

    private final AccountRepository accountRepository;
    private final EmployeeRepository employeeRepository;
    // Note: UserRepository removed - all staff operations now use EmployeeRepository
    private final EmployeeStoreRepository employeeStoreRepository;
    private final StoreRepository storeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public StaffResponse createStaff(StaffRequest staffRequest) {
        log.info("Creating STAFF employee with email: {}", staffRequest.getEmail());
        
        if (accountRepository.findByEmailAndIsDeletedFalse(staffRequest.getEmail()).isPresent()) {
            throw new AppException(ErrorCode.EMAIL_EXISTS);
        }
        
        if (employeeRepository.existsEmployeeByPhone(staffRequest.getPhone())) {
            throw new AppException(ErrorCode.PHONE_EXISTS);
        }

        // Create account with STAFF role
        Account account = Account.builder()
                .email(staffRequest.getEmail())
                .password(passwordEncoder.encode(staffRequest.getPassword()))
                .role(EnumRole.STAFF)
                .status(staffRequest.getStatus() != null ? staffRequest.getStatus() : EnumStatus.ACTIVE)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();
        
        Account savedAccount = accountRepository.save(account);
        log.info("Created account for STAFF: {}", savedAccount.getId());

        // Generate employee code
        String employeeCode = generateEmployeeCode(EnumRole.STAFF);

        // Create Employee entity (NOT User)
        Employee staff = Employee.builder()
                .code(employeeCode)
                .fullName(staffRequest.getFullName())
                .phone(staffRequest.getPhone())
                .birthday(staffRequest.getBirthday())
                .gender(staffRequest.getGender())
                .status(staffRequest.getStatus() != null ? staffRequest.getStatus() : EnumStatus.ACTIVE)
                .avatar(staffRequest.getAvatar())
                .cccd(staffRequest.getCccd())
                .department(staffRequest.getDepartment())
                .position(staffRequest.getPosition())
                .salary(staffRequest.getSalary())
                .account(savedAccount)
                .build();

        Employee savedStaff = employeeRepository.save(staff);
        log.info("Created STAFF employee: {} with code: {}", savedStaff.getId(), employeeCode);
        
        // Handle store assignments if provided
        if (staffRequest.getStoreIds() != null && !staffRequest.getStoreIds().isEmpty()) {
            for (String storeId : staffRequest.getStoreIds()) {
                // Validate store exists
                storeRepository.findByIdAndIsDeletedFalse(storeId)
                        .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));
                
                EmployeeStore employeeStore = EmployeeStore.builder()
                        .employeeId(savedStaff.getId())
                        .storeId(storeId)
                        .build();
                employeeStoreRepository.save(employeeStore);
            }
            log.info("Assigned STAFF {} to {} stores", savedStaff.getId(), staffRequest.getStoreIds().size());
        }
        
        return toStaffResponseFromEmployee(savedStaff);
    }
    
    private String generateEmployeeCode(EnumRole role) {
        String prefix;
        switch (role) {
            case ADMIN: prefix = "ADM"; break;
            // SELLER role removed - use STAFF instead
            case BRANCH_MANAGER: prefix = "MGR"; break;
            case DELIVERY: prefix = "DLV"; break;
            case STAFF: prefix = "STF"; break;
            default: prefix = "EMP";
        }
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 1000);
        return String.format("%s-%d-%03d", prefix, timestamp, random);
    }

    @Override
    @Transactional
    public StaffResponse updateStaff(String id, StaffUpdateRequest staffRequest) {
        Employee existingStaff = employeeRepository.findEmployeeById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Check if the employee is actually a STAFF member
        if (!EnumRole.STAFF.equals(existingStaff.getAccount().getRole())) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        if (staffRequest.getPhone() != null && !staffRequest.getPhone().equals(existingStaff.getPhone())) {
            Optional<Employee> otherEmployee = employeeRepository.findByPhoneAndIsDeletedFalse(staffRequest.getPhone());
            if (otherEmployee.isPresent() && !otherEmployee.get().getId().equals(id)) {
                            throw new AppException(ErrorCode.PHONE_EXISTS);
                        }
        }

        if (staffRequest.getEmail() != null && !staffRequest.getEmail().equals(existingStaff.getAccount().getEmail())) {
            accountRepository.findByEmailAndIsDeletedFalse(staffRequest.getEmail())
                    .ifPresent(account -> {
                        if (!account.getId().equals(existingStaff.getAccount().getId())) {
                            throw new AppException(ErrorCode.EMAIL_EXISTS);
                        }
                    });
        }

        if (staffRequest.getFullName() != null) {
            existingStaff.setFullName(staffRequest.getFullName());
        }
        if (staffRequest.getPhone() != null) {
            existingStaff.setPhone(staffRequest.getPhone());
        }
        if (staffRequest.getBirthday() != null) {
            existingStaff.setBirthday(staffRequest.getBirthday());
        }
        if (staffRequest.getGender() != null) {
            existingStaff.setGender(staffRequest.getGender());
        }
        if (staffRequest.getStatus() != null) {
            existingStaff.setStatus(staffRequest.getStatus());
            existingStaff.getAccount().setStatus(staffRequest.getStatus());
        }
        if (staffRequest.getAvatar() != null) {
            existingStaff.setAvatar(staffRequest.getAvatar());
        }
        if (staffRequest.getCccd() != null) {
            existingStaff.setCccd(staffRequest.getCccd());
        }
        if (staffRequest.getDepartment() != null) {
            existingStaff.setDepartment(staffRequest.getDepartment());
        }
        if (staffRequest.getPosition() != null) {
            existingStaff.setPosition(staffRequest.getPosition());
        }
        if (staffRequest.getSalary() != null) {
            existingStaff.setSalary(staffRequest.getSalary());
        }
        if (staffRequest.getEmail() != null) {
            existingStaff.getAccount().setEmail(staffRequest.getEmail());
        }

        Employee updatedStaff = employeeRepository.save(existingStaff);
        if (staffRequest.getEmail() != null || staffRequest.getStatus() != null) {
            accountRepository.save(existingStaff.getAccount());
        }

        // Handle store assignments if provided
        if (staffRequest.getStoreIds() != null) {
            // Remove existing store assignments
            List<EmployeeStore> existingEmployeeStores = employeeStoreRepository.findByEmployeeIdAndIsDeletedFalse(id);
            for (EmployeeStore employeeStore : existingEmployeeStores) {
                employeeStore.setIsDeleted(true);
                employeeStoreRepository.save(employeeStore);
            }
            
            // Add new store assignments
            if (!staffRequest.getStoreIds().isEmpty()) {
                for (String storeId : staffRequest.getStoreIds()) {
                    // Validate store exists
                    storeRepository.findByIdAndIsDeletedFalse(storeId)
                            .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));
                    
                    EmployeeStore employeeStore = EmployeeStore.builder()
                            .employeeId(updatedStaff.getId())
                            .storeId(storeId)
                            .build();
                    employeeStoreRepository.save(employeeStore);
                }
                log.info("Updated staff {} store assignments to {} stores", updatedStaff.getId(), staffRequest.getStoreIds().size());
            } else {
                log.info("Removed all store assignments for staff {}", updatedStaff.getId());
            }
        }

        return toStaffResponseFromEmployee(updatedStaff);
    }

    @Override
    public StaffResponse getStaffById(String id) {
        // Query from EmployeeRepository since STAFF is now in employees table
        log.info("Fetching STAFF employee by id: {}", id);
        Employee staff = employeeRepository.findEmployeeById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        if (!EnumRole.STAFF.equals(staff.getAccount().getRole())) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        
        return toStaffResponseFromEmployee(staff);
    }

    @Override
    public List<StaffResponse> getAllStaff() {
        // Query from EmployeeRepository since STAFF is now in employees table
        log.info("Fetching all STAFF employees");
        List<Employee> employees = employeeRepository.findAllEmployees().stream()
                .filter(emp -> EnumRole.STAFF.equals(emp.getAccount().getRole()))
                .collect(Collectors.toList());
        
        return employees.stream()
                .map(this::toStaffResponseFromEmployee)
                .collect(Collectors.toList());
    }

    @Override
    public List<StaffResponse> getStaffByStatus(String status) {
        // Query from EmployeeRepository since STAFF is now in employees table
        log.info("Fetching STAFF employees by status: {}", status);
        EnumStatus enumStatus = EnumStatus.valueOf(status.toUpperCase());
        List<Employee> employees = employeeRepository.findAllEmployees().stream()
                .filter(emp -> EnumRole.STAFF.equals(emp.getAccount().getRole()))
                .filter(emp -> enumStatus.equals(emp.getStatus()))
                .collect(Collectors.toList());
        
        return employees.stream()
                .map(this::toStaffResponseFromEmployee)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<StaffResponse> getStaffWithPagination(int page, int size) {
        // Query from EmployeeRepository since STAFF is now in employees table
        log.info("Fetching staff with pagination - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Employee> employeePage = employeeRepository.findAllEmployees(pageable);
        
        // Filter to only STAFF role
        List<StaffResponse> staffResponses = employeePage.getContent().stream()
                .filter(emp -> EnumRole.STAFF.equals(emp.getAccount().getRole()))
                .map(this::toStaffResponseFromEmployee)
                .collect(Collectors.toList());

        // Get total count of STAFF employees
        long totalStaffElements = employeeRepository.countEmployeesByRole(EnumRole.STAFF);
        int totalStaffPages = (int) Math.ceil((double) totalStaffElements / size);

        return PageResponse.<StaffResponse>builder()
                .content(staffResponses)
                .totalElements(totalStaffElements)
                .totalPages(totalStaffPages)
                .size(size)
                .number(page)
                .first(page == 0)
                .last(page >= totalStaffPages - 1)
                .build();
    }

    @Override
    public PageResponse<StaffResponse> searchStaff(String searchTerm, int page, int size) {
        // Query from EmployeeRepository since STAFF is now in employees table
        log.info("Searching staff with term: {} - page: {}, size: {}", searchTerm, page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Employee> employeePage = employeeRepository.searchEmployees(searchTerm, pageable);
        
        // Filter to only STAFF role
        List<StaffResponse> staffResponses = employeePage.getContent().stream()
                .filter(emp -> EnumRole.STAFF.equals(emp.getAccount().getRole()))
                .map(this::toStaffResponseFromEmployee)
                .collect(Collectors.toList());

        // Get total count of matching STAFF employees
        long totalStaffElements = employeeRepository.findAllEmployees().stream()
                .filter(emp -> EnumRole.STAFF.equals(emp.getAccount().getRole()))
                .filter(emp -> {
                    String lowerSearch = searchTerm.toLowerCase();
                    return (emp.getFullName() != null && emp.getFullName().toLowerCase().contains(lowerSearch)) ||
                           (emp.getPhone() != null && emp.getPhone().toLowerCase().contains(lowerSearch)) ||
                           (emp.getAccount() != null && emp.getAccount().getEmail() != null && 
                            emp.getAccount().getEmail().toLowerCase().contains(lowerSearch));
                })
                .count();
        
        int totalStaffPages = (int) Math.ceil((double) totalStaffElements / size);

        return PageResponse.<StaffResponse>builder()
                .content(staffResponses)
                .totalElements(totalStaffElements)
                .totalPages(totalStaffPages)
                .size(size)
                .number(page)
                .first(page == 0)
                .last(page >= totalStaffPages - 1)
                .build();
    }

    @Override
    @Transactional
    public void deleteStaff(String id) {
        // Query from EmployeeRepository since STAFF is now in employees table
        log.info("Deleting STAFF employee: {}", id);
        Employee staff = employeeRepository.findEmployeeById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        if (!EnumRole.STAFF.equals(staff.getAccount().getRole())) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        
        staff.setStatus(EnumStatus.DELETED);
        staff.setIsDeleted(true);
        staff.getAccount().setIsDeleted(true);
        
        employeeRepository.save(staff);
        accountRepository.save(staff.getAccount());
        log.info("Deleted STAFF employee: {}", id);
    }

    @Override
    @Transactional
    public void disableStaff(String id) {
        // Query from EmployeeRepository since STAFF is now in employees table
        log.info("Disabling STAFF employee: {}", id);
        Employee staff = employeeRepository.findEmployeeById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!EnumRole.STAFF.equals(staff.getAccount().getRole())) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        staff.setStatus(EnumStatus.INACTIVE);
        staff.getAccount().setStatus(EnumStatus.INACTIVE);
        
        employeeRepository.save(staff);
        accountRepository.save(staff.getAccount());
        log.info("Disabled STAFF employee: {}", id);
    }

    @Override
    @Transactional
    public void enableStaff(String id) {
        // Query from EmployeeRepository since STAFF is now in employees table
        log.info("Enabling STAFF employee: {}", id);
        Employee staff = employeeRepository.findEmployeeById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!EnumRole.STAFF.equals(staff.getAccount().getRole())) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        staff.setStatus(EnumStatus.ACTIVE);
        staff.getAccount().setStatus(EnumStatus.ACTIVE);
        
        employeeRepository.save(staff);
        accountRepository.save(staff.getAccount());
        log.info("Enabled STAFF employee: {}", id);
    }

    @Override
    public StaffResponse getStaffByEmail(String email) {
        // Query from EmployeeRepository since STAFF is now in employees table
        log.info("Fetching STAFF employee by email: {}", email);
        Employee employee = employeeRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_FOUND));
        
        if (!EnumRole.STAFF.equals(employee.getAccount().getRole())) {
            throw new AppException(ErrorCode.EMAIL_NOT_FOUND);
        }
        
        return toStaffResponseFromEmployee(employee);
    }

    @Override
    public StaffResponse getStaffByPhone(String phone) {
        // Query from EmployeeRepository since STAFF is now in employees table
        log.info("Fetching STAFF employee by phone: {}", phone);
        Employee employee = employeeRepository.findByPhoneAndIsDeletedFalse(phone)
                .orElseThrow(() -> new AppException(ErrorCode.PHONE_NOT_FOUND));
        
        if (!EnumRole.STAFF.equals(employee.getAccount().getRole())) {
            throw new AppException(ErrorCode.PHONE_NOT_FOUND);
        }
        
        return toStaffResponseFromEmployee(employee);
    }

    @Override
    public List<StaffResponse> getStaffByDepartment(String department) {
        // Query from EmployeeRepository since STAFF is now in employees table
        log.info("Fetching STAFF employees by department: {}", department);
        List<Employee> employees = employeeRepository.findAllEmployees().stream()
                .filter(emp -> EnumRole.STAFF.equals(emp.getAccount().getRole()))
                .filter(emp -> department.equals(emp.getDepartment()))
                .collect(Collectors.toList());
        
        return employees.stream()
                .map(this::toStaffResponseFromEmployee)
                .collect(Collectors.toList());
    }

    private StaffResponse toStaffResponseFromEmployee(Employee employee) {
        // Load storeIds for the employee
        List<String> storeIds = employeeStoreRepository.findByEmployeeIdAndIsDeletedFalse(employee.getId())
                .stream()
                .map(EmployeeStore::getStoreId)
                .collect(Collectors.toList());

        return StaffResponse.builder()
                .id(employee.getId())
                .birthday(employee.getBirthday())
                .gender(employee.getGender())
                .fullName(employee.getFullName())
                .avatar(employee.getAvatar())
                .phone(employee.getPhone())
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .status(employee.getStatus())
                .cccd(employee.getCccd())
                .department(employee.getDepartment())
                .position(employee.getPosition())
                .salary(employee.getSalary())
                .email(employee.getAccount() != null ? employee.getAccount().getEmail() : null)
                .role(employee.getAccount() != null ? employee.getAccount().getRole() : null)
                .storeIds(storeIds)
                .build();
    }
    
    // Legacy method - DEPRECATED: User entity no longer has department, position, salary
    // This method should not be used. All staff should be in employees table.
    // If this method is called, it means staff data hasn't been migrated yet.
    // TODO: Remove this method after verifying all staff are migrated to employees table
    @Deprecated
    @SuppressWarnings("unused")
    private StaffResponse toStaffResponse(User staff) {
        log.warn("toStaffResponse(User) called - Staff should be in employees table. User ID: {}", staff.getId());
        
        // Try to find employee record
        Optional<Employee> employeeOpt = employeeRepository.findById(staff.getId());
        if (employeeOpt.isPresent()) {
            return toStaffResponseFromEmployee(employeeOpt.get());
        }
        
        // Fallback: return with null values for department, position, salary
        // This should not happen if migration is complete
        List<String> storeIds = employeeStoreRepository.findByEmployeeIdAndIsDeletedFalse(staff.getId())
                .stream()
                .map(EmployeeStore::getStoreId)
                .collect(Collectors.toList());

        return StaffResponse.builder()
                .id(staff.getId())
                .birthday(staff.getBirthday())
                .gender(staff.getGender())
                .fullName(staff.getFullName())
                .avatar(staff.getAvatar())
                .phone(staff.getPhone())
                .createdAt(staff.getCreatedAt())
                .updatedAt(staff.getUpdatedAt())
                .status(staff.getStatus())
                .cccd(staff.getCccd())
                .department(null) // User entity no longer has this field
                .position(null)   // User entity no longer has this field
                .salary(null)     // User entity no longer has this field
                .email(staff.getAccount() != null ? staff.getAccount().getEmail() : null)
                .role(staff.getAccount() != null ? staff.getAccount().getRole() : null)
                .storeIds(storeIds)
                .build();
    }
}
