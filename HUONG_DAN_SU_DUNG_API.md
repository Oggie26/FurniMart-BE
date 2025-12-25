# Hướng Dẫn Sử Dụng API

## 1. Dashboard APIs

### 1.1. Admin Dashboard
**Endpoint:** `GET /api/dashboard/admin`

**Mô tả:** Lấy dữ liệu dashboard cho Admin (tổng doanh thu, số cửa hàng, người dùng, sản phẩm bán chạy, biểu đồ doanh thu)

**Quyền:** ADMIN

**Ví dụ:**
```bash
GET /api/dashboard/admin
Authorization: Bearer {token}
```

**Response:**
```json
{
  "status": 200,
  "message": "Admin dashboard data retrieved successfully",
  "data": {
    "totalRevenue": 1000000,
    "activeStores": 10,
    "totalUsers": 500,
    "topProducts": [...],
    "revenueChart": [...]
  }
}
```

---

### 1.2. Manager Dashboard
**Endpoint:** `GET /api/dashboard/manager?storeId={storeId}`

**Mô tả:** Lấy dữ liệu dashboard cho Manager (doanh thu chi nhánh, đơn hàng đang chờ/giao, sản phẩm sắp hết, đơn hàng cho shipper)

**Quyền:** BRANCH_MANAGER

**Tham số:**
- `storeId` (required): ID của cửa hàng

**Ví dụ:**
```bash
GET /api/dashboard/manager?storeId=store123
Authorization: Bearer {token}
```

---

### 1.3. Staff Dashboard
**Endpoint:** `GET /api/dashboard/staff`

**Mô tả:** Lấy dữ liệu dashboard cho Staff (doanh thu cá nhân, số đơn đã tạo, số đơn đang chờ của cửa hàng)

**Quyền:** STAFF

**Ví dụ:**
```bash
GET /api/dashboard/staff
Authorization: Bearer {token}
```

---

## 2. Chat APIs - Kết Nối Staff

### 2.1. Yêu Cầu Kết Nối Staff
**Endpoint:** `POST /api/chats/{chatId}/request-staff`

**Mô tả:** Customer yêu cầu kết nối với staff để được hỗ trợ

**Quyền:** CUSTOMER

**Tham số:**
- `chatId` (path): ID của chat

**Ví dụ:**
```bash
POST /api/chats/chat123/request-staff
Authorization: Bearer {token}
```

**Response:**
```json
{
  "status": 200,
  "message": "Staff connection requested",
  "data": {
    "id": "chat123",
    "mode": "WAITING_STAFF",
    "participants": [...]
  }
}
```

**Lưu ý:** 
- Chat sẽ chuyển từ mode `AI` sang `WAITING_STAFF`
- Staff sẽ nhận được thông báo qua WebSocket về chat đang chờ

---

### 2.2. Chấp Nhận Kết Nối Staff
**Endpoint:** `POST /api/chats/{chatId}/accept-staff`

**Mô tả:** Staff chấp nhận kết nối với customer

**Quyền:** STAFF, ADMIN

**Tham số:**
- `chatId` (path): ID của chat

**Ví dụ:**
```bash
POST /api/chats/chat123/accept-staff
Authorization: Bearer {token}
```

**Response:**
```json
{
  "status": 200,
  "message": "Staff connection accepted",
  "data": {
    "id": "chat123",
    "mode": "STAFF_CONNECTED",
    "staffId": "staff456",
    "participants": [...]
  }
}
```

**Lưu ý:**
- Chat sẽ chuyển từ mode `WAITING_STAFF` sang `STAFF_CONNECTED`
- Staff sẽ được thêm vào chat như một participant
- Customer sẽ nhận được thông báo qua WebSocket

---

### 2.3. Kết Thúc Chat Với Staff
**Endpoint:** `POST /api/chats/{chatId}/end-staff-chat`

