# PowerShell script to create SWIFT Share Transfer Template
$base64Auth = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("admin:admin123"))
$headers = @{Authorization="Basic $base64Auth"}

# Template content with parameter placeholders
$templateContent = '<RequestPayload xmlns="SWIFTNetBusinessEnvelope"><AppHdr xmlns="urn:iso:std:iso:20022:tech:xsd:head.001.001.01"><Fr><OrgId><Id><OrgId><AnyBIC>{{FROM_BIC}}</AnyBIC></OrgId></Id></OrgId></Fr><To><OrgId><Id><OrgId><AnyBIC>{{TO_BIC}}</AnyBIC></OrgId></Id></OrgId></To><BizMsgIdr>{{MESSAGE_TYPE}}</BizMsgIdr><MsgDefIdr>{{MSG_DEF_ID}}</MsgDefIdr><CreDt>{{CREATION_DATE}}</CreDt></AppHdr><Document xmlns="urn:iso:std:iso:20022:tech:xsd:sese.023.001.06"><SctiesSttlmTxInstr><TxId><AcctOwnrTxId>{{TRANSACTION_ID}}</AcctOwnrTxId></TxId><SttlmParams><SttlmDt>{{SETTLEMENT_DATE}}</SttlmDt><SctiesTxTp><Cd>{{TRADE_TYPE}}</Cd></SctiesTxTp><SttlmTxCond><Cd>{{SETTLEMENT_CONDITION}}</Cd></SttlmTxCond></SttlmParams><TradDtls><TradDt>{{TRADE_DATE}}</TradDt><SctiesMvmntTp>{{MOVEMENT_TYPE}}</SctiesMvmntTp><Pmt>{{PAYMENT_TYPE}}</Pmt></TradDtls><FinInstrmId><ISIN>{{ISIN_CODE}}</ISIN><Nm>{{SECURITY_NAME}}</Nm></FinInstrmId><QtyAndAcctDtls><SttlmQty><Qty><Unit>{{QUANTITY}}</Unit></Qty></SttlmQty><SfkpgAcct><Id>{{ACCOUNT_ID}}</Id></SfkpgAcct></QtyAndAcctDtls><SttlmPtiesSts><Pty1><Pty><Nm>{{PARTY1_NAME}}</Nm></Pty><Acct><Id>{{PARTY1_ACCOUNT}}</Id></Acct></Pty1><Pty2><Pty><Nm>{{PARTY2_NAME}}</Nm></Pty><Acct><Id>{{PARTY2_ACCOUNT}}</Id></Acct></Pty2></SttlmPtiesSts><AddtlTxDtls><TxDesc>{{TRANSACTION_DESCRIPTION}}</TxDesc></AddtlTxDtls></SctiesSttlmTxInstr></Document></RequestPayload>'

# Template parameters definition
$parameters = @{
    "FROM_BIC" = "string"
    "TO_BIC" = "string"
    "MESSAGE_TYPE" = "string"
    "MSG_DEF_ID" = "string"
    "CREATION_DATE" = "datetime"
    "TRANSACTION_ID" = "string"
    "SETTLEMENT_DATE" = "date"
    "TRADE_TYPE" = "string"
    "SETTLEMENT_CONDITION" = "string"
    "TRADE_DATE" = "date"
    "MOVEMENT_TYPE" = "string"
    "PAYMENT_TYPE" = "string"
    "ISIN_CODE" = "string"
    "SECURITY_NAME" = "string"
    "QUANTITY" = "number"
    "ACCOUNT_ID" = "string"
    "PARTY1_NAME" = "string"
    "PARTY1_ACCOUNT" = "string"
    "PARTY2_NAME" = "string"
    "PARTY2_ACCOUNT" = "string"
    "TRANSACTION_DESCRIPTION" = "string"
}

# Create request body
$body = @{
    templateName = "SWIFT_SHARE_TRANSFER_DETAILED"
    templateType = "SWIFT"
    templateContent = $templateContent
    description = "Detailed SWIFT Share Transfer Instruction Template with all parameters"
    parameters = $parameters
    isActive = $true
    createdBy = "System"
} | ConvertTo-Json -Depth 10

# Send request to create template
try {
    Write-Host "Creating SWIFT Share Transfer Template..." -ForegroundColor Yellow
    $response = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/msmq/templates" -Method POST -Body $body -ContentType "application/json" -Headers $headers
    
    Write-Host "Template created successfully!" -ForegroundColor Green
    Write-Host "Template ID: $($response.data.id)" -ForegroundColor Cyan
    Write-Host "Template Name: $($response.data.templateName)" -ForegroundColor Cyan
    Write-Host "Message: $($response.message)" -ForegroundColor Cyan
    
} catch {
    Write-Host "Error creating template: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        Write-Host "Status Code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
        Write-Host "Response: $($_.Exception.Response)" -ForegroundColor Red
    }
}

Write-Host "`nTemplate creation completed!" -ForegroundColor Yellow
