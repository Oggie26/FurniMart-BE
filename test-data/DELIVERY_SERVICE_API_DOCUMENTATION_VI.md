# Tài Liệu API Delivery Service cho Frontend

## Tổng Quan

Delivery Service quản lý toàn bộ quy trình giao hàng từ việc phân công đơn hàng đến xác nhận giao hàng. Bao gồm hai module chính:
- **Delivery Assignment**: Quản lý việc phân công đơn hàng cho nhân viên giao hàng
- **Delivery Confirmation**: Quản lý xác nhận giao hàng với quét mã QR

**Base URL**: `http://152.53.227.115:8080/api` (qua API Gateway)

---

## Xác Thực

Tất cả các API (trừ các endpoint công khai) yêu cầu JWT Bearer token trong header Authorization:
```
Authorization: Bearer <your-jwt-token>
```

---

## 1. API PHÂN CÔNG GIAO HÀNG

### 1.1. Phân Công Đơn Hàng Cho Nhân Viên Giao Hàng
**POST** `/api/delivery/assign`

**Vai trò**: `STAFF`, `BRANCH_MANAGER`

**Mô tả**: Phân công đơn hàng cho nhân viên giao hàng. Sau khi phân công thành công, trạng thái phân công sẽ là `ASSIGNED`.

**Request Body**:
```json
{
  "orderId": 407,                    // Bắt buộc: ID đơn hàng
  "storeId": "d7a7205d-...",        // Bắt buộc: ID cửa hàng
  "deliveryStaffId": "5526baac-...", // Bắt buộc: ID nhân viên giao hàng
  "estimatedDeliveryDate": "2025-11-15T10:00:00", // Tùy chọn: Định dạng ISO 8601
  "notes": "Xử lý cẩn thận"       // Tùy chọn: Ghi chú
}
```

**Response** (201 Created):
```json
{
  "status": 201,
  "message": "Phân công đơn hàng cho nhân viên giao hàng thành công",
  "data": {
    "id": 1,
    "orderId": 407,
    "storeId": "d7a7205d-...",
    "deliveryStaffId": "5526baac-...",
    "assignedBy": "staff@furnimart.com",
    "assignedAt": "2025-11-13T17:20:00",
    "estimatedDeliveryDate": "2025-11-15T10:00:00",
    "status": "ASSIGNED",
    "notes": "Xử lý cẩn thận",
    "invoiceGenerated": false,
    "productsPrepared": false,
    "order": { /* Chi tiết đơn hàng */ },
    "store": { /* Chi tiết cửa hàng */ }
  }
}
```

**Lỗi Trả Về**:
- `400`: Đơn hàng đã được phân công
- `404`: Không tìm thấy đơn hàng HOẶC Không tìm thấy cửa hàng
- `403`: Từ chối truy cập (sai vai trò)

---

### 1.2. Tạo Hóa Đơn Cho Đơn Hàng
**POST** `/api/delivery/generate-invoice/{orderId}`

**Vai trò**: `STAFF`

**Mô tả**: Tạo hóa đơn cho đơn hàng. Mỗi đơn hàng chỉ có thể tạo hóa đơn một lần. Sau khi tạo thành công, `invoiceGenerated` sẽ được đặt thành `true`.

**Path Parameters**:
- `orderId` (Long): ID đơn hàng

**Response** (200 OK):
```json
{
  "status": 200,
  "message": "Tạo hóa đơn thành công",
  "data": {
    "id": 1,
    "invoiceGenerated": true,
    "invoiceGeneratedAt": "2025-11-13T17:25:00",
    // ... các trường phân công khác
  }
}
```

**Lỗi Trả Về**:
- `400`: Hóa đơn đã được tạo
- `404`: Không tìm thấy phân công giao hàng

---

### 1.3. Chuẩn Bị Sản Phẩm Cho Giao Hàng
**POST** `/api/delivery/prepare-products`

**Vai trò**: `STAFF`

**Mô tả**: Chuẩn bị sản phẩm cho giao hàng. Hệ thống sẽ kiểm tra tồn kho. Nếu không đủ hàng, sẽ trả về lỗi với chi tiết. Sau khi chuẩn bị thành công, `productsPrepared` sẽ được đặt thành `true` và trạng thái sẽ chuyển sang `READY`.

