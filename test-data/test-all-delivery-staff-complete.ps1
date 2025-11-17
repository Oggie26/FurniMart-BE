# Script Test Đầy Đủ: Tất Cả DELIVERY và STAFF Functions
# PowerShell Script for Windows

# ============================================
# CONFIGURATION
# ============================================
$BASE_URL = "http://152.53.227.115:8086"  # Gateway URL
$DELIVERY_SERVICE_URL = "http://152.53.227.115:8089"  # Direct Delivery Service URL
$USER_SERVICE_URL = "http://152.53.227.115:8086"  # User Service URL (via Gateway)
$ORDER_SERVICE_URL = "http://152.53.227.115:8087"  # Order Service URL (via Gateway)

# Test credentials
$STAFF_EMAIL = "staff@furnimart.com"
$STAFF_PASSWORD = "Staff@123"
$DELIVERY_EMAIL = "delivery@furnimart.com"
$DELIVERY_PASSWORD = "Delivery@123"

# Import helper functions
$helpersPath = Join-Path $PSScriptRoot "delivery-test-helpers.ps1"
if (Test-Path $helpersPath) {
    . $helpersPath
    Write-Host "Loaded helper functions" -ForegroundColor Green
}

# ============================================
# FUNCTIONS
# ============================================

function Write-TestHeader {
    param($Title)
    Write-Host "`n========================================" -ForegroundColor Cyan
    Write-Host $Title -ForegroundColor Cyan
    Write-Host "========================================`n" -ForegroundColor Cyan
}

function Write-Success {
    param($Message)
    Write-Host "OK $Message" -ForegroundColor Green
}

function Write-Error {
    param($Message)
    Write-Host "ERROR $Message" -ForegroundColor Red
}

function Write-Info {
    param($Message)
    Write-Host "INFO $Message" -ForegroundColor Yellow
}

# ============================================
# PART 1: STAFF FUNCTIONS TEST
# ============================================
Write-TestHeader "PART 1: TEST STAFF FUNCTIONS"

# Login as STAFF
Write-Info "Dang nhap voi tai khoan STAFF..."
$staffLoginBody = @{
    email = $STAFF_EMAIL
    password = $STAFF_PASSWORD
} | ConvertTo-Json

try {
    $staffLoginResponse = Invoke-RestMethod -Uri "$BASE_URL/api/auth/login" `
        -Method POST `
        -Body $staffLoginBody `
        -ContentType "application/json"
    
    $STAFF_TOKEN = $staffLoginResponse.data.token
    Write-Success "Dang nhap STAFF thanh cong!"
} catch {
    Write-Error "Dang nhap STAFF that bai: $($_.Exception.Message)"
    exit 1
}

# Get stores
Write-Info "Lay danh sach stores..."
try {
    $storesResponse = Invoke-RestMethod -Uri "$USER_SERVICE_URL/api/stores" `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $STAFF_TOKEN"}
    
    if ($storesResponse.data -and $storesResponse.data.Count -gt 0) {
        $STORE_ID = $storesResponse.data[0].id
        Write-Success "Store ID: $STORE_ID"
    } else {
        Write-Error "Khong tim thay store nao"
        exit 1
    }
} catch {
    Write-Error "Lay stores that bai: $($_.Exception.Message)"
    exit 1
}

# Get orders
Write-Info "Lay danh sach orders..."
$ORDER_ID = $null
try {
    $ordersUrl = $ORDER_SERVICE_URL + "/api/orders/search?keyword=&page=0&size=10"
    $ordersResponse = Invoke-RestMethod -Uri $ordersUrl `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $STAFF_TOKEN"}
    
    if ($ordersResponse.data -and $ordersResponse.data.content -and $ordersResponse.data.content.Count -gt 0) {
        $ORDER_ID = $ordersResponse.data.content[0].id
        Write-Success "Order ID: $ORDER_ID"
    } else {
        Write-Info "Khong tim thay order. Su dung order ID mac dinh: 1"
        $ORDER_ID = 1
    }
} catch {
    Write-Info "Lay orders that bai (co the loi 500). Su dung order ID mac dinh: 1"
    $ORDER_ID = 1
}

# Test 1: Get assignment by order ID
Write-Info "Test 1: Lay assignment theo order ID..."
try {
    $assignmentResponse = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/assignments/order/$ORDER_ID" `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $STAFF_TOKEN"}
    
    Write-Success "Lay assignment thanh cong!"
    Write-Host "  Assignment ID: $($assignmentResponse.data.id)" -ForegroundColor Cyan
    Write-Host "  Status: $($assignmentResponse.data.status)" -ForegroundColor Cyan
    Write-Host "  Invoice Generated: $($assignmentResponse.data.invoiceGenerated)" -ForegroundColor Cyan
    Write-Host "  Products Prepared: $($assignmentResponse.data.productsPrepared)" -ForegroundColor Cyan
    $ASSIGNMENT_ID = $assignmentResponse.data.id
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 404) {
        Write-Info "Order chua duoc assign (404). Day la expected neu order chua co assignment."
    } else {
        Write-Error "Lay assignment that bai: $($_.Exception.Message)"
    }
}

