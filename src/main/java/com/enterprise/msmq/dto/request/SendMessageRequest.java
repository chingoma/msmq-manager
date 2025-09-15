package com.enterprise.msmq.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
        description = "Message label for identification (optional)",
        example = "PAYMENT_CONFIRMATION"
    )
    private String label;

    @Schema(
        description = "Message priority (0-7, where 0 is highest, default: 3)",
        example = "3",
        minimum = "0",
        maximum = "7"
    )
    @Builder.Default
    private Integer priority = 3;

    @Schema(
        description = "Message correlation ID for related messages (optional)",
        example = "corr-12345-67890"
    )
    private String correlationId;

    @Schema(
        description = "Message type identifier (optional)",
        example = "SWIFT_MT103"
    )
    private String messageType;

    @Schema(
        description = "Source queue name (optional)",
        example = "payment-request-queue"
    )
    private String sourceQueue;

    @Schema(
        description = "Custom message properties (optional)",
        example = "{\"businessUnit\": \"payments\", \"customerId\": \"CUST001\"}"
    )
    private Map<String, Object> properties;

    // XML Processing Options
    
    @Schema(
        description = "Whether to validate XML structure before sending (only applies if body contains XML)",
        example = "true",
        defaultValue = "false"
    )
    @Builder.Default
    private Boolean validateXml = false;

    @Schema(
        description = "Whether to format/prettify XML before sending (only applies if body contains XML)",
        example = "false",
        defaultValue = "false"
    )
    @Builder.Default
    private Boolean formatXml = false;

    @Schema(
        description = "Content type hint for the message body (auto-detected if not specified)",
        example = "application/xml",
        allowableValues = {"text/plain", "application/xml", "text/xml", "application/json"}
    )
    private String contentType;
}
