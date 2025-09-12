package com.enterprise.msmq.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Map;

/**
 * Request DTO for sending MSMQ messages.
 * Contains only the fields that should be provided by the client.
 * 
 * @author Enterprise Development Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request for sending a new MSMQ message")
public class SendMessageRequest {

    @Schema(
        description = "The message body content",
        example = "Payment transaction processed successfully",
        required = true
    )
    @NotBlank(message = "Message body is required")
    private String body;

    @Schema(
        description = "Message label for identification",
        example = "PAYMENT_CONFIRMATION"
    )
    private String label;

    @Schema(
        description = "Message priority (0-7, where 0 is highest)",
        example = "3",
        minimum = "0",
        maximum = "7"
    )
    @Positive(message = "Priority must be positive")
    private Integer priority;

    @Schema(
        description = "Message correlation ID for related messages",
        example = "corr-12345-67890"
    )
    private String correlationId;

    @Schema(
        description = "Message type identifier",
        example = "SWIFT_MT103"
    )
    private String messageType;

    @Schema(
        description = "Destination queue name",
        example = "payment-processing-queue",
        required = true
    )
    @NotBlank(message = "Destination queue is required")
    private String destinationQueue;

    @Schema(
        description = "Source queue name (optional)",
        example = "payment-request-queue"
    )
    private String sourceQueue;

    @Schema(
        description = "Message timeout in milliseconds",
        example = "30000",
        minimum = "1000"
    )
    @Positive(message = "Timeout must be positive")
    private Long timeoutMs;

    @Schema(
        description = "Whether the message is transactional",
        example = "false"
    )
    private Boolean transactional;

    @Schema(
        description = "Whether the message requires acknowledgment",
        example = "true"
    )
    private Boolean requiresAck;

    @Schema(
        description = "Custom message properties",
        example = "{\"businessUnit\": \"payments\", \"customerId\": \"CUST001\"}"
    )
    private Map<String, Object> properties;

    @Schema(
        description = "Message template name to use",
        example = "SWIFT_PAYMENT_TEMPLATE"
    )
    private String templateName;

    @Schema(
        description = "Template parameters for message generation",
        example = "{\"amount\": \"1000.00\", \"currency\": \"USD\", \"recipient\": \"John Doe\"}"
    )
    private Map<String, String> templateParameters;
}
