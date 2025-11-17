# Vòng Đời của QR Code trong Hệ Thống FurniMart

## 📋 Tổng Quan

QR Code được sử dụng để xác nhận việc giao hàng thành công từ delivery staff đến customer. Mỗi QR Code là duy nhất và được gắn với một đơn hàng cụ thể.

---

## 🔄 Vòng Đời của QR Code

### **1. TẠO QR CODE (Generation)**

#### **Khi nào được tạo?**
QR Code được tạo tự động khi **BRANCH_MANAGER chấp nhận đơn hàng** (status = `MANAGER_ACCEPT`).

#### **Quy trình tạo:**
```72:87:order-service/src/main/java/com/example/orderservice/service/AssignOrderServiceImpl.java
    private void handleManagerAccept(Order order) {
        QRCodeService.QRCodeResult qrCodeResult = qrCodeService.generateQRCode(order.getId());
        
        ProcessOrder process = ProcessOrder.builder()
                .order(order)
                .status(EnumProcessOrder.MANAGER_ACCEPT)
                .createdAt(new Date())
                .build();
        processOrderRepository.save(process);
        
        order.setStatus(EnumProcessOrder.MANAGER_ACCEPT);
        order.setQrCode(qrCodeResult.getQrCodeString());
        order.setQrCodeGeneratedAt(new Date());
        order.setProcessOrders(order.getProcessOrders());
        orderRepository.save(order);
    }
```

#### **Cách tạo QR Code:**
```28:48:order-service/src/main/java/com/example/orderservice/service/QRCodeService.java
    public String generateQRCodeString(Long orderId) {
        try {
            String data = "ORDER_" + orderId + "_" + LocalDateTime.now();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return "QR_" + hexString.substring(0, 16).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            log.error("Error generating QR code string", e);
            return "QR_" + orderId + "_" + System.currentTimeMillis();
        }
    }
```

**Đặc điểm:**
- Format: `QR_` + 16 ký tự hex (từ SHA-256 hash)
- Dữ liệu hash: `"ORDER_" + orderId + "_" + LocalDateTime.now()`
- Unique: Mỗi QR Code là duy nhất (unique constraint trong database)
- Lưu trong `Order` entity:
  - `qrCode`: String (unique)
  - `qrCodeGeneratedAt`: Date (timestamp khi tạo)

---

### **2. LƯU TRỮ TRONG ORDER**

Sau khi được tạo, QR Code được lưu trong `Order` entity:

```67:72:order-service/src/main/java/com/example/orderservice/entity/Order.java
    @Column(name = "qr_code", unique = true)
    private String qrCode;

    @Column(name = "qr_code_generated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date qrCodeGeneratedAt;
```

**Trạng thái Order:**
- Status: `MANAGER_ACCEPT`
- QR Code: Đã được tạo và lưu
- QR Code Generated At: Timestamp khi tạo

---

### **3. SAO CHÉP SANG DELIVERY CONFIRMATION**

Khi **DELIVERY staff tạo delivery confirmation**, QR Code được sao chép từ Order sang DeliveryConfirmation:

```44:73:delivery-service/src/main/java/com/example/deliveryservice/service/DeliveryConfirmationServiceImpl.java
    public DeliveryConfirmationResponse createDeliveryConfirmation(DeliveryConfirmationRequest request) {
        log.info("Creating delivery confirmation for order: {}", request.getOrderId());

        String deliveryPhotosJson = null;
        if (request.getDeliveryPhotos() != null && !request.getDeliveryPhotos().isEmpty()) {
            try {
                deliveryPhotosJson = objectMapper.writeValueAsString(request.getDeliveryPhotos());
            } catch (JsonProcessingException e) {
                log.error("Error serializing delivery photos", e);
            }
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String deliveryStaffId = authentication.getName();

        // Get QR code from order service instead of generating it
        String qrCode = getQRCodeFromOrder(request.getOrderId());

        DeliveryConfirmation confirmation = DeliveryConfirmation.builder()
                .orderId(request.getOrderId())
                .deliveryStaffId(deliveryStaffId)
                .customerId(null)
                .deliveryPhotos(deliveryPhotosJson)
                .deliveryNotes(request.getDeliveryNotes())
                .qrCode(qrCode)
                .deliveryLatitude(request.getDeliveryLatitude())
                .deliveryLongitude(request.getDeliveryLongitude())
                .deliveryAddress(request.getDeliveryAddress())
                .status(DeliveryConfirmationStatus.DELIVERED)
                .build();
```

