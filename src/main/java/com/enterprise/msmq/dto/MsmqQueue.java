package com.enterprise.msmq.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * MSMQ Queue DTO for API operations.
 * 
 * This class represents an MSMQ queue with its properties and status
 * information.
 * 
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MsmqQueue {

    /**
     * Queue name identifier.
     */
    @JsonProperty("name")
    private String name;

    /**
     * Queue path in MSMQ format.
     */
    @JsonProperty("path")
    private String path;

    /**
     * Queue type (e.g., "PRIVATE", "PUBLIC", "SYSTEM").
     */
    @JsonProperty("type")
    private String type;

    /**
     * Queue status (e.g., "ACTIVE", "INACTIVE", "ERROR").
     */
    @JsonProperty("status")
    private String status;

    /**
     * Current number of messages in the queue.
     */
    @JsonProperty("messageCount")
    private Long messageCount;

    /**
     * Maximum number of messages the queue can hold.
     */
    @JsonProperty("maxMessageCount")
    private Long maxMessageCount;

    /**
     * Current queue size in bytes.
     */
    @JsonProperty("size")
    private Long size;

    /**
     * Maximum queue size in bytes.
     */
    @JsonProperty("maxSize")
    private Long maxSize;

    /**
     * Queue creation timestamp.
     */
    @JsonProperty("createdTime")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime createdTime;

    /**
     * Last modification timestamp.
     */
    @JsonProperty("modifiedTime")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime modifiedTime;

    /**
     * Last access timestamp.
     */
    @JsonProperty("lastAccessTime")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime lastAccessTime;

    /**
     * Queue description.
     */
    @JsonProperty("description")
    private String description;

    /**
     * Whether the queue is transactional.
     */
    @JsonProperty("transactional")
    private Boolean transactional;

    /**
     * Whether the queue is journaled.
     */
    @JsonProperty("journaled")
    private Boolean journaled;

    /**
     * Whether the queue is authenticated.
     */
    @JsonProperty("authenticated")
    private Boolean authenticated;

    /**
     * Whether the queue is encrypted.
     */
    @JsonProperty("encrypted")
    private Boolean encrypted;

    /**
     * Queue owner information.
     */
    @JsonProperty("owner")
    private String owner;

    /**
     * Queue permissions.
     */
    @JsonProperty("permissions")
    private String permissions;

    /**
     * Error message if queue is in error state.
     */
    @JsonProperty("errorMessage")
    private String errorMessage;

    /**
     * Default constructor.
     */
    public MsmqQueue() {
        this.createdTime = LocalDateTime.now();
        this.modifiedTime = LocalDateTime.now();
        this.lastAccessTime = LocalDateTime.now();
        this.status = "ACTIVE";
        this.messageCount = 0L;
        this.size = 0L;
        this.transactional = false;
        this.journaled = false;
        this.authenticated = false;
        this.encrypted = false;
    }

    /**
     * Constructor with basic queue properties.
     * 
     * @param name the queue name
     * @param path the queue path
     */
    public MsmqQueue(String name, String path) {
        this();
        this.name = name;
        this.path = path;
    }

    /**
     * Constructor with all queue properties.
     * 
     * @param name the queue name
     * @param path the queue path
     * @param type the queue type
     * @param description the queue description
     */
    public MsmqQueue(String name, String path, String type, String description) {
        this(name, path);
        this.type = type;
        this.description = description;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(Long messageCount) {
        this.messageCount = messageCount;
    }

    public Long getMaxMessageCount() {
        return maxMessageCount;
    }

    public void setMaxMessageCount(Long maxMessageCount) {
        this.maxMessageCount = maxMessageCount;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Long getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(Long maxSize) {
        this.maxSize = maxSize;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(LocalDateTime modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public LocalDateTime getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(LocalDateTime lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getTransactional() {
        return transactional;
    }

    public void setTransactional(Boolean transactional) {
        this.transactional = transactional;
    }

    public Boolean getJournaled() {
        return journaled;
    }

    public void setJournaled(Boolean journaled) {
        this.journaled = journaled;
    }

    public Boolean getAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(Boolean authenticated) {
        this.authenticated = authenticated;
    }

    public Boolean getEncrypted() {
        return encrypted;
    }

    public void setEncrypted(Boolean encrypted) {
        this.encrypted = encrypted;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Checks if the queue is full.
     * 
     * @return true if the queue is at maximum capacity
     */
    public boolean isFull() {
        return maxMessageCount != null && messageCount >= maxMessageCount;
    }

    /**
     * Checks if the queue is empty.
     * 
     * @return true if the queue has no messages
     */
    public boolean isEmpty() {
        return messageCount == 0;
    }

    /**
     * Checks if the queue is in error state.
     * 
     * @return true if the queue status is "ERROR"
     */
    public boolean isInError() {
        return "ERROR".equals(status);
    }

    /**
     * Gets the queue utilization percentage.
     * 
     * @return utilization percentage (0-100)
     */
    public double getUtilizationPercentage() {
        if (maxSize == null || maxSize == 0) {
            return 0.0;
        }
        return (double) size / maxSize * 100;
    }

    @Override
    public String toString() {
        return "MsmqQueue{" +
                "name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", type='" + type + '\'' +
                ", status='" + status + '\'' +
                ", messageCount=" + messageCount +
                ", maxMessageCount=" + maxMessageCount +
                ", size=" + size +
                ", maxSize=" + maxSize +
                ", createdTime=" + createdTime +
                ", modifiedTime=" + modifiedTime +
                ", lastAccessTime=" + lastAccessTime +
                ", description='" + description + '\'' +
                ", transactional=" + transactional +
                ", journaled=" + journaled +
                ", authenticated=" + authenticated +
                ", encrypted=" + encrypted +
                ", owner='" + owner + '\'' +
                ", permissions='" + permissions + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
