package com.enterprise.msmq.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO for queue statistics and monitoring information.
 * Contains comprehensive statistics about queue operations.
 * 
 * @author Enterprise Development Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Comprehensive queue statistics and monitoring information")
public class QueueStatisticsResponse {

    @Schema(
        description = "Total number of queues in the system",
        example = "25"
    )
    private Long totalQueues;

    @Schema(
        description = "Number of active queues",
        example = "22"
    )
    private Long activeQueues;

    @Schema(
        description = "Number of inactive queues",
        example = "3"
    )
    private Long inactiveQueues;

    @Schema(
        description = "Total number of messages across all queues",
        example = "15420"
    )
    private Long totalMessages;

    @Schema(
        description = "Total queue size in bytes",
        example = "1048576000"
    )
    private Long totalSizeBytes;

    @Schema(
        description = "Queue count by direction",
        example = "{\"INCOMING_ONLY\": 8, \"OUTGOING_ONLY\": 5, \"BIDIRECTIONAL\": 12}"
    )
    private Map<String, Long> queuesByDirection;

    @Schema(
        description = "Queue count by purpose",
        example = "{\"GENERAL\": 10, \"SWIFT_MESSAGES\": 8, \"SYSTEM_NOTIFICATIONS\": 7}"
    )
    private Map<String, Long> queuesByPurpose;

    @Schema(
        description = "Queue count by type",
        example = "{\"PRIVATE\": 20, \"PUBLIC\": 3, \"SYSTEM\": 2}"
    )
    private Map<String, Long> queuesByType;

    @Schema(
        description = "Performance metrics for queue operations",
        example = "{\"avgSyncTimeMs\": 1500, \"avgMessageProcessingTimeMs\": 250, \"syncSuccessRate\": 0.98}"
    )
    private Map<String, Object> performanceMetrics;

    @Schema(
        description = "Last synchronization timestamp",
        example = "2024-01-15T14:30:00.000Z"
    )
    private LocalDateTime lastSyncTime;

    @Schema(
        description = "Last synchronization status",
        example = "SUCCESS"
    )
    private String lastSyncStatus;

    @Schema(
        description = "Number of failed synchronizations in the last 24 hours",
        example = "2"
    )
    private Long syncFailures24h;

    @Schema(
        description = "Number of active alerts",
        example = "5"
    )
    private Long activeAlerts;

    @Schema(
        description = "Alert count by severity",
        example = "{\"INFO\": 2, \"WARNING\": 2, \"ERROR\": 1, \"CRITICAL\": 0}"
    )
    private Map<String, Long> alertsBySeverity;

    @Schema(
        description = "System health status",
        example = "HEALTHY"
    )
    private String systemHealth;

    @Schema(
        description = "Timestamp when statistics were generated",
        example = "2024-01-15T14:35:00.000Z"
    )
    private LocalDateTime generatedAt;

    @Schema(
        description = "Additional custom metrics",
        example = "{\"customMetric1\": \"value1\", \"customMetric2\": \"value2\"}"
    )
    private Map<String, Object> customMetrics;
}
