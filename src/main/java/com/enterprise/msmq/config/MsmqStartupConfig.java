package com.enterprise.msmq.config;

import com.enterprise.msmq.dto.QueueSyncResult;
import com.enterprise.msmq.service.DefaultConfigurationInitializerService;
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
    private final DefaultConfigurationInitializerService defaultConfigInitializerService;

    /**
     * Event listener that triggers when the application is ready.
     * This method will:
     * 1. Initialize default email configurations and mailing lists
     * 2. Synchronize MSMQ queues with the application database
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            log.info("🚀 Application is ready, starting initialization sequence...");

            // Wait a bit for all services to be fully initialized
            Thread.sleep(2000);
            
            // Step 1: Initialize default configurations (email and mailing lists)
            log.info("Step 1: Initializing default configurations...");
            defaultConfigInitializerService.initializeDefaultConfigurations();

            // Step 2: Perform queue synchronization
            log.info("Step 2: Starting MSMQ queue synchronization...");
            QueueSyncResult syncResult = queueSyncService.syncQueuesFromMsmq();
            
            log.info("✅ MSMQ startup initialization completed successfully");
            log.info("   - Queue synchronization: {}", syncResult.getSummary());

            // Log initialization status
            DefaultConfigurationInitializerService.InitializationStatus status =
                defaultConfigInitializerService.getInitializationStatus();
            log.info("   - Default email config exists: {}", status.getDefaultEmailConfigExists());
            log.info("   - Default mailing list exists: {} ({})",
                status.getDefaultMailingListExists(), status.getDefaultMailingListName());

        } catch (Exception e) {
            log.error("❌ Error during MSMQ startup initialization", e);
        }
    }
}
