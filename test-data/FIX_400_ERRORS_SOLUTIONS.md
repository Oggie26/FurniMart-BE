# Giải Pháp Sửa Lỗi 400 Bad Request

## 📋 Tổng Quan

Tài liệu này đưa ra các giải pháp cụ thể để sửa và tránh các lỗi 400 Bad Request trong delivery service.

---

## 🔴 Vấn Đề 1: CODE_EXISTED - Order Đã Được Assign/Prepare/Generate Invoice

### Nguyên Nhân:
- Mỗi order chỉ có thể được assign/prepare/generate invoice **1 lần duy nhất**
- Nếu thực hiện lại sẽ trả về lỗi 400 với message "Code has existed"

### Giải Pháp:

#### 1. **Phía Client (Frontend/API Caller):**

##### a) Kiểm Tra Trước Khi Thực Hiện:
```powershell
# Ví dụ: Kiểm tra assignment trước khi assign
function Test-AssignOrder {
    param($OrderId, $StoreId, $Token)
    
    # Bước 1: Kiểm tra order đã được assign chưa
    try {
        $existingAssignment = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/assignments/order/$OrderId" `
            -Method GET `
            -Headers @{"Authorization" = "Bearer $Token"}
        
        Write-Host "Order đã được assign rồi!" -ForegroundColor Yellow
        Write-Host "Assignment ID: $($existingAssignment.data.id)" -ForegroundColor Cyan
        Write-Host "Status: $($existingAssignment.data.status)" -ForegroundColor Cyan
        return $false  # Không thể assign lại
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -eq 404) {
            # Order chưa được assign → Có thể assign
            Write-Host "Order chưa được assign. Tiến hành assign..." -ForegroundColor Green
            return $true
        } else {
            Write-Host "Lỗi khi kiểm tra: $($_.Exception.Message)" -ForegroundColor Red
            return $false
        }
    }
}
```

##### b) Xử Lý Lỗi 400 Một Cách Thân Thiện:
```powershell
function Invoke-AssignOrder {
    param($OrderId, $StoreId, $Token)
    
    try {
        $response = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/assign" `
            -Method POST `
            -Body (@{orderId=$OrderId; storeId=$StoreId} | ConvertTo-Json) `
            -ContentType "application/json" `
            -Headers @{"Authorization" = "Bearer $Token"}
        
        Write-Host "Assign thành công!" -ForegroundColor Green
        return $response
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -eq 400) {
            $errorBody = $_.ErrorDetails.Message | ConvertFrom-Json
            if ($errorBody.message -like "*existed*") {
                Write-Host "Order đã được assign rồi. Vui lòng sử dụng order khác." -ForegroundColor Yellow
                # Có thể tự động lấy assignment hiện tại
                $existingAssignment = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/assignments/order/$OrderId" `
                    -Method GET `
                    -Headers @{"Authorization" = "Bearer $Token"}
                return $existingAssignment
            }
        }
        throw
    }
}
```

#### 2. **Phía Server (Backend):**

##### a) Cải Thiện Error Message:
```java
// File: DeliveryServiceImpl.java

// Thay vì:
if (assignment.getInvoiceGenerated()) {
    throw new AppException(ErrorCode.CODE_EXISTED);
}

// Nên sửa thành:
if (assignment.getInvoiceGenerated()) {
    throw new AppException(
        ErrorCode.CODE_EXISTED, 
        "Invoice đã được generate cho order này. Assignment ID: " + assignment.getId()
    );
}
```

##### b) Tạo Error Code Riêng Cho Từng Trường Hợp:
```java
// File: ErrorCode.java

// Thêm các error codes mới:
ASSIGNMENT_ALREADY_EXISTS(1224, "Order đã được assign. Assignment ID: {0}", HttpStatus.BAD_REQUEST),
INVOICE_ALREADY_GENERATED(1225, "Invoice đã được generate cho order này", HttpStatus.BAD_REQUEST),
PRODUCTS_ALREADY_PREPARED(1226, "Products đã được prepare cho order này", HttpStatus.BAD_REQUEST),
```

##### c) Trả Về Thông Tin Hữu Ích Hơn:
```java
// File: DeliveryServiceImpl.java

@Override
@Transactional
public DeliveryAssignmentResponse assignOrderToDelivery(AssignOrderRequest request) {
    log.info("Assigning order {} to delivery", request.getOrderId());

    // Check if order already assigned
    Optional<DeliveryAssignment> existingAssignment = 
        deliveryAssignmentRepository.findByOrderIdAndIsDeletedFalse(request.getOrderId());
    
    if (existingAssignment.isPresent()) {
        DeliveryAssignment assignment = existingAssignment.get();
        throw new AppException(
            ErrorCode.ASSIGNMENT_ALREADY_EXISTS,
            String.format("Order đã được assign. Assignment ID: %d, Status: %s", 
                assignment.getId(), assignment.getStatus())
        );
    }
    
    // ... rest of the code
}
```