**Request Body**:
```json
{
  "orderId": 407,        // Bắt buộc
  "notes": "Ghi chú tùy chọn"
}
```

**Response** (200 OK):
```json
{
  "status": 200,
  "message": "Chuẩn bị sản phẩm thành công",
  "data": {
    "id": 1,
    "productsPrepared": true,
    "productsPreparedAt": "2025-11-13T17:30:00",
    "status": "READY",
    // ... các trường khác
  }
}
```

**Lỗi Trả Về**:
- `400`: Sản phẩm đã được chuẩn bị HOẶC Không đủ hàng (chi tiết trong message)
- `404`: Không tìm thấy phân công giao hàng HOẶC Không tìm thấy đơn hàng

---

### 1.4. Lấy Danh Sách Phân Công Giao Hàng Theo Cửa Hàng
**GET** `/api/delivery/assignments/store/{storeId}`

**Vai trò**: `STAFF`, `BRANCH_MANAGER`

**Mô tả**: Lấy tất cả các phân công giao hàng cho một cửa hàng cụ thể.

**Path Parameters**:
- `storeId` (String): ID cửa hàng

**Response** (200 OK):
```json
{
  "status": 200,
  "message": "Lấy danh sách phân công giao hàng thành công",
  "data": [
    {
      "id": 1,
      "orderId": 407,
      "status": "ASSIGNED",
      // ... đầy đủ chi tiết phân công
    }
  ]
}
```

---

### 1.5. Lấy Phân Công Giao Hàng Theo ID Đơn Hàng
**GET** `/api/delivery/assignments/order/{orderId}`

**Vai trò**: `STAFF`, `BRANCH_MANAGER`

**Mô tả**: Lấy thông tin phân công giao hàng cho một đơn hàng cụ thể.

**Path Parameters**:
- `orderId` (Long): ID đơn hàng

**Response** (200 OK):
```json
{
  "status": 200,
  "message": "Lấy phân công giao hàng thành công",
  "data": {
    "id": 1,
    "orderId": 407,
    // ... đầy đủ chi tiết phân công
  }
}
```

**Lỗi Trả Về**:
- `404`: Không tìm thấy phân công giao hàng

---

### 1.6. Cập Nhật Trạng Thái Giao Hàng
**PUT** `/api/delivery/assignments/{assignmentId}/status?status={status}`

**Vai trò**: `BRANCH_MANAGER`, `DELIVERY`

**Mô tả**: Cập nhật trạng thái giao hàng. Các giá trị trạng thái hợp lệ: `ASSIGNED`, `PREPARING`, `READY`, `IN_TRANSIT`, `DELIVERED`, `CANCELLED`.

**Path Parameters**:
- `assignmentId` (Long): ID phân công giao hàng

**Query Parameters**:
- `status` (String): Một trong các giá trị: `ASSIGNED`, `PREPARING`, `READY`, `IN_TRANSIT`, `DELIVERED`, `CANCELLED`

**Ví dụ**:
```
PUT /api/delivery/assignments/1/status?status=IN_TRANSIT
```

**Response** (200 OK):
```json
{
  "status": 200,
  "message": "Cập nhật trạng thái giao hàng thành công",
  "data": {
    "id": 1,
    "status": "IN_TRANSIT",
    // ... các trường khác
  }
}
```

**Lỗi Trả Về**:
- `400`: Giá trị trạng thái không hợp lệ
- `404`: Không tìm thấy phân công giao hàng

---

### 1.7. Lấy Danh Sách Phân Công Giao Hàng Theo Nhân Viên Giao Hàng
**GET** `/api/delivery/assignments/staff/{deliveryStaffId}`

**Vai trò**: `DELIVERY`

**Mô tả**: Lấy tất cả các phân công giao hàng cho một nhân viên giao hàng cụ thể.

**Path Parameters**:
- `deliveryStaffId` (String): ID nhân viên giao hàng

