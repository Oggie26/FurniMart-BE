# Hướng Dẫn Rebuild Docker Images

## ⚠️ Vấn Đề

Docker Desktop chưa được khởi động trên máy local.

## ✅ Giải Pháp

### Cách 1: Khởi Động Docker Desktop (Khuyến Nghị)

1. **Mở Docker Desktop**
   - Tìm "Docker Desktop" trong Start Menu
   - Hoặc click vào icon Docker Desktop trong system tray

2. **Chờ Docker Desktop khởi động hoàn toàn**
   - Đợi đến khi icon Docker Desktop chuyển sang màu xanh
   - Kiểm tra bằng lệnh: `docker ps`

3. **Rebuild Docker images:**
```bash
# Rebuild user-service
docker build -t user-service ./user-service

# Rebuild order-service
docker build -t order-service ./order-service

# Rebuild delivery-service
docker build -t delivery-service ./delivery-service
```

### Cách 2: Sử Dụng Docker Compose

Nếu bạn có `docker-compose.yml`:

```bash
# Rebuild và restart tất cả services
docker-compose up -d --build

# Hoặc rebuild từng service
docker-compose build user-service
docker-compose build order-service
docker-compose build delivery-service

# Sau đó restart
docker-compose restart user-service
docker-compose restart order-service
docker-compose restart delivery-service
```

### Cách 3: Rebuild Trên Server

Nếu bạn có quyền truy cập server:

```bash
# SSH vào server
ssh user@server

# Pull code mới nhất
cd /path/to/FurniMart-BE
git pull origin main

# Rebuild và restart services
docker-compose down
docker-compose build --no-cache user-service order-service delivery-service
docker-compose up -d
```

### Cách 4: Sử Dụng CI/CD Pipeline

Nếu bạn có CI/CD pipeline (GitHub Actions, GitLab CI, etc.):

1. Push code lên git (đã làm ✅)
2. Pipeline sẽ tự động:
   - Build Docker images
   - Push lên Docker registry
   - Deploy lên server

## 📋 Tóm Tắt Các Thay Đổi Đã Commit

Các thay đổi đã được commit và push lên git:

1. **user-service**
   - Thêm endpoint `GET /api/employees/email/{email}`
   - Files: `EmployeeController.java`, `EmployeeServiceImpl.java`, `EmployeeService.java`

2. **order-service**
   - Sửa lỗi 500 trong `searchOrder()`
   - File: `OrderServiceImpl.java`

3. **delivery-service**
   - Cải thiện error handling
   - Thêm 4 error codes mới
   - Sửa SecurityConfig pattern
   - Files: `DeliveryServiceImpl.java`, `ErrorCode.java`, `SecurityConfig.java`, `InventoryClient.java`

## ✅ Đã Hoàn Thành

- ✅ Rebuild Maven projects (user-service, order-service, delivery-service)
- ✅ Commit các thay đổi lên git
- ✅ Push lên remote repository

## ⏳ Cần Làm

- ⏳ Rebuild Docker images (cần Docker Desktop chạy)
- ⏳ Restart containers để áp dụng thay đổi

## 🚀 Sau Khi Rebuild Docker Images

Sau khi rebuild Docker images thành công, bạn cần restart các containers:

```bash
# Restart từng service
docker-compose restart user-service
docker-compose restart order-service
docker-compose restart delivery-service

# Hoặc restart tất cả
docker-compose restart
```

Sau đó test lại các chức năng:
- ✅ STAFF functions
- ✅ DELIVERY functions
- ✅ Assign order delivery

