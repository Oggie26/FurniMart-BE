# Script Test: STAFF Remaining Functions
# PowerShell Script for Windows

# ============================================
# CONFIGURATION
# ============================================
$BASE_URL = "http://152.53.227.115:8086"  # Gateway URL
$DELIVERY_SERVICE_URL = "http://152.53.227.115:8089"  # Direct Delivery Service URL
$USER_SERVICE_URL = "http://152.53.227.115:8086"  # User Service URL (via Gateway)

$STAFF_EMAIL = "staff@furnimart.com"
$STAFF_PASSWORD = "Staff@123"

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
# STEP 1: LOGIN AS STAFF
# ============================================
Write-TestHeader "STEP 1: Dang Nhap Voi Tai Khoan STAFF"

$loginBody = @{
    email = $STAFF_EMAIL
    password = $STAFF_PASSWORD
} | ConvertTo-Json

try {
    Write-Info "Dang dang nhap voi email: $STAFF_EMAIL"
    $loginResponse = Invoke-RestMethod -Uri "$BASE_URL/api/auth/login" `
        -Method POST `
        -Body $loginBody `
        -ContentType "application/json"
    
    $TOKEN = $loginResponse.data.token
    Write-Success "Dang nhap thanh cong!"
    Write-Info "Token: $($TOKEN.Substring(0, 50))..."
} catch {
    Write-Error "Dang nhap that bai: $($_.Exception.Message)"
    if ($_.ErrorDetails.Message) {
        Write-Error "Chi tiet: $($_.ErrorDetails.Message)"
    }
    exit 1
}

# ============================================
# STEP 2: GET STORES
# ============================================
Write-TestHeader "STEP 2: Lay Danh Sach Stores"

$STORE_ID = $null
try {
    $storesResponse = Invoke-RestMethod -Uri "$USER_SERVICE_URL/api/stores" `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $TOKEN"}
    
    if ($storesResponse.data -and $storesResponse.data.Count -gt 0) {
        $STORE_ID = $storesResponse.data[0].id
        Write-Success "Store ID: $STORE_ID"
        Write-Info "Store name: $($storesResponse.data[0].name)"
    } else {
        Write-Error "Khong tim thay store nao"
        exit 1
    }
} catch {
    Write-Error "Lay danh sach stores that bai: $($_.Exception.Message)"
    exit 1
}

# ============================================
# STEP 3: GET DELIVERY ASSIGNMENT BY ORDER ID
# ============================================
Write-TestHeader "STEP 3: Lay Delivery Assignment Theo Order ID"

# Sử dụng order ID từ test trước hoặc mặc định
$ORDER_ID = 1  # Có thể thay đổi

try {
    Write-Info "Dang lay assignment cho order #$ORDER_ID..."
    $assignmentResponse = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/assignments/order/$ORDER_ID" `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $TOKEN"}
    
    Write-Success "Lay assignment thanh cong!"
    Write-Host "`nAssignment Details:" -ForegroundColor Cyan
    $assignmentResponse | ConvertTo-Json -Depth 10 | Write-Host
    
    Write-Info "Assignment ID: $($assignmentResponse.data.id)"
    Write-Info "Order ID: $($assignmentResponse.data.orderId)"
    Write-Info "Status: $($assignmentResponse.data.status)"
    Write-Info "Invoice Generated: $($assignmentResponse.data.invoiceGenerated)"
    Write-Info "Products Prepared: $($assignmentResponse.data.productsPrepared)"
    
    $ASSIGNMENT_ID = $assignmentResponse.data.id
    $ORDER_ID = $assignmentResponse.data.orderId
    
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 404) {
        Write-Info "Order chua duoc assign (404 Not Found). Day la expected neu order chua co assignment."
    } else {
        Write-Error "Lay assignment that bai: $($_.Exception.Message)"
        if ($_.ErrorDetails.Message) {
            Write-Error "Chi tiet: $($_.ErrorDetails.Message)"
        }
    }
    $ASSIGNMENT_ID = $null
}

# ============================================
# STEP 4: GET DELIVERY ASSIGNMENTS BY STORE
# ============================================
Write-TestHeader "STEP 4: Lay Danh Sach Assignments Theo Store"

if ($STORE_ID) {
    try {
        Write-Info "Dang lay danh sach assignments trong store $STORE_ID..."
        $assignmentsResponse = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/assignments/store/$STORE_ID" `
            -Method GET `
            -Headers @{"Authorization" = "Bearer $TOKEN"}
        
        Write-Success "Lay danh sach assignments thanh cong!"
        Write-Host "`nSo luong assignments: $($assignmentsResponse.data.Count)" -ForegroundColor Cyan
        
        if ($assignmentsResponse.data.Count -gt 0) {
            Write-Host "`nDanh sach assignments:" -ForegroundColor Cyan
            $assignmentsResponse.data | ForEach-Object {
                Write-Host "  - Assignment ID: $($_.id)" -ForegroundColor Yellow
                Write-Host "    Order ID: $($_.orderId)" -ForegroundColor Yellow
                Write-Host "    Status: $($_.status)" -ForegroundColor Yellow
                Write-Host "    Invoice Generated: $($_.invoiceGenerated)" -ForegroundColor Yellow
                Write-Host "    Products Prepared: $($_.productsPrepared)" -ForegroundColor Yellow
                Write-Host ""
            }
            
            # Lấy order ID đầu tiên nếu chưa có
            if (-not $ORDER_ID) {
                $ORDER_ID = $assignmentsResponse.data[0].orderId
                $ASSIGNMENT_ID = $assignmentsResponse.data[0].id
            }
        } else {
            Write-Info "Khong co assignment nao trong store nay"
        }
    } catch {
        Write-Error "Lay danh sach assignments that bai: $($_.Exception.Message)"
        if ($_.ErrorDetails.Message) {
            Write-Error "Chi tiet: $($_.ErrorDetails.Message)"
        }
    }
} else {
    Write-Error "Khong co store ID de test"
}