# Test 2: Get assignments by store
Write-Info "Test 2: Lay danh sach assignments theo store..."
try {
    $assignmentsResponse = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/assignments/store/$STORE_ID" `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $STAFF_TOKEN"}
    
    Write-Success "Lay danh sach assignments thanh cong!"
    Write-Host "  So luong: $($assignmentsResponse.data.Count)" -ForegroundColor Cyan
} catch {
    Write-Error "Lay assignments that bai: $($_.Exception.Message)"
}

# Test 3: Safe Assign Order (using helper)
if ($ORDER_ID -and $STORE_ID) {
    Write-Info "Test 3: Safe Assign Order..."
    $assignResult = Invoke-AssignOrderSafely -OrderId $ORDER_ID -StoreId $STORE_ID -Token $STAFF_TOKEN
    if ($assignResult) {
        if ($assignResult.Success) {
            Write-Success "Assign order thanh cong!"
            $ASSIGNMENT_ID = $assignResult.Assignment.id
        } elseif ($assignResult.AlreadyExists) {
            Write-Info "Order da duoc assign. Assignment ID: $($assignResult.Assignment.id)"
            $ASSIGNMENT_ID = $assignResult.Assignment.id
        }
    }
}

# Test 4: Safe Generate Invoice (using helper)
if ($ORDER_ID) {
    Write-Info "Test 4: Safe Generate Invoice..."
    $invoiceResult = Invoke-GenerateInvoiceSafely -OrderId $ORDER_ID -Token $STAFF_TOKEN
    if ($invoiceResult) {
        if ($invoiceResult.Success) {
            Write-Success "Generate invoice thanh cong!"
        } elseif ($invoiceResult.AlreadyGenerated) {
            Write-Info "Invoice da duoc generate roi."
        }
    }
}

# Test 5: Safe Prepare Products (using helper)
if ($ORDER_ID) {
    Write-Info "Test 5: Safe Prepare Products..."
    $prepareResult = Invoke-PrepareProductsSafely -OrderId $ORDER_ID -Token $STAFF_TOKEN -Notes "Prepared by test script"
    if ($prepareResult) {
        if ($prepareResult.Success) {
            Write-Success "Prepare products thanh cong!"
        } elseif ($prepareResult.AlreadyPrepared) {
            Write-Info "Products da duoc prepare roi."
        }
    }
}

# ============================================
# PART 2: DELIVERY FUNCTIONS TEST
# ============================================
Write-TestHeader "PART 2: TEST DELIVERY FUNCTIONS"

# Login as DELIVERY
Write-Info "Dang nhap voi tai khoan DELIVERY..."
$deliveryLoginBody = @{
    email = $DELIVERY_EMAIL
    password = $DELIVERY_PASSWORD
} | ConvertTo-Json

try {
    $deliveryLoginResponse = Invoke-RestMethod -Uri "$BASE_URL/api/auth/login" `
        -Method POST `
        -Body $deliveryLoginBody `
        -ContentType "application/json"
    
    $DELIVERY_TOKEN = $deliveryLoginResponse.data.token
    Write-Success "Dang nhap DELIVERY thanh cong!"
} catch {
    Write-Error "Dang nhap DELIVERY that bai: $($_.Exception.Message)"
    exit 1
}

# Get Delivery Staff ID
Write-Info "Lay Delivery Staff ID..."
try {
    $userResponse = Invoke-RestMethod -Uri "$USER_SERVICE_URL/api/employees/email/$DELIVERY_EMAIL" `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $DELIVERY_TOKEN"}
    $DELIVERY_STAFF_ID = $userResponse.data.id
    Write-Success "Delivery Staff ID: $DELIVERY_STAFF_ID"
} catch {
    Write-Error "Khong the lay Delivery Staff ID: $($_.Exception.Message)"
    exit 1
}

# Test 1: Get assignments by staff
Write-Info "Test 1: Lay danh sach assignments cua delivery staff..."
try {
    $assignmentsResponse = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/assignments/staff/$DELIVERY_STAFF_ID" `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $DELIVERY_TOKEN"}
    
    Write-Success "Lay danh sach assignments thanh cong!"
    Write-Host "  So luong: $($assignmentsResponse.data.Count)" -ForegroundColor Cyan
    
    if ($assignmentsResponse.data.Count -gt 0) {
        $ASSIGNMENT_ID = $assignmentsResponse.data[0].id
        $ORDER_ID = $assignmentsResponse.data[0].orderId
        Write-Host "  Assignment ID: $ASSIGNMENT_ID" -ForegroundColor Cyan
        Write-Host "  Order ID: $ORDER_ID" -ForegroundColor Cyan
        Write-Host "  Status: $($assignmentsResponse.data[0].status)" -ForegroundColor Cyan
    }
} catch {
    Write-Error "Lay assignments that bai: $($_.Exception.Message)"
}

