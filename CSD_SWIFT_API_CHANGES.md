# CSD SWIFT Message API Integration Changes

## Overview
This document outlines the changes made to the `CsdDirecXmlService` class to properly integrate with the CSD SWIFT Message API using ISO20022 message format.

## Changes Made

### 1. Updated BIC Codes
- Changed sender BIC from `MTRADEATSXX` to `BROKERBICXXX`
- Changed recipient BIC from `ESCROWCSDXX` to `CSDBICICXXX`
- Updated all references to depository and safekeeping BICs

### 2. Updated XML Message Structure
- Updated namespace from `sese.023.001.05` to `sese.023.001.07`
- Enhanced business message IDs to include operation type:
  - `PLEDGE-${TRANSACTION_ID}` for pledge operations
  - `RELEASE-${TRANSACTION_ID}` for release operations
- Added additional fields for market infrastructure ID and sender ID

### 3. Enhanced SOAP Envelope
- Added proper WS-Addressing headers required by the CSD API
- Added UUID-based message ID generation
- Updated XML namespace from `http://tempuri.org/` to `http://csd.services/`
- Updated SOAP action header to match the CSD service

### 4. Improved Message Sending
- Created dedicated `sendSoapRequest` method with proper headers:
  - Content-Type: `application/soap+xml; charset=utf-16`
  - SOAPAction: `http://csd.services/IDirectXMLService/SubmitRequest`
  - X-CSD-API-Version: `1.7`
  - X-CSD-Client-Id: `MSMQ-MANAGER`

### 5. Enhanced Transaction ID Generation
- Updated format to match CSD requirements: YYYYMMDDHHmmssSSS + random suffix
- Increased random suffix digits to ensure uniqueness across high-volume operations

### 6. Improved UTF-16 Encoding
- Enforced proper UTF-16 encoding as required by CSD SWIFT messages
- Added error handling for encoding issues

### 7. Enhanced Error Handling
- Updated error message extraction to handle ISO20022-specific error codes
- Added support for rejection reason codes from SWIFT messages

## Compliance
These changes ensure that our application correctly formats and sends pledge instructions according to the ISO20022 message format (sese.023.001.07) as specified in the CSD SWIFT Message API documentation.