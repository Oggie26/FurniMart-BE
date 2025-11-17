# Script tạo lại tất cả các Account từ TEST_ACCOUNTS.md
# Sử dụng: .\test-data\create-all-test-accounts.ps1

$baseUrl = "http://152.53.227.115:8086"
$adminToken = ""

# Colors
function Write-ColorOutput($ForegroundColor) {
    $fc = $host.UI.RawUI.ForegroundColor
    $host.UI.RawUI.ForegroundColor = $ForegroundColor
    if ($args) {
        Write-Output $args
    }
    $host.UI.RawUI.ForegroundColor = $fc
}

Write-ColorOutput Cyan "`n========================================"
Write-ColorOutput Cyan "TAO LAI TAT CA TEST ACCOUNTS"
Write-ColorOutput Cyan "========================================`n"

# Step 1: Login as admin để lấy token
Write-Host "Step 1: Dang nhap admin..." -ForegroundColor Yellow
$loginBody = @{
    email = "admin@furnimart.com"
    password = "admin123"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" `
        -Method POST `
        -ContentType "application/json" `
        -Body $loginBody
    
    $adminToken = $loginResponse.data.token
    Write-ColorOutput Green "✅ Đăng nhập thành công!"
} catch {
    Write-ColorOutput Red "❌ Đăng nhập thất bại: $_"
    Write-Host "Thử với account khác..." -ForegroundColor Yellow
    
    # Thử với string@gmail.com
    $loginBody2 = @{
        email = "string@gmail.com"
        password = "string"
    } | ConvertTo-Json
    
    try {
        $loginResponse2 = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" `
            -Method POST `
            -ContentType "application/json" `
            -Body $loginBody2
        
        $adminToken = $loginResponse2.data.token
        Write-ColorOutput Green "✅ Đăng nhập thành công với string@gmail.com!"
    } catch {
        Write-ColorOutput Red "❌ Không thể đăng nhập. Dừng script."
        exit 1
    }
}

# Step 2: Tạo các Admin accounts
Write-ColorOutput Cyan "`n========================================"
Write-ColorOutput Cyan "TAO ADMIN ACCOUNTS"
Write-ColorOutput Cyan "========================================`n"

