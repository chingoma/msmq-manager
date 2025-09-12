package com.enterprise.msmq.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * Request DTO for queue synchronization operations.
 * Contains only the fields that should be provided by the client.
 * 
 * @author Enterprise Development Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request for queue synchronization operations")
public class QueueSyncRequest {

    @Schema(
        description = "Specific queue names to synchronize (if empty, syncs all queues)",
        example = "[\"payment-queue\", \"notification-queue\"]"
    )
    private List<String> queueNames;

    @Schema(
        description = "Whether to perform a full synchronization",
        example = "false"
    )
    private Boolean fullSync;

    @Schema(
        description = "Whether to update queue classifications during sync",
        example = "true"
    )
    private Boolean updateClassifications;

    @Schema(
        description = "Whether to validate message routing rules",
        example = "true"
    )
    private Boolean validateRouting;

    @Schema(
        description = "Maximum number of queues to process in parallel",
        example = "5",
        minimum = "1",
        maximum = "20"
    )
    private Integer maxConcurrency;

    @Schema(
        description = "Timeout for individual queue operations in milliseconds",
        example = "30000",
        minimum = "5000"
    )
    private Long operationTimeoutMs;

    @Schema(
        description = "Whether to send notifications on sync completion",
        example = "true"
    )
    private Boolean sendNotifications;

    @Schema(
        description = "Custom sync options as key-value pairs",
        example = "{\"skipInactiveQueues\": \"true\", \"forceUpdate\": \"false\"}"
    )
    private java.util.Map<String, String> syncOptions;
}
