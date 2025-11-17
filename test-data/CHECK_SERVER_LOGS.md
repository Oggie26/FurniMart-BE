# Hướng dẫn kiểm tra Log trên Server

## Thông tin Server

- **IP:** `152.53.227.115`
- **User:** `nam`
- **SSH Command:** `ssh nam@152.53.227.115`
- **Project Path:** `~/FurniMart-BE`

---

## Bước 1: Kết nối SSH vào Server

### Windows (PowerShell)

```powershell
ssh nam@152.53.227.115
```

**Lưu ý:** Lần đầu kết nối sẽ hỏi xác nhận, gõ `yes` và nhấn Enter.

### Windows (CMD)

```cmd
ssh nam@152.53.227.115
```

### Linux/Mac

```bash
ssh nam@152.53.227.115
```

---

## Bước 2: Di chuyển đến thư mục dự án

```bash
cd ~/FurniMart-BE
# hoặc
cd /home/nam/FurniMart-BE
```

---

## Bước 3: Kiểm tra các Docker containers đang chạy

```bash
docker ps
```

**Kết quả mong đợi:**
```
CONTAINER ID   IMAGE                    STATUS         NAMES
abc123...      furnimart-be-user-service   Up 2 hours    user-service
def456...      postgres:16              Up 2 hours    user-db
...
```

---

## Bước 4: Xem log của từng service

### 4.1. Xem log User Service

```bash
# Xem log real-time (theo dõi liên tục)
docker logs -f user-service

# Xem 50 dòng log cuối cùng
docker logs --tail 50 user-service

# Xem 100 dòng log cuối cùng
docker logs --tail 100 user-service

# Xem log từ thời điểm cụ thể (ví dụ: 10 phút trước)
docker logs --since 10m user-service

# Xem log trong khoảng thời gian
docker logs --since 2025-11-07T09:00:00 --until 2025-11-07T10:00:00 user-service
```

### 4.2. Xem log các service khác

```bash
# API Gateway
docker logs -f api-gateway

# Product Service
docker logs -f product-service

# Order Service
docker logs -f order-service

# Inventory Service
docker logs -f inventory-service

# Delivery Service
docker logs -f delivery-service

# Notification Service
docker logs -f notification-service
```

### 4.3. Xem log Database

```bash
# User Database
docker logs -f user-db

# Product Database
docker logs -f product-db

# Order Database
docker logs -f order-db
```

### 4.4. Xem log Infrastructure Services

```bash
# Redis
docker logs -f redis

# Kafka
docker logs -f kafka

# Zookeeper
docker logs -f zookeeper

# Eureka Server
docker logs -f eureka-server
```

---

## Bước 5: Tìm kiếm trong log

### 5.1. Tìm kiếm theo từ khóa

```bash
# Tìm "ERROR" trong log user-service
docker logs user-service 2>&1 | grep -i "ERROR"

# Tìm "Exception" trong log
docker logs user-service 2>&1 | grep -i "Exception"

# Tìm theo employee ID
docker logs user-service 2>&1 | grep "0ed91bf2-6361-494a-baa1-a676213a9af0"

# Tìm theo store ID
docker logs user-service 2>&1 | grep "8d46e317-0596-4413-81b6-1a526398b3d7"
```

### 5.2. Tìm kiếm với context (dòng trước và sau)

```bash
# Tìm ERROR và hiển thị 5 dòng trước và sau
docker logs user-service 2>&1 | grep -i -A 5 -B 5 "ERROR"

# Tìm Exception với context
docker logs user-service 2>&1 | grep -i -A 10 -B 10 "Exception"
```

---

## Bước 6: Xem log của tất cả services cùng lúc

### 6.1. Sử dụng docker-compose

```bash
# Xem log tất cả services
docker-compose logs -f

# Xem log của nhiều services cụ thể
docker-compose logs -f user-service api-gateway

# Xem 100 dòng log cuối của tất cả services
docker-compose logs --tail 100
```

### 6.2. Xem log theo thời gian

```bash
# Xem log từ 30 phút trước
docker-compose logs --since 30m

# Xem log trong khoảng thời gian
docker-compose logs --since 2025-11-07T09:00:00 --until 2025-11-07T10:00:00
```

---

## Bước 7: Lưu log vào file

### 7.1. Lưu log vào file

```bash
# Lưu log user-service vào file
docker logs user-service > user-service-log.txt

# Lưu log với timestamp
docker logs user-service > user-service-log-$(date +%Y%m%d-%H%M%S).txt

# Lưu log tất cả services
docker-compose logs > all-services-log.txt
```

### 7.2. Tải log file về máy local (từ PowerShell trên Windows)