# Test 2: Update delivery status
if ($ASSIGNMENT_ID) {
    Write-Info "Test 2: Update delivery status..."
    try {
        $currentStatus = $assignmentsResponse.data[0].status
        $newStatus = "IN_TRANSIT"
        if ($currentStatus -eq "ASSIGNED") { $newStatus = "PREPARING" }
        if ($currentStatus -eq "PREPARING") { $newStatus = "READY" }
        if ($currentStatus -eq "READY") { $newStatus = "IN_TRANSIT" }
        if ($currentStatus -eq "IN_TRANSIT") { $newStatus = "DELIVERED" }
        
        $updateResponse = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/assignments/$ASSIGNMENT_ID/status?status=$newStatus" `
            -Method PUT `
            -Headers @{"Authorization" = "Bearer $DELIVERY_TOKEN"}
        
        Write-Success "Update status thanh cong!"
        Write-Host "  Old status: $currentStatus" -ForegroundColor Cyan
        Write-Host "  New status: $newStatus" -ForegroundColor Cyan
    } catch {
        Write-Error "Update status that bai: $($_.Exception.Message)"
    }
}

# Test 3: Create delivery confirmation
if ($ORDER_ID -and $ASSIGNMENT_ID) {
    Write-Info "Test 3: Tao delivery confirmation..."
    $confirmationBody = @{
        orderId = $ORDER_ID
        deliveryPhotos = @()
        deliveryNotes = "Da giao hang thanh cong. Khach hang da nhan hang."
        deliveryLatitude = 10.762622
        deliveryLongitude = 106.660172
        deliveryAddress = "123 Test Street, Ho Chi Minh City"
    } | ConvertTo-Json
    
    try {
        $confirmationResponse = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery-confirmations" `
            -Method POST `
            -Body $confirmationBody `
            -ContentType "application/json" `
            -Headers @{"Authorization" = "Bearer $DELIVERY_TOKEN"}
        
        Write-Success "Tao delivery confirmation thanh cong!"
        Write-Host "  Confirmation ID: $($confirmationResponse.data.id)" -ForegroundColor Cyan
    } catch {
        Write-Error "Tao confirmation that bai: $($_.Exception.Message)"
        if ($_.ErrorDetails.Message) {
            Write-Host "  Chi tiet: $($_.ErrorDetails.Message)" -ForegroundColor Red
        }
    }
}

# Test 4: Get delivery confirmations by staff
Write-Info "Test 4: Lay danh sach confirmations cua delivery staff..."
try {
    $confirmationsResponse = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery-confirmations/staff/$DELIVERY_STAFF_ID" `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $DELIVERY_TOKEN"}
    
    Write-Success "Lay danh sach confirmations thanh cong!"
    Write-Host "  So luong: $($confirmationsResponse.data.Count)" -ForegroundColor Cyan
} catch {
    Write-Error "Lay confirmations that bai: $($_.Exception.Message)"
}

# Test 5: Get delivery confirmation by order ID
if ($ORDER_ID) {
    Write-Info "Test 5: Lay confirmation theo order ID..."
    try {
        $confirmationByOrderResponse = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery-confirmations/order/$ORDER_ID" `
            -Method GET `
            -Headers @{"Authorization" = "Bearer $DELIVERY_TOKEN"}
        
        Write-Success "Lay confirmation thanh cong!"
    } catch {
        Write-Info "Lay confirmation that bai (co the chua co confirmation): $($_.Exception.Message)"
    }
}

# ============================================
# SUMMARY
# ============================================
Write-TestHeader "TEST SUMMARY"

Write-Host "STAFF Functions da test:" -ForegroundColor Cyan
Write-Host "  1. Get assignment by order ID" -ForegroundColor Green
Write-Host "  2. Get assignments by store" -ForegroundColor Green
Write-Host "  3. Safe assign order" -ForegroundColor Green
Write-Host "  4. Safe generate invoice" -ForegroundColor Green
Write-Host "  5. Safe prepare products" -ForegroundColor Green

Write-Host "`nDELIVERY Functions da test:" -ForegroundColor Cyan
Write-Host "  1. Get assignments by staff" -ForegroundColor Green
Write-Host "  2. Update delivery status" -ForegroundColor Green
Write-Host "  3. Create delivery confirmation" -ForegroundColor Green
Write-Host "  4. Get confirmations by staff" -ForegroundColor Green
Write-Host "  5. Get confirmation by order ID" -ForegroundColor Green

Write-Host "`nHoan thanh test day du!" -ForegroundColor Green