# Admin Account 1: string@gmail.com
Write-Host "Tạo admin: string@gmail.com..." -ForegroundColor Yellow
$admin1Body = @{
    email = "string@gmail.com"
    password = "string"
    role = "ADMIN"
    fullName = "Admin String"
    phone = "0900000001"
    status = "ACTIVE"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/employees/admins" `
        -Method POST `
        -ContentType "application/json" `
        -Headers @{Authorization = "Bearer $adminToken"} `
        -Body $admin1Body
    
    Write-ColorOutput Green "✅ Tạo thành công: string@gmail.com (ID: $($response.data.id))"
} catch {
    if ($_.Exception.Response.StatusCode -eq 400) {
        Write-ColorOutput Yellow "⚠️  Account đã tồn tại: string@gmail.com"
    } else {
        Write-ColorOutput Red "❌ Lỗi: $_"
    }
}

# Admin Account 2: admin@furnimart.com
Write-Host "Tạo admin: admin@furnimart.com..." -ForegroundColor Yellow
$admin2Body = @{
    email = "admin@furnimart.com"
    password = "admin123"
    role = "ADMIN"
    fullName = "Admin FurniMart"
    phone = "0900000000"
    status = "ACTIVE"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/employees/admins" `
        -Method POST `
        -ContentType "application/json" `
        -Headers @{Authorization = "Bearer $adminToken"} `
        -Body $admin2Body
    
    Write-ColorOutput Green "✅ Tạo thành công: admin@furnimart.com (ID: $($response.data.id))"
} catch {
    if ($_.Exception.Response.StatusCode -eq 400) {
        Write-ColorOutput Yellow "⚠️  Account đã tồn tại: admin@furnimart.com"
    } else {
        Write-ColorOutput Red "❌ Lỗi: $_"
    }
}

# Step 3: Tạo các Employee accounts
Write-ColorOutput Cyan "`n========================================"
Write-ColorOutput Cyan "TAO EMPLOYEE ACCOUNTS"
Write-ColorOutput Cyan "========================================`n"

# BRANCH_MANAGER
Write-Host "Tạo BRANCH_MANAGER: branchmanager@furnimart.com..." -ForegroundColor Yellow
$bmBody = @{
    email = "branchmanager@furnimart.com"
    password = "BranchManager@123"
    role = "BRANCH_MANAGER"
    fullName = "Branch Manager Test"
    phone = "0901234567"
    status = "ACTIVE"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/employees" `
        -Method POST `
        -ContentType "application/json" `
        -Headers @{Authorization = "Bearer $adminToken"} `
        -Body $bmBody
    
    Write-ColorOutput Green "✅ Tạo thành công: branchmanager@furnimart.com (ID: $($response.data.id))"
} catch {
    if ($_.Exception.Response.StatusCode -eq 400) {
        Write-ColorOutput Yellow "⚠️  Account đã tồn tại: branchmanager@furnimart.com"
    } else {
        Write-ColorOutput Red "❌ Lỗi: $_"
    }
}

# STAFF
Write-Host "Tạo STAFF: staff@furnimart.com..." -ForegroundColor Yellow
$staffBody = @{
    email = "staff@furnimart.com"
    password = "Staff@123"
    role = "STAFF"
    fullName = "Staff Test"
    phone = "0901234568"
    status = "ACTIVE"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/employees" `
        -Method POST `
        -ContentType "application/json" `
        -Headers @{Authorization = "Bearer $adminToken"} `
        -Body $staffBody
    
    Write-ColorOutput Green "✅ Tạo thành công: staff@furnimart.com (ID: $($response.data.id))"
} catch {
    if ($_.Exception.Response.StatusCode -eq 400) {
        Write-ColorOutput Yellow "⚠️  Account đã tồn tại: staff@furnimart.com"
    } else {
        Write-ColorOutput Red "❌ Lỗi: $_"
    }
}

# DELIVERY
Write-Host "Tạo DELIVERY: delivery@furnimart.com..." -ForegroundColor Yellow
$deliveryBody = @{
    email = "delivery@furnimart.com"
    password = "Delivery@123"
    role = "DELIVERY"
    fullName = "Delivery Staff Test"
    phone = "0901234569"
    status = "ACTIVE"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/employees" `
        -Method POST `
        -ContentType "application/json" `
        -Headers @{Authorization = "Bearer $adminToken"} `
        -Body $deliveryBody
    
    Write-ColorOutput Green "✅ Tạo thành công: delivery@furnimart.com (ID: $($response.data.id))"
} catch {
    if ($_.Exception.Response.StatusCode -eq 400) {
        Write-ColorOutput Yellow "⚠️  Account đã tồn tại: delivery@furnimart.com"
    } else {
        Write-ColorOutput Red "❌ Lỗi: $_"
    }
}

# MANAGER (BRANCH_MANAGER)
Write-Host "Tạo MANAGER (BRANCH_MANAGER): manager@furnimart.com..." -ForegroundColor Yellow
$managerBody = @{
    email = "manager@furnimart.com"
    password = "manager123"
    role = "BRANCH_MANAGER"
    fullName = "Manager Test"
    phone = "0901234570"
    status = "ACTIVE"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/employees" `
        -Method POST `
        -ContentType "application/json" `
        -Headers @{Authorization = "Bearer $adminToken"} `
        -Body $managerBody
    
    Write-ColorOutput Green "✅ Tạo thành công: manager@furnimart.com (ID: $($response.data.id))"
} catch {
    if ($_.Exception.Response.StatusCode -eq 400) {
        Write-ColorOutput Yellow "⚠️  Account đã tồn tại: manager@furnimart.com"
    } else {
        Write-ColorOutput Red "❌ Lỗi: $_"
    }
}

# Step 4: Tạo CUSTOMER account
Write-ColorOutput Cyan "`n========================================"
Write-ColorOutput Cyan "TAO CUSTOMER ACCOUNT"
Write-ColorOutput Cyan "========================================`n"

Write-Host "Tạo CUSTOMER: customer@gmail.com..." -ForegroundColor Yellow
$customerBody = @{
    email = "customer@gmail.com"
    password = "customer123"
    fullName = "Customer Test"
    phone = "0901234571"
    status = "ACTIVE"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/users" `
        -Method POST `
        -ContentType "application/json" `
        -Headers @{Authorization = "Bearer $adminToken"} `
        -Body $customerBody
    
    Write-ColorOutput Green "✅ Tạo thành công: customer@gmail.com (ID: $($response.data.id))"
} catch {
    if ($_.Exception.Response.StatusCode -eq 400) {
        Write-ColorOutput Yellow "⚠️  Account đã tồn tại: customer@gmail.com"
    } else {
        Write-ColorOutput Red "❌ Lỗi: $_"
    }
}

Write-ColorOutput Cyan "`n========================================"
Write-ColorOutput Cyan "HOAN TAT"
Write-ColorOutput Cyan "========================================`n"
Write-ColorOutput Green "Da hoan tat tao lai cac test accounts!"
