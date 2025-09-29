# Test script for database fallback when remote queue fetching fails
# This tests the /api/msmq/queues endpoint with database fallback

$baseUrl = "http://localhost:8081/api"

Write-Host "Testing Database Fallback for Queue Listing" -ForegroundColor Green
Write-Host "===========================================" -ForegroundColor Green
Write-Host "Base URL: $baseUrl" -ForegroundColor Cyan
Write-Host ""

# Test 1: Get all queues (should fallback to database when remote fails)
Write-Host "1. Testing queue listing with database fallback..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/msmq/queues" -Method GET -ContentType "application/json"
    Write-Host "   Status: $($response.success)" -ForegroundColor Green
    Write-Host "   Message: $($response.message)" -ForegroundColor Green
    Write-Host "   Found $($response.data.Count) queues" -ForegroundColor Green
    
    if ($response.data.Count -gt 0) {
        Write-Host "   Queues from database:" -ForegroundColor Cyan
        foreach ($queue in $response.data) {
            Write-Host "   - $($queue.name) ($($queue.type)) - $($queue.status)" -ForegroundColor Cyan
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

# Test 2: Sync a queue first to populate database
Write-Host "2. Syncing a queue to populate database..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/queue-sync/sync/securities-settlement-queue" -Method POST -ContentType "application/json"
    Write-Host "   Status: $($response.success)" -ForegroundColor Green
    Write-Host "   Message: $($response.message)" -ForegroundColor Green
} catch {
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Test 3: Get all queues again (should now show the synced queue)
Write-Host "3. Testing queue listing after sync..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/msmq/queues" -Method GET -ContentType "application/json"
    Write-Host "   Status: $($response.success)" -ForegroundColor Green
    Write-Host "   Message: $($response.message)" -ForegroundColor Green
    Write-Host "   Found $($response.data.Count) queues" -ForegroundColor Green
    
    if ($response.data.Count -gt 0) {
        Write-Host "   Queues from database:" -ForegroundColor Cyan
        foreach ($queue in $response.data) {
            Write-Host "   - $($queue.name) ($($queue.type)) - $($queue.status)" -ForegroundColor Cyan
        }
    }
} catch {
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "Test completed!" -ForegroundColor Green