**Lấy QR Code từ Order:**
```182:197:delivery-service/src/main/java/com/example/deliveryservice/service/DeliveryConfirmationServiceImpl.java
    private String getQRCodeFromOrder(Long orderId) {
        try {
            ResponseEntity<ApiResponse<OrderResponse>> response = orderClient.getOrderById(orderId);
            if (response.getBody() != null && response.getBody().getData() != null) {
                String qrCode = response.getBody().getData().getQrCode();
                if (qrCode != null && !qrCode.isEmpty()) {
                    return qrCode;
                }
            }
            log.warn("QR code not found for order: {}", orderId);
            return "QR_NOT_FOUND_" + orderId;
        } catch (Exception e) {
            log.error("Error fetching QR code for order {}: {}", orderId, e.getMessage());
            return "QR_ERROR_" + orderId;
        }
    }
```

**Lưu trong DeliveryConfirmation:**
```40:47:delivery-service/src/main/java/com/example/deliveryservice/entity/DeliveryConfirmation.java
    @Column(name = "qr_code", nullable = false, unique = true)
    private String qrCode;

    @Column(name = "qr_code_generated_at", nullable = false)
    private LocalDateTime qrCodeGeneratedAt;

    @Column(name = "qr_code_scanned_at")
    private LocalDateTime qrCodeScannedAt;
```

**Trạng thái:**
- DeliveryConfirmation Status: `DELIVERED`
- Order Status: Chuyển thành `DELIVERED` (sau khi tạo delivery confirmation)
- QR Code: Đã được sao chép từ Order
- QR Code Scanned At: `null` (chưa được scan)

---

### **4. SCAN QR CODE (Customer xác nhận nhận hàng)**

Customer scan QR Code để xác nhận đã nhận hàng:

```107:135:delivery-service/src/main/java/com/example/deliveryservice/service/DeliveryConfirmationServiceImpl.java
    public DeliveryConfirmationResponse scanQRCode(QRCodeScanRequest request) {
        log.info("Scanning QR code: {}", request.getQrCode());

        DeliveryConfirmation confirmation = deliveryConfirmationRepository.findByQrCodeAndIsDeletedFalse(request.getQrCode())
                .orElseThrow(() -> new AppException(ErrorCode.CODE_NOT_FOUND));

        if (confirmation.getQrCodeScannedAt() != null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (request.getCustomerSignature() != null) {
            confirmation.setCustomerSignature(request.getCustomerSignature());
        }

        confirmation.setQrCodeScannedAt(LocalDateTime.now());
        confirmation.setStatus(DeliveryConfirmationStatus.CONFIRMED);

        DeliveryConfirmation savedConfirmation = deliveryConfirmationRepository.save(confirmation);

        // Set order status to FINISHED
        try {
            orderClient.updateOrderStatus(confirmation.getOrderId(), EnumProcessOrder.FINISHED);
        } catch (Exception ex) {
            log.warn("Failed to update order {} to FINISHED: {}", confirmation.getOrderId(), ex.getMessage());
        }

        log.info("QR code scanned successfully for order: {}", confirmation.getOrderId());
        return toDeliveryConfirmationResponse(savedConfirmation);
    }
```

**Validation:**
- QR Code phải tồn tại trong DeliveryConfirmation
- QR Code chưa được scan (`qrCodeScannedAt == null`)
- Nếu đã scan rồi → throw `INVALID_REQUEST` error

