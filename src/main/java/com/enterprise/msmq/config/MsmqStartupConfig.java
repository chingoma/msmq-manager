package com.enterprise.msmq.config;

import com.enterprise.msmq.dto.QueueSyncResult;
import com.enterprise.msmq.service.MsmqQueueSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Configuration class for MSMQ startup operations.
 * This class handles initialization tasks when the application starts up.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MsmqStartupConfig {

    private final MsmqQueueSyncService queueSyncService;

    /**
     * Event listener that triggers when the application is ready.
     * This method will synchronize MSMQ queues with the application database.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            log.info("Application is ready, starting MSMQ queue synchronization...");
            
            // Wait a bit for all services to be fully initialized
            Thread.sleep(2000);
            
            // Perform queue synchronization
            QueueSyncResult syncResult = queueSyncService.syncQueuesFromMsmq();
            
            log.info("MSMQ startup synchronization completed successfully: {}", syncResult.getSummary());
            
        } catch (Exception e) {
            log.error("Error during MSMQ startup synchronization", e);
        }
    }
}
