package com.enterprise.msmq.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity class representing MSMQ message audit log.
 * Tracks all message operations for compliance, debugging, and monitoring.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@Entity
@Table(name = "msmq_message_audit")
public class MsmqMessageAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private String requestId;

    @Column(name = "queue_name", nullable = false)
    private String queueName;

    @Column(name = "operation_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private OperationType operationType;

    @Column(name = "message_id")
    private String messageId;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "client_ip")
    private String clientIp;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "message_size")
    private Long messageSize;

    @Column(name = "operation_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OperationStatus operationStatus;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "additional_data", columnDefinition = "TEXT")
    private String additionalData;

    /**
     * Enumeration for operation types.
     */
    public enum OperationType {
        SEND,
        RECEIVE,
        PEEK,
        PURGE,
        CREATE_QUEUE,
        DELETE_QUEUE,
        UPDATE_QUEUE,
        CONNECT,
        DISCONNECT
    }

    /**
     * Enumeration for operation status.
     */
    public enum OperationStatus {
        SUCCESS,
        FAILED,
        TIMEOUT,
        VALIDATION_ERROR,
        SYSTEM_ERROR
    }

    // Default constructor
    public MsmqMessageAudit() {
        this.timestamp = LocalDateTime.now();
    }

    // Constructor with required fields
    public MsmqMessageAudit(String requestId, String queueName, OperationType operationType, OperationStatus operationStatus) {
        this();
        this.requestId = requestId;
        this.queueName = queueName;
        this.operationType = operationType;
        this.operationStatus = operationStatus;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Long getMessageSize() {
        return messageSize;
    }

    public void setMessageSize(Long messageSize) {
        this.messageSize = messageSize;
    }

    public OperationStatus getOperationStatus() {
        return operationStatus;
    }

    public void setOperationStatus(OperationStatus operationStatus) {
        this.operationStatus = operationStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(String additionalData) {
        this.additionalData = additionalData;
    }

    @Override
    public String toString() {
        return "MsmqMessageAudit{" +
                "id=" + id +
                ", requestId='" + requestId + '\'' +
                ", queueName='" + queueName + '\'' +
                ", operationType=" + operationType +
                ", operationStatus=" + operationStatus +
                ", timestamp=" + timestamp +
                '}';
    }
}