**Response** (200 OK):
```json
{
  "status": 200,
  "message": "Lấy danh sách phân công giao hàng thành công",
  "data": [
    {
      "id": 1,
      "orderId": 407,
      "status": "ASSIGNED",
      // ... đầy đủ chi tiết phân công
    }
  ]
}
```

---

### 1.8. Theo Dõi Tiến Độ Giao Hàng (Quản Lý Chi Nhánh)
**GET** `/api/delivery/progress/store/{storeId}`

**Vai trò**: `BRANCH_MANAGER`

**Mô tả**: Theo dõi tiến độ giao hàng cho một cửa hàng. Trả về thống kê số lượng đơn hàng theo trạng thái.

**Path Parameters**:
- `storeId` (String): ID cửa hàng

**Response** (200 OK):
```json
{
  "status": 200,
  "message": "Lấy tiến độ giao hàng thành công",
  "data": {
    "storeId": "d7a7205d-...",
    "storeName": "Tên Cửa Hàng",
    "totalAssignments": 50,
    "assignedCount": 10,
    "preparingCount": 5,
    "readyCount": 8,
    "inTransitCount": 15,
    "deliveredCount": 12,
    "assignments": [ /* Danh sách phân công */ ]
  }
}
```

---

### 1.9. Lấy Thông Tin Chi Nhánh Cửa Hàng (Công Khai)
**GET** `/api/delivery/stores/{storeId}/branch-info`

**Vai trò**: `PUBLIC` (Không yêu cầu xác thực)

**Mô tả**: Lấy thông tin chi tiết cửa hàng bao gồm thông tin kho và tình trạng tồn kho.

**Path Parameters**:
- `storeId` (String): ID cửa hàng

**Response** (200 OK):
```json
{
  "status": 200,
  "message": "Lấy thông tin chi nhánh cửa hàng thành công",
  "data": {
    "store": {
      "id": "d7a7205d-...",
      "name": "Tên Cửa Hàng",
      "addressLine": "123 Đường Chính",
      // ... chi tiết cửa hàng
    },
    "productStockInfo": [
      {
        "productColorId": "prod-color-123",
        "productName": "Tên Sản Phẩm",
        "availableStock": 50,
        "inStock": true
      }
    ]
  }
}
```

---

## 2. API XÁC NHẬN GIAO HÀNG

### 2.1. Tạo Xác Nhận Giao Hàng
**POST** `/api/delivery-confirmations`

**Vai trò**: `DELIVERY`

**Mô tả**: Tạo xác nhận giao hàng với ảnh. API này sẽ:
- Tự động lấy mã QR từ Order Service
- Tạo xác nhận giao hàng với trạng thái `DELIVERED`
- Cập nhật trạng thái đơn hàng thành `DELIVERED`
- Tự động tạo bảo hành cho đơn hàng

**Request Body**:
```json
{
  "orderId": 407,                    // Bắt buộc
  "deliveryPhotos": [                // Tùy chọn: Danh sách URL ảnh
    "https://example.com/photo1.jpg",
    "https://example.com/photo2.jpg"
  ],
  "deliveryNotes": "Giao hàng an toàn", // Tùy chọn
  "deliveryLatitude": 10.762622,     // Tùy chọn
  "deliveryLongitude": 106.660172,  // Tùy chọn
  "deliveryAddress": "123 Đường Chính"   // Tùy chọn
}
```

**Response** (201 Created):
```json
{
  "status": 201,
  "message": "Tạo xác nhận giao hàng thành công",
  "data": {
    "id": 1,
    "orderId": 407,
    "deliveryStaffId": "5526baac-...",
    "customerId": "customer-123",
    "deliveryDate": "2025-11-13T17:40:00",
    "deliveryPhotos": ["https://..."],
    "qrCode": "QR_XXXXXXXXXXXX",
    "qrCodeGeneratedAt": "2025-11-13T17:40:00",
    "qrCodeScannedAt": null,
    "status": "DELIVERED",
    "isQrCodeScanned": false
  }
}
```

**Lỗi Trả Về**:
- `404`: Không tìm thấy đơn hàng

---

### 2.2. Quét Mã QR Để Xác Nhận Nhận Hàng
**POST** `/api/delivery-confirmations/scan-qr`

