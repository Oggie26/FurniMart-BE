# Script Tao Tai Khoan Test: BRANCH_MANAGER, STAFF, DELIVERY
# PowerShell Script for Windows

$BASE_URL = "http://152.53.227.115:8086"
$ADMIN_EMAIL = "admin@furnimart.com"
$ADMIN_PASSWORD = "Admin@123456"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Tao Tai Khoan Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Login
Write-Host "Dang dang nhap voi admin account..." -ForegroundColor Yellow
$loginBody = @{
    email = $ADMIN_EMAIL
    password = $ADMIN_PASSWORD
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$BASE_URL/api/auth/login" -Method POST -Body $loginBody -ContentType "application/json"
    $TOKEN = $loginResponse.data.token
    Write-Host "Dang nhap thanh cong!" -ForegroundColor Green
} catch {
    Write-Host "Dang nhap that bai: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 2: Create BRANCH_MANAGER
Write-Host ""
Write-Host "Dang tao BRANCH_MANAGER..." -ForegroundColor Yellow
$body1 = @{
    email = "branchmanager@furnimart.com"
    password = "BranchManager@123"
    fullName = "Nguyen Van Quan Ly"
    phone = "0911111111"
    role = "BRANCH_MANAGER"
    status = "ACTIVE"
    gender = $true
    birthday = "1990-01-01"
} | ConvertTo-Json

try {
    $response1 = Invoke-RestMethod -Uri "$BASE_URL/api/employees" -Method POST -Body $body1 -ContentType "application/json" -Headers @{"Authorization" = "Bearer $TOKEN"}
    Write-Host "Tao BRANCH_MANAGER thanh cong!" -ForegroundColor Green
    Write-Host "  ID: $($response1.data.id)" -ForegroundColor Cyan
} catch {
    $statusCode = $null
    try { $statusCode = $_.Exception.Response.StatusCode.value__ } catch {}
    if ($statusCode -eq 400 -or $statusCode -eq 409) {
        Write-Host "BRANCH_MANAGER co the da ton tai" -ForegroundColor Yellow
    } else {
        Write-Host "Tao BRANCH_MANAGER that bai: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Step 3: Create STAFF
Write-Host ""
Write-Host "Dang tao STAFF..." -ForegroundColor Yellow
$body2 = @{
    email = "staff@furnimart.com"
    password = "Staff@123"
    fullName = "Tran Thi Nhan Vien"
    phone = "0922222222"
    role = "STAFF"
    status = "ACTIVE"
    gender = $true
    birthday = "1990-01-01"
} | ConvertTo-Json

try {
    $response2 = Invoke-RestMethod -Uri "$BASE_URL/api/employees" -Method POST -Body $body2 -ContentType "application/json" -Headers @{"Authorization" = "Bearer $TOKEN"}
    Write-Host "Tao STAFF thanh cong!" -ForegroundColor Green
    Write-Host "  ID: $($response2.data.id)" -ForegroundColor Cyan
} catch {
    $statusCode = $null
    try { $statusCode = $_.Exception.Response.StatusCode.value__ } catch {}
    if ($statusCode -eq 400 -or $statusCode -eq 409) {
        Write-Host "STAFF co the da ton tai" -ForegroundColor Yellow
    } else {
        Write-Host "Tao STAFF that bai: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Step 4: Create DELIVERY
Write-Host ""
Write-Host "Dang tao DELIVERY..." -ForegroundColor Yellow
$body3 = @{
    email = "delivery@furnimart.com"
    password = "Delivery@123"
    fullName = "Le Van Giao Hang"
    phone = "0933333333"
    role = "DELIVERY"
    status = "ACTIVE"
    gender = $true
    birthday = "1990-01-01"
} | ConvertTo-Json

try {
    $response3 = Invoke-RestMethod -Uri "$BASE_URL/api/employees" -Method POST -Body $body3 -ContentType "application/json" -Headers @{"Authorization" = "Bearer $TOKEN"}
    Write-Host "Tao DELIVERY thanh cong!" -ForegroundColor Green
    Write-Host "  ID: $($response3.data.id)" -ForegroundColor Cyan
} catch {
    $statusCode = $null
    try { $statusCode = $_.Exception.Response.StatusCode.value__ } catch {}
    if ($statusCode -eq 400 -or $statusCode -eq 409) {
        Write-Host "DELIVERY co the da ton tai" -ForegroundColor Yellow
    } else {
        Write-Host "Tao DELIVERY that bai: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Summary
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Tom Tat Tai Khoan Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "BRANCH_MANAGER:" -ForegroundColor Yellow
Write-Host "  Email: branchmanager@furnimart.com" -ForegroundColor Gray
Write-Host "  Password: BranchManager@123" -ForegroundColor Gray
Write-Host ""

Write-Host "STAFF:" -ForegroundColor Yellow
Write-Host "  Email: staff@furnimart.com" -ForegroundColor Gray
Write-Host "  Password: Staff@123" -ForegroundColor Gray
Write-Host ""

Write-Host "DELIVERY:" -ForegroundColor Yellow
Write-Host "  Email: delivery@furnimart.com" -ForegroundColor Gray
Write-Host "  Password: Delivery@123" -ForegroundColor Gray
Write-Host ""

Write-Host "Hoan thanh!" -ForegroundColor Green
