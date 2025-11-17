# Script Test: Delivery Functions (DELIVERY Role)
# Test các chức năng của DELIVERY staff

$BASE_URL = "http://152.53.227.115:8086"
$DELIVERY_SERVICE_URL = "http://152.53.227.115:8089"

# DELIVERY credentials
$DELIVERY_EMAIL = "delivery@furnimart.com"
$DELIVERY_PASSWORD = "Delivery@123"
$DELIVERY_STAFF_ID = "880c5184-668f-4b09-b9af-99b59803918d"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test Delivery Functions (DELIVERY Role)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Login as DELIVERY
Write-Host "Step 1: Login as DELIVERY..." -ForegroundColor Yellow
$loginBody = @{
    email = $DELIVERY_EMAIL
    password = $DELIVERY_PASSWORD
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$BASE_URL/api/auth/login" -Method POST -Body $loginBody -ContentType "application/json"
    $TOKEN = $loginResponse.data.token
    Write-Host "Login success!" -ForegroundColor Green
} catch {
    Write-Host "Login failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 2: Get delivery assignments by staff
Write-Host ""
Write-Host "Step 2: Get delivery assignments by staff..." -ForegroundColor Yellow
try {
    $assignments = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/assignments/staff/$DELIVERY_STAFF_ID" `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $TOKEN"}
    
    Write-Host "Found $($assignments.data.Count) assignments" -ForegroundColor Green
    if ($assignments.data.Count -gt 0) {
        $assignmentId = $assignments.data[0].id
        $currentStatus = $assignments.data[0].status
        Write-Host "  First assignment ID: $assignmentId" -ForegroundColor Cyan
        Write-Host "  Current status: $currentStatus" -ForegroundColor Cyan
        
        # Step 3: Update delivery status
        Write-Host ""
        Write-Host "Step 3: Update delivery status..." -ForegroundColor Yellow
        
        # Try different statuses based on current status
        $newStatus = "PREPARING"
        if ($currentStatus -eq "PREPARING") { $newStatus = "READY" }
        elseif ($currentStatus -eq "READY") { $newStatus = "IN_TRANSIT" }
        elseif ($currentStatus -eq "IN_TRANSIT") { $newStatus = "DELIVERED" }
        
        try {
            $updateUrl = "$DELIVERY_SERVICE_URL/api/delivery/assignments/$assignmentId/status?status=$newStatus"
            $updated = Invoke-RestMethod -Uri $updateUrl `
                -Method PUT `
                -Headers @{"Authorization" = "Bearer $TOKEN"}
            
            Write-Host "Status updated successfully!" -ForegroundColor Green
            Write-Host "  New status: $($updated.data.status)" -ForegroundColor Cyan
        } catch {
            Write-Host "Update status failed: $($_.Exception.Message)" -ForegroundColor Red
            if ($_.ErrorDetails.Message) {
                Write-Host "  Details: $($_.ErrorDetails.Message)" -ForegroundColor Red
            }
        }
    } else {
        Write-Host "No assignments found. Please assign an order first." -ForegroundColor Yellow
    }
} catch {
    Write-Host "Get assignments failed: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails.Message) {
        Write-Host "  Details: $($_.ErrorDetails.Message)" -ForegroundColor Red
    }
}

# Step 4: Test unauthorized endpoints (should fail)
Write-Host ""
Write-Host "Step 4: Test unauthorized endpoints (should fail)..." -ForegroundColor Yellow

# Try to assign order (DELIVERY cannot assign)
Write-Host "  Testing assign order (should fail)..." -ForegroundColor Gray
try {
    $assignBody = @{
        orderId = 1
        storeId = "8d46e317-0596-4413-81b6-1a526398b3d7"
    } | ConvertTo-Json
    
    $result = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/assign" `
        -Method POST `
        -Body $assignBody `
        -ContentType "application/json" `
        -Headers @{"Authorization" = "Bearer $TOKEN"}
    
    Write-Host "  ERROR: Should have failed!" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 403) {
        Write-Host "  Correctly rejected (403 Forbidden)" -ForegroundColor Green
    } else {
        Write-Host "  Unexpected error: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "Test completed!" -ForegroundColor Green

