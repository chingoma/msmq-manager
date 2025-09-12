package com.enterprise.msmq.service;

import com.enterprise.msmq.dto.QueueSyncResult;
import com.enterprise.msmq.service.QueueMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.enterprise.msmq.model.MsmqQueueConfig;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service responsible for scheduled MSMQ queue synchronization.
 * Runs periodic synchronization to keep the database in sync with MSMQ.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledQueueSyncService {
    
    private final MsmqQueueSyncService queueSyncService;
    private final QueueMonitoringService monitoringService;
    
    @Value("${msmq.queue.sync.enabled:true}")
    private boolean syncEnabled;
    
    @Value("${msmq.queue.sync.interval:300000}")
    private long syncIntervalMs;
    
    /**
     * Scheduled task that runs queue synchronization at regular intervals.
     * Default interval is 5 minutes (300,000 ms).
     */
    @Scheduled(fixedRateString = "${msmq.queue.sync.interval:300000}")
    public void scheduledQueueSync() {
        if (!syncEnabled) {
            log.debug("Scheduled queue synchronization is disabled");
            return;
        }
        
        try {
            log.debug("Running scheduled queue synchronization...");
            long startTime = System.currentTimeMillis();
            
            QueueSyncResult result = queueSyncService.syncQueuesFromMsmq();
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Scheduled queue sync completed in {}ms: {}", duration, result.getSummary());
            
            // Record monitoring data and generate alerts
            monitoringService.recordSyncResults(result, duration);
            
            // Check for inactive queues that need alerts
            List<MsmqQueueConfig> inactiveQueues = queueSyncService.findQueuesNeedingSync(
                LocalDateTime.now().minusHours(24));
            monitoringService.checkInactiveQueues(inactiveQueues);
            
        } catch (Exception e) {
            log.error("Error during scheduled queue synchronization", e);
            // Don't rethrow - scheduled tasks should not fail the application
        }
    }
    
    /**
     * Get the current synchronization configuration.
     * 
     * @return configuration information
     */
    public String getSyncConfiguration() {
        return String.format("Scheduled Queue Sync - Enabled: %s, Interval: %dms (%d minutes)", 
                           syncEnabled, syncIntervalMs, syncIntervalMs / 60000);
    }
    
    /**
     * Check if scheduled synchronization is enabled.
     * 
     * @return true if enabled
     */
    public boolean isSyncEnabled() {
        return syncEnabled;
    }
    
    /**
     * Get the current sync interval in milliseconds.
     * 
     * @return sync interval in milliseconds
     */
    public long getSyncIntervalMs() {
        return syncIntervalMs;
    }
}