**Vai trò**: `CUSTOMER`

**Mô tả**: Quét mã QR để xác nhận nhận hàng. Mỗi mã QR chỉ có thể quét một lần. Sau khi quét thành công:
- Trạng thái chuyển sang `CONFIRMED`
- `qrCodeScannedAt` được đặt
- Trạng thái đơn hàng được cập nhật thành `FINISHED`

**Request Body**:
```json
{
  "qrCode": "QR_XXXXXXXXXXXX",      // Bắt buộc
  "customerSignature": "base64..."  // Tùy chọn: Chữ ký mã hóa Base64
}
```

**Response** (200 OK):
```json
{
  "status": 200,
  "message": "Quét mã QR thành công",
  "data": {
    "id": 1,
    "status": "CONFIRMED",
    "qrCodeScannedAt": "2025-11-13T18:00:00",
    "isQrCodeScanned": true
  }
}
```

**Lỗi Trả Về**:
- `400`: Mã QR đã được quét
- `404`: Không tìm thấy xác nhận giao hàng

---

### 2.3. Lấy Xác Nhận Giao Hàng Theo ID Đơn Hàng
**GET** `/api/delivery-confirmations/order/{orderId}`

**Vai trò**: `CUSTOMER` (chủ đơn hàng), `ADMIN`

**Mô tả**: Lấy thông tin xác nhận giao hàng cho một đơn hàng cụ thể.

**Path Parameters**:
- `orderId` (Long): ID đơn hàng

**Response** (200 OK):
```json
{
  "status": 200,
  "message": "Lấy xác nhận giao hàng thành công",
  "data": {
    "id": 1,
    "orderId": 407,
    "qrCode": "QR_XXXXXXXXXXXX",
    "status": "DELIVERED",
    // ... đầy đủ chi tiết xác nhận
  }
}
```

---

### 2.4. Lấy Xác Nhận Giao Hàng Theo Mã QR
**GET** `/api/delivery-confirmations/qr/{qrCode}`

**Vai trò**: `CUSTOMER`, `ADMIN`

**Mô tả**: Lấy thông tin xác nhận giao hàng theo mã QR.

**Path Parameters**:
- `qrCode` (String): Chuỗi mã QR

**Response** (200 OK):
```json
{
  "status": 200,
  "message": "Lấy xác nhận giao hàng thành công",
  "data": {
    "id": 1,
    "qrCode": "QR_XXXXXXXXXXXX",
    // ... đầy đủ chi tiết xác nhận
  }
}
```

---

### 2.5. Lấy Danh Sách Xác Nhận Giao Hàng Theo ID Khách Hàng
**GET** `/api/delivery-confirmations/customer/{customerId}`

**Vai trò**: `CUSTOMER` (chính họ), `ADMIN`

**Mô tả**: Lấy tất cả các xác nhận giao hàng cho một khách hàng cụ thể.

**Path Parameters**:
- `customerId` (String): ID khách hàng

**Response** (200 OK):
```json
{
  "status": 200,
  "message": "Lấy danh sách xác nhận giao hàng thành công",
  "data": [
    {
      "id": 1,
      "orderId": 407,
      "status": "CONFIRMED",
      // ... đầy đủ chi tiết xác nhận
    }
  ]
}
```

---

### 2.6. Lấy Danh Sách Xác Nhận Giao Hàng Theo ID Nhân Viên Giao Hàng
**GET** `/api/delivery-confirmations/staff/{deliveryStaffId}`

**Vai trò**: `DELIVERY`, `ADMIN`

**Mô tả**: Lấy tất cả các xác nhận giao hàng cho một nhân viên giao hàng cụ thể.

**Path Parameters**:
- `deliveryStaffId` (String): ID nhân viên giao hàng

**Response** (200 OK):
```json
{
  "status": 200,
  "message": "Lấy danh sách xác nhận giao hàng thành công",
  "data": [
    {
      "id": 1,
      "orderId": 407,
      // ... đầy đủ chi tiết xác nhận
    }
  ]
}
```

---

### 2.7. Lấy Tất Cả Xác Nhận Giao Hàng (Admin)
**GET** `/api/delivery-confirmations`