**Mô tả:** Kết thúc chat với staff, chuyển về AI mode

**Quyền:** CUSTOMER, STAFF, ADMIN

**Tham số:**
- `chatId` (path): ID của chat

**Ví dụ:**
```bash
POST /api/chats/chat123/end-staff-chat
Authorization: Bearer {token}
```

**Response:**
```json
{
  "status": 200,
  "message": "Staff chat ended",
  "data": {
    "id": "chat123",
    "mode": "AI",
    "participants": [...]
  }
}
```

**Lưu ý:**
- Chat sẽ chuyển từ `STAFF_CONNECTED` về `AI`
- Staff có thể bị xóa khỏi participants (tùy logic)

---

## 3. Chat APIs - Quản Lý Chat

### 3.1. Lấy Danh Sách Chat Mới Nhất
**Endpoint:** `GET /api/chats/latest`

**Mô tả:** Lấy 10 chat mới nhất, ưu tiên chat chưa đọc trước, sau đó mới đến chat đã đọc. Bao gồm tên customer, avatar, và tin nhắn cuối.

**Quyền:** ADMIN, CUSTOMER, STAFF

**Ví dụ:**
```bash
GET /api/chats/latest
Authorization: Bearer {token}
```

**Response:**
```json
{
  "status": 200,
  "message": "Latest chats retrieved successfully",
  "data": [
    {
      "id": "chat1",
      "customerName": "Nguyễn Văn A",
      "customerAvatar": "https://...",
      "latestMessage": "Xin chào",
      "unreadCount": 3,
      "mode": "AI"
    },
    ...
  ]
}
```

---

### 3.2. Tạo Chat Mới
**Endpoint:** `POST /api/chats`

**Mô tả:** Tạo một chat mới

**Quyền:** ADMIN, CUSTOMER, STAFF

**Body:**
```json
{
  "title": "Hỗ trợ đơn hàng",
  "mode": "AI",
  "participantIds": ["user1", "user2"]
}
```

**Ví dụ:**
```bash
POST /api/chats
Authorization: Bearer {token}
Content-Type: application/json

{
  "title": "Hỗ trợ đơn hàng",
  "mode": "AI"
}
```

---

### 3.3. Lấy Chat Theo ID
**Endpoint:** `GET /api/chats/{id}`

**Mô tả:** Lấy thông tin chi tiết của một chat

**Quyền:** ADMIN, CUSTOMER, STAFF

**Ví dụ:**
```bash
GET /api/chats/chat123
Authorization: Bearer {token}
```

---

### 3.4. Lấy Danh Sách Chat Của User
**Endpoint:** `GET /api/chats`

**Mô tả:** Lấy tất cả chat của user hiện tại

**Quyền:** ADMIN, CUSTOMER, STAFF

**Ví dụ:**
```bash
GET /api/chats
Authorization: Bearer {token}
```

---

### 3.5. Lấy Chat Có Phân Trang
**Endpoint:** `GET /api/chats/paginated?page=0&size=10`

**Mô tả:** Lấy danh sách chat có phân trang

**Quyền:** ADMIN, CUSTOMER, STAFF

**Tham số:**
- `page` (optional, default: 0): Số trang
- `size` (optional, default: 10): Số lượng mỗi trang

**Ví dụ:**
```bash
GET /api/chats/paginated?page=0&size=20
Authorization: Bearer {token}
```

---

### 3.6. Cập Nhật Chat
**Endpoint:** `PUT /api/chats/{id}`

**Mô tả:** Cập nhật thông tin chat

**Quyền:** ADMIN, CUSTOMER, STAFF

**Body:**
```json
{
  "title": "Tiêu đề mới",
  "mode": "AI"
}
```

**Ví dụ:**
```bash
PUT /api/chats/chat123
Authorization: Bearer {token}
Content-Type: application/json

{
  "title": "Tiêu đề mới"
}
```

---

