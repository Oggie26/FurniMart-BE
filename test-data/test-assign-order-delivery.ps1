# Script Test: Assign Order to Delivery
# PowerShell Script for Windows

# ============================================
# CONFIGURATION
# ============================================
$BASE_URL = "http://152.53.227.115:8086"  # Gateway URL
$DELIVERY_SERVICE_URL = "http://152.53.227.115:8089"  # Direct Delivery Service URL
$USER_SERVICE_URL = "http://152.53.227.115:8086"  # User Service URL (via Gateway)
$ORDER_SERVICE_URL = "http://152.53.227.115:8087"  # Order Service URL (via Gateway)

# Test credentials (s??? d???ng t??i kho???n STAFF ???? t???o)
$STAFF_EMAIL = "staff@furnimart.com"
$STAFF_PASSWORD = "Staff@123"

# Test data (s??? ???????c l???y t??? ?????ng t??? API)
$STORE_ID = $null
$ORDER_ID = $null
$DELIVERY_STAFF_ID = $null

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
    Write-Host "??? $Message" -ForegroundColor Green
}

function Write-Error {
    param($Message)
    Write-Host "??? $Message" -ForegroundColor Red
}

function Write-Info {
    param($Message)
    Write-Host "??????  $Message" -ForegroundColor Yellow
}

# ============================================
# STEP 1: LOGIN
# ============================================
Write-TestHeader "STEP 1: ????ng Nh???p"

$loginBody = @{
    email = $STAFF_EMAIL
    password = $STAFF_PASSWORD
} | ConvertTo-Json

