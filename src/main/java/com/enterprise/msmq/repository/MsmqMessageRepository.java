package com.enterprise.msmq.repository;

import com.enterprise.msmq.entity.MsmqMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for MsmqMessage entities.
 * Replaces in-memory storage with database persistence.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@Repository
public interface MsmqMessageRepository extends JpaRepository<MsmqMessage, Long> {

    /**
     * Find message by unique message ID.
     */
    Optional<MsmqMessage> findByMessageId(String messageId);

    /**
     * Find all messages for a specific queue.
     */
    List<MsmqMessage> findByQueueNameOrderByCreatedAtDesc(String queueName);

    /**
     * Find messages by correlation ID.
     */
    List<MsmqMessage> findByCorrelationId(String correlationId);

    /**
     * Find messages by processing status.
     */
    List<MsmqMessage> findByProcessingStatus(String processingStatus);

    /**
     * Find unprocessed messages for a queue.
     */
    List<MsmqMessage> findByQueueNameAndIsProcessedFalseOrderByCreatedAtAsc(String queueName);

    /**
     * Find expired messages.
     */
    @Query("SELECT m FROM MsmqMessage m WHERE m.expiresAt < :now")
    List<MsmqMessage> findExpiredMessages(@Param("now") LocalDateTime now);

    /**
     * Count messages in a queue.
     */
    long countByQueueName(String queueName);

    /**
     * Count unprocessed messages in a queue.
     */
    long countByQueueNameAndIsProcessedFalse(String queueName);

    /**
     * Delete all messages for a specific queue.
     */
    void deleteByQueueName(String queueName);

    /**
     * Find messages by priority range.
     */
    @Query("SELECT m FROM MsmqMessage m WHERE m.priority BETWEEN :minPriority AND :maxPriority ORDER BY m.priority DESC, m.createdAt ASC")
    List<MsmqMessage> findByPriorityRange(@Param("minPriority") Integer minPriority, @Param("maxPriority") Integer maxPriority);

    /**
     * Find messages created within a time range.
     */
    @Query("SELECT m FROM MsmqMessage m WHERE m.createdAt BETWEEN :startTime AND :endTime ORDER BY m.createdAt DESC")
    List<MsmqMessage> findByCreatedAtBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    // New methods for enhanced status tracking

    /**
     * Find messages by common reference ID (for paired messages like RECE/DELI).
     */
    List<MsmqMessage> findByCommonReferenceIdOrderByCreatedAtAsc(String commonReferenceId);

    /**
     * Find messages by transaction ID.
     */
    Optional<MsmqMessage> findByTransactionId(String transactionId);

    /**
     * Find messages by movement type (RECE, DELI, etc.).
     */
    List<MsmqMessage> findByMovementTypeOrderByCreatedAtDesc(String movementType);

    /**
     * Find messages by common reference ID and movement type.
     */
    List<MsmqMessage> findByCommonReferenceIdAndMovementTypeOrderByCreatedAtAsc(String commonReferenceId, String movementType);

    /**
     * Find messages by linked transaction ID.
     */
    List<MsmqMessage> findByLinkedTransactionIdOrderByCreatedAtAsc(String linkedTransactionId);

    /**
     * Find messages by environment (local, remote).
     */
    List<MsmqMessage> findByEnvironmentOrderByCreatedAtDesc(String environment);

    /**
     * Find messages by template name.
     */
    List<MsmqMessage> findByTemplateNameOrderByCreatedAtDesc(String templateName);

    /**
     * Find messages by processing status and environment.
     */
    List<MsmqMessage> findByProcessingStatusAndEnvironmentOrderByCreatedAtDesc(String processingStatus, String environment);

    /**
     * Count messages by common reference ID.
     */
    long countByCommonReferenceId(String commonReferenceId);

    /**
     * Find messages that need status updates (sent but not yet processed).
     */
    @Query("SELECT m FROM MsmqMessage m WHERE m.sentAt IS NOT NULL AND m.processedAt IS NULL AND m.processingStatus = 'SENT'")
    List<MsmqMessage> findMessagesNeedingStatusUpdate();

    /**
     * Find paired messages (RECE and DELI) by common reference ID.
     */
    @Query("SELECT m FROM MsmqMessage m WHERE m.commonReferenceId = :commonReferenceId AND m.movementType IN ('RECE', 'DELI') ORDER BY m.movementType, m.createdAt ASC")
    List<MsmqMessage> findPairedMessagesByCommonReferenceId(@Param("commonReferenceId") String commonReferenceId);
}