---

## 🔴 Vấn Đề 2: INVALID_REQUEST - Stock Không Đủ

### Nguyên Nhân:
- Khi prepare products, số lượng sản phẩm trong kho < số lượng order yêu cầu

### Giải Pháp:

#### 1. **Phía Client:**

##### a) Kiểm Tra Stock Trước Khi Prepare:
```powershell
function Test-PrepareProducts {
    param($OrderId, $Token)
    
    # Bước 1: Lấy thông tin order
    $order = Invoke-RestMethod -Uri "$ORDER_SERVICE_URL/api/orders/$OrderId" `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $Token"}
    
    # Bước 2: Kiểm tra stock cho từng sản phẩm
    $canPrepare = $true
    $insufficientProducts = @()
    
    foreach ($detail in $order.data.orderDetails) {
        $stockResponse = Invoke-RestMethod -Uri "$INVENTORY_SERVICE_URL/api/inventories/stock/total-available?productColorId=$($detail.productColorId)" `
            -Method GET `
            -Headers @{"Authorization" = "Bearer $Token"}
        
        $availableStock = $stockResponse.data
        if ($availableStock -lt $detail.quantity) {
            $canPrepare = $false
            $insufficientProducts += @{
                ProductColorId = $detail.productColorId
                Required = $detail.quantity
                Available = $availableStock
                Shortage = $detail.quantity - $availableStock
            }
        }
    }
    
    if (-not $canPrepare) {
        Write-Host "Không thể prepare products. Thiếu stock:" -ForegroundColor Red
        $insufficientProducts | ForEach-Object {
            Write-Host "  - Product: $($_.ProductColorId)" -ForegroundColor Yellow
            Write-Host "    Required: $($_.Required), Available: $($_.Available), Shortage: $($_.Shortage)" -ForegroundColor Yellow
        }
        return $false
    }
    
    Write-Host "Stock đủ. Có thể prepare products." -ForegroundColor Green
    return $true
}
```

#### 2. **Phía Server:**

##### a) Cải Thiện Error Message Với Chi Tiết:
```java
// File: DeliveryServiceImpl.java

@Override
@Transactional
public DeliveryAssignmentResponse prepareProducts(PrepareProductsRequest request) {
    log.info("Preparing products for order: {}", request.getOrderId());

    DeliveryAssignment assignment = deliveryAssignmentRepository
        .findByOrderIdAndIsDeletedFalse(request.getOrderId())
        .orElseThrow(() -> new AppException(ErrorCode.CODE_NOT_FOUND));

    if (assignment.getProductsPrepared()) {
        throw new AppException(ErrorCode.PRODUCTS_ALREADY_PREPARED);
    }

    ResponseEntity<ApiResponse<OrderResponse>> orderResponse = orderClient.getOrderById(request.getOrderId());
    if (orderResponse.getBody() == null || orderResponse.getBody().getData() == null) {
        throw new AppException(ErrorCode.CODE_NOT_FOUND);
    }

    OrderResponse order = orderResponse.getBody().getData();
    
    // Check stock availability for each product in order
    List<String> insufficientProducts = new ArrayList<>();
    
    if (order.getOrderDetails() != null) {
        for (OrderDetailResponse detail : order.getOrderDetails()) {
            ApiResponse<Integer> stockResponse = inventoryClient.getTotalAvailableStock(detail.getProductColorId());
            if (stockResponse != null && stockResponse.getData() != null) {
                int availableStock = stockResponse.getData();
                if (availableStock < detail.getQuantity()) {
                    insufficientProducts.add(String.format(
                        "Product %s: Required %d, Available %d, Shortage %d",
                        detail.getProductColorId(),
                        detail.getQuantity(),
                        availableStock,
                        detail.getQuantity() - availableStock
                    ));
                }
            }
        }
    }
    
    if (!insufficientProducts.isEmpty()) {
        String errorMessage = "Stock không đủ cho các sản phẩm sau:\n" + 
            String.join("\n", insufficientProducts);
        throw new AppException(ErrorCode.INSUFFICIENT_STOCK, errorMessage);
    }

    assignment.setProductsPrepared(true);
    assignment.setProductsPreparedAt(LocalDateTime.now());
    assignment.setStatus(DeliveryStatus.READY);

    DeliveryAssignment saved = deliveryAssignmentRepository.save(assignment);
    log.info("Products prepared for order: {}", request.getOrderId());

    return mapToResponse(saved);
}
```

##### b) Thêm Error Code Mới:
```java
// File: ErrorCode.java