**Sau khi scan:**
- `qrCodeScannedAt`: Được set = thời gian hiện tại
- `status`: Chuyển từ `DELIVERED` → `CONFIRMED`
- `customerSignature`: Có thể được lưu (nếu có)
- **Order Status**: Chuyển thành `FINISHED` (kết thúc vòng đời đơn hàng)

---

### **5. KẾT THÚC VÒNG ĐỜI**

Sau khi QR Code được scan:
- ✅ DeliveryConfirmation Status: `CONFIRMED`
- ✅ Order Status: `FINISHED`
- ✅ QR Code đã hoàn thành nhiệm vụ
- ✅ Warranty được tự động tạo (nếu có)

**QR Code không thể scan lại:**
- Nếu cố gắng scan lại → Error: `INVALID_REQUEST`

---

## 📊 Sơ Đồ Vòng Đời

### **Chi Tiết Bước "ASSIGN TO STORE"**

**ASSIGN TO STORE** là bước **tự động tìm và gán cửa hàng gần nhất** với địa chỉ giao hàng của khách hàng.

#### **Khi nào được gọi?**
1. **Tự động sau khi thanh toán thành công:**
   - Khi payment callback thành công → `updateOrderStatus(PAYMENT)` → tự động gọi `assignOrderToStore()`
   - Khi xử lý thanh toán COD → `handlePaymentCOD()` → tự động gọi `assignOrderToStore()`

2. **Thủ công qua API:**
   - `POST /api/orders/{orderId}/assign-store` (có thể gọi thủ công nếu cần)

#### **Quy trình thực hiện:**
```35:55:order-service/src/main/java/com/example/orderservice/service/AssignOrderServiceImpl.java
    public void assignOrderToStore(Long orderId) {

        Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        AddressResponse address = safeGetAddress(order.getAddressId());

        if (address == null) {
            throw new AppException(ErrorCode.ADDRESS_NOT_FOUND);
        }

        order.setStoreId(getStoreNear(address.getLatitude(), address.getLongitude(), 1));
        order.setStatus(EnumProcessOrder.ASSIGN_ORDER_STORE);
        ProcessOrder process =  ProcessOrder.builder()
                .order(order)
                .status(EnumProcessOrder.ASSIGN_ORDER_STORE)
                .createdAt(new Date())
                .build();

        processOrderRepository.save(process);
        orderRepository.save(order);
    }
```

#### **Cách tìm store gần nhất:**
```122:129:order-service/src/main/java/com/example/orderservice/service/AssignOrderServiceImpl.java
    private String getStoreNear(Double lat, Double lon, int limit) {
        ApiResponse<List<StoreDistance>> response = storeClient.getNearestStores(lat, lon, limit);
        System.out.println(response);
        if (response == null || response.getData() == null) {
            throw new AppException(ErrorCode.ADDRESS_NOT_FOUND);
        }
        return response.getData().getFirst().getStore().getId();
    }
```

**Các bước:**
1. ✅ Lấy địa chỉ giao hàng từ Order (`addressId`)
2. ✅ Lấy tọa độ (latitude, longitude) từ địa chỉ
3. ✅ Gọi API `getNearestStores(lat, lon, limit=1)` để tìm store gần nhất
4. ✅ Gán `storeId` vào Order
5. ✅ Chuyển status thành `ASSIGN_ORDER_STORE`
6. ✅ Tạo ProcessOrder record với status `ASSIGN_ORDER_STORE`

**Kết quả:**
- Order có `storeId` (cửa hàng được gán)
- Status: `ASSIGN_ORDER_STORE`
- **Chưa có QR Code** (QR Code chỉ được tạo khi Manager chấp nhận)

**Lưu ý:**
- ⚠️ Bước này **chưa tạo QR Code**
- ⚠️ Chỉ gán cửa hàng, chưa có Manager xác nhận
- ✅ Sau bước này, Manager của store sẽ nhận được thông báo và có thể Accept/Reject đơn hàng

---

