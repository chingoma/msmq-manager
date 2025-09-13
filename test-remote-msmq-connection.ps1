# Test script to verify remote MSMQ connection and send XML message
# Usage: .\test-remote-msmq-connection.ps1

$remoteHost = "192.168.2.170"
$queueName = "crdb_to_dse"

# MSMQ format names
$tcpQueuePath = "FormatName:DIRECT=TCP:$remoteHost\private$\$queueName"
$osQueuePath  = "FormatName:DIRECT=OS:$remoteHost\private$\$queueName"

Write-Host "Testing remote MSMQ connection..." -ForegroundColor Yellow
Write-Host "Remote Host: $remoteHost" -ForegroundColor Cyan
Write-Host "Queue Name: $queueName" -ForegroundColor Cyan
Write-Host "TCP Path: $tcpQueuePath" -ForegroundColor Cyan
Write-Host "OS Path: $osQueuePath" -ForegroundColor Cyan
Write-Host ""

try {
    Write-Host "Step 1: Loading System.Messaging assembly..." -ForegroundColor Green
    Add-Type -AssemblyName System.Messaging
    Write-Host "✅ System.Messaging loaded successfully" -ForegroundColor Green

    Write-Host ""
    Write-Host "Step 2: Testing network connectivity to $remoteHost..." -ForegroundColor Green
    if (Test-Connection -ComputerName $remoteHost -Count 1 -Quiet) {
        Write-Host "✅ Network connectivity to $remoteHost is working" -ForegroundColor Green
    } else {
        Write-Host "❌ Cannot ping $remoteHost - network connectivity issue" -ForegroundColor Red
        exit 1
    }

    Write-Host ""
    Write-Host "Step 3: Testing TCP port 1801 (MSMQ TCP port)..." -ForegroundColor Green
    $tcpTest = Test-NetConnection -ComputerName $remoteHost -Port 1801 -WarningAction SilentlyContinue
    if ($tcpTest.TcpTestSucceeded) {
        Write-Host "✅ TCP port 1801 is open on $remoteHost" -ForegroundColor Green
    } else {
        Write-Host "⚠️ TCP port 1801 is not accessible on $remoteHost" -ForegroundColor Yellow
        Write-Host "   Will try OS (native) protocol instead..." -ForegroundColor Yellow
    }

    # Step 4: Create MessageQueue object
    Write-Host ""
    Write-Host "Step 4: Creating MessageQueue object..." -ForegroundColor Green
    try {
        $queue = New-Object System.Messaging.MessageQueue $tcpQueuePath
        Write-Host "✅ TCP format MessageQueue created" -ForegroundColor Green
        $usingTcp = $true
    } catch {
        Write-Host "⚠️ TCP format failed: $($_.Exception.Message)" -ForegroundColor Yellow
        Write-Host "Trying OS (native) format..." -ForegroundColor Green
        $queue = New-Object System.Messaging.MessageQueue $osQueuePath
        Write-Host "✅ OS format MessageQueue created" -ForegroundColor Green
        $usingTcp = $false
    }

    # Step 5: Set formatter for XML
    Write-Host ""
    Write-Host "Step 5: Setting up XmlMessageFormatter..." -ForegroundColor Green
    $queue.Formatter = New-Object System.Messaging.XmlMessageFormatter([Type[]]@( [System.Xml.XmlDocument] ))
    Write-Host "✅ XmlMessageFormatter set successfully" -ForegroundColor Green

    # Step 6: Load XML message
    Write-Host ""
    Write-Host "Step 6: Loading XML message..." -ForegroundColor Green
    $xmlString = @"
<?xml version="1.0" encoding="utf-8"?>
<RequestPayload xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xmlns:xsd="http://www.w3.org/2001/XMLSchema"
xmlns="SWIFTNetBusinessEnvelope">
  <AppHdr xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:xsd="http://www.w3.org/2001/XMLSchema"
  xmlns="urn:iso:std:iso:20022:tech:xsd:head.001.001.01">
    <Fr>
      <OrgId>
        <Id>
          <OrgId>
            <AnyBIC>SAFMXXXXXXX</AnyBIC>
          </OrgId>
        </Id>
      </OrgId>
    </Fr>
    <To>
      <OrgId>
        <Id>
          <OrgId>
            <AnyBIC>DSTXTZTZXXX</AnyBIC>
          </OrgId>
        </Id>
      </OrgId>
    </To>
    <BizMsgIdr>Matched Deal Report</BizMsgIdr>
    <MsgDefIdr>sese.023.001.11.xsd</MsgDefIdr>
    <CreDt>2025-08-28T12:59:39.532578Z</CreDt>
  </AppHdr>
  <Document xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:xsd="http://www.w3.org/2001/XMLSchema"
  xmlns="urn:iso:std:iso:20022:tech:xsd:sese.023.001.11">
    <SctiesSttlmTxInstr>
      <TxId>01C83692B</TxId>
      <SttlmTpAndAddtlParams>
        <SctiesMvmntTp>DELI</SctiesMvmntTp>
        <Pmt>FREE</Pmt>
        <CmonId>616964F32</CmonId>
      </SttlmTpAndAddtlParams>
      <NbCounts>
        <TtlNb>
          <CurInstrNb>002</CurInstrNb>
          <TtlOfLkdInstrs>002</TtlOfLkdInstrs>
        </TtlNb>
      </NbCounts>
      <Lnkgs>
        <Ref>
          <OthrTxId>01C83692A</OthrTxId>
        </Ref>
      </Lnkgs>
      <TradDtls>
        <TradId>01C83692B</TradId>
        <TradId>616964F32</TradId>
        <PlcOfTrad>
          <MktTpAndId>
            <Id>
              <MktIdrCd>SAFM</MktIdrCd>
            </Id>
            <Tp>
              <Cd>OTCO</Cd>
            </Tp>
          </MktTpAndId>
        </PlcOfTrad>
        <TradDt>
          <Dt>
            <DtTm>2025-08-29T15:59:37</DtTm>
          </Dt>
        </TradDt>
        <SttlmDt>
          <Dt>
            <Dt>2025-09-03</Dt>
          </Dt>
        </SttlmDt>
        <DealPric>
          <Tp>
            <Yldd>false</Yldd>
          </Tp>
          <Val>
            <Amt Ccy="TZS">0</Amt>
          </Val>
        </DealPric>
        <TradTxCond>
          <Cd>MAPR</Cd>
        </TradTxCond>
        <TradOrgtrRole>
          <Prtry>
            <Id>MNOn</Id>
            <Issr>DSTXTZTZXXX</Issr>
            <SchmeNm>ORDER PLACEMENT PLATFORM</SchmeNm>
          </Prtry>
        </TradOrgtrRole>
        <MtchgSts>
          <Cd>MACH</Cd>
        </MtchgSts>
      </TradDtls>
      <FinInstrmId>
        <ISIN>TZ1996100214</ISIN>
        <Desc>DCB</Desc>
      </FinInstrmId>
      <QtyAndAcctDtls>
        <SttlmQty>
          <Qty>
            <FaceAmt>10</FaceAmt>
          </Qty>
        </SttlmQty>
        <AcctOwnr>
          <Id>
            <PrtryId>
              <Id>593129</Id>
              <Issr>CSD</Issr>
              <SchmeNm>SOR ACCOUNT</SchmeNm>
            </PrtryId>
          </Id>
        </AcctOwnr>
        <SfkpgAcct>
          <Id>B02/C</Id>
        </SfkpgAcct>
        <SfkpgPlc>
          <SfkpgPlcFrmt>
            <TpAndId>
              <SfkpgPlcTp>CUST</SfkpgPlcTp>
              <Id>DSTXTZTZXXX</Id>
            </TpAndId>
          </SfkpgPlcFrmt>
        </SfkpgPlc>
      </QtyAndAcctDtls>
      <SttlmParams>
        <SctiesTxTp>
          <Cd>TRAD</Cd>
        </SctiesTxTp>
        <BnfclOwnrsh>
          <Ind>true</Ind>
        </BnfclOwnrsh>
        <SttlmSysMtd>
          <Cd>NSET</Cd>
        </SttlmSysMtd>
      </SttlmParams>
      <DlvrgSttlmPties>
        <Dpstry>
          <Id>
            <AnyBIC>DSTXTZTZ</AnyBIC>
          </Id>
        </Dpstry>
        <Pty1>
          <Id>
            <PrtryId>
              <Id>B02/B</Id>
              <Issr>CSD</Issr>
              <SchmeNm>TRADING PARTY</SchmeNm>
            </PrtryId>
          </Id>
          <PrcgId>01C83692B</PrcgId>
        </Pty1>
        <Pty2>
          <Id>
            <PrtryId>
              <Id>593129</Id>
              <Issr>CSD</Issr>
              <SchmeNm>SOR ACCOUNT</SchmeNm>
            </PrtryId>
          </Id>
        </Pty2>
        <Pty3>
          <Id>
            <PrtryId>
              <Id>B02/C</Id>
              <Issr>CSD</Issr>
              <SchmeNm>MB SCA</SchmeNm>
            </PrtryId>
          </Id>
        </Pty3>
      </DlvrgSttlmPties>
      <RcvgSttlmPties>
        <Dpstry>
          <Id>
            <AnyBIC>SAFMXXXX</AnyBIC>
          </Id>
        </Dpstry>
        <Pty1>
          <Id>
            <PrtryId>
              <Id>B05/B</Id>
              <Issr>CSD</Issr>
              <SchmeNm>TRADING PARTY</SchmeNm>
            </PrtryId>
          </Id>
        </Pty1>
        <Pty2>
          <Id>
            <PrtryId>
              <Id>588990</Id>
              <Issr>CSD</Issr>
              <SchmeNm>SOR ACCOUNT</SchmeNm>
            </PrtryId>
          </Id>
        </Pty2>
        <Pty3>
          <Id>
            <PrtryId>
              <Id>B05/C</Id>
              <Issr>CSD</Issr>
              <SchmeNm>MB SCA</SchmeNm>
            </PrtryId>
          </Id>
        </Pty3>
      </RcvgSttlmPties>
      <SttlmAmt>
        <Amt Ccy="TZS">0</Amt>
        <CdtDbtInd>CRDT</CdtDbtInd>
      </SttlmAmt>
      <OthrAmts>
        <ChrgsFees><Amt Ccy="TZS">0</Amt></ChrgsFees>
        <ExctgBrkrAmt><Amt Ccy="TZS">0</Amt></ExctgBrkrAmt>
        <Othr><Amt Ccy="TZS">0</Amt></Othr>
        <RgltryAmt><Amt Ccy="TZS">0</Amt></RgltryAmt>
        <StockXchgTax><Amt Ccy="TZS">0</Amt></StockXchgTax>
        <TrfTax><Amt Ccy="TZS">0</Amt></TrfTax>
        <TxTax><Amt Ccy="TZS">0</Amt></TxTax>
        <ValAddedTax><Amt Ccy="TZS">0</Amt></ValAddedTax>
      </OthrAmts>
      <SplmtryData>
        <Envlp>
          <MarginParameters>
            <HaircutPercentage>0</HaircutPercentage>
            <TolerancePercentage>0</TolerancePercentage>
            <CashInterestDifferentialPercentage>0</CashInterestDifferentialPercentage>
          </MarginParameters>
        </Envlp>
      </SplmtryData>
      <SplmtryData>
        <Envlp>
          <SctiesFincgDtls>
            <TermntnDt xmlns="urn:iso:std:iso:20022:tech:xsd:sese.033.001.11">
              <Dt><Dt>2025-08-29</Dt></Dt>
            </TermntnDt>
            <RpRate xmlns="urn:iso:std:iso:20022:tech:xsd:sese.033.001.11">
              <Rate>0</Rate>
            </RpRate>
          </SctiesFincgDtls>
        </Envlp>
      </SplmtryData>
    </SctiesSttlmTxInstr>
  </Document>
</RequestPayload>
"@

    $xmlDoc = New-Object System.Xml.XmlDocument
    $xmlDoc.LoadXml($xmlString)
    Write-Host "✅ XML loaded successfully" -ForegroundColor Green

    # Step 7: Send XML message
    Write-Host ""
    Write-Host "Step 7: Sending XML message..." -ForegroundColor Green
    $queue.Send($xmlDoc, "Test XML Message")

    if ($usingTcp) {
        Write-Host "✅ Test message sent successfully using TCP protocol!" -ForegroundColor Green
        Write-Host "✅ Use FormatName TCP format: $tcpQueuePath" -ForegroundColor Green
    } else {
        Write-Host "✅ Test message sent successfully using OS (native) protocol!" -ForegroundColor Green
        Write-Host "✅ Use FormatName OS format: $osQueuePath" -ForegroundColor Green
    }

    Write-Host ""
    Write-Host "🎉 Remote MSMQ connection and message send successful!" -ForegroundColor Green

} catch {
    Write-Host ""
    Write-Host "❌ Error occurred:" -ForegroundColor Red
    Write-Host "Error Type: $($_.Exception.GetType().Name)" -ForegroundColor Red
    Write-Host "Error Message: $($_.Exception.Message)" -ForegroundColor Red

    if ($_.Exception.InnerException) {
        Write-Host "Inner Exception: $($_.Exception.InnerException.Message)" -ForegroundColor Red
    }

    Write-Host ""
    Write-Host "Common solutions:" -ForegroundColor Yellow
    Write-Host "1. Ensure MSMQ is installed on both local and remote machines" -ForegroundColor Yellow
    Write-Host "2. Create the queue on the remote machine first" -ForegroundColor Yellow
    Write-Host "3. Use correct FormatName syntax for remote queues" -ForegroundColor Yellow
    Write-Host "4. Verify network connectivity between machines" -ForegroundColor Yellow
    Write-Host "5. Ensure proper MSMQ permissions are configured" -ForegroundColor Yellow

} finally {
    if ($queue) {
        try {
            $queue.Close()
            Write-Host "Queue connection closed." -ForegroundColor Gray
        } catch {
            # Ignore close errors
        }
    }
}