**Vai trò**: `ADMIN`

**Mô tả**: Lấy tất cả các xác nhận giao hàng trong hệ thống.

**Response** (200 OK):
```json
{
  "status": 200,
  "message": "Lấy tất cả xác nhận giao hàng thành công",
  "data": [ /* Danh sách tất cả xác nhận */ ]
}
```

---

### 2.8. Lấy Xác Nhận Giao Hàng Đã Quét (Admin)
**GET** `/api/delivery-confirmations/scanned`

**Vai trò**: `ADMIN`

**Mô tả**: Lấy tất cả các xác nhận giao hàng đã được quét (đã xác nhận nhận hàng).

**Response** (200 OK):
```json
{
  "status": 200,
  "message": "Lấy xác nhận giao hàng đã quét thành công",
  "data": [ /* Danh sách xác nhận đã quét */ ]
}
```

---

### 2.9. Lấy Xác Nhận Giao Hàng Chưa Quét (Admin)
**GET** `/api/delivery-confirmations/unscanned`

**Vai trò**: `ADMIN`

**Mô tả**: Lấy tất cả các xác nhận giao hàng chưa được quét (chưa xác nhận nhận hàng).

**Response** (200 OK):
```json
{
  "status": 200,
  "message": "Lấy xác nhận giao hàng chưa quét thành công",
  "data": [ /* Danh sách xác nhận chưa quét */ ]
}
```

---

## 3. ENUMS & GIÁ TRỊ TRẠNG THÁI

### 3.1. Trạng Thái Giao Hàng
Sử dụng trong `DeliveryAssignment.status`:
- `ASSIGNED` - Đơn hàng đã được phân công cho nhân viên giao hàng
- `PREPARING` - Sản phẩm đang được chuẩn bị
- `READY` - Sản phẩm sẵn sàng giao hàng
- `IN_TRANSIT` - Nhân viên giao hàng đang trên đường
- `DELIVERED` - Đơn hàng đã được giao
- `CANCELLED` - Giao hàng đã bị hủy

### 3.2. Trạng Thái Xác Nhận Giao Hàng
Sử dụng trong `DeliveryConfirmation.status`:
- `DELIVERED` - Giao hàng hoàn tất, đang chờ khách hàng xác nhận
- `CONFIRMED` - Khách hàng đã quét mã QR và xác nhận nhận hàng
- `DISPUTED` - Tranh chấp giao hàng
- `CANCELLED` - Giao hàng đã bị hủy

---

## 4. MÔ HÌNH RESPONSE

### 4.1. DeliveryAssignmentResponse
```typescript
interface DeliveryAssignmentResponse {
  id: number;
  orderId: number;
  storeId: string;
  deliveryStaffId: string;
  assignedBy: string;
  assignedAt: string; // ISO 8601 datetime
  estimatedDeliveryDate: string; // ISO 8601 datetime
  status: "ASSIGNED" | "PREPARING" | "READY" | "IN_TRANSIT" | "DELIVERED" | "CANCELLED";
  notes: string;
  invoiceGenerated: boolean;
  invoiceGeneratedAt: string; // ISO 8601 datetime
  productsPrepared: boolean;
  productsPreparedAt: string; // ISO 8601 datetime
  order: OrderResponse; // Chi tiết đơn hàng đầy đủ
  store: StoreResponse; // Chi tiết cửa hàng đầy đủ
}
```

### 4.2. DeliveryConfirmationResponse
```typescript
interface DeliveryConfirmationResponse {
  id: number;
  orderId: number;
  deliveryStaffId: string;
  customerId: string;
  deliveryDate: string; // ISO 8601 datetime
  deliveryPhotos: string[]; // Mảng URL ảnh
  deliveryNotes: string;
  qrCode: string;
  qrCodeGeneratedAt: string; // ISO 8601 datetime
  qrCodeScannedAt: string | null; // ISO 8601 datetime
  customerSignature: string | null; // Mã hóa Base64
  status: "DELIVERED" | "CONFIRMED" | "DISPUTED" | "CANCELLED";
  deliveryLatitude: number | null;
  deliveryLongitude: number | null;
  deliveryAddress: string | null;
  isQrCodeScanned: boolean;
  createdAt: string; // ISO 8601 datetime
  updatedAt: string; // ISO 8601 datetime
}
```

