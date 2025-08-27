# PowerShell script to test sending message using SWIFT template
$base64Auth = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("admin:admin123"))
$headers = @{Authorization="Basic $base64Auth"}

# Test parameters for the SWIFT template
$parameters = @{
    "FROM_BIC" = "588990"
    "TO_BIC" = "593129"
    "MESSAGE_TYPE" = "ShareTransferInstruction"
    "MSG_DEF_ID" = "sese.023.001.06.xsd"
    "CREATION_DATE" = "2025-08-14T22:22:38.601927660Z"
    "TRANSACTION_ID" = "TX20250808-0002"
    "SETTLEMENT_DATE" = "2025-08-15"
    "TRADE_TYPE" = "TRAD"
    "SETTLEMENT_CONDITION" = "NOMC"
    "TRADE_DATE" = "2025-08-15"
    "MOVEMENT_TYPE" = "DELIV"
    "PAYMENT_TYPE" = "APMT"
    "ISIN_CODE" = "GB0002634946"
    "SECURITY_NAME" = "CRDB"
    "QUANTITY" = "30"
    "ACCOUNT_ID" = "1"
    "PARTY1_NAME" = "ALI OMAR OTHMAN"
    "PARTY1_ACCOUNT" = "1"
    "PARTY2_NAME" = "CHRISTIAN KINDOLE"
    "PARTY2_ACCOUNT" = "ACC-REC-2020"
    "TRANSACTION_DESCRIPTION" = "Settlement against payment"
}

# First, let's check if the template exists
try {
    Write-Host "Checking if template exists..." -ForegroundColor Yellow
    $templateResponse = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/msmq/templates/SWIFT_SHARE_TRANSFER_DETAILED" -Method GET -Headers $headers
    
    Write-Host "Template found!" -ForegroundColor Green
    Write-Host "Template Name: $($templateResponse.data.templateName)" -ForegroundColor Cyan
    Write-Host "Template Type: $($templateResponse.data.templateType)" -ForegroundColor Cyan
    Write-Host "Description: $($templateResponse.data.description)" -ForegroundColor Cyan
    
} catch {
    Write-Host "Template not found. Please create it first using create-swift-template.ps1" -ForegroundColor Red
    exit
}

# Now let's validate the parameters
try {
    Write-Host "`nValidating template parameters..." -ForegroundColor Yellow
    $validationResponse = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/msmq/templates/SWIFT_SHARE_TRANSFER_DETAILED/validate" -Method POST -Body ($parameters | ConvertTo-Json) -ContentType "application/json" -Headers $headers
    
    if ($validationResponse.data) {
        Write-Host "Parameters are valid! ✓" -ForegroundColor Green
    } else {
        Write-Host "Parameters are invalid! ✗" -ForegroundColor Red
        exit
    }
    
} catch {
    Write-Host "Parameter validation failed: $($_.Exception.Message)" -ForegroundColor Red
    exit
}

# Now let's send the message using the template
try {
    Write-Host "`nSending message using template..." -ForegroundColor Yellow
    $sendResponse = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/msmq/templates/SWIFT_SHARE_TRANSFER_DETAILED/send?queueName=test-queue-006" -Method POST -Body ($parameters | ConvertTo-Json) -ContentType "application/json" -Headers $headers
    
    if ($sendResponse.data) {
        Write-Host "Message sent successfully using template! ✓" -ForegroundColor Green
        Write-Host "Response: $($sendResponse.message)" -ForegroundColor Cyan
    } else {
        Write-Host "Failed to send message using template! ✗" -ForegroundColor Red
        Write-Host "Response: $($sendResponse.message)" -ForegroundColor Red
    }
    
} catch {
    Write-Host "Error sending message: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        Write-Host "Status Code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    }
}

Write-Host "`nTemplate message test completed!" -ForegroundColor Yellow
