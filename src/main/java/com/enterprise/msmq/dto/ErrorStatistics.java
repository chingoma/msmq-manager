package com.enterprise.msmq.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Data Transfer Object representing error statistics.
 * Contains error tracking and analysis data for MSMQ operations.
 *
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
public class ErrorStatistics {

    private LocalDateTime timestamp;
    private long totalErrors;
    private Map<String, Long> errorsByCode;
    private Map<String, Long> errorsByOperation;
    private Map<String, Long> errorsByQueue;
    private String lastErrorMessage;
    private LocalDateTime lastErrorTime;

    /**
     * Default constructor.
     */
    public ErrorStatistics() {
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public long getTotalErrors() {
        return totalErrors;
    }

    public void setTotalErrors(long totalErrors) {
        this.totalErrors = totalErrors;
    }

    public Map<String, Long> getErrorsByCode() {
        return errorsByCode;
    }

    public void setErrorsByCode(Map<String, Long> errorsByCode) {
        this.errorsByCode = errorsByCode;
    }

    public Map<String, Long> getErrorsByOperation() {
        return errorsByOperation;
    }

    public void setErrorsByOperation(Map<String, Long> errorsByOperation) {
        this.errorsByOperation = errorsByOperation;
    }

    public Map<String, Long> getErrorsByQueue() {
        return errorsByQueue;
    }

    public void setErrorsByQueue(Map<String, Long> errorsByQueue) {
        this.errorsByQueue = errorsByQueue;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }

    public LocalDateTime getLastErrorTime() {
        return lastErrorTime;
    }

    public void setLastErrorTime(LocalDateTime lastErrorTime) {
        this.lastErrorTime = lastErrorTime;
    }

    @Override
    public String toString() {
        return "ErrorStatistics{" +
                "timestamp=" + timestamp +
                ", totalErrors=" + totalErrors +
                ", lastErrorMessage='" + lastErrorMessage + '\'' +
                ", lastErrorTime=" + lastErrorTime +
                '}';
    }
}
