# Test script for the fixed queue sync endpoint
# This tests both valid and invalid queue names

$baseUrl = "http://localhost:8081/api"

Write-Host "Testing Fixed Queue Sync Endpoint" -ForegroundColor Green
Write-Host "===================================" -ForegroundColor Green
Write-Host "Base URL: $baseUrl" -ForegroundColor Cyan
Write-Host ""

# Test 1: Valid queue name (should succeed)
Write-Host "1. Testing VALID queue sync (securities-settlement-queue)..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/queue-sync/sync/securities-settlement-queue" -Method POST -ContentType "application/json"
    Write-Host "   Status: $($response.success)" -ForegroundColor Green
    Write-Host "   Message: $($response.message)" -ForegroundColor Green
    Write-Host "   Data: $($response.data)" -ForegroundColor Green
} catch {
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Test 2: Invalid queue name (should fail)
Write-Host "2. Testing INVALID queue sync (securities-settlement-queuesss)..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/queue-sync/sync/securities-settlement-queuesss" -Method POST -ContentType "application/json"
    Write-Host "   Status: $($response.success)" -ForegroundColor Red
    Write-Host "   Message: $($response.message)" -ForegroundColor Red
    Write-Host "   Data: $($response.data)" -ForegroundColor Red
} catch {
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "   Response Body: $responseBody" -ForegroundColor Red
    }
}

Write-Host ""

# Test 3: Another valid queue name
Write-Host "3. Testing another VALID queue sync (testqueue)..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/queue-sync/sync/testqueue" -Method POST -ContentType "application/json"
    Write-Host "   Status: $($response.success)" -ForegroundColor Green
    Write-Host "   Message: $($response.message)" -ForegroundColor Green
    Write-Host "   Data: $($response.data)" -ForegroundColor Green
} catch {
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Test 4: Get all queues to see what was synced
Write-Host "4. Getting all queues to see synced results..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/queues" -Method GET -ContentType "application/json"
    Write-Host "   Found $($response.data.Count) queues" -ForegroundColor Green
    foreach ($queue in $response.data) {
        Write-Host "   - $($queue.name) ($($queue.type)) - $($queue.status)" -ForegroundColor Cyan
    }
} catch {
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "Test completed!" -ForegroundColor Green