try {
    Write-Info "??ang ????ng nh???p v???i email: $STAFF_EMAIL"
    $loginResponse = Invoke-RestMethod -Uri "$BASE_URL/api/auth/login" `
        -Method POST `
        -Body $loginBody `
        -ContentType "application/json"
    
    $TOKEN = $loginResponse.data.token
    Write-Success "????ng nh???p th??nh c??ng!"
    Write-Info "Token: $($TOKEN.Substring(0, 50))..."
} catch {
    Write-Error "????ng nh???p th???t b???i: $($_.Exception.Message)"
    if ($_.ErrorDetails.Message) {
        Write-Error "Chi ti???t: $($_.ErrorDetails.Message)"
    }
    exit 1
}

# ============================================
# STEP 1.5: L???Y TH??NG TIN T??? ?????NG T??? API
# ============================================
Write-TestHeader "STEP 1.5: L???y Th??ng Tin T??? ?????ng T??? API"

# L???y danh s??ch stores
try {
    Write-Info "??ang l???y danh s??ch stores..."
    $storesResponse = Invoke-RestMethod -Uri "$USER_SERVICE_URL/api/stores" `
        -Method GET `
        -Headers @{
            "Authorization" = "Bearer $TOKEN"
        }
    
    if ($storesResponse.data -and $storesResponse.data.Count -gt 0) {
        $STORE_ID = $storesResponse.data[0].id
        Write-Success "???? l???y store ID: $STORE_ID"
        Write-Info "Store name: $($storesResponse.data[0].name)"
    } else {
        Write-Error "Kh??ng t??m th???y store n??o. Vui l??ng t???o store tr?????c."
        exit 1
    }
} catch {
    Write-Error "L???y danh s??ch stores th???t b???i: $($_.Exception.Message)"
    Write-Info "S??? d???ng store ID m???c ?????nh..."
    $STORE_ID = "8d46e317-0596-4413-81b6-1a526398b3d7"  # Fallback
}

# L???y danh s??ch orders (t??m order ch??a ???????c assign)
try {
    Write-Info "??ang t??m ki???m orders..."
    $ordersUrl = "$ORDER_SERVICE_URL/api/orders/search?keyword=`&page=0`&size=10"
    $ordersResponse = Invoke-RestMethod -Uri $ordersUrl `
        -Method GET `
        -Headers @{
            "Authorization" = "Bearer $TOKEN"
        }
    
    if ($ordersResponse.data -and $ordersResponse.data.content -and $ordersResponse.data.content.Count -gt 0) {
        # T??m order ?????u ti??n ch??a ???????c assign
        foreach ($order in $ordersResponse.data.content) {
            # Ki???m tra xem order ???? ???????c assign ch??a
            try {
                $checkAssignment = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/assignments/order/$($order.id)" `
                    -Method GET `
                    -Headers @{
                        "Authorization" = "Bearer $TOKEN"
                    } -ErrorAction SilentlyContinue
                
                # N???u kh??ng c?? assignment, s??? d???ng order n??y
                if (-not $checkAssignment) {
                    $ORDER_ID = $order.id
                    Write-Success "???? t??m th???y order ch??a ???????c assign: $ORDER_ID"
                    break
                }
            } catch {
                # N???u kh??ng t??m th???y assignment (404), s??? d???ng order n??y
                $ORDER_ID = $order.id
                Write-Success "???? t??m th???y order ch??a ???????c assign: $ORDER_ID"
                break
            }
        }
        
        if (-not $ORDER_ID) {
            Write-Info "T???t c??? orders ???? ???????c assign. S??? s??? d???ng order ID ?????u ti??n ????? test error case..."
            $ORDER_ID = $ordersResponse.data.content[0].id
        }
    } else {
        Write-Info "Kh??ng t??m th???y order n??o. S??? s??? d???ng order ID m???c ?????nh..."
        $ORDER_ID = 1  # Fallback
    }
} catch {
    Write-Error "L???y danh s??ch orders th???t b???i: $($_.Exception.Message)"
    Write-Info "S??? d???ng order ID m???c ?????nh..."
    $ORDER_ID = 1  # Fallback
}

# L???y danh s??ch delivery staff
try {
    Write-Info "??ang l???y danh s??ch delivery staff..."
    $deliveryResponse = Invoke-RestMethod -Uri "$USER_SERVICE_URL/api/employees/role/delivery" `
        -Method GET `
        -Headers @{
            "Authorization" = "Bearer $TOKEN"
        }
    
    if ($deliveryResponse.data -and $deliveryResponse.data.Count -gt 0) {
        $DELIVERY_STAFF_ID = $deliveryResponse.data[0].id
        Write-Success "???? l???y delivery staff ID: $DELIVERY_STAFF_ID"
        Write-Info "Delivery staff name: $($deliveryResponse.data[0].fullName)"
    } else {
        Write-Info "Kh??ng t??m th???y delivery staff. S??? test kh??ng c?? deliveryStaffId..."
        $DELIVERY_STAFF_ID = $null
    }
} catch {
    Write-Info "L???y danh s??ch delivery staff th???t b???i (c?? th??? kh??ng c?? quy???n). S??? test kh??ng c?? deliveryStaffId..."
    $DELIVERY_STAFF_ID = $null
}

Write-Host "`nTh??ng tin ???? l???y:" -ForegroundColor Cyan
Write-Host "  Store ID: $STORE_ID" -ForegroundColor Yellow
Write-Host "  Order ID: $ORDER_ID" -ForegroundColor Yellow
Write-Host "  Delivery Staff ID: $(if ($DELIVERY_STAFF_ID) { $DELIVERY_STAFF_ID } else { 'null (optional)' })" -ForegroundColor Yellow

# ============================================
# STEP 2: TEST ASSIGN ORDER - SUCCESS CASE
# ============================================
Write-TestHeader "STEP 2: Test Assign Order - Success Case"

# T???o assign body (ch??? th??m deliveryStaffId n???u c??)
$assignBodyHash = @{
    orderId = $ORDER_ID
    storeId = $STORE_ID
    estimatedDeliveryDate = (Get-Date).AddDays(1).ToString("yyyy-MM-ddTHH:mm:ss")
    notes = "Test assignment t??? PowerShell script"
}

if ($DELIVERY_STAFF_ID) {
    $assignBodyHash.deliveryStaffId = $DELIVERY_STAFF_ID
}

$assignBody = $assignBodyHash | ConvertTo-Json

try {
    Write-Info "??ang assign order #$ORDER_ID cho delivery staff..."
    Write-Info "Request body: $assignBody"
    
    $assignResponse = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/assign" `
        -Method POST `
        -Body $assignBody `
        -ContentType "application/json" `
        -Headers @{
            "Authorization" = "Bearer $TOKEN"
        }
    
    Write-Success "Assign order th??nh c??ng!"
    Write-Host "`nResponse:" -ForegroundColor Cyan
    $assignResponse | ConvertTo-Json -Depth 10 | Write-Host
    
    $ASSIGNMENT_ID = $assignResponse.data.id
    Write-Info "Assignment ID: $ASSIGNMENT_ID"
    
} catch {
    Write-Error "Assign order th???t b???i: $($_.Exception.Message)"
    if ($_.ErrorDetails.Message) {
        $errorDetails = $_.ErrorDetails.Message | ConvertFrom-Json
        Write-Host "`nError Details:" -ForegroundColor Red
        $errorDetails | ConvertTo-Json -Depth 5 | Write-Host
    }
}

# ============================================
# STEP 3: TEST VALIDATION ERRORS
# ============================================
Write-TestHeader "STEP 3: Test Validation Errors"

# Test 3.1: Missing orderId
Write-Info "Test 3.1: Missing orderId"
$invalidBody1 = @{
    storeId = $STORE_ID
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/assign" `
        -Method POST `
        -Body $invalidBody1 `
        -ContentType "application/json" `
        -Headers @{
            "Authorization" = "Bearer $TOKEN"
        }
    Write-Error "Test failed: Should return 400 error"
} catch {
    if ($_.Exception.Response.StatusCode -eq 400) {
        Write-Success "Test passed: Returned 400 Bad Request as expected"
    } else {
        Write-Error "Test failed: Expected 400, got $($_.Exception.Response.StatusCode)"
    }
}

# Test 3.2: Missing storeId
Write-Info "`nTest 3.2: Missing storeId"
$invalidBody2 = @{
    orderId = $ORDER_ID
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/assign" `
        -Method POST `
        -Body $invalidBody2 `
        -ContentType "application/json" `
        -Headers @{
            "Authorization" = "Bearer $TOKEN"
        }
    Write-Error "Test failed: Should return 400 error"
} catch {
    if ($_.Exception.Response.StatusCode -eq 400) {
        Write-Success "Test passed: Returned 400 Bad Request as expected"
    } else {
        Write-Error "Test failed: Expected 400, got $($_.Exception.Response.StatusCode)"
    }
}

# ============================================
# STEP 4: TEST ORDER NOT FOUND
# ============================================
Write-TestHeader "STEP 4: Test Order Not Found"

$notFoundBody = @{
    orderId = 99999  # Order ID kh??ng t???n t???i
    storeId = $STORE_ID
} | ConvertTo-Json

try {
    Write-Info "??ang test v???i order ID kh??ng t???n t???i..."
    $response = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/assign" `
        -Method POST `
        -Body $notFoundBody `
        -ContentType "application/json" `
        -Headers @{
            "Authorization" = "Bearer $TOKEN"
        }
    Write-Error "Test failed: Should return 404 error"
} catch {
    if ($_.Exception.Response.StatusCode -eq 404) {
        Write-Success "Test passed: Returned 404 Not Found as expected"
    } else {
        Write-Error "Test failed: Expected 404, got $($_.Exception.Response.StatusCode)"
    }
}

# ============================================
# STEP 5: TEST UNAUTHORIZED (No Token)
# ============================================
Write-TestHeader "STEP 5: Test Unauthorized (No Token)"

$testBody = @{
    orderId = $ORDER_ID
    storeId = $STORE_ID
} | ConvertTo-Json

try {
    Write-Info "??ang test kh??ng c?? token..."
    $response = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/assign" `
        -Method POST `
        -Body $testBody `
        -ContentType "application/json"
    Write-Error "Test failed: Should return 401 error"
} catch {
    if ($_.Exception.Response.StatusCode -eq 401) {
        Write-Success "Test passed: Returned 401 Unauthorized as expected"
    } else {
        Write-Error "Test failed: Expected 401, got $($_.Exception.Response.StatusCode)"
    }
}

# ============================================
# STEP 6: GET ASSIGNMENTS BY STORE
# ============================================
Write-TestHeader "STEP 6: Get Assignments by Store"

try {
    Write-Info "??ang l???y danh s??ch assignments trong store..."
    $assignmentsResponse = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/assignments/store/$STORE_ID" `
        -Method GET `
        -Headers @{
            "Authorization" = "Bearer $TOKEN"
        }
    
    Write-Success "L???y danh s??ch assignments th??nh c??ng!"
    Write-Host "`nS??? l?????ng assignments: $($assignmentsResponse.data.Count)" -ForegroundColor Cyan
    if ($assignmentsResponse.data.Count -gt 0) {
        Write-Host "`nDanh s??ch assignments:" -ForegroundColor Cyan
        $assignmentsResponse.data | ForEach-Object {
            Write-Host "  - Assignment ID: $($_.id), Order ID: $($_.orderId), Status: $($_.status)"
        }
    }
} catch {
    Write-Error "L???y danh s??ch assignments th???t b???i: $($_.Exception.Message)"
}

# ============================================
# SUMMARY
# ============================================
Write-TestHeader "TEST SUMMARY"

Write-Host "Cac test da chay:" -ForegroundColor Cyan
Write-Host "  1. Dang nhap" -ForegroundColor Green
Write-Host "  2. Assign order (success case)" -ForegroundColor Green
Write-Host "  3. Validation errors (missing fields)" -ForegroundColor Green
Write-Host "  4. Order not found" -ForegroundColor Green
Write-Host "  5. Unauthorized (no token)" -ForegroundColor Green
Write-Host "  6. Get assignments by store" -ForegroundColor Green
Write-Host ""
Write-Host "Hoan thanh test!" -ForegroundColor Green