### 4.3. DeliveryProgressResponse
```typescript
interface DeliveryProgressResponse {
  storeId: string;
  storeName: string;
  totalAssignments: number;
  assignedCount: number;
  preparingCount: number;
  readyCount: number;
  inTransitCount: number;
  deliveredCount: number;
  assignments: DeliveryAssignmentResponse[];
}
```

---

## 5. QUY TRÌNH GIAO HÀNG

### Quy Trình Giao Hàng Hoàn Chỉnh:

```
1. ĐƠN HÀNG ĐƯỢC TẠO
   ↓
2. PHÂN CÔNG CHO CỬA HÀNG (Tự động)
   - Trạng thái đơn hàng: ASSIGN_ORDER_STORE
   - Cửa hàng được phân công dựa trên địa chỉ giao hàng
   ↓
3. QUẢN LÝ CHẤP NHẬN/TỪ CHỐI
   - Nếu CHẤP NHẬN: Mã QR được tạo, trạng thái: MANAGER_ACCEPT
   - Nếu TỪ CHỐI: Quay lại ASSIGN_ORDER_STORE
   ↓
4. NHÂN VIÊN PHÂN CÔNG ĐƠN HÀNG CHO NHÂN VIÊN GIAO HÀNG
   POST /api/delivery/assign
   - Trạng thái phân công: ASSIGNED
   ↓
5. NHÂN VIÊN TẠO HÓA ĐƠN (Tùy chọn)
   POST /api/delivery/generate-invoice/{orderId}
   - invoiceGenerated: true
   ↓
6. NHÂN VIÊN CHUẨN BỊ SẢN PHẨM
   POST /api/delivery/prepare-products
   - productsPrepared: true
   - Trạng thái phân công: READY
   ↓
7. NHÂN VIÊN GIAO HÀNG CẬP NHẬT TRẠNG THÁI
   PUT /api/delivery/assignments/{id}/status?status=IN_TRANSIT
   - Trạng thái: IN_TRANSIT
   ↓
8. NHÂN VIÊN GIAO HÀNG TẠO XÁC NHẬN GIAO HÀNG
   POST /api/delivery-confirmations
   - Trạng thái đơn hàng: DELIVERED
   - Trạng thái xác nhận: DELIVERED
   - Mã QR được sao chép từ đơn hàng
   ↓
9. KHÁCH HÀNG QUÉT MÃ QR
   POST /api/delivery-confirmations/scan-qr
   - Trạng thái xác nhận: CONFIRMED
   - Trạng thái đơn hàng: FINISHED
   - qrCodeScannedAt: timestamp
```

---

## 6. LỖI TRẢ VỀ THƯỜNG GẶP

Tất cả các API trả về lỗi theo định dạng này:
```json
{
  "status": 400,
  "message": "Mô tả lỗi",
  "error": "ERROR_CODE",
  "timestamp": "2025-11-13T17:20:00"
}
```

**Mã Lỗi Thường Gặp**:
- `400`: Bad Request (lỗi validation, lỗi logic nghiệp vụ)
- `401`: Unauthenticated (thiếu hoặc token không hợp lệ)
- `403`: Access Denied (không đủ quyền)
- `404`: Not Found (không tìm thấy tài nguyên)
- `500`: Internal Server Error

---

## 7. GHI CHÚ CHO FRONTEND

### Điểm Quan Trọng:

1. **Truy Cập Dựa Trên Vai Trò**: Mỗi API có yêu cầu vai trò cụ thể. Đảm bảo kiểm tra vai trò người dùng trước khi hiển thị/ẩn các phần tử UI.

2. **Luồng Trạng Thái**: Trạng thái giao hàng tuân theo một luồng cụ thể. Không cho phép bỏ qua trạng thái (ví dụ: không thể chuyển từ ASSIGNED trực tiếp sang DELIVERED).

