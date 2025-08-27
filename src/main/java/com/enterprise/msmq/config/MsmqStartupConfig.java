package com.enterprise.msmq.config;

import com.enterprise.msmq.service.MsmqQueueSyncService;
import com.enterprise.msmq.util.MsmqQueueManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
public class MsmqStartupConfig {

    private static final Logger logger = LoggerFactory.getLogger(MsmqStartupConfig.class);

    @Autowired
    private MsmqQueueSyncService msmqQueueSyncService;

    @Autowired
    private MsmqQueueManager msmqQueueManager;

    /**
     * Event listener that triggers when the application is ready.
     * This method will synchronize MSMQ queues with the application database.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            logger.info("Application is ready, starting MSMQ queue synchronization...");
            
            // Wait a bit for all services to be fully initialized
            Thread.sleep(2000);
            
            // Synchronize queues from MSMQ to application database
            msmqQueueManager.syncQueuesFromMsmq();
            
            logger.info("MSMQ startup synchronization completed successfully");
            
        } catch (Exception e) {
            logger.error("Error during MSMQ startup synchronization", e);
        }
    }
}
