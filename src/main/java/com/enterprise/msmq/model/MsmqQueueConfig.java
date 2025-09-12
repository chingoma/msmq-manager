package com.enterprise.msmq.model;

import com.enterprise.msmq.enums.QueueDirection;
import com.enterprise.msmq.enums.QueuePurpose;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

/**
 * Entity class representing MSMQ queue configuration.
 * Stores persistent configuration for MSMQ queues.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@Entity
@Table(name = "msmq_queue_config")
public class MsmqQueueConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Queue name is required")
    @Column(name = "queue_name", unique = true, nullable = false)
    private String queueName;

    @Column(name = "queue_path")
    private String queuePath;

    @Column(name = "description")
    private String description;

    @NotNull(message = "Maximum message size is required")
    @Positive(message = "Maximum message size must be positive")
    @Column(name = "max_message_size")
    private Long maxMessageSize;

    @Column(name = "is_transactional")
    private Boolean isTransactional = false;

    @Column(name = "is_private")
    private Boolean isPrivate = false;

    @Column(name = "is_authenticated")
    private Boolean isAuthenticated = false;

    @Column(name = "is_encrypted")
    private Boolean isEncrypted = false;

    @Column(name = "retry_count")
    private Integer retryCount = 3;

    @Column(name = "retry_interval_ms")
    private Long retryIntervalMs = 5000L;

    @Column(name = "timeout_ms")
    private Long timeoutMs = 30000L;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "last_sync_time")
    private LocalDateTime lastSyncTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "queue_direction")
    private QueueDirection queueDirection = QueueDirection.BIDIRECTIONAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "queue_purpose")
    private QueuePurpose queuePurpose = QueuePurpose.GENERAL;

    // Default constructor
    public MsmqQueueConfig() {
        this.createdAt = LocalDateTime.now();
    }

    // Constructor with required fields
    public MsmqQueueConfig(String queueName, String queuePath, Long maxMessageSize) {
        this();
        this.queueName = queueName;
        this.queuePath = queuePath;
        this.maxMessageSize = maxMessageSize;
        this.queueDirection = QueueDirection.BIDIRECTIONAL; // Default to bidirectional
        this.queuePurpose = QueuePurpose.GENERAL; // Default to general purpose
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public String getQueuePath() {
        return queuePath;
    }

    public void setQueuePath(String queuePath) {
        this.queuePath = queuePath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getMaxMessageSize() {
        return maxMessageSize;
    }

    public void setMaxMessageSize(Long maxMessageSize) {
        this.maxMessageSize = maxMessageSize;
    }

    public Boolean getIsTransactional() {
        return isTransactional;
    }

    public void setIsTransactional(Boolean isTransactional) {
        this.isTransactional = isTransactional;
    }

    public Boolean getIsPrivate() {
        return isPrivate;
    }

    public void setIsPrivate(Boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public Boolean getIsAuthenticated() {
        return isAuthenticated;
    }

    public void setIsAuthenticated(Boolean isAuthenticated) {
        this.isAuthenticated = isAuthenticated;
    }

    public Boolean getIsEncrypted() {
        return isEncrypted;
    }

    public void setIsEncrypted(Boolean isEncrypted) {
        this.isEncrypted = isEncrypted;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Long getRetryIntervalMs() {
        return retryIntervalMs;
    }

    public void setRetryIntervalMs(Long retryIntervalMs) {
        this.retryIntervalMs = retryIntervalMs;
    }

    public Long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public LocalDateTime getLastSyncTime() {
        return lastSyncTime;
    }
    
    public void setLastSyncTime(LocalDateTime lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
    }
    
    /**
     * Update the last synchronization time to now.
     */
    public void updateLastSyncTime() {
        this.lastSyncTime = LocalDateTime.now();
    }

    public QueueDirection getQueueDirection() {
        return queueDirection;
    }

    public void setQueueDirection(QueueDirection queueDirection) {
        this.queueDirection = queueDirection;
    }

    public QueuePurpose getQueuePurpose() {
        return queuePurpose;
    }

    public void setQueuePurpose(QueuePurpose queuePurpose) {
        this.queuePurpose = queuePurpose;
    }

    /**
     * Pre-persist hook to set creation timestamp.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Pre-update hook to set update timestamp.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "MsmqQueueConfig{" +
                "id=" + id +
                ", queueName='" + queueName + '\'' +
                ", queuePath='" + queuePath + '\'' +
                ", maxMessageSize=" + maxMessageSize +
                ", isTransactional=" + isTransactional +
                ", isActive=" + isActive +
                ", queueDirection=" + queueDirection +
                ", queuePurpose=" + queuePurpose +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MsmqQueueConfig that = (MsmqQueueConfig) o;
        return queueName != null ? queueName.equals(that.queueName) : that.queueName == null;
    }

    @Override
    public int hashCode() {
        return queueName != null ? queueName.hashCode() : 0;
    }
}
