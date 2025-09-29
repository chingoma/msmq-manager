# Test script for queue sync endpoint
# This tests the specific queue synchronization functionality

$baseUrl = "http://localhost:8081/api"
$queueName = "securities-settlement-queue"

Write-Host "Testing Queue Sync Endpoint" -ForegroundColor Green
Write-Host "============================" -ForegroundColor Green
Write-Host "Base URL: $baseUrl" -ForegroundColor Cyan
Write-Host "Queue Name: $queueName" -ForegroundColor Cyan
Write-Host ""

# Test 1: Sync specific queue
Write-Host "1. Testing specific queue sync..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/queue-sync/sync/$queueName" -Method POST -ContentType "application/json"
    Write-Host "   Response: $($response | ConvertTo-Json -Depth 3)" -ForegroundColor Green
    Write-Host "   Status: SUCCESS" -ForegroundColor Green
} catch {
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "   Response Body: $responseBody" -ForegroundColor Red
    }
}

Write-Host ""

# Test 2: Get all queues to see if the synced queue appears
Write-Host "2. Testing get all queues..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/queues" -Method GET -ContentType "application/json"
    Write-Host "   Found $($response.data.Count) queues" -ForegroundColor Green
    foreach ($queue in $response.data) {
        Write-Host "   - $($queue.name) ($($queue.type))" -ForegroundColor Cyan
    }
} catch {
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Test 3: Get sync statistics
Write-Host "3. Testing sync statistics..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/queue-sync/statistics" -Method GET -ContentType "application/json"
    Write-Host "   Statistics: $($response | ConvertTo-Json -Depth 3)" -ForegroundColor Green
} catch {
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "Test completed!" -ForegroundColor Green
