# Hướng Dẫn Các Thao Tác Với Wallet

## Tổng Quan

Wallet (Ví điện tử) là hệ thống quản lý số dư và giao dịch cho người dùng trong hệ thống FurniMart. Mỗi user (CUSTOMER) sẽ tự động có một wallet khi đăng ký tài khoản.

### Cấu Trúc Wallet

- **ID**: UUID tự động tạo
- **Code**: Mã ví duy nhất (unique, max 50 ký tự)
- **Balance**: Số dư (BigDecimal, precision 15, scale 2)
- **Status**: Trạng thái ví (ACTIVE, INACTIVE, SUSPENDED)
- **UserId**: ID của user sở hữu ví
- **Transactions**: Danh sách các giao dịch

### Wallet Status

- `ACTIVE`: Ví đang hoạt động bình thường
- `INACTIVE`: Ví bị vô hiệu hóa
- `SUSPENDED`: Ví bị tạm khóa

### Transaction Types

- `DEPOSIT`: Nạp tiền vào ví
- `WITHDRAWAL`: Rút tiền từ ví
- `TRANSFER_IN`: Nhận tiền từ chuyển khoản
- `TRANSFER_OUT`: Chuyển tiền đi
- `PAYMENT`: Thanh toán
- `REFUND`: Hoàn tiền
- `BONUS`: Thưởng
- `PENALTY`: Phạt

---

## API Endpoints

**Base URL**: `http://152.53.227.115:8086/api/wallets`

**Authentication**: Tất cả API đều yêu cầu Bearer Token (trừ khi có ghi chú khác)

---

## 1. Wallet CRUD Operations

### 1.1. Tạo Wallet

**Endpoint**: `POST /api/wallets`

**Mô tả**: Tạo wallet mới. Lưu ý: Wallet tự động được tạo cho CUSTOMER khi đăng ký. API này chủ yếu dành cho ADMIN/STAFF tạo wallet thủ công.

**Request Body**:
```json
{
  "code": "WALLET-001",
  "balance": 0.00,
  "status": "ACTIVE",
  "userId": "user-id-here"
}
```

**Response** (201 Created):
```json
{
  "status": 201,
  "message": "Wallet created successfully",
  "data": {
    "id": "wallet-id",
    "code": "WALLET-001",
    "balance": 0.00,
    "status": "ACTIVE",
    "userId": "user-id-here",
    "userFullName": "User Full Name",
    "createdAt": "2025-11-15T10:00:00",
    "updatedAt": "2025-11-15T10:00:00"
  }
}
```

**Lưu ý**:
- Mỗi user chỉ có thể có 1 wallet
- Code phải unique
- Nếu user đã có wallet bị soft-delete, hệ thống sẽ restore wallet đó thay vì tạo mới

---

### 1.2. Lấy Wallet Theo ID

**Endpoint**: `GET /api/wallets/{id}`

**Mô tả**: Lấy thông tin wallet theo ID

**Response** (200 OK):
```json
{
  "status": 200,
  "message": "Wallet retrieved successfully",
  "data": {
    "id": "wallet-id",
    "code": "WALLET-001",
    "balance": 1000.50,
    "status": "ACTIVE",
    "userId": "user-id",
    "userFullName": "User Name",
    "createdAt": "2025-11-15T10:00:00",
    "updatedAt": "2025-11-15T10:00:00"
  }
}
```

---

### 1.3. Lấy Wallet Theo User ID

**Endpoint**: `GET /api/wallets/user/{userId}`

**Mô tả**: Lấy wallet của một user cụ thể

**Response**: Tương tự như 1.2

---

### 1.4. Lấy Wallet Theo Code

**Endpoint**: `GET /api/wallets/code/{code}`

**Mô tả**: Lấy wallet theo mã code

**Response**: Tương tự như 1.2

---

### 1.5. Lấy Tất Cả Wallets

**Endpoint**: `GET /api/wallets`

**Mô tả**: Lấy danh sách tất cả wallets (chỉ wallets không bị xóa)

