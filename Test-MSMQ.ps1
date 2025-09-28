# MSMQ Testing Script for Pledge Instructions
# Author: GitHub Copilot
# Date: September 26, 2025
# Description: Tests MSMQ connectivity and sends test messages to local and remote queues
# Run as Administrator: Right-click on this file and select "Run with PowerShell"

# Make sure we load the System.Messaging assembly first
Add-Type -AssemblyName System.Messaging

# Set text color functions
function Write-Success($message) {
    Write-Host $message -ForegroundColor Green
}

function Write-Error($message) {
    Write-Host $message -ForegroundColor Red
}

function Write-Info($message) {
    Write-Host $message -ForegroundColor Cyan
}

function Write-Warning($message) {
    Write-Host $message -ForegroundColor Yellow
}

# Function to test if MSMQ is installed
function Test-MsmqInstallation {
    try {
        $msmqService = Get-Service MSMQ -ErrorAction Stop
        Write-Success "✅ MSMQ Service is installed and its status is: $($msmqService.Status)"
        return $true
    } catch {
        Write-Error "❌ MSMQ Service is not installed on this computer"
        Write-Warning "Please install MSMQ via 'Windows Features' in Control Panel"
        return $false
    }
}

# Function to test local queue operations
function Test-LocalQueue {
    $testQueueName = "test_msmq_queue"
    $queuePath = ".\private$\$testQueueName"
    
    # Create test queue if it doesn't exist
    if (-not [System.Messaging.MessageQueue]::Exists($queuePath)) {
        try {
            [System.Messaging.MessageQueue]::Create($queuePath)
            Write-Success "✅ Created local test queue: $queuePath"
        } catch {
            Write-Error "❌ Failed to create local test queue: $($_.Exception.Message)"
            return $false
        }
    } else {
        Write-Info "ℹ️ Local test queue already exists: $queuePath"
    }
    
    # Send test message
    try {
        $queue = New-Object System.Messaging.MessageQueue($queuePath)
        $message = New-Object System.Messaging.Message
        $message.Body = "Test message sent at $(Get-Date)"
        $message.Label = "Test Message"
        $queue.Send($message)
        Write-Success "✅ Successfully sent test message to local queue"
    } catch {
        Write-Error "❌ Failed to send test message to local queue: $($_.Exception.Message)"
        return $false
    }
    
    # Receive test message
    try {
        $queue = New-Object System.Messaging.MessageQueue($queuePath)
        $queue.MessageReadPropertyFilter.Body = $true
        $queue.MessageReadPropertyFilter.Label = $true
        $receivedMessage = $queue.Receive([TimeSpan]::FromSeconds(5))
        Write-Success "✅ Successfully received message from local queue"
        Write-Success "   Label: $($receivedMessage.Label)"
        Write-Success "   Body: $($receivedMessage.Body)"
        return $true
    } catch {
        Write-Error "❌ Failed to receive message from local queue: $($_.Exception.Message)"
        return $false
    }
}

# Function to test remote queue operations
function Test-RemoteQueue {
    param(
        [string]$server,
        [string]$queueName
    )
    
    # Test various format name patterns
    $formatOptions = @(
        "FormatName:DIRECT=TCP:$server\private$\$queueName",
        "FormatName:DIRECT=TCP:$server\\private$\\$queueName",
        "FormatName:DIRECT=OS:$server\private$\$queueName",
        "TCP:$server\private$\$queueName"
    )
    
    $success = $false
    foreach ($format in $formatOptions) {
        Write-Info "Testing connection to $format"
        
        # Test if we can send a message
        try {
            $queue = New-Object System.Messaging.MessageQueue($format)
            $message = New-Object System.Messaging.Message
            $message.Body = "Test message sent at $(Get-Date) to $server"
            $message.Label = "Remote Test Message"
            
            # Use string formatter to ensure text messages work
            $message.Formatter = New-Object System.Messaging.XmlMessageFormatter(@("System.String"))
            
            $queue.Send($message)
            Write-Success "✅ Successfully sent test message to remote queue on $server using format: $format"
            $success = $true
            break
        } catch {
            Write-Error "❌ Failed to send message to remote queue using $format : $($_.Exception.Message)"
        }
    }
    
    return $success
}

