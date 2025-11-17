# Script Test: Staff Functions (STAFF Role)
# Test các chức năng của STAFF: assign orders, generate invoices, prepare products

$BASE_URL = "http://152.53.227.115:8086"
$DELIVERY_SERVICE_URL = "http://152.53.227.115:8089"
$ORDER_SERVICE_URL = "http://152.53.227.115:8087"

# STAFF credentials
$STAFF_EMAIL = "staff@furnimart.com"
$STAFF_PASSWORD = "Staff@123"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test Staff Functions (STAFF Role)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Login as STAFF
Write-Host "Step 1: Login as STAFF..." -ForegroundColor Yellow
$loginBody = @{
    email = $STAFF_EMAIL
    password = $STAFF_PASSWORD
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$BASE_URL/api/auth/login" -Method POST -Body $loginBody -ContentType "application/json"
    $TOKEN = $loginResponse.data.token
    Write-Host "Login success!" -ForegroundColor Green
} catch {
    Write-Host "Login failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 2: Get stores
Write-Host ""
Write-Host "Step 2: Get stores..." -ForegroundColor Yellow
try {
    $stores = Invoke-RestMethod -Uri "$BASE_URL/api/stores" `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $TOKEN"}
    
    if ($stores.data.Count -gt 0) {
        $STORE_ID = $stores.data[0].id
        Write-Host "  Store ID: $STORE_ID" -ForegroundColor Cyan
    } else {
        Write-Host "  No stores found" -ForegroundColor Yellow
        $STORE_ID = "8d46e317-0596-4413-81b6-1a526398b3d7"
    }
} catch {
    Write-Host "  Using default store ID" -ForegroundColor Yellow
    $STORE_ID = "8d46e317-0596-4413-81b6-1a526398b3d7"
}

# Step 3: Get orders
Write-Host ""
Write-Host "Step 3: Get orders..." -ForegroundColor Yellow
try {
    $ordersUrl = "$ORDER_SERVICE_URL/api/orders/search?keyword=&page=0&size=10"
    $orders = Invoke-RestMethod -Uri $ordersUrl `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $TOKEN"}
    
    if ($orders.data.content.Count -gt 0) {
        $ORDER_ID = $orders.data.content[0].id
        Write-Host "  Order ID: $ORDER_ID" -ForegroundColor Cyan
    } else {
        Write-Host "  No orders found" -ForegroundColor Yellow
        $ORDER_ID = 1
    }
} catch {
    Write-Host "  Using default order ID" -ForegroundColor Yellow
    $ORDER_ID = 1
}

# Step 4: Generate invoice
Write-Host ""
Write-Host "Step 4: Generate invoice for order $ORDER_ID..." -ForegroundColor Yellow
try {
    $invoice = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/generate-invoice/$ORDER_ID" `
        -Method POST `
        -Headers @{"Authorization" = "Bearer $TOKEN"}
    
    Write-Host "  Invoice generated successfully!" -ForegroundColor Green
    Write-Host "  Assignment ID: $($invoice.data.id)" -ForegroundColor Cyan
} catch {
    Write-Host "  Generate invoice failed: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails.Message) {
        Write-Host "    Details: $($_.ErrorDetails.Message)" -ForegroundColor Red
    }
}

# Step 5: Prepare products
Write-Host ""
Write-Host "Step 5: Prepare products for order $ORDER_ID..." -ForegroundColor Yellow
try {
    $prepareBody = @{
        orderId = $ORDER_ID
        storeId = $STORE_ID
        notes = "Prepared by STAFF test script"
    } | ConvertTo-Json
    
    $prepared = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/prepare-products" `
        -Method POST `
        -Body $prepareBody `
        -ContentType "application/json" `
        -Headers @{"Authorization" = "Bearer $TOKEN"}
    
    Write-Host "  Products prepared successfully!" -ForegroundColor Green
    Write-Host "  Status: $($prepared.data.status)" -ForegroundColor Cyan
} catch {
    Write-Host "  Prepare products failed: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails.Message) {
        Write-Host "    Details: $($_.ErrorDetails.Message)" -ForegroundColor Red
    }
}

# Step 6: Get assignments by store
Write-Host ""
Write-Host "Step 6: Get delivery assignments by store..." -ForegroundColor Yellow
try {
    $assignments = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/assignments/store/$STORE_ID" `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $TOKEN"}
    
    Write-Host "  Found $($assignments.data.Count) assignments" -ForegroundColor Green
} catch {
    Write-Host "  Get assignments failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "Test completed!" -ForegroundColor Green

