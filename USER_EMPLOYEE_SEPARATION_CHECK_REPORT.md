# BÁO CÁO KIỂM TRA: PHÂN TÁCH USER (CUSTOMER) VÀ EMPLOYEE

## 📊 TÓM TẮT KIỂM TRA

### ✅ **ĐÃ ĐÚNG: Code sử dụng Employee cho các role employee**

| Service/Component | Entity Sử Dụng | Roles | Trạng Thái |
|-------------------|----------------|-------|------------|
| **EmployeeServiceImpl** | `Employee` | ADMIN, SELLER, BRANCH_MANAGER, DELIVERER, STAFF | ✅ **ĐÚNG** |
| **EmployeeRepository** | `Employee` | Tất cả employee roles | ✅ **ĐÚNG** |
| **EmployeeStore** | `Employee` | Tất cả employee roles | ✅ **ĐÚNG** |

### ❌ **VẪN SAI: Code vẫn sử dụng User cho Employee roles**

| Service/Component | Entity Sử Dụng | Roles | Trạng Thái | Vấn Đề |
|-------------------|----------------|-------|------------|--------|
| **UserServiceImpl.createUser()** | `User` | CUSTOMER, ADMIN (có thể) | ⚠️ **CẦN SỬA** | Có thể tạo ADMIN qua UserService |
| **UserServiceImpl.getAllEmployees()** | `User` | SELLER, BRANCH_MANAGER, DELIVERER, STAFF | ❌ **SAI** | Query từ UserRepository thay vì EmployeeRepository |
| **UserServiceImpl.getEmployeesByRole()** | `User` | Employee roles | ❌ **SAI** | Query từ UserRepository |
| **UserServiceImpl.getEmployeesWithPagination()** | `User` | Employee roles | ❌ **SAI** | Query từ UserRepository |
| **StaffServiceImpl** | `User` | STAFF | ❌ **SAI** | Tạo User thay vì Employee cho STAFF role |
| **Account Entity** | Chỉ có `User` | Tất cả roles | ❌ **SAI** | Thiếu quan hệ với Employee |

---

## 🔍 CHI TIẾT CÁC VẤN ĐỀ

### 1. ❌ **UserServiceImpl.createUser() - Có thể tạo ADMIN**

**Vị trí:** `UserServiceImpl.createUser()`

**Vấn đề:**
```java
// Line 44-48: Comment nói có thể tạo ADMIN qua UserService
// NOTE: For employee creation (SELLER, BRANCH_MANAGER, DELIVERER, STAFF),
// it is recommended to use EmployeeService.createEmployee() for better validation
// and to ensure ADMIN roles cannot be created through employee endpoints.
// This method is primarily used for ADMIN and CUSTOMER creation.
```

**Code thực tế:**
```67:76:user-service/src/main/java/com/example/userservice/service/UserServiceImpl.java
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
```

**Vấn đề:** Method này không validate role, có thể tạo ADMIN qua UserService → **SAI**

**Giải pháp:** Thêm validation để chỉ cho phép tạo CUSTOMER role

---

### 2. ❌ **UserServiceImpl.getAllEmployees() - Query từ UserRepository**

**Vị trí:** `UserServiceImpl.getAllEmployees()`

**Vấn đề:**
```355:361:user-service/src/main/java/com/example/userservice/service/UserServiceImpl.java
public List<UserResponse> getAllEmployees() {
    List<EnumRole> employeeRoles = Arrays.asList(EnumRole.SELLER, EnumRole.BRANCH_MANAGER, EnumRole.DELIVERER, EnumRole.STAFF);
    List<User> employees = userRepository.findEmployeesByRoles(employeeRoles);
    return employees.stream()
            .map(this::toUserResponse)
            .collect(Collectors.toList());
}
```

**Vấn đề:** Query từ `UserRepository` (bảng `users`) thay vì `EmployeeRepository` (bảng `employees`) → **SAI**

**Giải pháp:** Method này nên được xóa hoặc redirect sang `EmployeeService.getAllEmployees()`

---

### 3. ❌ **UserServiceImpl.getEmployeesByRole() - Query từ UserRepository**

**Vị trí:** `UserServiceImpl.getEmployeesByRole()`

**Vấn đề:**
```364:374:user-service/src/main/java/com/example/userservice/service/UserServiceImpl.java
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
```

**Vấn đề:** Query từ `UserRepository` → **SAI**

**Giải pháp:** Redirect sang `EmployeeService.getEmployeesByRole()`

---

### 4. ❌ **UserServiceImpl.getEmployeesWithPagination() - Query từ UserRepository**

**Vị trí:** `UserServiceImpl.getEmployeesWithPagination()`

