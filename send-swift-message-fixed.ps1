# PowerShell script to send complex XML message to MSMQ (Fixed Version)
$base64Auth = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("admin:admin123"))
$headers = @{Authorization="Basic $base64Auth"}

# Complex XML message content - properly escaped for JSON
$xmlMessage = '<RequestPayload xmlns="SWIFTNetBusinessEnvelope"><AppHdr xmlns="urn:iso:std:iso:20022:tech:xsd:head.001.001.01"><Fr><OrgId><Id><OrgId><AnyBIC>588990</AnyBIC></OrgId></Id></OrgId></Fr><To><OrgId><Id><OrgId><AnyBIC>593129</AnyBIC></OrgId></Id></OrgId></To><BizMsgIdr>ShareTransferInstruction</BizMsgIdr><MsgDefIdr>sese.023.001.06.xsd</MsgDefIdr><CreDt>2025-08-14T22:22:38.601927660Z</CreDt></AppHdr><Document xmlns="urn:iso:std:iso:20022:tech:xsd:sese.023.001.06"><SctiesSttlmTxInstr><TxId><AcctOwnrTxId>TX20250808-0002</AcctOwnrTxId></TxId><SttlmParams><SttlmDt>2025-08-15</SttlmDt><SctiesTxTp><Cd>TRAD</Cd></SctiesTxTp><SttlmTxCond><Cd>NOMC</Cd></SttlmTxCond></SttlmParams><TradDtls><TradDt>2025-08-15</TradDt><SctiesMvmntTp>DELIV</SctiesMvmntTp><Pmt>APMT</Pmt></TradDtls><FinInstrmId><ISIN>GB0002634946</ISIN><Nm>CRDB</Nm></FinInstrmId><QtyAndAcctDtls><SttlmQty><Qty><Unit>30</Unit></Qty></SttlmQty><SfkpgAcct><Id>1</Id></SfkpgAcct></QtyAndAcctDtls><SttlmPtiesSts><Pty1><Pty><Nm>ALI OMAR OTHMAN</Nm></Pty><Acct><Id>1</Id></Acct></Pty1><Pty2><Pty><Nm>CHRISTIAN KINDOLE</Nm></Pty><Acct><Id>ACC-REC-2020</Id></Acct></Pty2></SttlmPtiesSts><AddtlTxDtls><TxDesc>Settlement against payment</TxDesc></AddtlTxDtls></SctiesSttlmTxInstr></Document></RequestPayload>'

# Create request body
$body = @{
    body = $xmlMessage
    priority = 1
    correlationId = "SWIFT-TX-001"
} | ConvertTo-Json

# Send message to MSMQ via our API
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/msmq/queues/testqueue/messages" -Method POST -Body $body -ContentType "application/json" -Headers $headers
    Write-Host "Message sent successfully!" -ForegroundColor Green
    Write-Host "Response: $($response | ConvertTo-Json -Depth 10)"
} catch {
    Write-Host "Error sending message: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Status Code: $($_.Exception.Response.StatusCode.value__)"
}
