package com.enterprise.msmq.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Data Transfer Object representing performance metrics.
 * Contains performance data for MSMQ operations and system resources.
 *
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
public class PerformanceMetrics {

    private LocalDateTime timestamp;
    private long totalRequests;
    private long successfulRequests;
    private long failedRequests;
    private double averageResponseTime;
    private double throughput;
    private Map<String, Object> queueMetrics;
    private Map<String, Object> systemMetrics;

    /**
     * Default constructor.
     */
    public PerformanceMetrics() {
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public long getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(long totalRequests) {
        this.totalRequests = totalRequests;
    }

    public long getSuccessfulRequests() {
        return successfulRequests;
    }

    public void setSuccessfulRequests(long successfulRequests) {
        this.successfulRequests = successfulRequests;
    }

    public long getFailedRequests() {
        return failedRequests;
    }

    public void setFailedRequests(long failedRequests) {
        this.failedRequests = failedRequests;
    }

    public double getAverageResponseTime() {
        return averageResponseTime;
    }

    public void setAverageResponseTime(double averageResponseTime) {
        this.averageResponseTime = averageResponseTime;
    }

    public double getThroughput() {
        return throughput;
    }

    public void setThroughput(double throughput) {
        this.throughput = throughput;
    }

    public Map<String, Object> getQueueMetrics() {
        return queueMetrics;
    }

    public void setQueueMetrics(Map<String, Object> queueMetrics) {
        this.queueMetrics = queueMetrics;
    }

    public Map<String, Object> getSystemMetrics() {
        return systemMetrics;
    }

    public void setSystemMetrics(Map<String, Object> systemMetrics) {
        this.systemMetrics = systemMetrics;
    }

    @Override
    public String toString() {
        return "PerformanceMetrics{" +
                "timestamp=" + timestamp +
                ", totalRequests=" + totalRequests +
                ", successfulRequests=" + successfulRequests +
                ", failedRequests=" + failedRequests +
                ", averageResponseTime=" + averageResponseTime +
                ", throughput=" + throughput +
                '}';
    }
}
