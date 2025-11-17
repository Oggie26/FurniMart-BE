# Script Tạo Dữ Liệu Test: Orders và Delivery Assignments
# Tạo orders và assignments để test các chức năng delivery

$BASE_URL = "http://152.53.227.115:8086"
$ORDER_SERVICE_URL = "http://152.53.227.115:8087"
$DELIVERY_SERVICE_URL = "http://152.53.227.115:8089"

# Admin credentials để tạo customer
$ADMIN_EMAIL = "admin@furnimart.com"
$ADMIN_PASSWORD = "Admin@123456"

# STAFF credentials để assign orders
$STAFF_EMAIL = "staff@furnimart.com"
$STAFF_PASSWORD = "Staff@123"

# DELIVERY staff ID
$DELIVERY_STAFF_ID = "880c5184-668f-4b09-b9af-99b59803918d"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Create Test Data: Orders & Assignments" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Login as Admin
Write-Host "Step 1: Login as Admin..." -ForegroundColor Yellow
$adminLoginBody = @{
    email = $ADMIN_EMAIL
    password = $ADMIN_PASSWORD
} | ConvertTo-Json

try {
    $adminLogin = Invoke-RestMethod -Uri "$BASE_URL/api/auth/login" -Method POST -Body $adminLoginBody -ContentType "application/json"
    $ADMIN_TOKEN = $adminLogin.data.token
    Write-Host "Admin login success!" -ForegroundColor Green
} catch {
    Write-Host "Admin login failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 2: Create test customer (if not exists)
Write-Host ""
Write-Host "Step 2: Create test customer..." -ForegroundColor Yellow
$customerBody = @{
    email = "testcustomer@furnimart.com"
    password = "TestCustomer@123"
    fullName = "Test Customer"
    phone = "0944444444"
    gender = $true
    birthday = "1990-01-01"
} | ConvertTo-Json

try {
    $customer = Invoke-RestMethod -Uri "$BASE_URL/api/users/register" -Method POST -Body $customerBody -ContentType "application/json"
    Write-Host "Customer created! ID: $($customer.data.id)" -ForegroundColor Green
    $CUSTOMER_ID = $customer.data.id
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 400 -or $_.Exception.Response.StatusCode.value__ -eq 409) {
        Write-Host "Customer may already exist, trying to login..." -ForegroundColor Yellow
        $customerLoginBody = @{
            email = "testcustomer@furnimart.com"
            password = "TestCustomer@123"
        } | ConvertTo-Json
        try {
            $customerLogin = Invoke-RestMethod -Uri "$BASE_URL/api/auth/login" -Method POST -Body $customerLoginBody -ContentType "application/json"
            $CUSTOMER_TOKEN = $customerLogin.data.token
            # Extract customer ID from token (you may need to decode JWT)
            Write-Host "Customer login success!" -ForegroundColor Green
        } catch {
            Write-Host "Customer login failed" -ForegroundColor Red
        }
    } else {
        Write-Host "Create customer failed: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Step 3: Login as STAFF
Write-Host ""
Write-Host "Step 3: Login as STAFF..." -ForegroundColor Yellow
$staffLoginBody = @{
    email = $STAFF_EMAIL
    password = $STAFF_PASSWORD
} | ConvertTo-Json

try {
    $staffLogin = Invoke-RestMethod -Uri "$BASE_URL/api/auth/login" -Method POST -Body $staffLoginBody -ContentType "application/json"
    $STAFF_TOKEN = $staffLogin.data.token
    Write-Host "STAFF login success!" -ForegroundColor Green
} catch {
    Write-Host "STAFF login failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 4: Get stores
Write-Host ""
Write-Host "Step 4: Get stores..." -ForegroundColor Yellow
try {
    $stores = Invoke-RestMethod -Uri "$BASE_URL/api/stores" `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $STAFF_TOKEN"}
    
    if ($stores.data.Count -gt 0) {
        $STORE_ID = $stores.data[0].id
        Write-Host "Store ID: $STORE_ID" -ForegroundColor Cyan
    } else {
        Write-Host "No stores found, using default" -ForegroundColor Yellow
        $STORE_ID = "8d46e317-0596-4413-81b6-1a526398b3d7"
    }
} catch {
    Write-Host "Get stores failed, using default" -ForegroundColor Yellow
    $STORE_ID = "8d46e317-0596-4413-81b6-1a526398b3d7"
}

# Step 5: Get existing orders
Write-Host ""
Write-Host "Step 5: Get existing orders..." -ForegroundColor Yellow
try {
    $ordersUrl = "$ORDER_SERVICE_URL/api/orders/search?keyword=&page=0&size=10"
    $orders = Invoke-RestMethod -Uri $ordersUrl `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $STAFF_TOKEN"}
    
    if ($orders.data.content.Count -gt 0) {
        Write-Host "Found $($orders.data.content.Count) existing orders" -ForegroundColor Green
        
        # Step 6: Assign orders to delivery
        Write-Host ""
        Write-Host "Step 6: Assign orders to delivery..." -ForegroundColor Yellow
        
        $assignedCount = 0
        foreach ($order in $orders.data.content) {
            # Check if already assigned
            try {
                $checkUrl = "$DELIVERY_SERVICE_URL/api/delivery/assignments/order/$($order.id)"
                $existing = Invoke-RestMethod -Uri $checkUrl `
                    -Method GET `
                    -Headers @{"Authorization" = "Bearer $STAFF_TOKEN"}
                Write-Host "  Order $($order.id) already assigned, skipping..." -ForegroundColor Yellow
            } catch {
                # Not assigned, create assignment
                try {
                    $assignBody = @{
                        orderId = $order.id
                        storeId = $STORE_ID
                        deliveryStaffId = $DELIVERY_STAFF_ID
                        estimatedDeliveryDate = (Get-Date).AddDays(1).ToString("yyyy-MM-ddTHH:mm:ss")
                        notes = "Test assignment created by script"
                    } | ConvertTo-Json
                    
                    $assignment = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/assign" `
                        -Method POST `
                        -Body $assignBody `
                        -ContentType "application/json" `
                        -Headers @{"Authorization" = "Bearer $STAFF_TOKEN"}
                    
                    Write-Host "  Assigned order $($order.id) successfully! Assignment ID: $($assignment.data.id)" -ForegroundColor Green
                    $assignedCount++
                } catch {
                    Write-Host "  Failed to assign order $($order.id): $($_.Exception.Message)" -ForegroundColor Red
                }
            }
        }
        
        Write-Host ""
        Write-Host "Assigned $assignedCount orders to delivery" -ForegroundColor Green
    } else {
        Write-Host "No orders found. Please create orders first." -ForegroundColor Yellow
        Write-Host ""
        Write-Host "To create orders, you need:" -ForegroundColor Cyan
        Write-Host "  1. Customer account (created above)" -ForegroundColor Gray
        Write-Host "  2. Products in inventory" -ForegroundColor Gray
        Write-Host "  3. Cart with items" -ForegroundColor Gray
        Write-Host "  4. Address" -ForegroundColor Gray
        Write-Host "  5. Call POST /api/orders/checkout" -ForegroundColor Gray
    }
} catch {
    Write-Host "Get orders failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "This is OK if there are no orders yet" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test Data Creation Completed!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. Run test scripts to verify functionality" -ForegroundColor Gray
Write-Host "  2. Check delivery assignments: GET /api/delivery/assignments/staff/{deliveryStaffId}" -ForegroundColor Gray
Write-Host "  3. Update delivery status: PUT /api/delivery/assignments/{assignmentId}/status" -ForegroundColor Gray

