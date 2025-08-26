package com.enterprise.msmq.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MSMQ Message DTO for API operations.
 * 
 * This class represents an MSMQ message with all its properties
 * including body, headers, and metadata.
 * 
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MsmqMessage {

    /**
     * Unique identifier for the message.
     */
    @JsonProperty("messageId")
    private String messageId;

    /**
     * The message body content.
     */
    @JsonProperty("body")
    private String body;

    /**
     * Message label for identification.
     */
    @JsonProperty("label")
    private String label;

    /**
     * Message priority (0-7, where 0 is highest).
     */
    @JsonProperty("priority")
    private Integer priority;

    /**
     * Message correlation ID for related messages.
     */
    @JsonProperty("correlationId")
    private String correlationId;

    /**
     * Message type identifier.
     */
    @JsonProperty("messageType")
    private String messageType;

    /**
     * Timestamp when the message was created.
     */
    @JsonProperty("createdTime")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime createdTime;

    /**
     * Timestamp when the message was sent.
     */
    @JsonProperty("sentTime")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime sentTime;

    /**
     * Timestamp when the message was received.
     */
    @JsonProperty("receivedTime")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime receivedTime;

    /**
     * Source queue name.
     */
    @JsonProperty("sourceQueue")
    private String sourceQueue;

    /**
     * Destination queue name.
     */
    @JsonProperty("destinationQueue")
    private String destinationQueue;

    /**
     * Message size in bytes.
     */
    @JsonProperty("size")
    private Long size;

    /**
     * Message delivery count.
     */
    @JsonProperty("deliveryCount")
    private Integer deliveryCount;

    /**
     * Maximum delivery count before message expires.
     */
    @JsonProperty("maxDeliveryCount")
    private Integer maxDeliveryCount;

    /**
     * Message expiration time.
     */
    @JsonProperty("expirationTime")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime expirationTime;

    /**
     * Custom message properties.
     */
    @JsonProperty("properties")
    private Map<String, Object> properties;

    /**
     * Message status (e.g., "QUEUED", "PROCESSING", "COMPLETED", "FAILED").
     */
    @JsonProperty("status")
    private String status;

    /**
     * Error message if message processing failed.
     */
    @JsonProperty("errorMessage")
    private String errorMessage;

    /**
     * Default constructor.
     */
    public MsmqMessage() {
        this.createdTime = LocalDateTime.now();
        this.priority = 3; // Default normal priority
        this.deliveryCount = 0;
        this.maxDeliveryCount = 5;
    }

    /**
     * Constructor with basic message properties.
     * 
     * @param body the message body
     * @param label the message label
     */
    public MsmqMessage(String body, String label) {
        this();
        this.body = body;
        this.label = label;
    }

    /**
     * Constructor with all message properties.
     * 
     * @param messageId the message ID
     * @param body the message body
     * @param label the message label
     * @param priority the message priority
     * @param correlationId the correlation ID
     */
    public MsmqMessage(String messageId, String body, String label, Integer priority, String correlationId) {
        this(body, label);
        this.messageId = messageId;
        this.priority = priority;
        this.correlationId = correlationId;
    }

    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getSentTime() {
        return sentTime;
    }

    public void setSentTime(LocalDateTime sentTime) {
        this.sentTime = sentTime;
    }

    public LocalDateTime getReceivedTime() {
        return receivedTime;
    }

    public void setReceivedTime(LocalDateTime receivedTime) {
        this.receivedTime = receivedTime;
    }

    public String getSourceQueue() {
        return sourceQueue;
    }

    public void setSourceQueue(String sourceQueue) {
        this.sourceQueue = sourceQueue;
    }

    public String getDestinationQueue() {
        return destinationQueue;
    }

    public void setDestinationQueue(String destinationQueue) {
        this.destinationQueue = destinationQueue;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Integer getDeliveryCount() {
        return deliveryCount;
    }

    public void setDeliveryCount(Integer deliveryCount) {
        this.deliveryCount = deliveryCount;
    }

    public Integer getMaxDeliveryCount() {
        return maxDeliveryCount;
    }

    public void setMaxDeliveryCount(Integer maxDeliveryCount) {
        this.maxDeliveryCount = maxDeliveryCount;
    }

    public LocalDateTime getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(LocalDateTime expirationTime) {
        this.expirationTime = expirationTime;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Checks if the message has expired.
     * 
     * @return true if the message has expired
     */
    public boolean isExpired() {
        return expirationTime != null && LocalDateTime.now().isAfter(expirationTime);
    }

    /**
     * Checks if the message has exceeded maximum delivery count.
     * 
     * @return true if max delivery count exceeded
     */
    public boolean isMaxDeliveryExceeded() {
        return deliveryCount >= maxDeliveryCount;
    }

    @Override
    public String toString() {
        return "MsmqMessage{" +
                "messageId='" + messageId + '\'' +
                ", body='" + body + '\'' +
                ", label='" + label + '\'' +
                ", priority=" + priority +
                ", correlationId='" + correlationId + '\'' +
                ", messageType='" + messageType + '\'' +
                ", createdTime=" + createdTime +
                ", sentTime=" + sentTime +
                ", receivedTime=" + receivedTime +
                ", sourceQueue='" + sourceQueue + '\'' +
                ", destinationQueue='" + destinationQueue + '\'' +
                ", size=" + size +
                ", deliveryCount=" + deliveryCount +
                ", maxDeliveryCount=" + maxDeliveryCount +
                ", expirationTime=" + expirationTime +
                ", properties=" + properties +
                ", status='" + status + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