### **Bước Tiếp Theo: MANAGER ACCEPT/REJECT**

Sau khi đơn hàng được gán cho cửa hàng (`ASSIGN_ORDER_STORE`), **BRANCH_MANAGER** của cửa hàng đó sẽ nhận thông báo và cần quyết định chấp nhận hoặc từ chối đơn hàng.

#### **API Endpoint:**
```173:196:order-service/src/main/java/com/example/orderservice/controller/OrderController.java
    @PostMapping("/{orderId}/manager-decision")
    public ApiResponse<String> managerAcceptOrRejectOrder(
            @PathVariable Long orderId,
            @RequestParam(required = false) String storeId,
            @RequestParam(required = false) String reason,
            @RequestParam EnumProcessOrder status
    ) {
        assignOrderService.acceptRejectOrderByManager(orderId, storeId, reason, status);

        String message;
        if (status == EnumProcessOrder.MANAGER_ACCEPT) {
            message = "Quản lý đã chấp nhận đơn hàng #" + orderId;
        } else if (status == EnumProcessOrder.MANAGER_REJECT) {
            message = "Quản lý đã từ chối đơn hàng #" + orderId
                    + (storeId != null ? " và gán lại cho cửa hàng khác" : "");
        } else {
            message = "Trạng thái không hợp lệ";
        }

        return ApiResponse.<String>builder()
                .status(HttpStatus.OK.value())
                .message(message)
                .build();
    }
```

**Request:**
- `POST /api/orders/{orderId}/manager-decision`
- **Role**: `BRANCH_MANAGER`
- **Parameters:**
  - `status`: `MANAGER_ACCEPT` hoặc `MANAGER_REJECT`
  - `storeId`: (optional) Nếu REJECT, có thể gán lại cho store khác
  - `reason`: (optional) Lý do từ chối

#### **Trường hợp 1: MANAGER ACCEPT ✅**

Khi Manager chấp nhận đơn hàng:

```72:87:order-service/src/main/java/com/example/orderservice/service/AssignOrderServiceImpl.java
    private void handleManagerAccept(Order order) {
        QRCodeService.QRCodeResult qrCodeResult = qrCodeService.generateQRCode(order.getId());
        
        ProcessOrder process = ProcessOrder.builder()
                .order(order)
                .status(EnumProcessOrder.MANAGER_ACCEPT)
                .createdAt(new Date())
                .build();
        processOrderRepository.save(process);
        
        order.setStatus(EnumProcessOrder.MANAGER_ACCEPT);
        order.setQrCode(qrCodeResult.getQrCodeString());
        order.setQrCodeGeneratedAt(new Date());
        order.setProcessOrders(order.getProcessOrders());
        orderRepository.save(order);
    }
```

**Kết quả:**
- ✅ Status: `MANAGER_ACCEPT`
- ✅ **QR Code được tạo** (đây là lần đầu tiên QR Code được tạo)
- ✅ `qrCodeGeneratedAt`: Timestamp khi tạo
- ✅ Đơn hàng sẵn sàng để assign cho delivery staff

#### **Trường hợp 2: MANAGER REJECT ❌**

Khi Manager từ chối đơn hàng:

```89:107:order-service/src/main/java/com/example/orderservice/service/AssignOrderServiceImpl.java
    private void handleManagerReject(Order order, String storeId, String reason) {
        ProcessOrder rejectProcess = ProcessOrder.builder()
                .order(order)
                .status(EnumProcessOrder.MANAGER_REJECT)
                .createdAt(new Date())
                .build();
        processOrderRepository.save(rejectProcess);
        order.setReason(reason);
        order.setStoreId(storeId);
        order.setStatus(EnumProcessOrder.ASSIGN_ORDER_STORE);
        orderRepository.save(order);

        ProcessOrder assignProcess = ProcessOrder.builder()
                .order(order)
                .status(EnumProcessOrder.ASSIGN_ORDER_STORE)
                .createdAt(new Date())
                .build();
        processOrderRepository.save(assignProcess);
    }
```