**Response** (200 OK):
```json
{
  "status": 200,
  "message": "Wallets retrieved successfully",
  "data": [
    {
      "id": "wallet-id-1",
      "code": "WALLET-001",
      "balance": 1000.50,
      "status": "ACTIVE",
      "userId": "user-id-1",
      "userFullName": "User 1",
      "createdAt": "2025-11-15T10:00:00",
      "updatedAt": "2025-11-15T10:00:00"
    },
    {
      "id": "wallet-id-2",
      "code": "WALLET-002",
      "balance": 500.00,
      "status": "ACTIVE",
      "userId": "user-id-2",
      "userFullName": "User 2",
      "createdAt": "2025-11-15T10:00:00",
      "updatedAt": "2025-11-15T10:00:00"
    }
  ]
}
```

---

### 1.6. Cập Nhật Wallet

**Endpoint**: `PUT /api/wallets/{id}`

**Mô tả**: Cập nhật thông tin wallet

**Request Body**:
```json
{
  "code": "WALLET-001-UPDATED",
  "balance": 1500.00,
  "status": "ACTIVE",
  "userId": "user-id-here"
}
```

**Response** (200 OK):
```json
{
  "status": 200,
  "message": "Wallet updated successfully",
  "data": {
    "id": "wallet-id",
    "code": "WALLET-001-UPDATED",
    "balance": 1500.00,
    "status": "ACTIVE",
    "userId": "user-id",
    "userFullName": "User Name",
    "createdAt": "2025-11-15T10:00:00",
    "updatedAt": "2025-11-15T11:00:00"
  }
}
```

---

### 1.7. Xóa Wallet

**Endpoint**: `DELETE /api/wallets/{id}`

**Mô tả**: Xóa wallet (soft delete)

**Response** (204 No Content):
```json
{
  "status": 204,
  "message": "Wallet deleted successfully",
  "data": null
}
```

---

## 2. Wallet Transactions

### 2.1. Tạo Transaction

**Endpoint**: `POST /api/wallets/transactions`

**Mô tả**: Tạo giao dịch wallet (nạp tiền, rút tiền, v.v.)

**Request Body**:
```json
{
  "code": "TXN-001",
  "amount": 100.00,
  "type": "DEPOSIT",
  "description": "Nạp tiền vào ví",
  "referenceId": "REF-12345",
  "walletId": "wallet-id-here"
}
```

**Response** (201 Created):
```json
{
  "status": 201,
  "message": "Transaction created successfully",
  "data": {
    "id": "transaction-id",
    "code": "TXN-001",
    "amount": 100.00,
    "type": "DEPOSIT",
    "description": "Nạp tiền vào ví",
    "referenceId": "REF-12345",
    "walletId": "wallet-id",
    "balanceBefore": 1000.00,
    "balanceAfter": 1100.00,
    "status": "COMPLETED",
    "createdAt": "2025-11-15T10:00:00"
  }
}
```

**Lưu ý**:
- Khi tạo transaction, số dư wallet sẽ tự động được cập nhật
- Transaction type `DEPOSIT`, `TRANSFER_IN`, `REFUND`, `BONUS` sẽ tăng số dư
- Transaction type `WITHDRAWAL`, `TRANSFER_OUT`, `PAYMENT`, `PENALTY` sẽ giảm số dư
- Wallet phải ở trạng thái `ACTIVE` mới có thể tạo transaction

---

### 2.2. Lấy Transaction Theo ID

**Endpoint**: `GET /api/wallets/transactions/{id}`

**Mô tả**: Lấy thông tin transaction theo ID

**Response** (200 OK):
```json
{
  "status": 200,
  "message": "Transaction retrieved successfully",
  "data": {
    "id": "transaction-id",
    "code": "TXN-001",
    "amount": 100.00,
    "type": "DEPOSIT",
    "description": "Nạp tiền vào ví",
    "referenceId": "REF-12345",
    "walletId": "wallet-id",
    "balanceBefore": 1000.00,
    "balanceAfter": 1100.00,
    "status": "COMPLETED",
    "createdAt": "2025-11-15T10:00:00"
  }
}
```

---

### 2.3. Lấy Transactions Theo Wallet ID

**Endpoint**: `GET /api/wallets/{walletId}/transactions`

**Mô tả**: Lấy tất cả transactions của một wallet

