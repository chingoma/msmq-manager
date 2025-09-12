package com.enterprise.msmq.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Map;

/**
 * Request DTO for creating queue alerts.
 * Contains only the fields that should be provided by the client.
 * 
 * @author Enterprise Development Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request for creating a new queue alert")
public class CreateAlertRequest {

    @Schema(
        description = "Alert title or summary",
        example = "Queue synchronization failed",
        required = true
    )
    @NotBlank(message = "Alert title is required")
    private String title;

    @Schema(
        description = "Detailed alert description",
        example = "Failed to synchronize payment-processing-queue due to access denied",
        required = true
    )
    @NotBlank(message = "Alert description is required")
    private String description;

    @Schema(
        description = "Alert type (QUEUE_DELETED, PERFORMANCE_DEGRADATION, SYNC_FAILURE, etc.)",
        example = "SYNC_FAILURE",
        required = true
    )
    @NotBlank(message = "Alert type is required")
    private String alertType;

    @Schema(
        description = "Alert severity (INFO, WARNING, ERROR, CRITICAL)",
        example = "ERROR",
        required = true
    )
    @NotBlank(message = "Alert severity is required")
    private String severity;

    @Schema(
        description = "Queue name associated with the alert",
        example = "payment-processing-queue"
    )
    private String queueName;

    @Schema(
        description = "Source system or component that generated the alert",
        example = "QueueSyncService"
    )
    private String source;

    @Schema(
        description = "Whether the alert requires immediate attention",
        example = "true"
    )
    private Boolean requiresImmediateAction;

    @Schema(
        description = "Suggested actions to resolve the alert",
        example = "Check queue permissions and restart sync service"
    )
    private String suggestedActions;

    @Schema(
        description = "Custom alert properties as key-value pairs",
        example = "{\"retryCount\": \"3\", \"lastError\": \"Access denied\"}"
    )
    private Map<String, Object> properties;

    @Schema(
        description = "Tags for categorizing the alert",
        example = "[\"sync\", \"permissions\", \"payment\"]"
    )
    private java.util.List<String> tags;

    @Schema(
        description = "Whether to send email notifications for this alert",
        example = "true"
    )
    private Boolean sendEmailNotification;

    @Schema(
        description = "Email recipients (overrides default mailing list)",
        example = "[\"admin@company.com\", \"ops@company.com\"]"
    )
    private java.util.List<String> emailRecipients;
}