**Vấn đề:**
```393:413:user-service/src/main/java/com/example/userservice/service/UserServiceImpl.java
public PageResponse<UserResponse> getEmployeesWithPagination(int page, int size) {
    log.info("Fetching employees with pagination - page: {}, size: {}", page, size);
    
    List<EnumRole> employeeRoles = Arrays.asList(EnumRole.SELLER, EnumRole.BRANCH_MANAGER, EnumRole.DELIVERER, EnumRole.STAFF);
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    Page<User> employeePage = userRepository.findEmployeesByRoles(employeeRoles, pageable);
    
    List<UserResponse> employeeResponses = employeePage.getContent().stream()
            .map(this::toUserResponse)
            .collect(Collectors.toList());
    // ...
}
```

**Vấn đề:** Query từ `UserRepository` → **SAI**

**Giải pháp:** Redirect sang `EmployeeService.getEmployeesWithPagination()`

---

### 5. ❌ **StaffServiceImpl - Tạo User cho STAFF role**

**Vị trí:** `StaffServiceImpl.createStaff()`

**Vấn đề:**
```67:81:user-service/src/main/java/com/example/userservice/service/StaffServiceImpl.java
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
```

**Vấn đề:** Tạo `User` entity cho STAFF role → **SAI**. Nên tạo `Employee` entity

**Giải pháp:** Refactor `StaffServiceImpl` để dùng `Employee` entity

---

### 6. ❌ **Account Entity - Thiếu quan hệ với Employee**

**Vị trí:** `Account.java`

**Vấn đề:**
```54:55:user-service/src/main/java/com/example/userservice/entity/Account.java
@OneToOne(mappedBy = "account", cascade = CascadeType.ALL)
private User user;
```

**Vấn đề:** Account chỉ có quan hệ với `User`, không có với `Employee` → **SAI**

**Giải pháp:** Thêm quan hệ với `Employee` (có thể dùng `@OneToOne` với `@JoinColumn` hoặc `@OneToMany`)

**Lưu ý:** Account có thể có quan hệ với User HOẶC Employee (tùy role), nên cần thiết kế lại

---

## ✅ **ĐÃ ĐÚNG: Code sử dụng User cho CUSTOMER**

| Service/Component | Entity | Role | Trạng Thái |
|-------------------|--------|------|------------|
| **AuthServiceImpl.register()** | `User` | CUSTOMER | ✅ **ĐÚNG** |
| **GoogleOAuth2Service.createGoogleUser()** | `User` | CUSTOMER | ✅ **ĐÚNG** |
| **UserServiceImpl** (các method khác) | `User` | CUSTOMER | ✅ **ĐÚNG** |

---

## 📝 QUÁ TRÌNH ĐĂNG NHẬP BẰNG GOOGLE

### **Flow Diagram:**

```
1. Client gửi Google Access Token
   ↓
2. AuthController.googleLogin() nhận request
   ↓
3. GoogleOAuth2Service.authenticateWithGoogle()
   ↓
4. Verify token với Google API
   ├─ GET https://www.googleapis.com/oauth2/v2/userinfo
   └─ Lấy thông tin: email, name, picture, googleId
   ↓
5. Kiểm tra Account đã tồn tại chưa
   ├─ Nếu CHƯA có → Tạo mới
   │   ├─ Tạo Account với role = CUSTOMER
   │   └─ Tạo User với thông tin từ Google
   └─ Nếu ĐÃ có → Kiểm tra status
       ├─ INACTIVE → Throw USER_BLOCKED
       └─ DELETED → Throw USER_DELETED
   ↓
6. Generate JWT Tokens
   ├─ Access Token
   └─ Refresh Token
   ↓
7. Lưu tokens vào Redis (TokenService)
   ↓
8. Trả về LoginResponse với tokens
```

### **Chi tiết code:**

#### **Step 1: Nhận request**
```116:126:user-service/src/main/java/com/example/userservice/controller/AuthController.java
@PostMapping("/google/login")
@ResponseStatus(HttpStatus.OK)
@Operation(summary = "Đăng nhập với Google", description = "API đăng nhập bằng Google OAuth2")
public ResponseEntity<ApiResponse<LoginResponse>> googleLogin(@RequestBody @Valid GoogleLoginRequest request) {
    return ResponseEntity.ok(ApiResponse.<LoginResponse>builder()
            .status(HttpStatus.OK.value())
            .message("Đăng nhập với Google thành công")
            .data(googleOAuth2Service.authenticateWithGoogle(request.getAccessToken()))
            .timestamp(LocalDateTime.now())
            .build());
}
```

#### **Step 2: Verify token với Google**
```96:122:user-service/src/main/java/com/example/userservice/service/GoogleOAuth2Service.java
private GoogleUserInfo getGoogleUserInfo(String accessToken) {
    try {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "https://www.googleapis.com/oauth2/v2/userinfo",
                HttpMethod.GET,
                entity,
                String.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            return GoogleUserInfo.builder()
                    .email(jsonNode.get("email").asText())
                    .name(jsonNode.has("name") ? jsonNode.get("name").asText() : null)
                    .picture(jsonNode.has("picture") ? jsonNode.get("picture").asText() : null)
                    .googleId(jsonNode.has("id") ? jsonNode.get("id").asText() : null)
                    .build();
        }
    } catch (Exception e) {
        log.error("Error fetching Google user info: {}", e.getMessage());
    }
    return null;
}
```

