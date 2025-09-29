package com.enterprise.msmq.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MSMQ Message DTO for API operations.
 * <p>
 * This class represents an MSMQ message with all its properties
 * including body, headers, and metadata.
 * 
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MsmqMessage {

    // Getters and Setters
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
