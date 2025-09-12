# Test Securities Settlement with PowerShell MSMQ
$base64AuthInfo = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes('admin:admin123'))
$headers = @{Authorization = "Basic $base64AuthInfo"}

Write-Host "Testing Securities Settlement with PowerShell MSMQ..." -ForegroundColor Yellow

try {
    $response = Invoke-RestMethod -Uri 'http://localhost:8081/api/v1/securities-settlement/test' -Method POST -ContentType 'application/json' -Headers $headers -Body '{"testType": "FULL"}'
    Write-Host "Test Response:" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 3
} catch {
    Write-Host "Test Failed:" -ForegroundColor Red
    Write-Host $_.Exception.Message
}

Write-Host "`nTest completed!"
