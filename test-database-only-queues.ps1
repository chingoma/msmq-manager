# Test script for database-only queue fetching
# This tests the /api/msmq/queues endpoint with database-only approach

$baseUrl = "http://localhost:8081/api"

Write-Host "Testing Database-Only Queue Fetching" -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Green
Write-Host "Base URL: $baseUrl" -ForegroundColor Cyan
Write-Host ""

# Test 1: Get all queues (should fetch from database only)
Write-Host "1. Testing database-only queue listing..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/msmq/queues" -Method GET -ContentType "application/json"
    Write-Host "   Status: $($response.success)" -ForegroundColor Green
    Write-Host "   Message: $($response.message)" -ForegroundColor Green
    Write-Host "   Found $($response.data.Count) queues from database" -ForegroundColor Green
    
    if ($response.data.Count -gt 0) {
        Write-Host "   Queues from database:" -ForegroundColor Cyan
        foreach ($queue in $response.data) {
            $status = if ($queue.description -like "*ACTIVE*") { "ACTIVE" } elseif ($queue.description -like "*INACTIVE*") { "INACTIVE" } else { "UNKNOWN" }
            Write-Host "   - $($queue.name) ($($queue.type)) - $status" -ForegroundColor Cyan
        }
    } else {
        Write-Host "   No queues found in database" -ForegroundColor Yellow
    }
} catch {
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "   Response Body: $responseBody" -ForegroundColor Red
    }
}

Write-Host ""

# Test 2: Sync some queues to populate database
Write-Host "2. Syncing queues to populate database..." -ForegroundColor Yellow
$queuesToSync = @("securities-settlement-queue", "testqueue", "orders-queue")

foreach ($queueName in $queuesToSync) {
    try {
        Write-Host "   Syncing queue: $queueName" -ForegroundColor Cyan
        $response = Invoke-RestMethod -Uri "$baseUrl/queue-sync/sync/$queueName" -Method POST -ContentType "application/json"
        if ($response.success) {
            Write-Host "   ✓ $queueName synced successfully" -ForegroundColor Green
        } else {
            Write-Host "   ✗ $queueName sync failed: $($response.message)" -ForegroundColor Red
        }
    } catch {
        Write-Host "   ✗ $queueName sync error: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""

# Test 3: Get all queues again (should now show synced queues)
Write-Host "3. Testing queue listing after sync..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/msmq/queues" -Method GET -ContentType "application/json"
    Write-Host "   Status: $($response.success)" -ForegroundColor Green
    Write-Host "   Message: $($response.message)" -ForegroundColor Green
    Write-Host "   Found $($response.data.Count) queues from database" -ForegroundColor Green
    
    if ($response.data.Count -gt 0) {
        Write-Host "   All queues from database (including inactive):" -ForegroundColor Cyan
        foreach ($queue in $response.data) {
            $status = if ($queue.description -like "*ACTIVE*") { "ACTIVE" } elseif ($queue.description -like "*INACTIVE*") { "INACTIVE" } else { "UNKNOWN" }
            Write-Host "   - $($queue.name) ($($queue.type)) - $status" -ForegroundColor Cyan
        }
    }
} catch {
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Test 4: Test with invalid queue sync (should create inactive entry)
Write-Host "4. Testing invalid queue sync (should create inactive entry)..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/queue-sync/sync/invalid-queue-name" -Method POST -ContentType "application/json"
    if ($response.success) {
        Write-Host "   ✓ Invalid queue sync handled successfully" -ForegroundColor Green
    } else {
        Write-Host "   ✗ Invalid queue sync failed: $($response.message)" -ForegroundColor Red
    }
} catch {
    Write-Host "   ✗ Invalid queue sync error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Test 5: Final queue listing (should show all queues including inactive)
Write-Host "5. Final queue listing (all queues including inactive)..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/msmq/queues" -Method GET -ContentType "application/json"
    Write-Host "   Status: $($response.success)" -ForegroundColor Green
    Write-Host "   Found $($response.data.Count) total queues from database" -ForegroundColor Green
    
    if ($response.data.Count -gt 0) {
        Write-Host "   Complete queue list:" -ForegroundColor Cyan
        foreach ($queue in $response.data) {
            $status = if ($queue.description -like "*ACTIVE*") { "ACTIVE" } elseif ($queue.description -like "*INACTIVE*") { "INACTIVE" } else { "UNKNOWN" }
            $color = if ($status -eq "ACTIVE") { "Green" } elseif ($status -eq "INACTIVE") { "Red" } else { "Yellow" }
            Write-Host "   - $($queue.name) ($($queue.type)) - $status" -ForegroundColor $color
        }
    }
} catch {
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "Database-only queue fetching test completed!" -ForegroundColor Green
Write-Host "All queue data is now fetched from database regardless of MSMQ status." -ForegroundColor Green