# Function to send XML message to remote queue
function Send-XmlMessage {
    param(
        [string]$server,
        [string]$queueName,
        [string]$xmlContent
    )
    
    $formatName = "FormatName:DIRECT=TCP:$server\private$\$queueName"
    Write-Info "Sending XML message to $formatName"
    
    try {
        $queue = New-Object System.Messaging.MessageQueue($formatName)
        $message = New-Object System.Messaging.Message
        $message.Body = $xmlContent
        $message.Label = "XML Pledge Instruction"
        
        # Use string formatter to ensure text messages work
        $message.Formatter = New-Object System.Messaging.XmlMessageFormatter(@("System.String"))
        
        $queue.Send($message)
        Write-Success "✅ Successfully sent XML message to remote queue"
        return $true
    } catch {
        Write-Error "❌ Failed to send XML message to remote queue: $($_.Exception.Message)"
        return $false
    }
}

# Function to test network connectivity
function Test-NetworkConnectivity {
    param(
        [string]$server
    )
    
    Write-Info "Testing network connectivity to $server..."
    
    # Test basic ping
    if (Test-Connection -ComputerName $server -Count 2 -Quiet) {
        Write-Success "✅ Server $server is reachable via ping"
    } else {
        Write-Error "❌ Server $server is NOT reachable via ping"
    }
    
    # Test MSMQ ports
    $ports = @(1801, 2101, 2103, 2105)
    foreach ($port in $ports) {
        $connection = New-Object System.Net.Sockets.TcpClient
        try {
            $connection.Connect($server, $port)
            Write-Success "✅ Port $port is open on $server"
        } catch {
            Write-Error "❌ Port $port is NOT open on $server"
        } finally {
            $connection.Dispose()
        }
    }
}

# XML Template for Pledge Balance Instruction
$pledgeBalanceXml = @"
<RequestPayload 
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
  xmlns:xsd="http://www.w3.org/2001/XMLSchema" 
  xmlns="SWIFTNetBusinessEnvelope">
  <AppHdr 
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
    xmlns:xsd="http://www.w3.org/2001/XMLSchema" 
    xmlns="urn:iso:std:iso:20022:tech:xsd:head.001.001.01">
    <Fr><OrgId><Id><OrgId><AnyBIC>TANZTZTXCSD</AnyBIC></OrgId></Id></OrgId></Fr>
    <To><OrgId><Id><OrgId><AnyBIC>DSTXTZTZXXX</AnyBIC></OrgId></Id></OrgId></To>
    <BizMsgIdr>Pledge Transaction</BizMsgIdr>
    <MsgDefIdr>sese.023.001.09.xsd</MsgDefIdr>
    <CreDt>$(Get-Date -Format 'yyyy-MM-ddTHH:mm:ss.0000000Z')</CreDt>
  </AppHdr>
  <Document 
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
    xmlns:xsd="http://www.w3.org/2001/XMLSchema" 
    xmlns="urn:iso:std:iso:20022:tech:xsd:sese.023.001.09">
    <SctiesSttlmTxInstr>
      <TxId>TX$(Get-Random -Minimum 100000 -Maximum 999999)</TxId>
      <SttlmTpAndAddtlParams>
        <SctiesMvmntTp>DELI</SctiesMvmntTp>
        <Pmt>FREE</Pmt>
      </SttlmTpAndAddtlParams>
      <TradDtls>
        <TradDt><Dt><Dt>$(Get-Date -Format 'yyyy-MM-dd')</Dt></Dt></TradDt>
        <SttlmDt><Dt><Dt>$(Get-Date -Format 'yyyy-MM-dd')</Dt></Dt></SttlmDt>
        <MtchgSts><Cd>MACH</Cd></MtchgSts>
        <SttlmInstrPrcgAddtlDtls>Pledge Balance Test</SttlmInstrPrcgAddtlDtls>
      </TradDtls>
      <FinInstrmId>
        <ISIN>TZ1234567890</ISIN>
        <Desc>TEST SECURITY</Desc>
      </FinInstrmId>
      <QtyAndAcctDtls>
        <SttlmQty><Qty><Unit>1000</Unit></Qty></SttlmQty>
        <AcctOwnr>
          <Id><PrtryId>
            <Id>BP12345</Id>
            <Issr>BANK OF TANZANIA</Issr>
            <SchmeNm>SOR ACCOUNT</SchmeNm>
          </PrtryId></Id>
        </AcctOwnr>
        <SfkpgAcct><Id>12345</Id></SfkpgAcct>
        <SfkpgPlc>
          <SfkpgPlcFrmt><TpAndId>
            <SfkpgPlcTp>NCSD</SfkpgPlcTp>
            <Id>TANZTZTXCSD</Id>
          </TpAndId></SfkpgPlcFrmt>
        </SfkpgPlc>
      </QtyAndAcctDtls>
      <SttlmParams>
        <SctiesTxTp><Cd>COLI</Cd></SctiesTxTp>
      </SttlmParams>
      <RcvgSttlmPties>
        <Dpstry><Id><AnyBIC>TANZTZTXCSD</AnyBIC></Id></Dpstry>
        <Pty1>
          <Id><PrtryId>
            <Id>BP67890</Id>
            <Issr>CSD</Issr>
            <SchmeNm>PLEDGEE</SchmeNm>
          </PrtryId></Id>
          <PrcgId>$(Get-Random -Minimum 1000000 -Maximum 9999999)</PrcgId>
        </Pty1>
      </RcvgSttlmPties>
    </SctiesSttlmTxInstr>
  </Document>
