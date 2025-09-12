package com.enterprise.msmq.controller;

import com.enterprise.msmq.dto.ApiResponse;
import com.enterprise.msmq.dto.QueueSyncResult;
import com.enterprise.msmq.enums.ResponseCode;
import com.enterprise.msmq.model.MsmqQueueConfig;
import com.enterprise.msmq.service.MsmqQueueSyncService;
import com.enterprise.msmq.service.ScheduledQueueSyncService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for MSMQ queue synchronization operations.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@RestController
@RequestMapping("/queue-sync")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Queue Synchronization", description = "Endpoints for managing MSMQ queue synchronization")
public class QueueSyncController {

    private final MsmqQueueSyncService queueSyncService;
    private final ScheduledQueueSyncService scheduledQueueSyncService;

    /**
     * Manually trigger queue synchronization.
     * 
     * @return synchronization result
     */
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<QueueSyncResult>> syncQueues() {
        try {
            log.info("Manual queue synchronization requested");
            QueueSyncResult result = queueSyncService.syncQueuesFromMsmq();
            
            return ResponseEntity.ok(ApiResponse.success("Queue synchronization completed successfully", result));
                
        } catch (Exception e) {
            log.error("Error during manual queue synchronization", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Queue synchronization failed: " + e.getMessage()));
        }
    }

    /**
     * Get synchronization statistics.
     * 
     * @return sync statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSyncStatistics() {
        try {
            Map<String, Object> stats = queueSyncService.getSyncStatistics();
            
            return ResponseEntity.ok(ApiResponse.success("Sync statistics retrieved successfully", stats));
                
        } catch (Exception e) {
            log.error("Error retrieving sync statistics", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to retrieve sync statistics: " + e.getMessage()));
        }
    }

    /**
     * Find queues that need synchronization.
     * 
     * @param hours number of hours to look back (default: 24)
     * @return list of queues needing sync
     */
    @GetMapping("/queues-needing-sync")
    public ResponseEntity<ApiResponse<List<MsmqQueueConfig>>> findQueuesNeedingSync(
            @RequestParam(defaultValue = "24") int hours) {
        try {
            LocalDateTime since = LocalDateTime.now().minusHours(hours);
            List<MsmqQueueConfig> queues = queueSyncService.findQueuesNeedingSync(since);
            
            return ResponseEntity.ok(ApiResponse.success("Found " + queues.size() + " queues needing synchronization", queues));
                
        } catch (Exception e) {
            log.error("Error finding queues needing sync", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to find queues needing sync: " + e.getMessage()));
        }
    }

    /**
     * Force synchronization of a specific queue.
     * 
     * @param queueName the name of the queue to sync
     * @return sync result
     */
    @PostMapping("/sync/{queueName}")
    public ResponseEntity<ApiResponse<Boolean>> syncSpecificQueue(@PathVariable String queueName) {
        try {
            log.info("Specific queue synchronization requested for: {}", queueName);
            boolean success = queueSyncService.syncSpecificQueue(queueName);
            
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("Queue '" + queueName + "' synchronized successfully", true));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ResponseCode.BUSINESS_ERROR, "Failed to synchronize queue '" + queueName + "'"));
            }
                
        } catch (Exception e) {
            log.error("Error syncing specific queue: {}", queueName, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Error syncing queue '" + queueName + "': " + e.getMessage()));
        }
    }

    /**
     * Get scheduled sync configuration.
     * 
     * @return sync configuration
     */
    @GetMapping("/config")
    public ResponseEntity<ApiResponse<String>> getSyncConfiguration() {
        try {
            String config = scheduledQueueSyncService.getSyncConfiguration();
            
            return ResponseEntity.ok(ApiResponse.success("Sync configuration retrieved successfully", config));
                
        } catch (Exception e) {
            log.error("Error retrieving sync configuration", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to retrieve sync configuration: " + e.getMessage()));
        }
    }

    /**
     * Check if scheduled synchronization is enabled.
     * 
     * @return sync status
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSyncStatus() {
        try {
            Map<String, Object> status = Map.of(
                "scheduledSyncEnabled", scheduledQueueSyncService.isSyncEnabled(),
                "syncIntervalMs", scheduledQueueSyncService.getSyncIntervalMs(),
                "lastSyncTime", LocalDateTime.now() // This would ideally come from a service
            );
            
            return ResponseEntity.ok(ApiResponse.success("Sync status retrieved successfully", status));
                
        } catch (Exception e) {
            log.error("Error retrieving sync status", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to retrieve sync status: " + e.getMessage()));
        }
    }
}