#### **Step 3: Tạo User mới (nếu chưa có)**
```124:153:user-service/src/main/java/com/example/userservice/service/GoogleOAuth2Service.java
private Account createGoogleUser(GoogleUserInfo userInfo) {
    // Create account
    Account account = Account.builder()
            .email(userInfo.getEmail())
            .password("GOOGLE_OAUTH") // Placeholder, not used for Google OAuth
            .role(EnumRole.CUSTOMER)  // ✅ LUÔN TẠO CUSTOMER
            .status(EnumStatus.ACTIVE)
            .enabled(true)
            .accountNonExpired(true)
            .accountNonLocked(true)
            .credentialsNonExpired(true)
            .build();

    Account savedAccount = accountRepository.save(account);

    // Create user
    User user = User.builder()
            .fullName(userInfo.getName() != null ? userInfo.getName() : userInfo.getEmail())
            .phone(null) // Google doesn't provide phone by default
            .status(EnumStatus.ACTIVE)
            .avatar(userInfo.getPicture())
            .point(0)
            .account(savedAccount)
            .build();

    userRepository.save(user);

    log.info("Created new user from Google OAuth: {}", userInfo.getEmail());
    return savedAccount;
}
```

#### **Step 4: Generate JWT và trả về**
```69:88:user-service/src/main/java/com/example/userservice/service/GoogleOAuth2Service.java
// Generate JWT tokens
// Note: Google OAuth creates CUSTOMER role, which doesn't have store relationships
List<String> storeIds = List.of();

Map<String, Object> claims = Map.of(
        "role", account.getRole(),
        "userId", account.getId(),
        "storeId", storeIds
);

String accessToken = jwtService.generateToken(claims, account.getEmail());
String refreshToken = jwtService.generateRefreshToken(claims, account.getEmail());

tokenService.saveToken(account.getEmail(), accessToken, jwtService.getJwtExpiration());
tokenService.saveRefreshToken(account.getEmail(), refreshToken, jwtService.getRefreshExpiration());

return LoginResponse.builder()
        .token(accessToken)
        .refreshToken(refreshToken)
        .build();
```

---

## ⚠️ **VẤN ĐỀ QUAN TRỌNG: Account Entity**

**Account entity hiện tại:**
```54:55:user-service/src/main/java/com/example/userservice/entity/Account.java
@OneToOne(mappedBy = "account", cascade = CascadeType.ALL)
private User user;
```

**Vấn đề:** 
- Account chỉ có quan hệ với `User`
- Employee cũng có `account_id` nhưng không có quan hệ ngược lại trong Account
- Điều này có thể gây vấn đề khi query Account → Employee

**Giải pháp đề xuất:**
1. Thêm quan hệ với Employee vào Account (có thể dùng `@OneToOne` với `@JoinColumn`)
2. Hoặc dùng `@OneToMany` nếu một Account có thể có cả User và Employee (không nên)
3. Hoặc tách Account thành CustomerAccount và EmployeeAccount (không khuyến khích)

**Cách tốt nhất:** Thêm quan hệ với Employee:
```java
@OneToOne(mappedBy = "account", cascade = CascadeType.ALL)
private User user;

@OneToOne(mappedBy = "account", cascade = CascadeType.ALL)
private Employee employee;
```

---

## 📋 TÓM TẮT CÁC VẤN ĐỀ CẦN SỬA

### **Mức độ NGHIÊM TRỌNG:**

1. **CRITICAL:** StaffServiceImpl tạo User cho STAFF → Nên tạo Employee
2. **CRITICAL:** Account entity thiếu quan hệ với Employee
3. **HIGH:** UserServiceImpl.createUser() có thể tạo ADMIN → Nên validate chỉ CUSTOMER
4. **MEDIUM:** UserServiceImpl.getAllEmployees() query từ UserRepository → Nên redirect sang EmployeeService
5. **MEDIUM:** UserServiceImpl.getEmployeesByRole() query từ UserRepository → Nên redirect
6. **MEDIUM:** UserServiceImpl.getEmployeesWithPagination() query từ UserRepository → Nên redirect

---

## ✅ **QUÁ TRÌNH ĐĂNG NHẬP BẰNG GOOGLE - ĐÃ ĐÚNG**

**GoogleOAuth2Service đã đúng:**
- ✅ Luôn tạo role = CUSTOMER
- ✅ Tạo User entity (không phải Employee)
- ✅ Lưu vào bảng `users`
- ✅ Không có store relationships (đúng cho CUSTOMER)

**Flow hoàn chỉnh:**
1. Verify Google token → Lấy thông tin user
2. Kiểm tra Account tồn tại
3. Nếu chưa có → Tạo Account (CUSTOMER) + User
4. Nếu đã có → Kiểm tra status
5. Generate JWT tokens
6. Lưu tokens vào Redis
7. Trả về tokens

---

**Ngày kiểm tra:** $(Get-Date)
**Trạng thái:** Cần sửa một số vấn đề để đảm bảo phân tách đúng

