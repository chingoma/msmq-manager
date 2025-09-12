# 🏦 Securities Settlement System

## 📋 Overview

The Securities Settlement System is a comprehensive solution for handling securities transfers using MSMQ as a message broker. It generates paired RECE and DELI messages for each settlement operation, ensuring proper credit/debit operations and cross-referencing for ATS (Automated Trading System) matching.

## 🏗️ Architecture

```
Client Request → Settlement Service → Enhanced Template → Two Messages (RECE + DELI)
                ↓
            Auto-Generated Transaction IDs
                ↓
            Cross-Referenced Messages
                ↓
            MSMQ Queue → SWIFT Processor → ATS
```

## 🔑 Key Features

### **1. Paired Message Generation**
- **RECE Message**: Credits the seller's account (CRDT)
- **DELI Message**: Debits the buyer's account (DBIT)
- Both messages are automatically generated and sent simultaneously

### **2. Auto-Generated Transaction IDs**
- **Base Transaction ID**: Format `YYMMDD + Random` (e.g., `250830A1B2C3`)
- **RECE Transaction ID**: Base + `A` (e.g., `250830A1B2C3A`)
- **DELI Transaction ID**: Base + `B` (e.g., `250830A1B2C3B`)

### **3. Cross-Referencing**
- Each message contains the other's transaction ID
- Common correlation ID links both messages
- ATS can match RECE and DELI for settlement

### **4. Single Template Approach**
- Uses existing `SWIFT_SHARE_TRANSFER_DETAILED` template
- Dynamic parameter substitution for RECE vs DELI
- No need for separate templates

## 📁 File Structure

```
src/main/java/com/enterprise/msmq/
├── controller/
│   └── SecuritiesSettlementController.java          # REST API endpoints
├── service/
│   ├── SecuritiesSettlementService.java             # Core settlement logic
│   └── SecuritiesSettlementTestService.java         # Test integration
└── dto/
    ├── request/
    │   └── SecuritiesSettlementRequest.java         # Settlement request DTO
    └── response/
        └── SecuritiesSettlementResponse.java         # Settlement response DTO
```

## 🚀 API Endpoints

### **POST** `/api/v1/securities-settlement/settle`
Initiates a securities settlement operation.

**Request Body:**
```json
{
  "isinCode": "TZ1996100214",
  "securityName": "DCB",
  "quantity": 10,
  "sellerAccountId": "588990",
  "buyerAccountId": "593129",
  "sellerName": "John Doe",
  "buyerName": "Jane Smith",
  "tradeDate": "2025-08-29",
  "settlementDate": "2025-09-03",
  "commonReferenceId": "616964F32",
  "priority": 1,
  "queueName": "securities-settlement-queue"
}
```

**Response:**
```json
{
  "statusCode": "600",
  "message": "Securities settlement completed successfully",
  "data": {
    "success": true,
    "baseTransactionId": "250830A1B2C3",
    "receTransactionId": "250830A1B2C3A",
    "deliTransactionId": "250830A1B2C3B",
    "correlationId": "CORR-A1B2C3D4",
    "commonReferenceId": "616964F32",
    "queueName": "securities-settlement-queue",
    "isinCode": "TZ1996100214",
    "securityName": "DCB",
    "quantity": 10,
    "sellerAccountId": "588990",
    "buyerAccountId": "593129",
    "processedAt": "2025-08-30T17:30:00",
    "receStatus": "SENT",
    "deliStatus": "SENT"
  }
}
```

### **GET** `/api/v1/securities-settlement/health`
Health check endpoint.

### **GET** `/api/v1/securities-settlement/info`
Provides detailed information about the settlement workflow.

## 🔧 Configuration

### **Template Parameters**
The system automatically generates these parameters for each message:

#### **RECE Message Parameters**
- `MOVEMENT_TYPE`: `RECE`
- `CREDIT_DEBIT_INDICATOR`: `CRDT`
- `ACCOUNT_ID`: Seller's account ID
- `LINKED_TRANSACTION_ID`: DELI transaction ID

#### **DELI Message Parameters**
- `MOVEMENT_TYPE`: `DELI`
- `CREDIT_DEBIT_INDICATOR`: `DBIT`
- `ACCOUNT_ID`: Buyer's account ID
- `LINKED_TRANSACTION_ID`: RECE transaction ID

#### **Common Parameters**
- `BASE_TRANSACTION_ID`: Base transaction ID (without A/B)
- `RECE_TRANSACTION_ID`: Full RECE transaction ID
- `DELI_TRANSACTION_ID`: Full DELI transaction ID
- `ISIN_CODE`: Security ISIN code
- `SECURITY_NAME`: Security name
- `QUANTITY`: Transfer quantity
- `SELLER_ACCOUNT_ID`: Seller account ID
- `BUYER_ACCOUNT_ID`: Buyer account ID
- `SELLER_NAME`: Seller name
- `BUYER_NAME`: Buyer name
- `TRADE_DATE`: Trade date
- `SETTLEMENT_DATE`: Settlement date
- `CREATION_DATE`: Message creation timestamp
- `COMMON_REFERENCE_ID`: Common reference ID