INSUFFICIENT_STOCK(1227, "Stock không đủ", HttpStatus.BAD_REQUEST),
```

---

## 🔴 Vấn Đề 3: Validation Errors (@NotNull)

### Nguyên Nhân:
- Thiếu các trường bắt buộc trong request body

### Giải Pháp:

#### 1. **Phía Client:**

##### a) Validate Request Trước Khi Gửi:
```powershell
function Test-ValidateAssignOrderRequest {
    param($OrderId, $StoreId)
    
    $errors = @()
    
    if (-not $OrderId) {
        $errors += "Order ID is required"
    } elseif ($OrderId -le 0) {
        $errors += "Order ID must be positive"
    }
    
    if (-not $StoreId) {
        $errors += "Store ID is required"
    } elseif ($StoreId -notmatch "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$") {
        $errors += "Store ID must be a valid UUID"
    }
    
    if ($errors.Count -gt 0) {
        Write-Host "Validation errors:" -ForegroundColor Red
        $errors | ForEach-Object { Write-Host "  - $_" -ForegroundColor Yellow }
        return $false
    }
    
    return $true
}
```

#### 2. **Phía Server:**

##### a) Cải Thiện Validation Messages:
```java
// File: AssignOrderRequest.java

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignOrderRequest {
    @NotNull(message = "Order ID không được để trống")
    @Min(value = 1, message = "Order ID phải lớn hơn 0")
    private Long orderId;
    
    @NotNull(message = "Store ID không được để trống")
    @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", 
             message = "Store ID phải là UUID hợp lệ")
    private String storeId;
    
    private String deliveryStaffId; // Optional
    
    @Future(message = "Estimated delivery date phải là ngày trong tương lai")
    private LocalDateTime estimatedDeliveryDate;
    
    @Size(max = 500, message = "Notes không được vượt quá 500 ký tự")
    private String notes;
}
```

---

## 🎯 Giải Pháp Tổng Thể

### 1. **Tạo Helper Functions:**

```powershell
# File: delivery-test-helpers.ps1

function Get-AssignmentStatus {
    param($OrderId, $Token)
    # Kiểm tra và trả về trạng thái assignment
}

function Test-CanAssignOrder {
    param($OrderId, $Token)
    # Kiểm tra có thể assign order không
}

function Test-CanPrepareProducts {
    param($OrderId, $Token)
    # Kiểm tra có thể prepare products không
}

function Test-CanGenerateInvoice {
    param($OrderId, $Token)
    # Kiểm tra có thể generate invoice không
}
```

### 2. **Cải Thiện Error Handling:**

```java
// File: GlobalExceptionHandler.java (nếu có)

@ExceptionHandler(AppException.class)
public ResponseEntity<ApiResponse<Void>> handleAppException(AppException e) {
    ApiResponse<Void> response = ApiResponse.<Void>builder()
        .status(e.getErrorCode().getStatusCode().value())
        .message(e.getMessage())  // Sử dụng message chi tiết
        .build();
    return ResponseEntity.status(e.getErrorCode().getStatusCode()).body(response);
}
```

### 3. **Tạo Pre-flight Checks:**

```powershell
# File: pre-flight-checks.ps1

function Invoke-AssignOrderWithChecks {
    param($OrderId, $StoreId, $Token)
    
    # Pre-flight check 1: Validate request
    if (-not (Test-ValidateAssignOrderRequest -OrderId $OrderId -StoreId $StoreId)) {
        return $null
    }
    
    # Pre-flight check 2: Check if already assigned
    if (-not (Test-CanAssignOrder -OrderId $OrderId -Token $Token)) {
        return $null
    }
    
    # Pre-flight check 3: Verify order exists
    # ...
    
    # All checks passed, proceed with assignment
    return Invoke-AssignOrder -OrderId $OrderId -StoreId $StoreId -Token $Token
}
```

---

## 📝 Checklist Cải Thiện

### Phía Client:
- [ ] Thêm validation trước khi gọi API
- [ ] Kiểm tra trạng thái trước khi thực hiện operation
- [ ] Xử lý lỗi 400 một cách thân thiện
- [ ] Hiển thị thông báo rõ ràng cho user

### Phía Server:
- [ ] Cải thiện error messages (chi tiết hơn)
- [ ] Tạo error codes riêng cho từng trường hợp
- [ ] Trả về thông tin hữu ích (assignment ID, status, etc.)
- [ ] Validate request đầy đủ với messages rõ ràng

---

## 🚀 Kết Luận

Các giải pháp trên sẽ giúp:
1. ✅ **Tránh lỗi 400** bằng cách kiểm tra trước
2. ✅ **Xử lý lỗi tốt hơn** với messages rõ ràng
3. ✅ **Cải thiện UX** với thông báo thân thiện
4. ✅ **Dễ debug** với error messages chi tiết

**Ưu tiên**: Implement các giải pháp phía client trước (dễ hơn), sau đó cải thiện phía server.