# ============================================
# STEP 5: GENERATE INVOICE (Nếu chưa generate)
# ============================================
Write-TestHeader "STEP 5: Generate Invoice Cho Order"

if ($ORDER_ID) {
    try {
        Write-Info "Dang generate invoice cho order #$ORDER_ID..."
        
        # Kiểm tra xem invoice đã được generate chưa
        try {
            $checkAssignment = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/assignments/order/$ORDER_ID" `
                -Method GET `
                -Headers @{"Authorization" = "Bearer $TOKEN"}
            
            if ($checkAssignment.data.invoiceGenerated) {
                Write-Info "Invoice da duoc generate roi. Bo qua buoc nay."
            } else {
                $invoiceResponse = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/generate-invoice/$ORDER_ID" `
                    -Method POST `
                    -Headers @{"Authorization" = "Bearer $TOKEN"}
                
                Write-Success "Generate invoice thanh cong!"
                Write-Host "`nResponse:" -ForegroundColor Cyan
                $invoiceResponse | ConvertTo-Json -Depth 10 | Write-Host
            }
        } catch {
            Write-Info "Khong the kiem tra assignment. Thu generate invoice..."
            $invoiceResponse = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/generate-invoice/$ORDER_ID" `
                -Method POST `
                -Headers @{"Authorization" = "Bearer $TOKEN"}
            
            Write-Success "Generate invoice thanh cong!"
            Write-Host "`nResponse:" -ForegroundColor Cyan
            $invoiceResponse | ConvertTo-Json -Depth 10 | Write-Host
        }
    } catch {
        Write-Error "Generate invoice that bai: $($_.Exception.Message)"
        if ($_.ErrorDetails.Message) {
            try {
                $errorDetails = $_.ErrorDetails.Message | ConvertFrom-Json
                Write-Host "`nError Details:" -ForegroundColor Red
                $errorDetails | ConvertTo-Json -Depth 5 | Write-Host
            } catch {
                Write-Host "Error Details: $($_.ErrorDetails.Message)" -ForegroundColor Red
            }
        }
    }
} else {
    Write-Info "Khong co order ID de generate invoice"
}

# ============================================
# STEP 6: PREPARE PRODUCTS (Nếu chưa prepare)
# ============================================
Write-TestHeader "STEP 6: Prepare Products Cho Order"

if ($ORDER_ID) {
    try {
        Write-Info "Dang prepare products cho order #$ORDER_ID..."
        
        # Kiểm tra xem products đã được prepare chưa
        try {
            $checkAssignment = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/assignments/order/$ORDER_ID" `
                -Method GET `
                -Headers @{"Authorization" = "Bearer $TOKEN"}
            
            if ($checkAssignment.data.productsPrepared) {
                Write-Info "Products da duoc prepare roi. Bo qua buoc nay."
            } else {
                $prepareBody = @{
                    orderId = $ORDER_ID
                    notes = "Products prepared by staff"
                } | ConvertTo-Json
                
                $prepareResponse = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/prepare-products" `
                    -Method POST `
                    -Body $prepareBody `
                    -ContentType "application/json" `
                    -Headers @{"Authorization" = "Bearer $TOKEN"}
                
                Write-Success "Prepare products thanh cong!"
                Write-Host "`nResponse:" -ForegroundColor Cyan
                $prepareResponse | ConvertTo-Json -Depth 10 | Write-Host
            }
        } catch {
            Write-Info "Khong the kiem tra assignment. Thu prepare products..."
            $prepareBody = @{
                orderId = $ORDER_ID
                notes = "Products prepared by staff"
            } | ConvertTo-Json
            
            $prepareResponse = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/prepare-products" `
                -Method POST `
                -Body $prepareBody `
                -ContentType "application/json" `
                -Headers @{"Authorization" = "Bearer $TOKEN"}
            
            Write-Success "Prepare products thanh cong!"
            Write-Host "`nResponse:" -ForegroundColor Cyan
            $prepareResponse | ConvertTo-Json -Depth 10 | Write-Host
        }
    } catch {
        Write-Error "Prepare products that bai: $($_.Exception.Message)"
        if ($_.ErrorDetails.Message) {
            try {
                $errorDetails = $_.ErrorDetails.Message | ConvertFrom-Json
                Write-Host "`nError Details:" -ForegroundColor Red
                $errorDetails | ConvertTo-Json -Depth 5 | Write-Host
            } catch {
                Write-Host "Error Details: $($_.ErrorDetails.Message)" -ForegroundColor Red
            }
        }
    }
} else {
    Write-Info "Khong co order ID de prepare products"
}

# ============================================
# SUMMARY
# ============================================
Write-TestHeader "TEST SUMMARY"

Write-Host "Cac test da chay:" -ForegroundColor Cyan
Write-Host "  1. Dang nhap STAFF" -ForegroundColor Green
Write-Host "  2. Lay danh sach stores" -ForegroundColor Green
Write-Host "  3. Lay assignment theo order ID" -ForegroundColor Green
Write-Host "  4. Lay danh sach assignments theo store" -ForegroundColor Green
Write-Host "  5. Generate invoice" -ForegroundColor Green
Write-Host "  6. Prepare products" -ForegroundColor Green
Write-Host ""
Write-Host "Hoan thanh test!" -ForegroundColor Green

