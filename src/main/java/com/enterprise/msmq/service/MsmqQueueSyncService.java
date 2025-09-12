package com.enterprise.msmq.service;

import com.enterprise.msmq.dto.MsmqQueue;
import com.enterprise.msmq.dto.QueueSyncResult;
import com.enterprise.msmq.model.MsmqQueueConfig;
import com.enterprise.msmq.enums.QueueDirection;
import com.enterprise.msmq.enums.QueuePurpose;
import com.enterprise.msmq.enums.ResponseCode;
import com.enterprise.msmq.exception.MsmqException;
import com.enterprise.msmq.factory.MsmqQueueManagerFactory;
import com.enterprise.msmq.repository.MsmqQueueConfigRepository;
import com.enterprise.msmq.service.contracts.IMsmqQueueManager;
import com.enterprise.msmq.service.QueueMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service responsible for synchronizing MSMQ queues with the application database.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MsmqQueueSyncService {
    
    private final MsmqQueueManagerFactory queueManagerFactory;
    private final MsmqQueueConfigRepository queueConfigRepository;
    private final QueueMonitoringService monitoringService;
    
    @Value("${msmq.queue.sync.retry-attempts:3}")
    private int maxRetryAttempts;
    
    @Value("${msmq.queue.sync.retry-delay:1000}")
    private long retryDelayMs;
    
    /**
     * Main synchronization method that synchronizes MSMQ queues with the database.
     * 
     * @return QueueSyncResult containing the synchronization results
     * @throws MsmqException if synchronization fails
     */
    public QueueSyncResult syncQueuesFromMsmq() {
        try {
            log.info("Starting MSMQ queue synchronization...");
            
            // Get queue manager
            IMsmqQueueManager queueManager = queueManagerFactory.createQueueManager();
            
            // Fetch all queues from MSMQ system
            List<MsmqQueue> msmqQueues = queueManager.getAllQueues();
            log.info("Found {} queues in MSMQ system", msmqQueues.size());
            
            // Get existing queue configurations from database
            List<MsmqQueueConfig> existingConfigs = queueConfigRepository.findByIsActiveTrue();
            log.info("Found {} existing queue configurations in database", existingConfigs.size());
            
                        // Perform synchronization
            QueueSyncResult result = performSync(msmqQueues, existingConfigs);
            
            // Update last sync time for all active configs
            updateLastSyncTime();
            
            log.info("Queue synchronization completed: {}", result.getSummary());
            return result;
            
        } catch (Exception e) {
            log.error("Error during queue synchronization", e);
            throw new MsmqException(ResponseCode.SYSTEM_ERROR, "Queue synchronization failed", e);
        }
    }
    
    /**
     * Perform the actual synchronization logic.
     * 
     * @param msmqQueues queues from MSMQ system
     * @param existingConfigs existing configurations from database
     * @return synchronization result
     */
    private QueueSyncResult performSync(List<MsmqQueue> msmqQueues, 
                                      List<MsmqQueueConfig> existingConfigs) {
        
        QueueSyncResult result = QueueSyncResult.builder()
            .syncTime(LocalDateTime.now())
            .status("SUCCESS")
            .build();
        
        // Process each MSMQ queue
        for (MsmqQueue msmqQueue : msmqQueues) {
            try {
                String queueName = msmqQueue.getName();
                
                // Check if queue already exists in database
                Optional<MsmqQueueConfig> existingConfig = queueConfigRepository.findByQueueName(queueName);
                
                if (existingConfig.isPresent()) {
                    // Update existing configuration
                    MsmqQueueConfig config = existingConfig.get();
                    config.setQueuePath(msmqQueue.getPath());
                    config.setUpdatedBy("SYSTEM_SYNC");
                    config.updateLastSyncTime();
                    queueConfigRepository.save(config);
                    result.incrementUpdated();
                    log.debug("Updated queue configuration: {}", queueName);
                } else {
                    // Create new configuration
                    createQueueConfig(msmqQueue);
                    result.incrementCreated();
                    log.debug("Created new queue configuration: {}", queueName);
                }
            } catch (Exception e) {
                log.error("Error processing queue: {}", msmqQueue.getName(), e);
                // Continue with other queues instead of failing completely
            }
        }
        
        // Handle deleted queues (in database but not in MSMQ)
        handleDeletedQueues(msmqQueues, existingConfigs, result);
        
        return result;
    }
    
    /**
     * Create a new queue configuration from MSMQ queue data.
     * 
     * @param msmqQueue the MSMQ queue data
     */
    private void createQueueConfig(MsmqQueue msmqQueue) {
        MsmqQueueConfig config = new MsmqQueueConfig();
        config.setQueueName(msmqQueue.getName());
        config.setQueuePath(msmqQueue.getPath());
        config.setIsActive(true);
        config.setCreatedBy("SYSTEM_SYNC");
        config.setMaxMessageSize(1024L); // Default max message size
        
        // NEW: Automatically classify the queue based on its name
        config.setQueueDirection(classifyQueue(msmqQueue.getName()));
        config.setQueuePurpose(determineQueuePurpose(msmqQueue.getName()));
        
        config.updateLastSyncTime();
        
        queueConfigRepository.save(config);
        
        log.debug("Created queue '{}' with direction={}, purpose={}", 
                 msmqQueue.getName(), config.getQueueDirection(), config.getQueuePurpose());
    }
    
    /**
     * Handle queues that exist in database but not in MSMQ system.
     * 
     * @param msmqQueues current MSMQ queues
     * @param existingConfigs existing database configurations
     * @param result the sync result to update
     */
    private void handleDeletedQueues(List<MsmqQueue> msmqQueues, 
                                   List<MsmqQueueConfig> existingConfigs,
                                   QueueSyncResult result) {
        
        // Simple check - mark queues as inactive if they don't exist in MSMQ
        for (MsmqQueueConfig config : existingConfigs) {
            boolean found = msmqQueues.stream()
                .anyMatch(q -> q.getName().equals(config.getQueueName()));
                
            if (!found) {
                try {
                    // Queue no longer exists in MSMQ, mark as inactive
                    config.setIsActive(false);
                    config.setUpdatedBy("SYSTEM_SYNC");
                    config.updateLastSyncTime();
                    queueConfigRepository.save(config);
                    result.incrementDeleted();
                    log.debug("Marked queue as inactive: {}", config.getQueueName());
                } catch (Exception e) {
                    log.error("Error marking queue as inactive: {}", config.getQueueName(), e);
                }
            }
        }
    }
    
    /**
     * Update the last synchronization time for all active configurations.
     */
    private void updateLastSyncTime() {
        try {
            List<MsmqQueueConfig> activeConfigs = queueConfigRepository.findByIsActiveTrue();
            
            for (MsmqQueueConfig config : activeConfigs) {
                config.updateLastSyncTime();
            }
            
            queueConfigRepository.saveAll(activeConfigs);
            log.debug("Updated last sync time for {} active configurations", activeConfigs.size());
            
        } catch (Exception e) {
            log.error("Error updating last sync time for configurations", e);
        }
    }
    
    /**
     * Get synchronization statistics.
     * 
     * @return statistics about the current queue state
     */
    public Map<String, Object> getSyncStatistics() {
        long totalConfigs = queueConfigRepository.count();
        long activeConfigs = queueConfigRepository.countByIsActiveTrue();
        long inactiveConfigs = totalConfigs - activeConfigs;
        
        return Map.of(
            "totalConfigurations", totalConfigs,
            "activeConfigurations", activeConfigs,
            "inactiveConfigurations", inactiveConfigs,
            "lastSyncTime", LocalDateTime.now()
        );
    }
    
    /**
     * Find queues that need synchronization.
     * 
     * @param since time threshold for sync check
     * @return list of configurations needing sync
     */
    public List<MsmqQueueConfig> findQueuesNeedingSync(LocalDateTime since) {
        return queueConfigRepository.findQueuesNeedingSync(since);
    }
    
    /**
     * Force synchronization of a specific queue.
     * 
     * @param queueName the name of the queue to sync
     * @return true if sync was successful
     */
    public boolean syncSpecificQueue(String queueName) {
        try {
            Optional<MsmqQueueConfig> queueOpt = queueConfigRepository.findByQueueName(queueName);
            if (queueOpt.isEmpty()) {
                log.warn("Queue configuration not found: {}", queueName);
                return false;
            }
            
            IMsmqQueueManager queueManager = queueManagerFactory.createQueueManager();
            Optional<MsmqQueue> msmqQueue = queueManager.getQueue(queueName);
            
            if (msmqQueue.isPresent()) {
                MsmqQueueConfig config = queueOpt.get();
                config.setQueuePath(msmqQueue.get().getPath());
                config.setUpdatedBy("SYSTEM_SYNC");
                config.updateLastSyncTime();
                queueConfigRepository.save(config);
                log.info("Successfully synced specific queue: {}", queueName);
                return true;
            } else {
                // Queue no longer exists in MSMQ
                MsmqQueueConfig config = queueOpt.get();
                config.setIsActive(false);
                config.updateLastSyncTime();
                queueConfigRepository.save(config);
                log.info("Marked queue as inactive during specific sync: {}", queueName);
                return true;
            }
            
        } catch (Exception e) {
            log.error("Error syncing specific queue: {}", queueName, e);
            return false;
        }
    }
    
    // =====================================================
    // NEW: Queue Direction Classification Methods
    // =====================================================
    
    /**
     * Classify a queue based on its name and characteristics.
     * 
     * @param queueName the name of the queue to classify
     * @return the determined queue direction
     */
    public QueueDirection classifyQueue(String queueName) {
        if (queueName == null || queueName.trim().isEmpty()) {
            return QueueDirection.BIDIRECTIONAL; // Default fallback
        }
        
        String lowerName = queueName.toLowerCase();
        
        // Classify based on queue name patterns
        if (lowerName.contains("incoming") || lowerName.contains("input") || 
            lowerName.contains("receive") || lowerName.contains("ingest") ||
            lowerName.contains("source") || lowerName.contains("from")) {
            return QueueDirection.INCOMING_ONLY;
        }
        
        if (lowerName.contains("outgoing") || lowerName.contains("output") || 
            lowerName.contains("send") || lowerName.contains("deliver") ||
            lowerName.contains("target") || lowerName.contains("to") ||
            lowerName.contains("notification") || lowerName.contains("alert")) {
            return QueueDirection.OUTGOING_ONLY;
        }
        
        // Default to bidirectional for most queues
        return QueueDirection.BIDIRECTIONAL;
    }
    
    /**
     * Determine the business purpose of a queue based on its name.
     * 
     * @param queueName the name of the queue to analyze
     * @return the determined queue purpose
     */
    public QueuePurpose determineQueuePurpose(String queueName) {
        if (queueName == null || queueName.trim().isEmpty()) {
            return QueuePurpose.GENERAL; // Default fallback
        }
        
        String lowerName = queueName.toLowerCase();
        
        // Classify based on queue name patterns
        if (lowerName.contains("swift") || lowerName.contains("mt") || 
            lowerName.contains("mx") || lowerName.contains("financial") ||
            lowerName.contains("payment") || lowerName.contains("transfer")) {
            return QueuePurpose.SWIFT_MESSAGES;
        }
        
        if (lowerName.contains("system") || lowerName.contains("notification") || 
            lowerName.contains("alert") || lowerName.contains("monitor") ||
            lowerName.contains("health") || lowerName.contains("status")) {
            return QueuePurpose.SYSTEM_NOTIFICATIONS;
        }
        
        if (lowerName.contains("error") || lowerName.contains("dead") || 
            lowerName.contains("retry") || lowerName.contains("failed") ||
            lowerName.contains("dlq") || lowerName.contains("nack")) {
            return QueuePurpose.ERROR_HANDLING;
        }
        
        if (lowerName.contains("audit") || lowerName.contains("log") || 
            lowerName.contains("compliance") || lowerName.contains("record") ||
            lowerName.contains("trace")) {
            return QueuePurpose.AUDIT_LOGS;
        }
        
        if (lowerName.contains("sync") || lowerName.contains("replication") || 
            lowerName.contains("backup") || lowerName.contains("mirror")) {
            return QueuePurpose.DATA_SYNC;
        }
        
        if (lowerName.contains("batch") || lowerName.contains("bulk") || 
            lowerName.contains("job") || lowerName.contains("process")) {
            return QueuePurpose.BATCH_PROCESSING;
        }
        
        if (lowerName.contains("urgent") || lowerName.contains("critical") || 
            lowerName.contains("emergency") || lowerName.contains("priority")) {
            return QueuePurpose.URGENT_MESSAGES;
        }
        
        // Default to general purpose
        return QueuePurpose.GENERAL;
    }
    
    /**
     * Get all queues by direction.
     * 
     * @param direction the queue direction to filter by
     * @return list of queue configurations with the specified direction
     */
    public List<MsmqQueueConfig> getQueuesByDirection(QueueDirection direction) {
        return queueConfigRepository.findByQueueDirectionAndIsActiveTrue(direction);
    }
    
    /**
     * Get all queues by purpose.
     * 
     * @param purpose the queue purpose to filter by
     * @return list of queue configurations with the specified purpose
     */
    public List<MsmqQueueConfig> getQueuesByPurpose(QueuePurpose purpose) {
        return queueConfigRepository.findByQueuePurposeAndIsActiveTrue(purpose);
    }
    
    /**
     * Validate if a message can be routed between source and destination queues.
     * 
     * @param sourceQueue the source queue name
     * @param destinationQueue the destination queue name
     * @return true if routing is valid, false otherwise
     */
    public boolean validateMessageRouting(String sourceQueue, String destinationQueue) {
        try {
            Optional<MsmqQueueConfig> sourceConfig = queueConfigRepository.findByQueueName(sourceQueue);
            Optional<MsmqQueueConfig> destConfig = queueConfigRepository.findByQueueName(destinationQueue);
            
            // If either queue doesn't exist, allow routing (will be created during sync)
            if (sourceConfig.isEmpty() || destConfig.isEmpty()) {
                return true;
            }
            
            MsmqQueueConfig source = sourceConfig.get();
            MsmqQueueConfig dest = destConfig.get();
            
            // Source queue must allow receiving (to get messages from)
            if (!source.getQueueDirection().allowsReceiving()) {
                log.warn("Source queue '{}' does not allow receiving messages", sourceQueue);
                return false;
            }
            
            // Destination queue must allow sending (to send messages to)
            if (!dest.getQueueDirection().allowsSending()) {
                log.warn("Destination queue '{}' does not allow sending messages", destinationQueue);
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("Error validating message routing from '{}' to '{}'", sourceQueue, destinationQueue, e);
            return false;
        }
    }
    
    /**
     * Update queue classification for existing queues.
     * This method can be called to reclassify queues after business rules change.
     * 
     * @return number of queues that were reclassified
     */
    public int updateQueueClassifications() {
        try {
            log.info("Starting queue classification update...");
            int updatedCount = 0;
            
            List<MsmqQueueConfig> allActiveQueues = queueConfigRepository.findByIsActiveTrue();
            
            for (MsmqQueueConfig config : allActiveQueues) {
                QueueDirection newDirection = classifyQueue(config.getQueueName());
                QueuePurpose newPurpose = determineQueuePurpose(config.getQueueName());
                
                boolean needsUpdate = false;
                
                if (!newDirection.equals(config.getQueueDirection())) {
                    config.setQueueDirection(newDirection);
                    needsUpdate = true;
                }
                
                if (!newPurpose.equals(config.getQueuePurpose())) {
                    config.setQueuePurpose(newPurpose);
                    needsUpdate = true;
                }
                
                if (needsUpdate) {
                    config.setUpdatedBy("SYSTEM_CLASSIFICATION");
                    queueConfigRepository.save(config);
                    updatedCount++;
                    log.debug("Updated classification for queue '{}': direction={}, purpose={}", 
                             config.getQueueName(), newDirection, newPurpose);
                }
            }
            
            log.info("Queue classification update completed. {} queues updated.", updatedCount);
            return updatedCount;
            
        } catch (Exception e) {
            log.error("Error updating queue classifications", e);
            return 0;
        }
    }
}
