# Simple script to create test accounts
$BASE_URL = "http://152.53.227.115:8086"
$ADMIN_EMAIL = "admin@furnimart.com"
$ADMIN_PASSWORD = "Admin@123456"

Write-Host "Login as admin..." -ForegroundColor Yellow
$loginBody = '{"email":"admin@furnimart.com","password":"Admin@123456"}'
try {
    $loginResponse = Invoke-RestMethod -Uri "$BASE_URL/api/auth/login" -Method POST -Body $loginBody -ContentType "application/json"
    $TOKEN = $loginResponse.data.token
    Write-Host "Login success! Token: $($TOKEN.Substring(0,30))..." -ForegroundColor Green
} catch {
    Write-Host "Login failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Response: $($_.ErrorDetails.Message)" -ForegroundColor Red
    exit 1
}

Write-Host "`nCreating BRANCH_MANAGER..." -ForegroundColor Yellow
$body1 = '{"email":"branchmanager@furnimart.com","password":"BranchManager@123","fullName":"Nguyen Van Quan Ly","phone":"0911111111","role":"BRANCH_MANAGER","status":"ACTIVE","gender":true,"birthday":"1990-01-01"}'
try {
    $r1 = Invoke-RestMethod -Uri "$BASE_URL/api/employees" -Method POST -Body $body1 -ContentType "application/json" -Headers @{"Authorization"="Bearer $TOKEN"}
    Write-Host "Created! ID: $($r1.data.id)" -ForegroundColor Green
} catch {
    Write-Host "Failed or exists: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host "`nCreating STAFF..." -ForegroundColor Yellow
$body2 = '{"email":"staff@furnimart.com","password":"Staff@123","fullName":"Tran Thi Nhan Vien","phone":"0922222222","role":"STAFF","status":"ACTIVE","gender":true,"birthday":"1990-01-01"}'
try {
    $r2 = Invoke-RestMethod -Uri "$BASE_URL/api/employees" -Method POST -Body $body2 -ContentType "application/json" -Headers @{"Authorization"="Bearer $TOKEN"}
    Write-Host "Created! ID: $($r2.data.id)" -ForegroundColor Green
} catch {
    Write-Host "Failed or exists: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host "`nCreating DELIVERY..." -ForegroundColor Yellow
$body3 = '{"email":"delivery@furnimart.com","password":"Delivery@123","fullName":"Le Van Giao Hang","phone":"0933333333","role":"DELIVERY","status":"ACTIVE","gender":true,"birthday":"1990-01-01"}'
try {
    $r3 = Invoke-RestMethod -Uri "$BASE_URL/api/employees" -Method POST -Body $body3 -ContentType "application/json" -Headers @{"Authorization"="Bearer $TOKEN"}
    Write-Host "Created! ID: $($r3.data.id)" -ForegroundColor Green
} catch {
    Write-Host "Failed or exists: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host "`n=== Test Accounts ===" -ForegroundColor Cyan
Write-Host "BRANCH_MANAGER: branchmanager@furnimart.com / BranchManager@123" -ForegroundColor Yellow
Write-Host "STAFF: staff@furnimart.com / Staff@123" -ForegroundColor Yellow
Write-Host "DELIVERY: delivery@furnimart.com / Delivery@123" -ForegroundColor Yellow