</RequestPayload>
"@

# XML Template for Pledge Release Instruction
$pledgeReleaseXml = @"
<RequestPayload 
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
  xmlns:xsd="http://www.w3.org/2001/XMLSchema" 
  xmlns="SWIFTNetBusinessEnvelope">
  <AppHdr 
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
    xmlns:xsd="http://www.w3.org/2001/XMLSchema" 
    xmlns="urn:iso:std:iso:20022:tech:xsd:head.001.001.01">
    <Fr><OrgId><Id><OrgId><AnyBIC>TANZTZTXCSD</AnyBIC></OrgId></Id></OrgId></Fr>
    <To><OrgId><Id><OrgId><AnyBIC>DSTXTZTZXXX</AnyBIC></OrgId></Id></OrgId></To>
    <BizMsgIdr>Pledge Transaction</BizMsgIdr>
    <MsgDefIdr>sese.023.001.09.xsd</MsgDefIdr>
    <CreDt>$(Get-Date -Format 'yyyy-MM-ddTHH:mm:ss.0000000Z')</CreDt>
  </AppHdr>
  <Document 
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
    xmlns:xsd="http://www.w3.org/2001/XMLSchema" 
    xmlns="urn:iso:std:iso:20022:tech:xsd:sese.023.001.09">
    <SctiesSttlmTxInstr>
      <TxId>TX$(Get-Random -Minimum 100000 -Maximum 999999)</TxId>
      <SttlmTpAndAddtlParams>
        <SctiesMvmntTp>RECE</SctiesMvmntTp>
        <Pmt>FREE</Pmt>
      </SttlmTpAndAddtlParams>
      <TradDtls>
        <TradDt><Dt><Dt>$(Get-Date -Format 'yyyy-MM-dd')</Dt></Dt></TradDt>
        <SttlmDt><Dt><Dt>$(Get-Date -Format 'yyyy-MM-dd')</Dt></Dt></SttlmDt>
        <MtchgSts><Cd>MACH</Cd></MtchgSts>
        <SttlmInstrPrcgAddtlDtls>Pledge Release Test</SttlmInstrPrcgAddtlDtls>
      </TradDtls>
      <FinInstrmId>
        <ISIN>TZ1234567890</ISIN>
        <Desc>TEST SECURITY</Desc>
      </FinInstrmId>
      <QtyAndAcctDtls>
        <SttlmQty><Qty><Unit>1000</Unit></Qty></SttlmQty>
        <AcctOwnr>
          <Id><PrtryId>
            <Id>BP12345</Id>
            <Issr>BANK OF TANZANIA</Issr>
            <SchmeNm>SOR ACCOUNT</SchmeNm>
          </PrtryId></Id>
        </AcctOwnr>
        <SfkpgAcct><Id>12345</Id></SfkpgAcct>
        <SfkpgPlc>
          <SfkpgPlcFrmt><TpAndId>
            <SfkpgPlcTp>NCSD</SfkpgPlcTp>
            <Id>TANZTZTXCSD</Id>
          </TpAndId></SfkpgPlcFrmt>
        </SfkpgPlc>
      </QtyAndAcctDtls>
      <SttlmParams>
        <SctiesTxTp><Cd>COLO</Cd></SctiesTxTp>
      </SttlmParams>
      <DlvrgSttlmPties>
        <Dpstry><Id><AnyBIC>TANZTZTXCSD</AnyBIC></Id></Dpstry>
        <Pty1>
          <Id><PrtryId>
            <Id>BP67890</Id>
            <Issr>CSD</Issr>
            <SchmeNm>PLEDGEE</SchmeNm>
          </PrtryId></Id>
          <PrcgId>$(Get-Random -Minimum 1000000 -Maximum 9999999)</PrcgId>
        </Pty1>
      </DlvrgSttlmPties>
    </SctiesSttlmTxInstr>
  </Document>
