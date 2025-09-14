package com.example.userservice.service;

import com.example.userservice.entity.Account;
import com.example.userservice.entity.User;
import com.example.userservice.enums.EnumStatus;
import com.example.userservice.enums.ErrorCode;
import com.example.userservice.exception.AppException;
import com.example.userservice.repository.AccountRepository;
import com.example.userservice.repository.UserRepository;
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

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse createUser(UserRequest userRequest) {
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

        User user = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.setStatus(EnumStatus.DELETED);
        user.setIsDeleted(true);
        user.getAccount().setIsDeleted(true);
        
        userRepository.save(user);
        accountRepository.save(user.getAccount());
        
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

    private UserResponse toUserResponse(User user) {
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
                .build();
    }
}

