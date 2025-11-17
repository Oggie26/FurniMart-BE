# Script to check which accounts exist in the system and remove non-existent ones from TEST_ACCOUNTS.md

$baseUrl = "http://152.53.227.115:8086"
$adminEmail = "string@gmail.com"
$adminPassword = "string"

# Colors for output
function Write-Success { param($msg) Write-Host "[SUCCESS] $msg" -ForegroundColor Green }
function Write-Error { param($msg) Write-Host "[ERROR] $msg" -ForegroundColor Red }
function Write-Info { param($msg) Write-Host "[INFO] $msg" -ForegroundColor Cyan }
function Write-Warning { param($msg) Write-Host "[WARNING] $msg" -ForegroundColor Yellow }

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

# Step 2: List of accounts to check from TEST_ACCOUNTS.md
$accountsToCheck = @(
    @{ Email = "admin@furnimart.com"; Role = "ADMIN"; Section = "Admin Account 1" },
    @{ Email = "string@gmail.com"; Role = "ADMIN"; Section = "Admin Account 2" },
    @{ Email = "branchmanager@furnimart.com"; Role = "BRANCH_MANAGER"; Section = "1. BRANCH_MANAGER" },
    @{ Email = "staff@furnimart.com"; Role = "STAFF"; Section = "2. STAFF" },
    @{ Email = "delivery@furnimart.com"; Role = "DELIVERY"; Section = "3. DELIVERY" },
    @{ Email = "manager@furnimart.com"; Role = "BRANCH_MANAGER"; Section = "5. MANAGER" },
    @{ Email = "customer@gmail.com"; Role = "CUSTOMER"; Section = "6. CUSTOMER" }
)

$existingAccounts = @()
$nonExistentAccounts = @()

Write-Info "`nStep 2: Checking accounts in the system..."

foreach ($account in $accountsToCheck) {
    $email = $account.Email
    $role = $account.Role
    
    Write-Info "Checking: $email ($role)..."
    
    try {
        if ($role -eq "CUSTOMER") {
            # Check customer via /api/users/email/{email}
            $response = Invoke-RestMethod -Uri "$baseUrl/api/users/email/$email" -Method GET
            if ($response.data -and $response.data.email -eq $email) {
                Write-Success "  ✓ Account exists: $email"
                $existingAccounts += $account
            } else {
                Write-Warning "  ✗ Account not found: $email"
                $nonExistentAccounts += $account
            }
        } else {
            # Check employee via /api/employees/email/{email}
            $response = Invoke-RestMethod -Uri "$baseUrl/api/employees/email/$email" -Method GET -Headers $headers
            if ($response.data -and $response.data.email -eq $email) {
                Write-Success "  ✓ Account exists: $email"
                $existingAccounts += $account
            } else {
                Write-Warning "  ✗ Account not found: $email"
                $nonExistentAccounts += $account
            }
        }
    } catch {
        $statusCode = $null
        try {
            $statusCode = $_.Exception.Response.StatusCode.value__
        } catch {
            # Status code not available
        }
        
        if ($statusCode -eq 404 -or $statusCode -eq 403) {
            Write-Warning "  ✗ Account not found: $email (Status: $statusCode)"
            $nonExistentAccounts += $account
        } else {
            Write-Error "  ✗ Error checking $email : $_"
            # Assume it doesn't exist if we can't check
            $nonExistentAccounts += $account
        }
    }
}

# Step 3: Display summary
Write-Info "`n=== Summary ==="
Write-Success "Existing accounts: $($existingAccounts.Count)"
foreach ($acc in $existingAccounts) {
    Write-Host "  ✓ $($acc.Email) ($($acc.Role))"
}

Write-Warning "`nNon-existent accounts: $($nonExistentAccounts.Count)"
foreach ($acc in $nonExistentAccounts) {
    Write-Host "  ✗ $($acc.Email) ($($acc.Role)) - Section: $($acc.Section)"
}

# Step 4: Update TEST_ACCOUNTS.md
if ($nonExistentAccounts.Count -gt 0) {
    Write-Info "`nStep 3: Updating TEST_ACCOUNTS.md to remove non-existent accounts..."
    $testAccountsFile = "test-data\TEST_ACCOUNTS.md"
    
    if (Test-Path $testAccountsFile) {
        $content = Get-Content $testAccountsFile -Raw
        
        # Remove sections for non-existent accounts
        foreach ($acc in $nonExistentAccounts) {
            $section = $acc.Section
            Write-Info "Removing section: $section"
            
            # Escape special regex characters in section name
            $escapedSection = [regex]::Escape($section)
            
            # Pattern to match the section (from ### to next ### or end of file)
            $pattern = "(?s)###\s+$escapedSection.*?(?=###|`$)"
            $content = $content -replace $pattern, ""
        }
        
        # Clean up multiple empty lines (3 or more newlines become 2)
        $content = $content -replace "(`r`n){3,}", "`r`n`r`n"
        
        Set-Content -Path $testAccountsFile -Value $content -NoNewline
        Write-Success "TEST_ACCOUNTS.md updated! Removed $($nonExistentAccounts.Count) non-existent account(s)."
    } else {
        Write-Error "TEST_ACCOUNTS.md not found!"
    }
} else {
    Write-Success "`nAll accounts exist in the system! No changes needed."
}

Write-Success "`n✅ Check completed!"

