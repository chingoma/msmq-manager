package com.enterprise.msmq.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO representing the result of MSMQ queue synchronization operations.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Result of MSMQ queue synchronization operation")
public class QueueSyncResult {
    
    @Schema(
        description = "Total number of queues processed during synchronization",
        example = "15"
    )
    @Builder.Default
    private int totalQueues = 0;
    
    @Schema(
        description = "Number of new queues created during synchronization",
        example = "3"
    )
    @Builder.Default
    private int createdQueues = 0;
    
    @Schema(
        description = "Number of existing queues updated during synchronization",
        example = "10"
    )
    @Builder.Default
    private int updatedQueues = 0;
    
    @Schema(
        description = "Number of queues deleted during synchronization",
        example = "2"
    )
    @Builder.Default
    private int deletedQueues = 0;
    
    @Schema(
        description = "Timestamp when synchronization completed",
        example = "2024-01-15T14:30:00.000Z"
    )
    @Builder.Default
    private LocalDateTime syncTime = LocalDateTime.now();
    
    @Schema(
        description = "Overall synchronization status",
        example = "SUCCESS",
        allowableValues = {"SUCCESS", "PARTIAL_SUCCESS", "FAILED"}
    )
    @Builder.Default
    private String status = "SUCCESS";
    
    @Schema(
        description = "Error message if synchronization failed or partially failed",
        example = "Failed to sync 2 queues due to permission issues"
    )
    private String errorMessage;
    
    /**
     * Increment the count of created queues.
     */
    public void incrementCreated() {
        this.createdQueues++;
        this.totalQueues = createdQueues + updatedQueues + deletedQueues;
    }
    
    /**
     * Increment the count of updated queues.
     */
    public void incrementUpdated() {
        this.updatedQueues++;
        this.totalQueues = createdQueues + updatedQueues + deletedQueues;
    }
    
    /**
     * Increment the count of deleted queues.
     */
    public void incrementDeleted() {
        this.deletedQueues++;
        this.totalQueues = createdQueues + updatedQueues + deletedQueues;
    }
    
    /**
     * Check if the synchronization was successful.
     * 
     * @return true if successful, false otherwise
     */
    public boolean isSuccessful() {
        return "SUCCESS".equals(status);
    }
    
    /**
     * Get a summary of the synchronization operation.
     * 
     * @return formatted summary string
     */
    public String getSummary() {
        return String.format("Sync completed: %d total, %d created, %d updated, %d deleted", 
                           totalQueues, createdQueues, updatedQueues, deletedQueues);
    }
}