**Response** (200 OK):
```json
{
  "status": 200,
  "message": "Transactions retrieved successfully",
  "data": [
    {
      "id": "transaction-id-1",
      "code": "TXN-001",
      "amount": 100.00,
      "type": "DEPOSIT",
      "description": "Nạp tiền",
      "balanceBefore": 1000.00,
      "balanceAfter": 1100.00,
      "status": "COMPLETED",
      "createdAt": "2025-11-15T10:00:00"
    },
    {
      "id": "transaction-id-2",
      "code": "TXN-002",
      "amount": 50.00,
      "type": "WITHDRAWAL",
      "description": "Rút tiền",
      "balanceBefore": 1100.00,
      "balanceAfter": 1050.00,
      "status": "COMPLETED",
      "createdAt": "2025-11-15T09:00:00"
    }
  ]
}
```

---

### 2.4. Lấy Transactions Với Pagination

**Endpoint**: `GET /api/wallets/{walletId}/transactions/paged?page=0&size=10`

**Mô tả**: Lấy transactions với phân trang

**Query Parameters**:
- `page`: Số trang (default: 0)
- `size`: Số lượng items mỗi trang (default: 10)

**Response** (200 OK):
```json
{
  "status": 200,
  "message": "Transactions retrieved successfully",
  "data": {
    "content": [
      {
        "id": "transaction-id-1",
        "code": "TXN-001",
        "amount": 100.00,
        "type": "DEPOSIT",
        "balanceBefore": 1000.00,
        "balanceAfter": 1100.00,
        "status": "COMPLETED",
        "createdAt": "2025-11-15T10:00:00"
      }
    ],
    "number": 0,
    "size": 10,
    "totalElements": 25,
    "totalPages": 3,
    "first": true,
    "last": false
  }
}
```

---

## 3. Wallet Operations (Convenience Methods)

### 3.1. Nạp Tiền (Deposit)

**Endpoint**: `POST /api/wallets/{walletId}/deposit?amount=100.00&description=Nạp tiền&referenceId=REF-123`

**Mô tả**: Nạp tiền vào wallet (tạo transaction DEPOSIT tự động)

**Query Parameters**:
- `amount` (required): Số tiền nạp
- `description` (optional): Mô tả
- `referenceId` (optional): Mã tham chiếu

**Response** (200 OK):
```json
{
  "status": 200,
  "message": "Deposit completed successfully",
  "data": {
    "id": "wallet-id",
    "code": "WALLET-001",
    "balance": 1100.00,
    "status": "ACTIVE",
    "userId": "user-id",
    "userFullName": "User Name",
    "createdAt": "2025-11-15T10:00:00",
    "updatedAt": "2025-11-15T11:00:00"
  }
}
```

---

### 3.2. Rút Tiền (Withdraw)

**Endpoint**: `POST /api/wallets/{walletId}/withdraw?amount=50.00&description=Rút tiền&referenceId=REF-456`

**Mô tả**: Rút tiền từ wallet (tạo transaction WITHDRAWAL tự động)

**Query Parameters**:
- `amount` (required): Số tiền rút
- `description` (optional): Mô tả
- `referenceId` (optional): Mã tham chiếu

**Response**: Tương tự như Deposit

**Lưu ý**: Số dư phải đủ để rút, nếu không sẽ báo lỗi

---

### 3.3. Chuyển Khoản (Transfer)

**Endpoint**: `POST /api/wallets/transfer?fromWalletId=wallet-1&toWalletId=wallet-2&amount=100.00&description=Chuyển khoản&referenceId=REF-789`

**Mô tả**: Chuyển tiền giữa 2 wallets (tạo 2 transactions: TRANSFER_OUT và TRANSFER_IN)

**Query Parameters**:
- `fromWalletId` (required): ID wallet nguồn
- `toWalletId` (required): ID wallet đích
- `amount` (required): Số tiền chuyển
- `description` (optional): Mô tả
- `referenceId` (optional): Mã tham chiếu