</RequestPayload>
"@

# Main menu function
function Show-Menu {
    Clear-Host
    Write-Host "======================================" -ForegroundColor Yellow
    Write-Host "        MSMQ Testing Tool" -ForegroundColor Yellow
    Write-Host "======================================" -ForegroundColor Yellow
    Write-Host "1: Check MSMQ Installation"
    Write-Host "2: Test Local Queue"
    Write-Host "3: Test Network Connectivity"
    Write-Host "4: Test Remote Queue"
    Write-Host "5: Send Pledge Balance XML"
    Write-Host "6: Send Pledge Release XML"
    Write-Host "7: Run All Tests"
    Write-Host "Q: Quit"
    Write-Host "======================================" -ForegroundColor Yellow
}

# Run all tests function
function Run-AllTests {
    Write-Info "`n>> Checking MSMQ Installation"
    $msmqInstalled = Test-MsmqInstallation
    if (-not $msmqInstalled) {
        return
    }

    Write-Info "`n>> Testing Local Queue"
    Test-LocalQueue

    Write-Info "`n>> Testing Network Connectivity"
    Test-NetworkConnectivity -server $remoteServer

    Write-Info "`n>> Testing Remote Queue"
    Test-RemoteQueue -server $remoteServer -queueName $remoteQueue

    Write-Info "`n>> Sending Pledge Balance XML"
    Send-XmlMessage -server $remoteServer -queueName $remoteQueue -xmlContent $pledgeBalanceXml

    Write-Info "`n>> Sending Pledge Release XML"
    Send-XmlMessage -server $remoteServer -queueName $remoteQueue -xmlContent $pledgeReleaseXml
}

# Set remote server and queue
$remoteServer = "192.168.2.170"
$remoteQueue = "crdb_to_dse"

# Main execution loop
do {
    Show-Menu
    $selection = Read-Host "Enter your selection"
    switch ($selection) {
        '1' {
            Test-MsmqInstallation
            pause
        }
        '2' {
            Test-LocalQueue
            pause
        }
        '3' {
            Test-NetworkConnectivity -server $remoteServer
            pause
        }
        '4' {
            Test-RemoteQueue -server $remoteServer -queueName $remoteQueue
            pause
        }
        '5' {
            Send-XmlMessage -server $remoteServer -queueName $remoteQueue -xmlContent $pledgeBalanceXml
            pause
        }
        '6' {
            Send-XmlMessage -server $remoteServer -queueName $remoteQueue -xmlContent $pledgeReleaseXml
            pause
        }
        '7' {
            Run-AllTests
            pause
        }
        'q' {
            return
        }
    }
} while ($selection -ne 'q')