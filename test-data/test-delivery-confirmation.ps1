# Script Test: Delivery Confirmation Functions (DELIVERY Role)
# PowerShell Script for Windows

# ============================================
# CONFIGURATION
# ============================================
$BASE_URL = "http://152.53.227.115:8086"  # Gateway URL
$DELIVERY_SERVICE_URL = "http://152.53.227.115:8089"  # Direct Delivery Service URL
$USER_SERVICE_URL = "http://152.53.227.115:8086"  # User Service URL (via Gateway)

$DELIVERY_EMAIL = "delivery@furnimart.com"
$DELIVERY_PASSWORD = "Delivery@123"

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
# STEP 1: LOGIN AS DELIVERY
# ============================================
Write-TestHeader "STEP 1: Dang Nhap Voi Tai Khoan DELIVERY"

$loginBody = @{
    email = $DELIVERY_EMAIL
    password = $DELIVERY_PASSWORD
} | ConvertTo-Json

try {
    Write-Info "Dang dang nhap voi email: $DELIVERY_EMAIL"
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
# STEP 2: GET DELIVERY STAFF ID
# ============================================
Write-TestHeader "STEP 2: Lay Delivery Staff ID"

$DELIVERY_STAFF_ID = $null
try {
    $userResponse = Invoke-RestMethod -Uri "$USER_SERVICE_URL/api/employees/email/$DELIVERY_EMAIL" `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $TOKEN"}
    $DELIVERY_STAFF_ID = $userResponse.data.id
    Write-Success "Delivery Staff ID: $DELIVERY_STAFF_ID"
    Write-Info "Full Name: $($userResponse.data.fullName)"
} catch {
    Write-Error "Khong the lay Delivery Staff ID: $($_.Exception.Message)"
    exit 1
}

# ============================================
# STEP 3: GET DELIVERY ASSIGNMENTS BY STAFF
# ============================================
Write-TestHeader "STEP 3: Lay Danh Sach Assignments Cua Delivery Staff"

$ASSIGNMENT_ID = $null
$ORDER_ID = $null

try {
    Write-Info "Dang lay danh sach assignments..."
    $assignmentsResponse = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/assignments/staff/$DELIVERY_STAFF_ID" `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $TOKEN"}
    
    if ($assignmentsResponse.data -and $assignmentsResponse.data.Count -gt 0) {
        Write-Success "Tim thay $($assignmentsResponse.data.Count) assignments"
        $ASSIGNMENT_ID = $assignmentsResponse.data[0].id
        $ORDER_ID = $assignmentsResponse.data[0].orderId
        Write-Info "Assignment ID: $ASSIGNMENT_ID"
        Write-Info "Order ID: $ORDER_ID"
        Write-Info "Status: $($assignmentsResponse.data[0].status)"
    } else {
        Write-Info "Khong tim thay assignment nao. Can assign order truoc."
        Write-Host "`nDe test delivery confirmation, can co:" -ForegroundColor Yellow
        Write-Host "  1. Order da duoc assign cho delivery staff nay" -ForegroundColor Yellow
        Write-Host "  2. Status cua assignment la IN_TRANSIT hoac DELIVERED" -ForegroundColor Yellow
        exit 0
    }
} catch {
    Write-Error "Lay danh sach assignments that bai: $($_.Exception.Message)"
    if ($_.ErrorDetails.Message) {
        Write-Error "Chi tiet: $($_.ErrorDetails.Message)"
    }
    exit 1
}

# ============================================
# STEP 4: CREATE DELIVERY CONFIRMATION
# ============================================
Write-TestHeader "STEP 4: Tao Delivery Confirmation"

if (-not $ASSIGNMENT_ID -or -not $ORDER_ID) {
    Write-Error "Khong co assignment de tao confirmation"
    exit 1
}

# Tạo delivery confirmation request
$confirmationBody = @{
    orderId = $ORDER_ID
    deliveryPhotos = @()  # List of photo URLs (optional)
    deliveryNotes = "Da giao hang thanh cong. Khach hang da nhan hang."
    deliveryLatitude = 10.762622  # Optional: GPS latitude
    deliveryLongitude = 106.660172  # Optional: GPS longitude
    deliveryAddress = "123 Test Street, Ho Chi Minh City"  # Optional: Delivery address
} | ConvertTo-Json

try {
    Write-Info "Dang tao delivery confirmation cho order #$ORDER_ID..."
    Write-Info "Request body: $confirmationBody"
    
    $confirmationResponse = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery-confirmations" `
        -Method POST `
        -Body $confirmationBody `
        -ContentType "application/json" `
        -Headers @{"Authorization" = "Bearer $TOKEN"}
    
    Write-Success "Tao delivery confirmation thanh cong!"
    Write-Host "`nResponse:" -ForegroundColor Cyan
    $confirmationResponse | ConvertTo-Json -Depth 10 | Write-Host
    
    $CONFIRMATION_ID = $confirmationResponse.data.id
    Write-Info "Confirmation ID: $CONFIRMATION_ID"
    
} catch {
    Write-Error "Tao delivery confirmation that bai: $($_.Exception.Message)"
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

# ============================================
# STEP 5: GET DELIVERY CONFIRMATIONS BY STAFF
# ============================================
Write-TestHeader "STEP 5: Lay Danh Sach Delivery Confirmations"

try {
    Write-Info "Dang lay danh sach confirmations cua delivery staff..."
    $confirmationsResponse = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery-confirmations/staff/$DELIVERY_STAFF_ID" `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $TOKEN"}
    
    Write-Success "Lay danh sach confirmations thanh cong!"
    Write-Host "`nSo luong confirmations: $($confirmationsResponse.data.Count)" -ForegroundColor Cyan
    
    if ($confirmationsResponse.data.Count -gt 0) {
        Write-Host "`nDanh sach confirmations:" -ForegroundColor Cyan
        $confirmationsResponse.data | ForEach-Object {
            Write-Host "  - Confirmation ID: $($_.id)" -ForegroundColor Yellow
            Write-Host "    Order ID: $($_.orderId)" -ForegroundColor Yellow
            Write-Host "    Type: $($_.confirmationType)" -ForegroundColor Yellow
            Write-Host "    Notes: $($_.notes)" -ForegroundColor Yellow
            Write-Host ""
        }
    }
} catch {
    Write-Error "Lay danh sach confirmations that bai: $($_.Exception.Message)"
    if ($_.ErrorDetails.Message) {
        Write-Error "Chi tiet: $($_.ErrorDetails.Message)"
    }
}

# ============================================
# STEP 6: GET DELIVERY CONFIRMATION BY ORDER ID
# ============================================
Write-TestHeader "STEP 6: Lay Delivery Confirmation Theo Order ID"

if ($ORDER_ID) {
    try {
        Write-Info "Dang lay confirmation cho order #$ORDER_ID..."
        $confirmationByOrderResponse = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery-confirmations/order/$ORDER_ID" `
            -Method GET `
            -Headers @{"Authorization" = "Bearer $TOKEN"}
        
        Write-Success "Lay confirmation thanh cong!"
        Write-Host "`nConfirmation Details:" -ForegroundColor Cyan
        $confirmationByOrderResponse | ConvertTo-Json -Depth 10 | Write-Host
    } catch {
        Write-Info "Lay confirmation that bai (co the chua co confirmation): $($_.Exception.Message)"
    }
} else {
    Write-Info "Khong co order ID de test"
}

# ============================================
# SUMMARY
# ============================================
Write-TestHeader "TEST SUMMARY"

Write-Host "Cac test da chay:" -ForegroundColor Cyan
Write-Host "  1. Dang nhap DELIVERY" -ForegroundColor Green
Write-Host "  2. Lay Delivery Staff ID" -ForegroundColor Green
Write-Host "  3. Lay danh sach assignments" -ForegroundColor Green
Write-Host "  4. Tao delivery confirmation" -ForegroundColor Green
Write-Host "  5. Lay danh sach confirmations" -ForegroundColor Green
Write-Host "  6. Lay confirmation theo order ID" -ForegroundColor Green
Write-Host ""
Write-Host "Hoan thanh test!" -ForegroundColor Green