**Kết quả:**
- ❌ Status: Quay lại `ASSIGN_ORDER_STORE`
- ❌ **Không tạo QR Code**
- ❌ `reason`: Lưu lý do từ chối
- ❌ `storeId`: Có thể được gán lại cho store khác (nếu có)
- ⚠️ Đơn hàng quay lại trạng thái chờ Manager khác xem và quyết định

**Lưu ý:**
- Nếu Manager REJECT và gán lại `storeId` khác → Đơn hàng sẽ được gán cho store mới
- Manager mới sẽ nhận thông báo và có thể ACCEPT hoặc REJECT
- Quá trình này có thể lặp lại cho đến khi có Manager chấp nhận

---

## 📊 Sơ Đồ Vòng Đời

```
┌─────────────────────────────────────────────────────────────┐
│ 1. ORDER CREATED                                            │
│    Status: PENDING                                           │
│    QR Code: null                                             │
│    StoreId: null                                             │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. ASSIGN TO STORE ⭐                                        │
│    Status: ASSIGN_ORDER_STORE                                │
│    QR Code: null                                             │
│    StoreId: [được gán tự động]                                │
│    ⭐ TỰ ĐỘNG TÌM CỬA HÀNG GẦN NHẤT                          │
│    - Dựa trên địa chỉ giao hàng (lat/lon)                   │
│    - Gọi API getNearestStores()                              │
│    - Chọn store gần nhất (limit=1)                           │
│    - Gán storeId vào Order                                   │
│    ⚠️ Chưa có QR Code ở bước này                            │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. MANAGER XEM ĐƠN HÀNG                                     │
│    Status: ASSIGN_ORDER_STORE (chờ quyết định)              │
│    QR Code: null                                             │
│    ⭐ Manager nhận thông báo có đơn hàng mới                  │
│    Manager xem chi tiết đơn hàng                             │
│    Manager quyết định: ACCEPT hoặc REJECT                    │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ├─── ACCEPT ───┐
                       │              │
                       └─── REJECT ───┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────┐
│ 3A. MANAGER ACCEPT ✅                                        │
│    Status: MANAGER_ACCEPT                                    │
│    QR Code: GENERATED (QR_XXXXXXXXXXXX)                     │
│    qrCodeGeneratedAt: [timestamp]                            │
│    ⭐ QR CODE ĐƯỢC TẠO Ở ĐÂY                                │
│    - Tạo QR Code duy nhất                                    │
│    - Lưu QR Code vào Order                                   │
│    - Đơn hàng sẵn sàng để assign delivery                   │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│ 3B. MANAGER REJECT ❌                                        │
│    Status: ASSIGN_ORDER_STORE (quay lại)                     │
│    QR Code: null                                             │
│    Reason: [lý do từ chối]                                  │
│    StoreId: [có thể gán lại store khác]                     │
│    ⭐ Đơn hàng quay lại trạng thái ASSIGN_ORDER_STORE       │
│    - Manager có thể gán lại cho store khác                  │
│    - Hoặc hệ thống tự động tìm store khác                    │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼ (nếu gán lại store)
┌─────────────────────────────────────────────────────────────┐
│ Lặp lại bước 3: Manager mới xem và quyết định              │
└─────────────────────────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. DELIVERY STAFF ASSIGNED                                  │
│    Status: ASSIGNED_TO_DELIVERY                             │
│    QR Code: [đã có]                                         │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. DELIVERY CONFIRMATION CREATED                            │
│    DeliveryConfirmation Status: DELIVERED                  │
│    Order Status: DELIVERED                                   │
│    QR Code: [sao chép từ Order]                             │
│    qrCodeScannedAt: null                                     │
│    ⭐ QR CODE ĐƯỢC SAO CHÉP SANG DELIVERY CONFIRMATION      │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. CUSTOMER SCAN QR CODE ✅                                 │
│    DeliveryConfirmation Status: CONFIRMED                   │
│    Order Status: FINISHED                                    │
│    qrCodeScannedAt: [timestamp]                             │
│    ⭐ QR CODE ĐƯỢC SCAN - HOÀN TẤT VÒNG ĐỜI                  │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔑 Các Trạng Thái Quan Trọng

### **Order Status:**
1. `PENDING` → Chưa có QR Code
2. `ASSIGN_ORDER_STORE` → Chưa có QR Code
3. `MANAGER_ACCEPT` → **QR Code được tạo** ⭐
4. `ASSIGNED_TO_DELIVERY` → QR Code đã có
5. `DELIVERED` → QR Code đã có, chưa scan
6. `FINISHED` → QR Code đã được scan ⭐

### **DeliveryConfirmation Status:**
1. `DELIVERED` → QR Code chưa scan (`qrCodeScannedAt == null`)
2. `CONFIRMED` → QR Code đã scan (`qrCodeScannedAt != null`) ⭐

---

## 📝 API Endpoints Liên Quan

### **1. Tạo QR Code (tự động)**
- **Endpoint**: `POST /api/orders/{orderId}/accept-reject`
- **Role**: `BRANCH_MANAGER`
- **Khi**: Manager chấp nhận đơn hàng
- **Kết quả**: QR Code được tạo và lưu trong Order

### **2. Lấy QR Code từ Order**
- **Endpoint**: `GET /api/orders/{orderId}`
- **Response**: Bao gồm `qrCode` và `qrCodeGeneratedAt`

### **3. Tạo Delivery Confirmation**
- **Endpoint**: `POST /api/delivery-confirmations`
- **Role**: `DELIVERY`
- **Kết quả**: QR Code được sao chép từ Order sang DeliveryConfirmation

### **4. Scan QR Code**
- **Endpoint**: `POST /api/delivery-confirmations/scan-qr`
- **Role**: `CUSTOMER`
- **Request Body**:
  ```json
  {
    "qrCode": "QR_XXXXXXXXXXXX",
    "customerSignature": "base64_encoded_signature" // optional
  }
  ```
- **Kết quả**: 
  - `qrCodeScannedAt` được set
  - Status: `CONFIRMED`
  - Order Status: `FINISHED`

### **5. Lấy Delivery Confirmation bằng QR Code**
- **Endpoint**: `GET /api/delivery-confirmations/qr/{qrCode}`
- **Role**: `CUSTOMER` hoặc `ADMIN`

---

## ⚠️ Lưu Ý Quan Trọng

1. **QR Code chỉ được tạo 1 lần**: Khi Manager chấp nhận đơn hàng
2. **QR Code là unique**: Mỗi Order chỉ có 1 QR Code duy nhất
3. **QR Code chỉ scan được 1 lần**: Nếu đã scan rồi thì không thể scan lại
4. **QR Code được sao chép**: Từ Order sang DeliveryConfirmation (không phải tạo mới)
5. **Sau khi scan**: Order chuyển sang status `FINISHED` (kết thúc vòng đời)

---

## 🎯 Tóm Tắt

| Giai Đoạn | QR Code Status | Order Status | DeliveryConfirmation Status |
|-----------|----------------|--------------|----------------------------|
| **Tạo đơn** | ❌ Chưa có | `PENDING` | - |
| **Assign store** | ❌ Chưa có | `ASSIGN_ORDER_STORE` | - |
| **Manager accept** | ✅ **ĐƯỢC TẠO** | `MANAGER_ACCEPT` | - |
| **Delivery assigned** | ✅ Đã có | `ASSIGNED_TO_DELIVERY` | - |
| **Delivery confirmation** | ✅ Đã có (sao chép) | `DELIVERED` | `DELIVERED` |
| **Customer scan** | ✅ **ĐÃ SCAN** | `FINISHED` | `CONFIRMED` |

---

**Tài liệu này mô tả đầy đủ vòng đời của QR Code từ khi được tạo đến khi được scan và hoàn tất.**

