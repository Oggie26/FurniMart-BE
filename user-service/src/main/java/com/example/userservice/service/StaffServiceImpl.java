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
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final UserStoreRepository userStoreRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public StaffResponse createStaff(StaffRequest staffRequest) {
        if (accountRepository.findByEmailAndIsDeletedFalse(staffRequest.getEmail()).isPresent()) {
            throw new AppException(ErrorCode.EMAIL_EXISTS);
        }
        
        if (userRepository.findByPhoneAndIsDeletedFalse(staffRequest.getPhone()).isPresent()) {
            throw new AppException(ErrorCode.PHONE_EXISTS);
        }

        Account account = Account.builder()
                .email(staffRequest.getEmail())
                .password(passwordEncoder.encode(staffRequest.getPassword()))
                .role(EnumRole.STAFF)
                .status(staffRequest.getStatus())
                .build();
        
        Account savedAccount = accountRepository.save(account);

        User staff = User.builder()
                .fullName(staffRequest.getFullName())
                .phone(staffRequest.getPhone())
                .birthday(staffRequest.getBirthday())
                .gender(staffRequest.getGender())
                .status(staffRequest.getStatus())
                .avatar(staffRequest.getAvatar())
                .cccd(staffRequest.getCccd())
                .department(staffRequest.getDepartment())
                .position(staffRequest.getPosition())
                .salary(staffRequest.getSalary())
                .account(savedAccount)
                .build();

        User savedStaff = userRepository.save(staff);
        
        // Handle store assignments if provided
        if (staffRequest.getStoreIds() != null && !staffRequest.getStoreIds().isEmpty()) {
            for (String storeId : staffRequest.getStoreIds()) {
                UserStore userStore = UserStore.builder()
                        .userId(savedStaff.getId())
                        .storeId(storeId)
                        .user(savedStaff)
                        .build();
                userStoreRepository.save(userStore);
            }
            log.info("Assigned staff {} to {} stores", savedStaff.getId(), staffRequest.getStoreIds().size());
        }
        
        return toStaffResponse(savedStaff);
    }

    @Override
    @Transactional
    public StaffResponse updateStaff(String id, StaffUpdateRequest staffRequest) {
        User existingStaff = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Check if the user is actually a staff member
        if (!EnumRole.STAFF.equals(existingStaff.getAccount().getRole())) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        if (staffRequest.getPhone() != null && !staffRequest.getPhone().equals(existingStaff.getPhone())) {
            userRepository.findByPhoneAndIsDeletedFalse(staffRequest.getPhone())
                    .ifPresent(user -> {
                        if (!user.getId().equals(id)) {
                            throw new AppException(ErrorCode.PHONE_EXISTS);
                        }
                    });
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

        User updatedStaff = userRepository.save(existingStaff);
        if (staffRequest.getEmail() != null || staffRequest.getStatus() != null) {
            accountRepository.save(existingStaff.getAccount());
        }

        // Handle store assignments if provided
        if (staffRequest.getStoreIds() != null) {
            // Remove existing store assignments
            List<UserStore> existingUserStores = userStoreRepository.findByUserIdAndIsDeletedFalse(id);
            for (UserStore userStore : existingUserStores) {
                userStore.setIsDeleted(true);
                userStoreRepository.save(userStore);
            }
            
            // Add new store assignments
            if (!staffRequest.getStoreIds().isEmpty()) {
                for (String storeId : staffRequest.getStoreIds()) {
                    UserStore userStore = UserStore.builder()
                            .userId(updatedStaff.getId())
                            .storeId(storeId)
                            .user(updatedStaff)
                            .build();
                    userStoreRepository.save(userStore);
                }
                log.info("Updated staff {} store assignments to {} stores", updatedStaff.getId(), staffRequest.getStoreIds().size());
            } else {
                log.info("Removed all store assignments for staff {}", updatedStaff.getId());
            }
        }

        return toStaffResponse(updatedStaff);
    }

    @Override
    public StaffResponse getStaffById(String id) {
        User staff = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        if (!EnumRole.STAFF.equals(staff.getAccount().getRole())) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        
        return toStaffResponse(staff);
    }

    @Override
    public List<StaffResponse> getAllStaff() {
        List<User> allUsers = userRepository.findByIsDeletedFalse();
        return allUsers.stream()
                .filter(user -> EnumRole.STAFF.equals(user.getAccount().getRole()))
                .map(this::toStaffResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<StaffResponse> getStaffByStatus(String status) {
        EnumStatus enumStatus = EnumStatus.valueOf(status.toUpperCase());
        List<User> users = userRepository.findByStatusAndIsDeletedFalse(enumStatus);
        return users.stream()
                .filter(user -> EnumRole.STAFF.equals(user.getAccount().getRole()))
                .map(this::toStaffResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<StaffResponse> getStaffWithPagination(int page, int size) {
        log.info("Fetching staff with pagination - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> userPage = userRepository.findByIsDeletedFalse(pageable);
        
        List<StaffResponse> staffResponses = userPage.getContent().stream()
                .filter(user -> EnumRole.STAFF.equals(user.getAccount().getRole()))
                .map(this::toStaffResponse)
                .collect(Collectors.toList());

        // Calculate adjusted pagination info since we're filtering
        long totalStaffElements = getAllStaff().size();
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
        log.info("Searching staff with term: {} - page: {}, size: {}", searchTerm, page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> userPage = userRepository.searchUsers(searchTerm, pageable);
        
        List<StaffResponse> staffResponses = userPage.getContent().stream()
                .filter(user -> EnumRole.STAFF.equals(user.getAccount().getRole()))
                .map(this::toStaffResponse)
                .collect(Collectors.toList());

        return PageResponse.<StaffResponse>builder()
                .content(staffResponses)
                .totalElements((long) staffResponses.size())
                .totalPages(staffResponses.isEmpty() ? 0 : 1)
                .size(size)
                .number(page)
                .first(page == 0)
                .last(true)
                .build();
    }

    @Override
    @Transactional
    public void deleteStaff(String id) {
        User staff = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        if (!EnumRole.STAFF.equals(staff.getAccount().getRole())) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        
        staff.setStatus(EnumStatus.DELETED);
        staff.setIsDeleted(true);
        staff.getAccount().setIsDeleted(true);
        
        userRepository.save(staff);
        accountRepository.save(staff.getAccount());
    }

    @Override
    @Transactional
    public void disableStaff(String id) {
        User staff = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!EnumRole.STAFF.equals(staff.getAccount().getRole())) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        staff.setStatus(EnumStatus.INACTIVE);
        staff.getAccount().setStatus(EnumStatus.INACTIVE);
        
        userRepository.save(staff);
        accountRepository.save(staff.getAccount());
    }

    @Override
    @Transactional
    public void enableStaff(String id) {
        User staff = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!EnumRole.STAFF.equals(staff.getAccount().getRole())) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        staff.setStatus(EnumStatus.ACTIVE);
        staff.getAccount().setStatus(EnumStatus.ACTIVE);
        
        userRepository.save(staff);
        accountRepository.save(staff.getAccount());
    }

    @Override
    public StaffResponse getStaffByEmail(String email) {
        User staff = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_FOUND));
        
        if (!EnumRole.STAFF.equals(staff.getAccount().getRole())) {
            throw new AppException(ErrorCode.EMAIL_NOT_FOUND);
        }
        
        return toStaffResponse(staff);
    }

    @Override
    public StaffResponse getStaffByPhone(String phone) {
        User staff = userRepository.findByPhoneAndIsDeletedFalse(phone)
                .orElseThrow(() -> new AppException(ErrorCode.PHONE_NOT_FOUND));
        
        if (!EnumRole.STAFF.equals(staff.getAccount().getRole())) {
            throw new AppException(ErrorCode.PHONE_NOT_FOUND);
        }
        
        return toStaffResponse(staff);
    }

    @Override
    public List<StaffResponse> getStaffByDepartment(String department) {
        List<User> allUsers = userRepository.findByIsDeletedFalse();
        return allUsers.stream()
                .filter(user -> EnumRole.STAFF.equals(user.getAccount().getRole()))
                .filter(user -> department.equals(user.getDepartment()))
                .map(this::toStaffResponse)
                .collect(Collectors.toList());
    }

    private StaffResponse toStaffResponse(User staff) {
        // Load storeIds for the staff
        List<String> storeIds = userStoreRepository.findByUserIdAndIsDeletedFalse(staff.getId())
                .stream()
                .map(UserStore::getStoreId)
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
                .department(staff.getDepartment())
                .position(staff.getPosition())
                .salary(staff.getSalary())
                .email(staff.getAccount() != null ? staff.getAccount().getEmail() : null)
                .role(staff.getAccount() != null ? staff.getAccount().getRole() : null)
                .storeIds(storeIds)
                .build();
    }
}