```powershell
# Sử dụng SCP để tải file về
scp nam@152.53.227.115:~/FurniMart-BE/user-service-log.txt ./
```

---

## Bước 8: Kiểm tra log lỗi cụ thể

### 8.1. Tìm lỗi 500 Internal Server Error

```bash
docker logs user-service 2>&1 | grep -i "500\|Internal Server Error\|INTERNAL_SERVER_ERROR"
```

### 8.2. Tìm lỗi khi thêm employee vào store

```bash
docker logs user-service 2>&1 | grep -i -A 10 "addUserToStore\|add.*store\|employee.*store"
```

### 8.3. Tìm lỗi database

```bash
docker logs user-service 2>&1 | grep -i -A 5 "database\|sql\|connection\|jdbc"
```

---

## Bước 9: Xem log real-time khi test API

### 9.1. Mở 2 terminal windows

**Terminal 1:** Xem log real-time
```bash
ssh nam@152.53.227.115
cd ~/FurniMart-BE
docker logs -f user-service
```

**Terminal 2:** Test API
```powershell
# Test API từ máy local
curl -X POST http://152.53.227.115:8086/api/stores/users ...
```

### 9.2. Hoặc sử dụng tmux/screen để chia màn hình

```bash
# Cài đặt tmux (nếu chưa có)
sudo apt-get install tmux  # Ubuntu/Debian
# hoặc
sudo yum install tmux      # CentOS/RHEL

# Tạo session mới
tmux new -s logs

# Chia màn hình (Ctrl+B, sau đó nhấn %)
# Bên trái: docker logs -f user-service
# Bên phải: docker logs -f api-gateway
```

---

## Bước 10: Kiểm tra log level và cấu hình

### 10.1. Kiểm tra log level trong application.yml

```bash
# Xem cấu hình log
cat user-service/src/main/resources/application.yml | grep -i "log\|logging"
```

### 10.2. Kiểm tra log file (nếu có)

```bash
# Tìm log files trong container
docker exec user-service find / -name "*.log" 2>/dev/null

# Xem log file trong container
docker exec user-service cat /path/to/logfile.log
```

---

## Các lệnh hữu ích khác

### Kiểm tra container status

```bash
# Xem status chi tiết
docker ps -a

# Xem resource usage
docker stats

# Xem logs của container đã dừng
docker logs user-service
```

### Restart service và xem log

```bash
# Restart service
docker-compose restart user-service

# Xem log ngay sau khi restart
docker logs -f user-service
```

### Xem log với timestamp

```bash
# Xem log với timestamp
docker logs -f -t user-service

# Xem log với timestamp và giới hạn số dòng
docker logs --tail 100 -t user-service
```

---

## Troubleshooting

### Lỗi: "Permission denied"

```bash
# Thử với sudo
sudo docker logs user-service

# Hoặc thêm user vào docker group
sudo usermod -aG docker $USER
# Sau đó logout và login lại
```

### Lỗi: "Cannot connect to Docker daemon"

```bash
# Kiểm tra Docker service
sudo systemctl status docker

# Khởi động Docker nếu chưa chạy
sudo systemctl start docker
```

### Container không tồn tại

```bash
# Liệt kê tất cả containers (kể cả đã dừng)
docker ps -a

# Kiểm tra tên container chính xác
docker-compose ps
```

---

## Ví dụ thực tế

### Kiểm tra lỗi khi thêm employee vào store

```bash
# 1. Kết nối SSH
ssh nam@152.53.227.115

# 2. Di chuyển đến project
cd ~/FurniMart-BE

# 3. Xem log real-time
docker logs -f user-service

# 4. Trong terminal khác, test API
# POST /api/stores/users với employeeId và storeId

# 5. Quan sát log để tìm lỗi
# Tìm các dòng có "ERROR", "Exception", hoặc "500"
```

### Tìm lỗi cụ thể

```bash
# Tìm lỗi liên quan đến employee ID cụ thể
docker logs user-service 2>&1 | grep -i -A 20 "0ed91bf2-6361-494a-baa1-a676213a9af0"

# Tìm lỗi trong 10 phút qua
docker logs --since 10m user-service 2>&1 | grep -i "error\|exception"
```

---

## Tips

1. **Sử dụng `-f` (follow):** Để xem log real-time, luôn thêm `-f`
2. **Sử dụng `--tail N`:** Để giới hạn số dòng hiển thị
3. **Sử dụng `grep`:** Để lọc log theo từ khóa
4. **Lưu log:** Luôn lưu log trước khi restart service để debug sau
5. **Timestamp:** Sử dụng `-t` để xem timestamp của mỗi dòng log

