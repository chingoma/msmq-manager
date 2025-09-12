package com.enterprise.msmq.service;

import com.enterprise.msmq.dto.QueueSyncResult;
import com.enterprise.msmq.dto.QueueAlert;
import com.enterprise.msmq.enums.AlertType;
import com.enterprise.msmq.enums.AlertSeverity;
import com.enterprise.msmq.model.MsmqQueueConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service responsible for monitoring MSMQ queue health and sending alerts.
 * Tracks queue changes, performance metrics, and system health.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QueueMonitoringService {

    private final EmailNotificationService emailNotificationService;
    
    @Value("${msmq.monitoring.alerts.enabled:true}")
    private boolean alertsEnabled;
    
    @Value("${msmq.monitoring.performance.threshold-ms:10000}")
    private long performanceThresholdMs;
    
    @Value("${msmq.monitoring.queue.inactive-threshold-hours:24}")
    private int inactiveThresholdHours;
    
    // In-memory tracking for performance and alerts
    private final Map<String, QueueHealthMetrics> queueHealthMap = new ConcurrentHashMap<>();
    private final List<QueueAlert> recentAlerts = new ArrayList<>();
    private final int MAX_ALERTS = 100; // Keep last 100 alerts
    
    /**
     * Record queue synchronization results and generate alerts.
     * 
     * @param result the synchronization result
     * @param durationMs how long the sync took
     */
    public void recordSyncResults(QueueSyncResult result, long durationMs) {
        if (!alertsEnabled) {
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        // Performance alert
        if (durationMs > performanceThresholdMs) {
            createAlert(AlertType.PERFORMANCE_DEGRADATION, 
                "Queue synchronization took " + durationMs + "ms (threshold: " + performanceThresholdMs + "ms)", 
                AlertSeverity.WARNING, now);
        }
        
        // Queue creation alerts
        if (result.getCreatedQueues() > 0) {
            createAlert(AlertType.QUEUE_CREATED, 
                result.getCreatedQueues() + " new queue(s) detected and created", 
                AlertSeverity.INFO, now);
        }
        
        // Queue deletion alerts
        if (result.getDeletedQueues() > 0) {
            createAlert(AlertType.QUEUE_DELETED, 
                result.getDeletedQueues() + " queue(s) marked as inactive", 
                AlertSeverity.WARNING, now);
        }
        
        // Sync failure alerts
        if (!result.isSuccessful()) {
            createAlert(AlertType.SYNC_FAILURE, 
                "Queue synchronization failed: " + result.getErrorMessage(), 
                AlertSeverity.ERROR, now);
        }
        
        // Update health metrics
        updateHealthMetrics(result, durationMs);
    }
    
    /**
     * Check for inactive queues and generate alerts.
     * 
     * @param inactiveQueues list of inactive queue configurations
     */
    public void checkInactiveQueues(List<MsmqQueueConfig> inactiveQueues) {
        if (!alertsEnabled || inactiveQueues.isEmpty()) {
            return;
        }
        
        LocalDateTime threshold = LocalDateTime.now().minusHours(inactiveThresholdHours);
        
        for (MsmqQueueConfig queue : inactiveQueues) {
            if (queue.getLastSyncTime() != null && queue.getLastSyncTime().isBefore(threshold)) {
                createAlert(AlertType.QUEUE_INACTIVE_TOO_LONG, 
                    "Queue '" + queue.getQueueName() + "' has been inactive for more than " + inactiveThresholdHours + " hours", 
                    AlertSeverity.WARNING, LocalDateTime.now());
            }
        }
    }
    
    /**
     * Monitor individual queue health and generate alerts.
     * 
     * @param queueName the name of the queue
     * @param isHealthy whether the queue is healthy
     * @param errorMessage error details if unhealthy
     */
    public void monitorQueueHealth(String queueName, boolean isHealthy, String errorMessage) {
        if (!alertsEnabled) {
            return;
        }
        
        QueueHealthMetrics metrics = queueHealthMap.computeIfAbsent(queueName, 
            k -> new QueueHealthMetrics(queueName));
        
        if (!isHealthy) {
            metrics.incrementFailureCount();
            
            if (metrics.getFailureCount() >= 3) { // Alert after 3 consecutive failures
                createAlert(AlertType.QUEUE_UNHEALTHY, 
                    "Queue '" + queueName + "' is unhealthy: " + errorMessage, 
                    AlertSeverity.ERROR, LocalDateTime.now());
            }
        } else {
            metrics.resetFailureCount();
        }
        
        metrics.setLastHealthCheck(LocalDateTime.now());
        metrics.setHealthy(isHealthy);
    }
    
    /**
     * Get current monitoring statistics.
     * 
     * @return monitoring statistics
     */
    public Map<String, Object> getMonitoringStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Alert statistics
        long errorAlerts = recentAlerts.stream()
            .filter(alert -> alert.getSeverity() == AlertSeverity.ERROR)
            .count();
        long warningAlerts = recentAlerts.stream()
            .filter(alert -> alert.getSeverity() == AlertSeverity.WARNING)
            .count();
        long infoAlerts = recentAlerts.stream()
            .filter(alert -> alert.getSeverity() == AlertSeverity.INFO)
            .count();
        
        stats.put("totalAlerts", recentAlerts.size());
        stats.put("errorAlerts", errorAlerts);
        stats.put("warningAlerts", warningAlerts);
        stats.put("infoAlerts", infoAlerts);
        stats.put("alertsEnabled", alertsEnabled);
        stats.put("performanceThresholdMs", performanceThresholdMs);
        stats.put("inactiveThresholdHours", inactiveThresholdHours);
        stats.put("monitoredQueues", queueHealthMap.size());
        
        // Health summary
        long healthyQueues = queueHealthMap.values().stream()
            .filter(QueueHealthMetrics::isHealthy)
            .count();
        long unhealthyQueues = queueHealthMap.size() - healthyQueues;
        
        stats.put("healthyQueues", healthyQueues);
        stats.put("unhealthyQueues", unhealthyQueues);
        stats.put("lastUpdate", LocalDateTime.now());
        
        return stats;
    }
    
    /**
     * Get recent alerts.
     * 
     * @param limit maximum number of alerts to return
     * @return list of recent alerts
     */
    public List<QueueAlert> getRecentAlerts(int limit) {
        return recentAlerts.stream()
            .sorted(Comparator.comparing(QueueAlert::getTimestamp).reversed())
            .limit(limit)
            .toList();
    }
    
    /**
     * Get alerts by severity.
     * 
     * @param severity the severity level to filter by
     * @return list of alerts with the specified severity
     */
    public List<QueueAlert> getAlertsBySeverity(AlertSeverity severity) {
        return recentAlerts.stream()
            .filter(alert -> alert.getSeverity() == severity)
            .sorted(Comparator.comparing(QueueAlert::getTimestamp).reversed())
            .toList();
    }
    
    /**
     * Clear old alerts to prevent memory buildup.
     */
    public void cleanupOldAlerts() {
        if (recentAlerts.size() > MAX_ALERTS) {
            recentAlerts.sort(Comparator.comparing(QueueAlert::getTimestamp).reversed());
            recentAlerts.subList(MAX_ALERTS, recentAlerts.size()).clear();
            log.debug("Cleaned up old alerts, keeping {} most recent", MAX_ALERTS);
        }
    }
    
    /**
     * Create a new alert.
     * 
     * @param type the type of alert
     * @param message the alert message
     * @param severity the severity level
     * @param timestamp when the alert occurred
     */
    private void createAlert(AlertType type, String message, AlertSeverity severity, LocalDateTime timestamp) {
        QueueAlert alert = QueueAlert.builder()
            .type(type)
            .message(message)
            .severity(severity)
            .timestamp(timestamp)
            .build();
        
        recentAlerts.add(alert);
        
        // Log the alert
        switch (severity) {
            case ERROR -> log.error("ALERT [{}]: {}", type, message);
            case WARNING -> log.warn("ALERT [{}]: {}", type, message);
            case INFO -> log.info("ALERT [{}]: {}", type, message);
        }
        
        // Cleanup old alerts
        cleanupOldAlerts();
        
        // Send email notification if enabled
        try {
            emailNotificationService.sendAlertNotification(alert);
        } catch (Exception e) {
            log.error("Failed to send email notification for alert: {}", alert.getMessage(), e);
        }
    }
    
    /**
     * Update health metrics based on sync results.
     * 
     * @param result the sync result
     * @param durationMs sync duration
     */
    private void updateHealthMetrics(QueueSyncResult result, long durationMs) {
        // Update overall system health
        QueueHealthMetrics systemMetrics = queueHealthMap.computeIfAbsent("SYSTEM", 
            k -> new QueueHealthMetrics("SYSTEM"));
        
        systemMetrics.setLastHealthCheck(LocalDateTime.now());
        systemMetrics.setHealthy(result.isSuccessful());
        systemMetrics.setLastSyncDuration(durationMs);
        
        if (result.isSuccessful()) {
            systemMetrics.incrementSuccessCount();
        } else {
            systemMetrics.incrementFailureCount();
        }
    }
    
    /**
     * Inner class for tracking queue health metrics.
     */
    private static class QueueHealthMetrics {
        private final String queueName;
        private boolean healthy = true;
        private LocalDateTime lastHealthCheck;
        private long lastSyncDuration;
        private int successCount = 0;
        private int failureCount = 0;
        
        public QueueHealthMetrics(String queueName) {
            this.queueName = queueName;
            this.lastHealthCheck = LocalDateTime.now();
        }
        
        // Getters and setters
        public boolean isHealthy() { return healthy; }
        public void setHealthy(boolean healthy) { this.healthy = healthy; }
        public LocalDateTime getLastHealthCheck() { return lastHealthCheck; }
        public void setLastHealthCheck(LocalDateTime lastHealthCheck) { this.lastHealthCheck = lastHealthCheck; }
        public long getLastSyncDuration() { return lastSyncDuration; }
        public void setLastSyncDuration(long lastSyncDuration) { this.lastSyncDuration = lastSyncDuration; }
        public int getSuccessCount() { return successCount; }
        public void incrementSuccessCount() { this.successCount++; }
        public int getFailureCount() { return failureCount; }
        public void incrementFailureCount() { this.failureCount++; }
        public void resetFailureCount() { this.failureCount = 0; }
    }
}