**Response** (200 OK):
```json
{
  "status": 200,
  "message": "Transfer completed successfully",
  "data": {
    "id": "wallet-id-1",
    "code": "WALLET-001",
    "balance": 900.00,
    "status": "ACTIVE",
    "userId": "user-id-1",
    "userFullName": "User 1",
    "createdAt": "2025-11-15T10:00:00",
    "updatedAt": "2025-11-15T11:00:00"
  }
}
```

**Lưu ý**: 
- Số dư wallet nguồn phải đủ
- Tạo 2 transactions: TRANSFER_OUT cho wallet nguồn và TRANSFER_IN cho wallet đích

---

### 3.4. Lấy Số Dư (Get Balance)

**Endpoint**: `GET /api/wallets/{walletId}/balance`

**Mô tả**: Lấy số dư hiện tại của wallet

**Response** (200 OK):
```json
{
  "status": 200,
  "message": "Balance retrieved successfully",
  "data": 1000.50
}
```

---

## 4. Error Codes

- `WALLET_NOT_FOUND`: Wallet không tồn tại
- `WALLET_CODE_EXISTS`: Code wallet đã tồn tại
- `USER_ALREADY_HAS_WALLET`: User đã có wallet
- `WALLET_NOT_ACTIVE`: Wallet không ở trạng thái ACTIVE
- `INSUFFICIENT_BALANCE`: Số dư không đủ
- `INVALID_AMOUNT`: Số tiền không hợp lệ

---

## 5. Best Practices

1. **Tự động tạo wallet**: Không cần tạo wallet thủ công cho CUSTOMER, hệ thống tự động tạo khi đăng ký
2. **Sử dụng convenience methods**: Dùng `/deposit`, `/withdraw`, `/transfer` thay vì tạo transaction thủ công
3. **Kiểm tra số dư**: Luôn kiểm tra số dư trước khi thực hiện rút tiền hoặc thanh toán
4. **Reference ID**: Sử dụng `referenceId` để liên kết với order, payment, v.v.
5. **Transaction code**: Tạo code transaction unique để dễ dàng tracking

---

## 6. Ví Dụ Sử Dụng

### Scenario 1: Customer nạp tiền vào ví

```bash
# 1. Lấy wallet của customer
GET /api/wallets/user/{customerUserId}

# 2. Nạp tiền
POST /api/wallets/{walletId}/deposit?amount=500.00&description=Nạp tiền từ thẻ&referenceId=PAYMENT-123
```

### Scenario 2: Customer thanh toán đơn hàng

```bash
# 1. Kiểm tra số dư
GET /api/wallets/{walletId}/balance

# 2. Tạo transaction PAYMENT
POST /api/wallets/transactions
{
  "code": "TXN-PAY-001",
  "amount": 250.00,
  "type": "PAYMENT",
  "description": "Thanh toán đơn hàng #ORD-001",
  "referenceId": "ORD-001",
  "walletId": "wallet-id"
}
```

### Scenario 3: Hoàn tiền cho customer

```bash
# Tạo transaction REFUND
POST /api/wallets/transactions
{
  "code": "TXN-REFUND-001",
  "amount": 250.00,
  "type": "REFUND",
  "description": "Hoàn tiền đơn hàng #ORD-001",
  "referenceId": "ORD-001",
  "walletId": "wallet-id"
}
```

---

## 7. Database Schema

### wallets table
- `id` (UUID, PK)
- `code` (VARCHAR(50), UNIQUE, NOT NULL)
- `balance` (DECIMAL(15,2), NOT NULL, DEFAULT 0)
- `status` (VARCHAR, NOT NULL)
- `user_id` (UUID, UNIQUE, NOT NULL, FK to users)
- `is_deleted` (BOOLEAN, DEFAULT false)
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)

### wallet_transactions table
- `id` (UUID, PK)
- `code` (VARCHAR(50), UNIQUE, NOT NULL)
- `amount` (DECIMAL(15,2), NOT NULL)
- `type` (VARCHAR, NOT NULL)
- `description` (TEXT)
- `reference_id` (VARCHAR)
- `wallet_id` (UUID, FK to wallets)
- `balance_before` (DECIMAL(15,2))
- `balance_after` (DECIMAL(15,2))
- `status` (VARCHAR, NOT NULL)
- `is_deleted` (BOOLEAN, DEFAULT false)
- `created_at` (TIMESTAMP)

