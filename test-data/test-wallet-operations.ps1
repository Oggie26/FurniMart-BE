# PowerShell Script to Test Wallet Operations
# Base URL: http://152.53.227.115:8086

$baseUrl = "http://152.53.227.115:8086"
$adminEmail = "admin@furnimart.com"
$adminPassword = "admin123"

function Write-Success { param($msg) Write-Host "[SUCCESS] $msg" -ForegroundColor Green }
function Write-Error { param($msg) Write-Host "[ERROR] $msg" -ForegroundColor Red }
function Write-Info { param($msg) Write-Host "[INFO] $msg" -ForegroundColor Cyan }
function Write-Warning { param($msg) Write-Host "[WARNING] $msg" -ForegroundColor Yellow }

Write-Host "`n=== TEST WALLET OPERATIONS ===" -ForegroundColor Cyan

# Step 1: Login as admin
Write-Info "Step 1: Logging in as admin..."
$loginBody = @{
    email = $adminEmail
    password = $adminPassword
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method POST -ContentType "application/json" -Body $loginBody
    $token = $loginResponse.data.token
    if (-not $token) {
        $token = $loginResponse.data.accessToken
    }
    Write-Success "Login successful!"
} catch {
    Write-Error "Failed to login: $_"
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

# Step 2: Get all wallets
Write-Info "`nStep 2: Getting all wallets..."
try {
    $walletsResponse = Invoke-RestMethod -Uri "$baseUrl/api/wallets" -Method GET -Headers $headers
    Write-Success "Found $($walletsResponse.data.Count) wallet(s)"
    
    if ($walletsResponse.data.Count -eq 0) {
        Write-Warning "No wallets found. You may need to create a customer account first."
        exit 0
    }
    
    # Display first wallet
    $firstWallet = $walletsResponse.data[0]
    Write-Host "`nFirst Wallet:" -ForegroundColor Yellow
    Write-Host "  ID: $($firstWallet.id)" -ForegroundColor White
    Write-Host "  Code: $($firstWallet.code)" -ForegroundColor White
    Write-Host "  Balance: $($firstWallet.balance)" -ForegroundColor White
    Write-Host "  Status: $($firstWallet.status)" -ForegroundColor White
    Write-Host "  User ID: $($firstWallet.userId)" -ForegroundColor White
    Write-Host "  User Name: $($firstWallet.userFullName)" -ForegroundColor White
    
    $walletId = $firstWallet.id
    $userId = $firstWallet.userId
    
} catch {
    Write-Error "Failed to get wallets: $_"
    exit 1
}

# Step 3: Get wallet by ID
Write-Info "`nStep 3: Getting wallet by ID..."
try {
    $walletResponse = Invoke-RestMethod -Uri "$baseUrl/api/wallets/$walletId" -Method GET -Headers $headers
    Write-Success "Wallet retrieved successfully"
    Write-Host "  Balance: $($walletResponse.data.balance)" -ForegroundColor White
} catch {
    Write-Error "Failed to get wallet: $_"
}

# Step 4: Get wallet by user ID
Write-Info "`nStep 4: Getting wallet by user ID..."
try {
    $walletByUserResponse = Invoke-RestMethod -Uri "$baseUrl/api/wallets/user/$userId" -Method GET -Headers $headers
    Write-Success "Wallet retrieved by user ID successfully"
} catch {
    Write-Error "Failed to get wallet by user ID: $_"
}

# Step 5: Get wallet balance
Write-Info "`nStep 5: Getting wallet balance..."
try {
    $balanceResponse = Invoke-RestMethod -Uri "$baseUrl/api/wallets/$walletId/balance" -Method GET -Headers $headers
    Write-Success "Current balance: $($balanceResponse.data)"
} catch {
    Write-Error "Failed to get balance: $_"
}

# Step 6: Deposit money
Write-Info "`nStep 6: Depositing 100.00 to wallet..."
try {
    $depositUrl = "$baseUrl/api/wallets/$walletId/deposit?amount=100.00&description=Test deposit&referenceId=TEST-DEP-001"
    $depositResponse = Invoke-RestMethod -Uri $depositUrl -Method POST -Headers $headers
    Write-Success "Deposit successful!"
    Write-Host "  New balance: $($depositResponse.data.balance)" -ForegroundColor White
} catch {
    Write-Error "Failed to deposit: $_"
    if ($_.ErrorDetails.Message) {
        Write-Host "  Details: $($_.ErrorDetails.Message)" -ForegroundColor Yellow
    }
}

# Step 7: Get transactions
Write-Info "`nStep 7: Getting wallet transactions..."
try {
    $transactionsResponse = Invoke-RestMethod -Uri "$baseUrl/api/wallets/$walletId/transactions" -Method GET -Headers $headers
    Write-Success "Found $($transactionsResponse.data.Count) transaction(s)"
    
    if ($transactionsResponse.data.Count -gt 0) {
        Write-Host "`nRecent Transactions:" -ForegroundColor Yellow
        $transactionsResponse.data | Select-Object -First 5 | ForEach-Object {
            Write-Host "  - $($_.type): $($_.amount) (Balance: $($_.balanceAfter))" -ForegroundColor White
        }
    }
} catch {
    Write-Error "Failed to get transactions: $_"
}

# Step 8: Create transaction manually
Write-Info "`nStep 8: Creating transaction manually..."
$transactionCode = "TXN-TEST-$(Get-Date -Format 'yyyyMMddHHmmss')"
$transactionBody = @{
    code = $transactionCode
    amount = 50.00
    type = "BONUS"
    description = "Test bonus transaction"
    referenceId = "TEST-REF-001"
    walletId = $walletId
} | ConvertTo-Json

try {
    $createTransactionResponse = Invoke-RestMethod -Uri "$baseUrl/api/wallets/transactions" -Method POST -Headers $headers -Body $transactionBody
    Write-Success "Transaction created successfully!"
    Write-Host "  Code: $($createTransactionResponse.data.code)" -ForegroundColor White
    Write-Host "  Amount: $($createTransactionResponse.data.amount)" -ForegroundColor White
    Write-Host "  Type: $($createTransactionResponse.data.type)" -ForegroundColor White
    Write-Host "  Balance After: $($createTransactionResponse.data.balanceAfter)" -ForegroundColor White
} catch {
    Write-Error "Failed to create transaction: $_"
    if ($_.ErrorDetails.Message) {
        Write-Host "  Details: $($_.ErrorDetails.Message)" -ForegroundColor Yellow
    }
}

# Step 9: Withdraw money (if balance is sufficient)
Write-Info "`nStep 9: Getting current balance before withdrawal..."
try {
    $currentBalanceResponse = Invoke-RestMethod -Uri "$baseUrl/api/wallets/$walletId/balance" -Method GET -Headers $headers
    $currentBalance = $currentBalanceResponse.data
    
    if ($currentBalance -ge 25.00) {
        Write-Info "Withdrawing 25.00 from wallet..."
        $withdrawUrl = "$baseUrl/api/wallets/$walletId/withdraw?amount=25.00&description=Test withdrawal&referenceId=TEST-WD-001"
        $withdrawResponse = Invoke-RestMethod -Uri $withdrawUrl -Method POST -Headers $headers
        Write-Success "Withdrawal successful!"
        Write-Host "  New balance: $($withdrawResponse.data.balance)" -ForegroundColor White
    } else {
        Write-Warning "Insufficient balance ($currentBalance) for withdrawal of 25.00"
    }
} catch {
    Write-Error "Failed to withdraw: $_"
    if ($_.ErrorDetails.Message) {
        Write-Host "  Details: $($_.ErrorDetails.Message)" -ForegroundColor Yellow
    }
}

# Step 10: Get transactions with pagination
Write-Info "`nStep 10: Getting transactions with pagination..."
try {
    $pagedTransactionsResponse = Invoke-RestMethod -Uri "$baseUrl/api/wallets/$walletId/transactions/paged?page=0&size=5" -Method GET -Headers $headers
    Write-Success "Paged transactions retrieved"
    Write-Host "  Total: $($pagedTransactionsResponse.data.totalElements)" -ForegroundColor White
    Write-Host "  Page: $($pagedTransactionsResponse.data.number + 1)/$($pagedTransactionsResponse.data.totalPages)" -ForegroundColor White
    Write-Host "  Items in this page: $($pagedTransactionsResponse.data.content.Count)" -ForegroundColor White
} catch {
    Write-Error "Failed to get paged transactions: $_"
}

Write-Success "`n✅ All wallet operations test completed!"