3. **Vòng Đời Mã QR**:
   - Mã QR được tạo khi quản lý chấp nhận đơn hàng
   - Mã QR được sao chép vào xác nhận giao hàng khi nhân viên giao hàng tạo xác nhận
   - Mã QR chỉ có thể quét một lần bởi khách hàng

4. **Định Dạng Ngày**: Tất cả ngày tháng ở định dạng ISO 8601: `yyyy-MM-ddTHH:mm:ss` (ví dụ: `2025-11-15T10:00:00`)

5. **Trường Tùy Chọn**: 
   - `deliveryStaffId` trong request phân công là tùy chọn (có thể phân công sau)
   - `estimatedDeliveryDate` là tùy chọn
   - `notes` là tùy chọn trong hầu hết các request

6. **Tải Ảnh Lên**: `deliveryPhotos` nên là mảng URL ảnh (tải ảnh riêng biệt và truyền URL)

7. **Kiểm Tra Tồn Kho**: Khi chuẩn bị sản phẩm, hệ thống tự động kiểm tra tồn kho. Nếu không đủ, thông báo lỗi sẽ chứa chi tiết sản phẩm thiếu.

---

## 8. TÀI KHOẢN TEST

Xem `test-data/TEST_ACCOUNTS.md` để biết thông tin đăng nhập tài khoản test.

**Tài khoản test được khuyến nghị**:
- **STAFF**: `staff@furnimart.com` / `Staff@123`
- **BRANCH_MANAGER**: `branchmanager@furnimart.com` / `BranchManager@123`
- **DELIVERY**: `delivery@furnimart.com` / `Delivery@123`
- **CUSTOMER**: `customer@gmail.com` / `customer123`

---

## 9. BẢNG TÓM TẮT API

| Endpoint | Method | Vai Trò | Mô Tả |
|----------|--------|---------|-------|
| `/api/delivery/assign` | POST | STAFF, BRANCH_MANAGER | Phân công đơn hàng cho nhân viên giao hàng |
| `/api/delivery/generate-invoice/{orderId}` | POST | STAFF | Tạo hóa đơn |
| `/api/delivery/prepare-products` | POST | STAFF | Chuẩn bị sản phẩm (kiểm tra tồn kho) |
| `/api/delivery/assignments/store/{storeId}` | GET | STAFF, BRANCH_MANAGER | Lấy phân công theo cửa hàng |
| `/api/delivery/assignments/order/{orderId}` | GET | STAFF, BRANCH_MANAGER | Lấy phân công theo đơn hàng |
| `/api/delivery/assignments/{id}/status` | PUT | BRANCH_MANAGER, DELIVERY | Cập nhật trạng thái giao hàng |
| `/api/delivery/assignments/staff/{staffId}` | GET | DELIVERY | Lấy phân công theo nhân viên |
| `/api/delivery/progress/store/{storeId}` | GET | BRANCH_MANAGER | Theo dõi tiến độ giao hàng |
| `/api/delivery/stores/{storeId}/branch-info` | GET | PUBLIC | Lấy thông tin chi nhánh cửa hàng |
| `/api/delivery-confirmations` | POST | DELIVERY | Tạo xác nhận giao hàng |
| `/api/delivery-confirmations/scan-qr` | POST | CUSTOMER | Quét mã QR |
| `/api/delivery-confirmations/order/{orderId}` | GET | CUSTOMER, ADMIN | Lấy xác nhận theo đơn hàng |
| `/api/delivery-confirmations/qr/{qrCode}` | GET | CUSTOMER, ADMIN | Lấy xác nhận theo mã QR |
| `/api/delivery-confirmations/customer/{customerId}` | GET | CUSTOMER, ADMIN | Lấy xác nhận theo khách hàng |
| `/api/delivery-confirmations/staff/{staffId}` | GET | DELIVERY, ADMIN | Lấy xác nhận theo nhân viên |

---

## 10. SWAGGER UI

Tài liệu API đầy đủ với tính năng test tương tác có sẵn tại:
- **Production**: `http://152.53.227.115:8089/swagger-ui/index.html`
- **Local**: `http://localhost:8089/swagger-ui/index.html`

---

**Cập Nhật Lần Cuối**: 2025-11-13
**Phiên Bản**: 1.0

