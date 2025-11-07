# Báo Cáo Các Bảng Trong Database - Server 152.53.227.115

## 📊 Thông Tin Server

- **Server:** 152.53.227.115
- **Port:** 5435
- **Username:** postgres
- **Password:** 123456

## 🗄️ Databases Có Sẵn

1. **postgres** (maintenance database)
2. **user_db** (user service database)

## 📋 Các Bảng Trong Database `user_db`

Dựa trên truy vấn từ database trên server 152.53.227.115, các bảng sau đây được tìm thấy trong schema `public`:

| # | Tên Bảng | Schema | Owner | Mô Tả (Dự đoán) |
|---|----------|--------|-------|-----------------|
| 1 | **accounts** | public | postgres | Quản lý tài khoản người dùng |
| 2 | **address** | public | postgres | Địa chỉ người dùng |
| 3 | **blogs** | public | postgres | Blog posts/articles |
| 4 | **chat_messages** | public | postgres | Tin nhắn trong chat |
| 5 | **chat_participants** | public | postgres | Người tham gia chat |
| 6 | **chats** | public | postgres | Các phòng chat |
| 7 | **stores** | public | postgres | Cửa hàng |
| 8 | **user_stores** | public | postgres | Quan hệ giữa user và store |
| 9 | **users** | public | postgres | Thông tin người dùng |
| 10 | **wallet_transactions** | public | postgres | Giao dịch ví |
| 11 | **wallets** | public | postgres | Ví tiền của người dùng |

**Tổng số bảng:** 11 bảng

## 🔍 Chi Tiết Từng Bảng

### 1. accounts
- Quản lý tài khoản người dùng

### 2. address
- Lưu trữ địa chỉ của người dùng

### 3. blogs
- Quản lý blog posts/articles

### 4. chat_messages
- Lưu trữ tin nhắn trong các cuộc trò chuyện

### 5. chat_participants
- Quản lý người tham gia trong các chat rooms

### 6. chats
- Quản lý các phòng chat/trò chuyện

### 7. stores
- Quản lý thông tin cửa hàng

### 8. user_stores
- Bảng quan hệ nhiều-nhiều giữa users và stores

### 9. users
- Bảng chính lưu trữ thông tin người dùng

### 10. wallet_transactions
- Lịch sử giao dịch ví (nạp tiền, rút tiền, thanh toán)

### 11. wallets
- Quản lý ví tiền của người dùng

## 📝 Ghi Chú

- Tất cả các bảng đều thuộc schema `public`
- Tất cả các bảng đều được sở hữu bởi user `postgres`
- Database này là `user_db` - phục vụ cho User Service trong kiến trúc microservices

## 🔗 Các Databases Khác (Theo docker-compose.yml)

Nếu server có các databases khác, bạn có thể kết nối với các port tương ứng:

- **product_db** - Port 5436 → Database cho Product Service
- **order_db** - Port 5437 → Database cho Order Service  
- **inventory_db** - Port 5438 → Database cho Inventory Service
- **delivery_db** - Port 5441 → Database cho Delivery Service

## 🧪 Truy Vấn Thêm

Để xem chi tiết cấu trúc của một bảng cụ thể:

```sql
SELECT 
    column_name,
    data_type,
    character_maximum_length,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_schema = 'public'
    AND table_name = 'TEN_BANG'
ORDER BY ordinal_position;
```

---

**Báo cáo được tạo:** $(Get-Date)
**Database:** user_db
**Server:** 152.53.227.115:5435



