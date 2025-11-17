# Helper Functions cho Delivery Service Testing
# PowerShell Script

$DELIVERY_SERVICE_URL = "http://152.53.227.115:8089"
$ORDER_SERVICE_URL = "http://152.53.227.115:8087"
$INVENTORY_SERVICE_URL = "http://152.53.227.115:8083"
$USER_SERVICE_URL = "http://152.53.227.115:8086"

# ============================================
# VALIDATION FUNCTIONS
# ============================================

function Test-ValidateAssignOrderRequest {
    param(
        [Parameter(Mandatory=$true)]
        $OrderId,
        
        [Parameter(Mandatory=$true)]
        $StoreId
    )
    
    $errors = @()
    
    if (-not $OrderId) {
        $errors += "Order ID is required"
    } elseif ($OrderId -le 0) {
        $errors += "Order ID must be positive"
    }
    
    if (-not $StoreId) {
        $errors += "Store ID is required"
    } elseif ($StoreId -notmatch "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$") {
        $errors += "Store ID must be a valid UUID"
    }
    
    if ($errors.Count -gt 0) {
        Write-Host "Validation errors:" -ForegroundColor Red
        $errors | ForEach-Object { Write-Host "  - $_" -ForegroundColor Yellow }
        return $false
    }
    
    return $true
}

# ============================================
# STATUS CHECK FUNCTIONS
# ============================================

