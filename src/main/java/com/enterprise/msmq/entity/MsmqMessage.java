package com.enterprise.msmq.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.enterprise.msmq.model.MsmqQueueConfig;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * JPA Entity for storing MSMQ messages in the database.
 * Replaces in-memory storage with persistent database storage.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@Entity
@Table(name = "msmq_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MsmqMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Message ID is required")
    @Column(name = "message_id", unique = true, nullable = false)
    private String messageId;

    @NotBlank(message = "Queue name is required")
    @Column(name = "queue_name", nullable = false)
    private String queueName;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "label", length = 500)
    private String label;

    @NotBlank(message = "Message body is required")
    @Column(name = "body", columnDefinition = "TEXT", nullable = false)
    private String body;

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0;

    @Column(name = "message_size")
    private Long messageSize;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "is_processed")
    @Builder.Default
    private Boolean isProcessed = false;

    @Column(name = "processing_status", length = 50)
    @Builder.Default
    private String processingStatus = "PENDING";

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 3;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "additional_properties", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> additionalProperties;

    // New fields for enhanced status tracking
    @Column(name = "common_reference_id", length = 50)
    private String commonReferenceId;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "movement_type", length = 20)
    private String movementType; // RECE, DELI, etc.

    @Column(name = "linked_transaction_id", length = 100)
    private String linkedTransactionId;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "environment", length = 20)
    private String environment; // local, remote

    @Column(name = "template_name", length = 100)
    private String templateName;

    // Foreign key relationship to queue config
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "queue_name", referencedColumnName = "queue_name", insertable = false, updatable = false)
    private MsmqQueueConfig queueConfig;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.messageSize == null && this.body != null) {
            this.messageSize = (long) this.body.getBytes().length;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.messageSize == null && this.body != null) {
            this.messageSize = (long) this.body.getBytes().length;
        }
    }
}
