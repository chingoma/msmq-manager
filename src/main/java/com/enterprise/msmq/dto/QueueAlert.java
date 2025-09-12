package com.enterprise.msmq.dto;

import com.enterprise.msmq.enums.AlertType;
import com.enterprise.msmq.enums.AlertSeverity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for queue monitoring alerts.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Queue monitoring alert information")
public class QueueAlert {
    
    @Schema(
        description = "The type of alert",
        example = "SYNC_FAILURE",
        allowableValues = {"QUEUE_DELETED", "PERFORMANCE_DEGRADATION", "SYNC_FAILURE", "QUEUE_FULL", "MESSAGE_EXPIRED"}
    )
    private AlertType type;
    
    @Schema(
        description = "The alert message",
        example = "Queue synchronization failed for payment-processing-queue"
    )
    private String message;
    
    @Schema(
        description = "The severity level of the alert",
        example = "ERROR",
        allowableValues = {"INFO", "WARNING", "ERROR", "CRITICAL"}
    )
    private AlertSeverity severity;
    
    @Schema(
        description = "When the alert was created",
        example = "2024-01-15T14:30:00.000Z"
    )
    private LocalDateTime timestamp;
    
    @Schema(
        description = "Additional context data for the alert",
        example = "Last successful sync: 2024-01-15T14:00:00.000Z, Error: Access denied"
    )
    private String context;
    
    @Schema(
        description = "The source queue name if applicable",
        example = "payment-processing-queue"
    )
    private String queueName;
}
