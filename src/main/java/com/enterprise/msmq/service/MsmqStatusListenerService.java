package com.enterprise.msmq.service;

import com.enterprise.msmq.dto.StatusMessageDto;
import com.enterprise.msmq.enums.QueueDirection;
import com.enterprise.msmq.enums.QueuePurpose;
import com.enterprise.msmq.exception.MsmqException;
import com.enterprise.msmq.model.MsmqQueueConfig;
import com.enterprise.msmq.repository.MsmqQueueConfigRepository;
import com.enterprise.msmq.service.contracts.IMsmqQueueManager;
import com.enterprise.msmq.factory.MsmqQueueManagerFactory;
import com.enterprise.msmq.dto.MsmqMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for listening to MSMQ status queues and processing status messages.
 * Monitors designated queues for status updates and processes them accordingly.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MsmqStatusListenerService {
    
    private final MsmqQueueManagerFactory queueManagerFactory;
    private final MsmqQueueConfigRepository queueConfigRepository;
    private final StatusMessageParserService statusMessageParserService;
    private final MessageStatusService messageStatusService;
    
    @Value("${msmq.listener.enabled:true}")
    private boolean listenerEnabled;
    
    @Value("${msmq.listener.polling-interval:5000}")
    private long pollingInterval;
    
    @Value("${msmq.listener.max-retries:3}")
    private int maxRetries;
    
    @Value("${msmq.status-queues.status-response:status_response_queue}")
    private String statusResponseQueue;
    
    @Value("${msmq.status-queues.error-notification:error_notification_queue}")
    private String errorNotificationQueue;
    
    @Value("${msmq.status-queues.processing-status:processing_status_queue}")
    private String processingStatusQueue;
    
    @Value("${msmq.status-queues.audit-logs:audit_logs_queue}")
    private String auditLogsQueue;
    
    private final Map<String, AtomicBoolean> listenerStates = new ConcurrentHashMap<>();
    private final Map<String, Integer> retryCounters = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initializeListeners() {
        if (!listenerEnabled) {
            log.info("MSMQ Status Listener is disabled");
            return;
        }
        
        log.info("Initializing MSMQ Status Listeners...");
        
        // Initialize listener states
        listenerStates.put(statusResponseQueue, new AtomicBoolean(false));
        listenerStates.put(errorNotificationQueue, new AtomicBoolean(false));
        listenerStates.put(processingStatusQueue, new AtomicBoolean(false));
        listenerStates.put(auditLogsQueue, new AtomicBoolean(false));
        
        // Start listeners
        startAllListeners();
        
        log.info("MSMQ Status Listeners initialized successfully");
    }
    
    @PreDestroy
    public void shutdownListeners() {
        log.info("Shutting down MSMQ Status Listeners...");
        stopAllListeners();
        log.info("MSMQ Status Listeners shutdown complete");
    }
    
    /**
     * Start all configured listeners.
     */
    public void startAllListeners() {
        if (!listenerEnabled) {
            return;
        }
        
        startListener(statusResponseQueue);
        startListener(errorNotificationQueue);
        startListener(processingStatusQueue);
        startListener(auditLogsQueue);
    }
    
    /**
     * Stop all configured listeners.
     */
    public void stopAllListeners() {
        stopListener(statusResponseQueue);
        stopListener(errorNotificationQueue);
        stopListener(processingStatusQueue);
        stopListener(auditLogsQueue);
    }
    
    /**
     * Start listening to a specific queue.
     */
    public void startListener(String queueName) {
        if (!listenerEnabled) {
            return;
        }
        
        AtomicBoolean listenerState = listenerStates.get(queueName);
        if (listenerState != null && listenerState.compareAndSet(false, true)) {
            log.info("Starting listener for queue: {}", queueName);
            retryCounters.put(queueName, 0);
        }
    }
    
    /**
     * Stop listening to a specific queue.
     */
    public void stopListener(String queueName) {
        AtomicBoolean listenerState = listenerStates.get(queueName);
        if (listenerState != null && listenerState.compareAndSet(true, false)) {
            log.info("Stopping listener for queue: {}", queueName);
        }
    }
    
    /**
     * Scheduled task to poll status queues.
     */
    @Scheduled(fixedDelayString = "${msmq.listener.polling-interval:5000}")
    public void pollStatusQueues() {
        if (!listenerEnabled) {
            return;
        }
        
        pollQueue(statusResponseQueue);
        pollQueue(errorNotificationQueue);
        pollQueue(processingStatusQueue);
        pollQueue(auditLogsQueue);
    }
    
    /**
     * Poll a specific queue for messages.
     */
    @Async
    public void pollQueue(String queueName) {
        AtomicBoolean listenerState = listenerStates.get(queueName);
        if (listenerState == null || !listenerState.get()) {
            return;
        }
        
        try {
            // Check if queue exists and is configured for receiving
            Optional<MsmqQueueConfig> queueConfig = queueConfigRepository.findByQueueName(queueName);
            if (queueConfig.isEmpty()) {
                log.debug("Queue {} not found in database, skipping", queueName);
                return;
            }
            
            MsmqQueueConfig config = queueConfig.get();
            if (!config.getIsActive() || !config.getQueueDirection().allowsReceiving()) {
                log.debug("Queue {} is not active or does not allow receiving, skipping", queueName);
                return;
            }
            
            // Create queue manager and receive message
            IMsmqQueueManager queueManager = queueManagerFactory.createQueueManager();
            Optional<com.enterprise.msmq.dto.MsmqMessage> receivedMessage = queueManager.receiveMessage(queueName);
            
            if (receivedMessage.isPresent()) {
                processStatusMessage(receivedMessage.get(), queueName);
                retryCounters.put(queueName, 0); // Reset retry counter on success
            }
            
        } catch (Exception e) {
            log.error("Error polling queue {}: {}", queueName, e.getMessage());
            handlePollingError(queueName, e);
        }
    }
    
    /**
     * Process a received status message.
     */
    private void processStatusMessage(com.enterprise.msmq.dto.MsmqMessage message, String sourceQueue) {
        try {
            log.info("Processing status message from queue: {}, Correlation ID: {}", 
                    sourceQueue, message.getCorrelationId());
            
            // Parse the status message
            StatusMessageDto statusMessage = statusMessageParserService.parseStatusMessage(
                    message.getBody(), sourceQueue);
            
            if (statusMessage == null) {
                log.warn("Failed to parse status message from queue: {}", sourceQueue);
                return;
            }
            
            // Update message status in database
            updateMessageStatusFromStatusMessage(statusMessage, sourceQueue);
            
            log.info("Successfully processed status message - Common ID: {}, Status: {}", 
                    statusMessage.getCommonId(), statusMessage.getProcessingStatus());
            
        } catch (Exception e) {
            log.error("Error processing status message from queue: {}", sourceQueue, e);
        }
    }
    
    /**
     * Update message status based on parsed status message.
     */
    private void updateMessageStatusFromStatusMessage(StatusMessageDto statusMessage, String sourceQueue) {
        try {
            String commonId = statusMessage.getCommonId();
            String processingStatus = statusMessage.getProcessingStatus();
            String movementType = statusMessage.getSecuritiesMovementType();
            
            if (commonId == null || commonId.isEmpty()) {
                log.warn("No common ID found in status message, cannot update status");
                return;
            }
            
            // Update all messages with this common reference ID
            messageStatusService.updatePairedMessageStatus(
                    commonId, 
                    processingStatus, 
                    statusMessage.getAdditionalReasonInfo()
            );
            
            log.info("Updated status for common reference ID: {} to: {} (Movement Type: {})", 
                    commonId, processingStatus, movementType);
            
        } catch (Exception e) {
            log.error("Error updating message status from status message", e);
        }
    }
    
    /**
     * Handle polling errors with retry logic.
     */
    private void handlePollingError(String queueName, Exception error) {
        int retryCount = retryCounters.getOrDefault(queueName, 0);
        
        if (retryCount < maxRetries) {
            retryCounters.put(queueName, retryCount + 1);
            log.warn("Polling error for queue {} (attempt {}/{}): {}", 
                    queueName, retryCount + 1, maxRetries, error.getMessage());
        } else {
            log.error("Max retries exceeded for queue {}, stopping listener", queueName);
            stopListener(queueName);
        }
    }
    
    /**
     * Get listener status for all queues.
     */
    public Map<String, Boolean> getListenerStatus() {
        Map<String, Boolean> status = new ConcurrentHashMap<>();
        listenerStates.forEach((queueName, state) -> 
                status.put(queueName, state.get()));
        return status;
    }
    
    /**
     * Get retry counters for all queues.
     */
    public Map<String, Integer> getRetryCounters() {
        return new ConcurrentHashMap<>(retryCounters);
    }
    
    /**
     * Check if a specific listener is running.
     */
    public boolean isListenerRunning(String queueName) {
        AtomicBoolean state = listenerStates.get(queueName);
        return state != null && state.get();
    }
    
    /**
     * Restart a specific listener.
     */
    public void restartListener(String queueName) {
        log.info("Restarting listener for queue: {}", queueName);
        stopListener(queueName);
        try {
            Thread.sleep(1000); // Wait 1 second before restarting
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        startListener(queueName);
    }
}
