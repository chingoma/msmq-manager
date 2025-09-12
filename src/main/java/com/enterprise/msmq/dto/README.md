# MSMQ Manager DTOs Documentation

This document provides an overview of all Data Transfer Objects (DTOs) used in the MSMQ Manager application.

## 📁 Directory Structure

```
src/main/java/com/enterprise/msmq/dto/
├── request/                    # Request DTOs (Client → Server)
│   ├── CreateQueueRequest.java
│   ├── SendMessageRequest.java
│   ├── QueueSyncRequest.java
│   └── CreateAlertRequest.java
├── response/                   # Response DTOs (Server → Client)
│   ├── QueueResponse.java
│   ├── MessageResponse.java
│   └── QueueStatisticsResponse.java
├── legacy/                     # Legacy DTOs (to be deprecated)
│   ├── MsmqQueue.java
│   ├── MsmqMessage.java
│   └── QueueSyncResult.java
└── common/                     # Common/Utility DTOs
    ├── ApiResponse.java
    ├── QueueAlert.java
    └── ResponseMetadata.java
```

## 🔄 Request DTOs

### CreateQueueRequest
- **Purpose**: Create new MSMQ queues
- **Fields**: `name`, `path`, `type`, `description`, `maxMessageCount`, `maxSize`, `transactional`, `journaled`, `authenticated`, `encrypted`, `owner`, `permissions`
- **Validation**: Required fields with `@NotBlank`, positive numbers with `@Positive`

### SendMessageRequest
- **Purpose**: Send messages to MSMQ queues
- **Fields**: `body`, `label`, `priority`, `correlationId`, `messageType`, `destinationQueue`, `sourceQueue`, `timeoutMs`, `transactional`, `requiresAck`, `properties`, `templateName`, `templateParameters`
- **Validation**: Required fields with `@NotBlank`, positive numbers with `@Positive`

### QueueSyncRequest
- **Purpose**: Configure queue synchronization operations
- **Fields**: `queueNames`, `fullSync`, `updateClassifications`, `validateRouting`, `maxConcurrency`, `operationTimeoutMs`, `sendNotifications`, `syncOptions`
- **Validation**: Optional configuration with reasonable defaults

### CreateAlertRequest
- **Purpose**: Create monitoring alerts
- **Fields**: `title`, `description`, `alertType`, `severity`, `queueName`, `source`, `requiresImmediateAction`, `suggestedActions`, `properties`, `tags`, `sendEmailNotification`, `emailRecipients`
- **Validation**: Required fields with `@NotBlank`

## 📤 Response DTOs

### QueueResponse
- **Purpose**: Return comprehensive queue information
- **Fields**: All queue properties including auto-generated fields like `id`, `createdTime`, `modifiedTime`, `lastAccessTime`, `status`, `messageCount`, `size`, `queueDirection`, `queuePurpose`, `isActive`
- **Use Case**: GET operations, queue details, synchronization results

### MessageResponse
- **Purpose**: Return comprehensive message information
- **Fields**: All message properties including auto-generated fields like `messageId`, `createdTime`, `sentTime`, `receivedTime`, `size`, `deliveryCount`, `state`, `ackStatus`, `result`, `processingTimeMs`
- **Use Case**: Message operations, delivery confirmations, processing results

### QueueStatisticsResponse
- **Purpose**: Return comprehensive system statistics
- **Fields**: `totalQueues`, `activeQueues`, `inactiveQueues`, `totalMessages`, `totalSizeBytes`, `queuesByDirection`, `queuesByPurpose`, `queuesByType`, `performanceMetrics`, `lastSyncTime`, `lastSyncStatus`, `syncFailures24h`, `activeAlerts`, `alertsBySeverity`, `systemHealth`, `generatedAt`, `customMetrics`
- **Use Case**: Monitoring dashboards, system health checks, performance analysis

## 🔧 Common DTOs

### ApiResponse<T>
- **Purpose**: Standardized API response wrapper
- **Generic Type**: T - The actual response data type
- **Fields**: `error`, `success`, `statusCode`, `message`, `data`, `timestamp`, `requestId`, `metadata`

### QueueSyncResult
- **Purpose**: Synchronization operation results
- **Fields**: `totalQueues`, `createdQueues`, `updatedQueues`, `deletedQueues`, `syncTime`, `status`, `errorMessage`
- **Methods**: `incrementCreated()`, `incrementUpdated()`, `incrementDeleted()`, `isSuccessful()`, `getSummary()`

### QueueAlert
- **Purpose**: Queue monitoring alerts
- **Fields**: `type`, `message`, `severity`, `timestamp`, `context`, `queueName`
- **Enums**: Uses `AlertType` and `AlertSeverity`

## 📊 Swagger Integration

All DTOs include comprehensive Swagger annotations:
- `@Schema(description = "...")` for class-level documentation
- `@Schema(description = "...", example = "...")` for field-level documentation
- `@Schema(allowableValues = {...})` for enum constraints
- `@Schema(minimum = "...", maximum = "...")` for numeric constraints
- `@Schema(required = true)` for required fields

## 🚀 Migration Guide

### From Legacy DTOs to New Structure

1. **Replace direct usage** of `MsmqQueue` with `CreateQueueRequest` (for POST) and `QueueResponse` (for GET)
2. **Replace direct usage** of `MsmqMessage` with `SendMessageRequest` (for POST) and `MessageResponse` (for GET)
3. **Update controllers** to use request/response DTOs appropriately
4. **Update services** to map between request DTOs and entities, and entities to response DTOs

### Example Migration

```java
// OLD (Legacy)
@PostMapping("/queues")
public ResponseEntity<MsmqQueue> createQueue(@RequestBody MsmqQueue queue) {
    // Direct entity usage
}

// NEW (Request/Response DTOs)
@PostMapping("/queues")
public ResponseEntity<QueueResponse> createQueue(@RequestBody @Valid CreateQueueRequest request) {
    // Use request DTO, return response DTO
}
```

## ✅ Benefits of New Structure

1. **Clear Separation**: Request vs Response concerns are separated
2. **Validation**: Request DTOs include proper validation annotations
3. **Documentation**: Comprehensive Swagger examples and descriptions
4. **Type Safety**: Generic response types for better type safety
5. **Maintainability**: Easier to evolve APIs without breaking changes
6. **Security**: No exposure of internal fields in requests

## 🔮 Future Enhancements

1. **Versioning**: Add API versioning support to DTOs
2. **Pagination**: Create pagination DTOs for list operations
3. **Filtering**: Create filter DTOs for search operations
4. **Bulk Operations**: Create bulk operation DTOs for batch processing
5. **Audit Trail**: Create audit DTOs for compliance tracking