## 🔄 Settlement Workflow

### **1. Request Processing**
1. Client sends settlement request with security and account details
2. System validates request parameters
3. System generates unique base transaction ID

### **2. Message Generation**
1. **RECE Message**: Credits seller's account
   - Movement Type: `RECE`
   - Credit/Debit: `CRDT`
   - Account: Seller Account
   - Links to: DELI Message

2. **DELI Message**: Debits buyer's account
   - Movement Type: `DELI`
   - Credit/Debit: `DBIT`
   - Account: Buyer Account
   - Links to: RECE Message

### **3. Message Sending**
1. Both messages use the same template with different parameters
2. Messages are sent to the specified MSMQ queue simultaneously
3. Each message contains cross-references to the other
4. Common correlation ID links both messages

### **4. ATS Processing**
1. SWIFT processor picks up both messages from the queue
2. ATS receives messages separately for matching
3. ATS uses transaction IDs and correlation ID to match RECE and DELI
4. Settlement is processed when both messages are matched

## 🧪 Testing

### **Automatic Test**
The system includes an automatic test that runs when the application starts:

```java
@Service
public class SecuritiesSettlementTestService {
    @EventListener(ApplicationReadyEvent.class)
    public void testSecuritiesSettlement() {
        // Creates test settlement request
        // Executes settlement
        // Logs results
    }
}
```

### **Manual Testing**
Use the REST API endpoints to test settlement operations:

```bash
# Test settlement
curl -X POST "http://localhost:8081/api/v1/securities-settlement/settle" \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic YWRtaW46YWRtaW4xMjM=" \
  -d '{
    "isinCode": "TZ1996100214",
    "securityName": "DCB",
    "quantity": 10,
    "sellerAccountId": "588990",
    "buyerAccountId": "593129",
    "sellerName": "John Doe",
    "buyerName": "Jane Smith",
    "tradeDate": "2025-08-29",
    "settlementDate": "2025-09-03",
    "commonReferenceId": "616964F32",
    "priority": 1,
    "queueName": "securities-settlement-queue"
  }'

# Check health
curl "http://localhost:8081/api/v1/securities-settlement/health"

# Get info
curl "http://localhost:8081/api/v1/securities-settlement/info"
```

## 🔒 Security

- **Authentication**: Required for all settlement operations
- **Authorization**: Only users with `ADMIN` role can initiate settlements
- **Validation**: Comprehensive request validation using Bean Validation
- **Logging**: Detailed logging for audit and debugging

## 📊 Monitoring

### **Success Metrics**
- Settlement success rate
- Message delivery status
- Transaction ID generation
- Processing time

### **Error Handling**
- Detailed error messages
- Status tracking for RECE and DELI messages
- Automatic retry mechanisms (if configured)
- Comprehensive logging

## 🚀 Future Enhancements

### **Planned Features**
1. **Settlement Timeout**: Configurable timeout for settlement completion
2. **Status Queue**: Separate queue for ATS settlement status updates
3. **Rollback Mechanisms**: Automatic and manual rollback capabilities
4. **Batch Processing**: Support for multiple settlements in a single request
5. **Settlement History**: Track and query settlement history
6. **Real-time Monitoring**: WebSocket-based real-time settlement status

### **Integration Points**
1. **ATS Integration**: Enhanced ATS message handling
2. **SWIFT Processor**: Dedicated SWIFT message processor
3. **Queue Monitoring**: Real-time queue status monitoring
4. **Alert System**: Settlement failure alerts and notifications

## 🐛 Troubleshooting

### **Common Issues**

#### **1. Template Not Found**
```
Error: Template "SWIFT_SHARE_TRANSFER_DETAILED" not found
```
**Solution**: Ensure the template exists in the database and is active.

#### **2. Queue Not Found**
```
Error: Queue "securities-settlement-queue" not found
```
**Solution**: Create the queue first using the MSMQ queue management endpoints.

#### **3. Validation Errors**
```
Error: Validation failed for field 'isinCode'
```
**Solution**: Check request parameters against validation rules.

### **Debug Information**
- Enable debug logging for detailed operation information
- Check application logs for transaction ID generation
- Verify message parameters in the generated messages
- Monitor queue status and message delivery

## 📚 References

- [MSMQ Manager API Documentation](../README.md)
- [Template System Documentation](../docs/templates.md)
- [Queue Management API](../docs/queues.md)
- [Message Processing](../docs/messages.md)

## 🤝 Support

For technical support or questions about the Securities Settlement System:

1. Check the application logs for detailed error information
2. Review the API documentation and examples
3. Test with the provided test endpoints
4. Contact the development team for assistance

---

**Version**: 1.0.0  
**Last Updated**: 2025-08-30  
**Author**: Enterprise Development Team
