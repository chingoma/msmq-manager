package com.enterprise.msmq.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO for MSMQ message operations.
 * Contains all message information including auto-generated fields.
 * 
 * @author Enterprise Development Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response containing MSMQ message information")
public class MessageResponse {

    @Schema(
        description = "Unique identifier for the message",
        example = "msg-12345-67890-abcde"
    )
    @JsonProperty("messageId")
    private String messageId;

    @Schema(
        description = "The message body content",
        example = "Payment transaction processed successfully"
    )
    @JsonProperty("body")
    private String body;

    @Schema(
        description = "Message label for identification",
        example = "PAYMENT_CONFIRMATION"
    )
    @JsonProperty("label")
    private String label;

    @Schema(
        description = "Message priority (0-7, where 0 is highest)",
        example = "3"
    )
    @JsonProperty("priority")
    private Integer priority;

    @Schema(
        description = "Message correlation ID for related messages",
        example = "corr-12345-67890"
    )
    @JsonProperty("correlationId")
    private String correlationId;

    @Schema(
        description = "Message type identifier",
        example = "SWIFT_MT103"
    )
    @JsonProperty("messageType")
    private String messageType;

    @Schema(
        description = "Timestamp when the message was created",
        example = "2024-01-15T10:30:00.000Z"
    )
    @JsonProperty("createdTime")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime createdTime;

    @Schema(
        description = "Timestamp when the message was sent",
        example = "2024-01-15T10:30:05.000Z"
    )
    @JsonProperty("sentTime")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime sentTime;

    @Schema(
        description = "Timestamp when the message was received",
        example = "2024-01-15T10:30:10.000Z"
    )
    @JsonProperty("receivedTime")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime receivedTime;

    @Schema(
        description = "Source queue name",
        example = "payment-request-queue"
    )
    @JsonProperty("sourceQueue")
    private String sourceQueue;

    @Schema(
        description = "Destination queue name",
        example = "payment-processing-queue"
    )
    @JsonProperty("destinationQueue")
    private String destinationQueue;

    @Schema(
        description = "Message size in bytes",
        example = "1024"
    )
    @JsonProperty("size")
    private Long size;

    @Schema(
        description = "Message delivery count",
        example = "1"
    )
    @JsonProperty("deliveryCount")
    private Integer deliveryCount;

    @Schema(
        description = "Message state (SENT, DELIVERED, PROCESSED, FAILED)",
        example = "DELIVERED"
    )
    @JsonProperty("state")
    private String state;

    @Schema(
        description = "Whether the message is transactional",
        example = "false"
    )
    @JsonProperty("transactional")
    private Boolean transactional;

    @Schema(
        description = "Whether the message requires acknowledgment",
        example = "true"
    )
    @JsonProperty("requiresAck")
    private Boolean requiresAck;

    @Schema(
        description = "Message acknowledgment status",
        example = "PENDING"
    )
    @JsonProperty("ackStatus")
    private String ackStatus;

    @Schema(
        description = "Custom message properties",
        example = "{\"businessUnit\": \"payments\", \"customerId\": \"CUST001\"}"
    )
    @JsonProperty("properties")
    private Map<String, Object> properties;

    @Schema(
        description = "Error message if message processing failed",
        example = "Queue not accessible"
    )
    @JsonProperty("errorMessage")
    private String errorMessage;

    @Schema(
        description = "Message processing result",
        example = "SUCCESS"
    )
    @JsonProperty("result")
    private String result;

    @Schema(
        description = "Processing duration in milliseconds",
        example = "150"
    )
    @JsonProperty("processingTimeMs")
    private Long processingTimeMs;

    @Schema(
        description = "Message template used (if applicable)",
        example = "SWIFT_PAYMENT_TEMPLATE"
    )
    @JsonProperty("templateName")
    private String templateName;

    @Schema(
        description = "Template parameters used (if applicable)",
        example = "{\"amount\": \"1000.00\", \"currency\": \"USD\"}"
    )
    @JsonProperty("templateParameters")
    private Map<String, String> templateParameters;
}