### 3.7. Xóa Chat
**Endpoint:** `DELETE /api/chats/{id}`

**Mô tả:** Xóa một chat

**Quyền:** ADMIN, CUSTOMER, STAFF

**Ví dụ:**
```bash
DELETE /api/chats/chat123
Authorization: Bearer {token}
```

---

### 3.8. Thêm Participant
**Endpoint:** `POST /api/chats/{id}/participants/{userId}`

**Mô tả:** Thêm người tham gia vào chat

**Quyền:** ADMIN, CUSTOMER, STAFF

**Ví dụ:**
```bash
POST /api/chats/chat123/participants/user456
Authorization: Bearer {token}
```

---

### 3.9. Xóa Participant
**Endpoint:** `DELETE /api/chats/{id}/participants/{userId}`

**Mô tả:** Xóa người tham gia khỏi chat

**Quyền:** ADMIN, CUSTOMER, STAFF

**Ví dụ:**
```bash
DELETE /api/chats/chat123/participants/user456
Authorization: Bearer {token}
```

---

### 3.10. Cập Nhật Vai Trò Participant
**Endpoint:** `PUT /api/chats/{id}/participants/{userId}/role?role=ADMIN`

**Mô tả:** Cập nhật vai trò của participant trong chat

**Quyền:** ADMIN, CUSTOMER, STAFF

**Tham số:**
- `role` (query): Vai trò mới (ADMIN, MODERATOR, MEMBER)

**Ví dụ:**
```bash
PUT /api/chats/chat123/participants/user456/role?role=MODERATOR
Authorization: Bearer {token}
```

---

### 3.11. Tìm Kiếm Chat
**Endpoint:** `GET /api/chats/search?searchTerm=keyword`

**Mô tả:** Tìm kiếm chat theo từ khóa

**Quyền:** ADMIN, CUSTOMER, STAFF

**Tham số:**
- `searchTerm` (required): Từ khóa tìm kiếm

**Ví dụ:**
```bash
GET /api/chats/search?searchTerm=đơn hàng
Authorization: Bearer {token}
```

---

### 3.12. Lấy Hoặc Tạo Chat Riêng
**Endpoint:** `POST /api/chats/private/{userId}`

**Mô tả:** Lấy hoặc tạo chat riêng với một user

**Quyền:** ADMIN, CUSTOMER, STAFF

**Ví dụ:**
```bash
POST /api/chats/private/user456
Authorization: Bearer {token}
```

---

### 3.13. Đánh Dấu Chat Đã Đọc
**Endpoint:** `POST /api/chats/{id}/read`

**Mô tả:** Đánh dấu chat là đã đọc

**Quyền:** ADMIN, CUSTOMER, STAFF

**Ví dụ:**
```bash
POST /api/chats/chat123/read
Authorization: Bearer {token}
```

---

### 3.14. Tắt/Bật Thông Báo Chat
**Endpoint:** `PATCH /api/chats/{id}/mute?muted=true`

**Mô tả:** Tắt hoặc bật thông báo cho chat

**Quyền:** ADMIN, CUSTOMER, STAFF

**Tham số:**
- `muted` (required): true để tắt, false để bật

**Ví dụ:**
```bash
PATCH /api/chats/chat123/mute?muted=true
Authorization: Bearer {token}
```

---

### 3.15. Ghim/Bỏ Ghim Chat
**Endpoint:** `PATCH /api/chats/{id}/pin?pinned=true`

**Mô tả:** Ghim hoặc bỏ ghim chat

**Quyền:** ADMIN, CUSTOMER, STAFF

**Tham số:**
- `pinned` (required): true để ghim, false để bỏ ghim

**Ví dụ:**
```bash
PATCH /api/chats/chat123/pin?pinned=true
Authorization: Bearer {token}
```

---

## 4. WebSocket - Real-time Chat

### 4.1. Kết Nối WebSocket
**Endpoint:** `ws://localhost:8080/ws/chat?userId={userId}`

