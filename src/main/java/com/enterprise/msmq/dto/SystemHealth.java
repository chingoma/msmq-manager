package com.enterprise.msmq.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Data Transfer Object representing system health information.
 * Contains comprehensive health status of the MSMQ system and its components.
 *
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
public class SystemHealth {

    private String status;
    private LocalDateTime timestamp;
    private String version;
    private long uptime;
    private Map<String, Object> components;
    private Map<String, Object> details;

    /**
     * Default constructor.
     */
    public SystemHealth() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructor with status.
     *
     * @param status the system status
     */
    public SystemHealth(String status) {
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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public long getUptime() {
        return uptime;
    }

    public void setUptime(long uptime) {
        this.uptime = uptime;
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
        return "SystemHealth{" +
                "status='" + status + '\'' +
                ", timestamp=" + timestamp +
                ", version='" + version + '\'' +
                ", uptime=" + uptime +
                '}';
    }
}