function Get-AssignmentStatus {
    param(
        [Parameter(Mandatory=$true)]
        $OrderId,
        
        [Parameter(Mandatory=$true)]
        $Token
    )
    
    try {
        $assignment = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/assignments/order/$OrderId" `
            -Method GET `
            -Headers @{"Authorization" = "Bearer $Token"}
        
        return @{
            Exists = $true
            Assignment = $assignment.data
            Status = $assignment.data.status
            InvoiceGenerated = $assignment.data.invoiceGenerated
            ProductsPrepared = $assignment.data.productsPrepared
        }
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -eq 404) {
            return @{
                Exists = $false
                Assignment = $null
            }
        }
        throw
    }
}

function Test-CanAssignOrder {
    param(
        [Parameter(Mandatory=$true)]
        $OrderId,
        
        [Parameter(Mandatory=$true)]
        $Token
    )
    
    $status = Get-AssignmentStatus -OrderId $OrderId -Token $Token
    
    if ($status.Exists) {
        Write-Host "Order đã được assign rồi!" -ForegroundColor Yellow
        Write-Host "  Assignment ID: $($status.Assignment.id)" -ForegroundColor Cyan
        Write-Host "  Status: $($status.Status)" -ForegroundColor Cyan
        return $false
    }
    
    Write-Host "Order chưa được assign. Có thể assign." -ForegroundColor Green
    return $true
}

function Test-CanGenerateInvoice {
    param(
        [Parameter(Mandatory=$true)]
        $OrderId,
        
        [Parameter(Mandatory=$true)]
        $Token
    )
    
    $status = Get-AssignmentStatus -OrderId $OrderId -Token $Token
    
    if (-not $status.Exists) {
        Write-Host "Order chưa được assign. Cần assign trước." -ForegroundColor Yellow
        return $false
    }
    
    if ($status.InvoiceGenerated) {
        Write-Host "Invoice đã được generate rồi!" -ForegroundColor Yellow
        return $false
    }
    
    Write-Host "Invoice chưa được generate. Có thể generate." -ForegroundColor Green
    return $true
}

function Test-CanPrepareProducts {
    param(
        [Parameter(Mandatory=$true)]
        $OrderId,
        
        [Parameter(Mandatory=$true)]
        $Token
    )
    
    $status = Get-AssignmentStatus -OrderId $OrderId -Token $Token
    
    if (-not $status.Exists) {
        Write-Host "Order chưa được assign. Cần assign trước." -ForegroundColor Yellow
        return $false
    }
    
    if ($status.ProductsPrepared) {
        Write-Host "Products đã được prepare rồi!" -ForegroundColor Yellow
        return $false
    }
    
    # Kiểm tra stock
    Write-Host "Đang kiểm tra stock..." -ForegroundColor Cyan
    try {
        $order = Invoke-RestMethod -Uri "$ORDER_SERVICE_URL/api/orders/$OrderId" `
            -Method GET `
            -Headers @{"Authorization" = "Bearer $Token"}
        
        $canPrepare = $true
        $insufficientProducts = @()
        
        if ($order.data.orderDetails) {
            foreach ($detail in $order.data.orderDetails) {
                try {
                    $stockResponse = Invoke-RestMethod -Uri "$INVENTORY_SERVICE_URL/api/inventories/stock/total-available?productColorId=$($detail.productColorId)" `
                        -Method GET `
                        -Headers @{"Authorization" = "Bearer $Token"}
                    
                    $availableStock = $stockResponse.data
                    if ($availableStock -lt $detail.quantity) {
                        $canPrepare = $false
                        $insufficientProducts += @{
                            ProductColorId = $detail.productColorId
                            Required = $detail.quantity
                            Available = $availableStock
                            Shortage = $detail.quantity - $availableStock
                        }
                    }
                } catch {
                    Write-Host "  Không thể kiểm tra stock cho product $($detail.productColorId)" -ForegroundColor Yellow
                }
            }
        }
        
        if (-not $canPrepare) {
            Write-Host "Stock không đủ:" -ForegroundColor Red
            $insufficientProducts | ForEach-Object {
                Write-Host "  - Product: $($_.ProductColorId)" -ForegroundColor Yellow
                Write-Host "    Required: $($_.Required), Available: $($_.Available), Shortage: $($_.Shortage)" -ForegroundColor Yellow
            }
            return $false
        }
        
        Write-Host "Stock đủ. Có thể prepare products." -ForegroundColor Green
        return $true
    } catch {
        Write-Host "Không thể kiểm tra stock: $($_.Exception.Message)" -ForegroundColor Yellow
        Write-Host "Vẫn sẽ thử prepare products..." -ForegroundColor Yellow
        return $true  # Cho phép thử
    }
}

# ============================================
# SAFE OPERATION FUNCTIONS
# ============================================

function Invoke-AssignOrderSafely {
    param(
        [Parameter(Mandatory=$true)]
        $OrderId,
        
        [Parameter(Mandatory=$true)]
        $StoreId,
        
        [Parameter(Mandatory=$true)]
        $Token,
        
        $DeliveryStaffId = $null,
        $EstimatedDeliveryDate = $null,
        $Notes = $null
    )
    
    # Pre-flight check 1: Validate request
    if (-not (Test-ValidateAssignOrderRequest -OrderId $OrderId -StoreId $StoreId)) {
        return $null
    }
    
    # Pre-flight check 2: Check if already assigned
    if (-not (Test-CanAssignOrder -OrderId $OrderId -Token $Token)) {
        $status = Get-AssignmentStatus -OrderId $OrderId -Token $Token
        if ($status.Exists) {
            Write-Host "Trả về assignment hiện tại thay vì tạo mới." -ForegroundColor Cyan
            return @{
                Success = $false
                AlreadyExists = $true
                Assignment = $status.Assignment
            }
        }
        return $null
    }
    
    # All checks passed, proceed with assignment
    try {
        $assignBody = @{
            orderId = $OrderId
            storeId = $StoreId
        }
        
        if ($DeliveryStaffId) { $assignBody.deliveryStaffId = $DeliveryStaffId }
        if ($EstimatedDeliveryDate) { $assignBody.estimatedDeliveryDate = $EstimatedDeliveryDate }
        if ($Notes) { $assignBody.notes = $Notes }
        
        $response = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/assign" `
            -Method POST `
            -Body ($assignBody | ConvertTo-Json) `
            -ContentType "application/json" `
            -Headers @{"Authorization" = "Bearer $Token"}
        
        Write-Host "Assign order thành công!" -ForegroundColor Green
        return @{
            Success = $true
            AlreadyExists = $false
            Assignment = $response.data
        }
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -eq 400) {
            $errorBody = $null
            try {
                $errorBody = $_.ErrorDetails.Message | ConvertFrom-Json
            } catch {
                # Ignore
            }
            
            if ($errorBody -and $errorBody.message -like "*existed*") {
                Write-Host "Order đã được assign rồi. Lấy assignment hiện tại..." -ForegroundColor Yellow
                $status = Get-AssignmentStatus -OrderId $OrderId -Token $Token
                if ($status.Exists) {
                    return @{
                        Success = $false
                        AlreadyExists = $true
                        Assignment = $status.Assignment
                    }
                }
            }
        }
        Write-Host "Lỗi khi assign order: $($_.Exception.Message)" -ForegroundColor Red
        throw
    }
}

function Invoke-GenerateInvoiceSafely {
    param(
        [Parameter(Mandatory=$true)]
        $OrderId,
        
        [Parameter(Mandatory=$true)]
        $Token
    )
    
    # Pre-flight check
    if (-not (Test-CanGenerateInvoice -OrderId $OrderId -Token $Token)) {
        $status = Get-AssignmentStatus -OrderId $OrderId -Token $Token
        if ($status.Exists -and $status.InvoiceGenerated) {
            Write-Host "Invoice đã được generate. Trả về assignment hiện tại." -ForegroundColor Cyan
            return @{
                Success = $false
                AlreadyGenerated = $true
                Assignment = $status.Assignment
            }
        }
        return $null
    }
    
    # All checks passed, proceed
    try {
        $response = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/generate-invoice/$OrderId" `
            -Method POST `
            -Headers @{"Authorization" = "Bearer $Token"}
        
        Write-Host "Generate invoice thành công!" -ForegroundColor Green
        return @{
            Success = $true
            AlreadyGenerated = $false
            Assignment = $response.data
        }
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -eq 400) {
            $errorBody = $null
            try {
                $errorBody = $_.ErrorDetails.Message | ConvertFrom-Json
            } catch {
                # Ignore
            }
            
            if ($errorBody -and $errorBody.message -like "*existed*") {
                Write-Host "Invoice đã được generate rồi. Lấy assignment hiện tại..." -ForegroundColor Yellow
                $status = Get-AssignmentStatus -OrderId $OrderId -Token $Token
                if ($status.Exists) {
                    return @{
                        Success = $false
                        AlreadyGenerated = $true
                        Assignment = $status.Assignment
                    }
                }
            }
        }
        Write-Host "Lỗi khi generate invoice: $($_.Exception.Message)" -ForegroundColor Red
        throw
    }
}

function Invoke-PrepareProductsSafely {
    param(
        [Parameter(Mandatory=$true)]
        $OrderId,
        
        [Parameter(Mandatory=$true)]
        $Token,
        
        $Notes = $null
    )
    
    # Pre-flight check
    if (-not (Test-CanPrepareProducts -OrderId $OrderId -Token $Token)) {
        $status = Get-AssignmentStatus -OrderId $OrderId -Token $Token
        if ($status.Exists -and $status.ProductsPrepared) {
            Write-Host "Products đã được prepare. Trả về assignment hiện tại." -ForegroundColor Cyan
            return @{
                Success = $false
                AlreadyPrepared = $true
                Assignment = $status.Assignment
            }
        }
        return $null
    }
    
    # All checks passed, proceed
    try {
        $prepareBody = @{
            orderId = $OrderId
        }
        if ($Notes) { $prepareBody.notes = $Notes }
        
        $response = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/prepare-products" `
            -Method POST `
            -Body ($prepareBody | ConvertTo-Json) `
            -ContentType "application/json" `
            -Headers @{"Authorization" = "Bearer $Token"}
        
        Write-Host "Prepare products thành công!" -ForegroundColor Green
        return @{
            Success = $true
            AlreadyPrepared = $false
            Assignment = $response.data
        }
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -eq 400) {
            $errorBody = $null
            try {
                $errorBody = $_.ErrorDetails.Message | ConvertFrom-Json
            } catch {
                # Ignore
            }
            
            if ($errorBody -and $errorBody.message -like "*existed*") {
                Write-Host "Products đã được prepare rồi. Lấy assignment hiện tại..." -ForegroundColor Yellow
                $status = Get-AssignmentStatus -OrderId $OrderId -Token $Token
                if ($status.Exists) {
                    return @{
                        Success = $false
                        AlreadyPrepared = $true
                        Assignment = $status.Assignment
                    }
                }
            }
        }
        Write-Host "Lỗi khi prepare products: $($_.Exception.Message)" -ForegroundColor Red
        throw
    }
}

# ============================================
# USAGE EXAMPLE
# ============================================

<#
# Example usage:

# Import helper functions
. .\delivery-test-helpers.ps1

# Login first
$loginResponse = Invoke-RestMethod -Uri "$BASE_URL/api/auth/login" `
    -Method POST `
    -Body (@{email="staff@furnimart.com"; password="Staff@123"} | ConvertTo-Json) `
    -ContentType "application/json"
$TOKEN = $loginResponse.data.token

# Safe assign order
$result = Invoke-AssignOrderSafely -OrderId 1 -StoreId "8d46e317-0596-4413-81b6-1a526398b3d7" -Token $TOKEN
if ($result.Success) {
    Write-Host "Assignment ID: $($result.Assignment.id)"
} elseif ($result.AlreadyExists) {
    Write-Host "Order đã được assign. Assignment ID: $($result.Assignment.id)"
}

# Safe generate invoice
$invoiceResult = Invoke-GenerateInvoiceSafely -OrderId 1 -Token $TOKEN

# Safe prepare products
$prepareResult = Invoke-PrepareProductsSafely -OrderId 1 -Token $TOKEN -Notes "Prepared by staff"
#>

