package com.enterprise.msmq.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Data Transfer Object representing health check results.
 * Contains detailed health status information for various system components.
 *
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
public class HealthCheckResult {

    private String status;
    private LocalDateTime timestamp;
    private Map<String, Object> components;
    private Map<String, Object> details;

    /**
     * Default constructor.
     */
    public HealthCheckResult() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructor with status.
     *
     * @param status the health status
     */
    public HealthCheckResult(String status) {
        this();
        this.status = status;
    }

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getComponents() {
        return components;
    }

    public void setComponents(Map<String, Object> components) {
        this.components = components;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    @Override
    public String toString() {
        return "HealthCheckResult{" +
                "status='" + status + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