**Mô tả:** Kết nối WebSocket để nhận tin nhắn real-time

**Tham số:**
- `userId` (query): ID của user

**Ví dụ (JavaScript):**
```javascript
const ws = new WebSocket('ws://localhost:8080/ws/chat?userId=user123');

ws.onopen = () => {
  console.log('Connected to WebSocket');
};

ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  console.log('Received:', message);
};
```

---

### 4.2. Gửi Tin Nhắn
**Message Type:** `MESSAGE`

**Format:**
```json
{
  "type": "MESSAGE",
  "chatId": "chat123",
  "content": "Xin chào",
  "timestamp": 1234567890
}
```

**Ví dụ:**
```javascript
ws.send(JSON.stringify({
  type: "MESSAGE",
  chatId: "chat123",
  content: "Xin chào",
  timestamp: Date.now()
}));
```

---

### 4.3. Gửi Typing Indicator
**Message Type:** `TYPING`

**Format:**
```json
{
  "type": "TYPING",
  "chatId": "chat123",
  "isTyping": true
}
```

---

### 4.4. Tham Gia Chat
**Message Type:** `JOIN_CHAT`

**Format:**
```json
{
  "type": "JOIN_CHAT",
  "chatId": "chat123"
}
```

---

### 4.5. Rời Chat
**Message Type:** `LEAVE_CHAT`

**Format:**
```json
{
  "type": "LEAVE_CHAT",
  "chatId": "chat123"
}
```

---

## 5. Luồng Hoạt Động Chat Với Staff

### 5.1. Customer Yêu Cầu Staff
1. Customer đang chat với AI
2. Customer gọi API: `POST /api/chats/{chatId}/request-staff`
3. Chat chuyển sang mode `WAITING_STAFF`
4. Staff nhận thông báo qua WebSocket về chat đang chờ

### 5.2. Staff Chấp Nhận
1. Staff xem danh sách chat đang chờ: `GET /api/chats/latest`
2. Staff chọn chat và gọi API: `POST /api/chats/{chatId}/accept-staff`
3. Chat chuyển sang mode `STAFF_CONNECTED`
4. Staff được thêm vào chat
5. Customer nhận thông báo qua WebSocket

### 5.3. Kết Thúc Chat
1. Customer hoặc Staff gọi API: `POST /api/chats/{chatId}/end-staff-chat`
2. Chat chuyển về mode `AI`
3. Staff có thể bị xóa khỏi participants

---

## 6. Lưu Ý Quan Trọng

1. **Authentication:** Tất cả API đều yêu cầu Bearer token trong header
2. **Quyền Truy Cập:** Mỗi API có quyền riêng, cần đảm bảo user có đúng role
3. **WebSocket:** Cần kết nối WebSocket để nhận thông báo real-time
4. **Chat Modes:**
   - `AI`: Chat với AI
   - `WAITING_STAFF`: Đang chờ staff kết nối
   - `STAFF_CONNECTED`: Đã kết nối với staff

---

## 7. Ví Dụ Hoàn Chỉnh

### Tạo Chat và Yêu Cầu Staff
```bash
# 1. Tạo chat mới
POST /api/chats
{
  "title": "Hỗ trợ đơn hàng #12345",
  "mode": "AI"
}

# Response: { "data": { "id": "chat123", ... } }

# 2. Yêu cầu staff
POST /api/chats/chat123/request-staff

# 3. Staff chấp nhận
POST /api/chats/chat123/accept-staff

# 4. Gửi tin nhắn qua WebSocket
{
  "type": "MESSAGE",
  "chatId": "chat123",
  "content": "Tôi cần hỗ trợ về đơn hàng"
}

# 5. Kết thúc chat
POST /api/chats/chat123/end-staff-chat
```

---

**Tác giả:** ThanhPhong383  
**Ngày tạo:** 24/12/2025

