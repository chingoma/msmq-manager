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
}
