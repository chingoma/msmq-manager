package com.enterprise.msmq.repository;

import com.enterprise.msmq.model.MsmqQueueConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for MSMQ queue configuration operations.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@Repository
public interface MsmqQueueConfigRepository extends JpaRepository<MsmqQueueConfig, Long> {
    
    /**
     * Find queue configuration by queue name.
     * 
     * @param queueName the name of the queue
     * @return optional containing the queue configuration if found
     */
    Optional<MsmqQueueConfig> findByQueueName(String queueName);
    
    /**
     * Find all active queue configurations.
     * 
     * @return list of active queue configurations
     */
        List<MsmqQueueConfig> findByIsActiveTrue();
    
    /**
     * Find all queue configurations that haven't been synchronized since the given time.
     * 
     * @param since the time to check against
     * @return list of queue configurations that need synchronization
     */
    @Query("SELECT qc FROM MsmqQueueConfig qc WHERE qc.lastSyncTime IS NULL OR qc.lastSyncTime < :since")
    List<MsmqQueueConfig> findQueuesNeedingSync(@Param("since") LocalDateTime since);
    
    /**
     * Find all queue configurations ordered by last sync time.
     * 
     * @return list of queue configurations ordered by last sync time
     */
    @Query("SELECT qc FROM MsmqQueueConfig qc ORDER BY qc.lastSyncTime ASC NULLS FIRST")
    List<MsmqQueueConfig> findAllOrderByLastSyncTime();    

    
    /**
     * Find all queue configurations by active status.
     * 
     * @param isActive whether the queue is active
     * @return list of queue configurations with the specified active status
     */
    List<MsmqQueueConfig> findByIsActive(boolean isActive);
    
    /**
     * Check if a queue configuration exists by name.
     * 
     * @param queueName the name of the queue
     * @return true if the queue configuration exists
     */
    boolean existsByQueueName(String queueName);
    
    /**
     * Delete queue configuration by queue name.
     * 
     * @param queueName the name of the queue to delete
     */
    void deleteByQueueName(String queueName);
    
    /**
     * Count active queue configurations.
     * 
     * @return count of active queue configurations
     */
    long countByIsActiveTrue();
    
    /**
     * Find queue configurations by partial name match.
     * 
     * @param queueNamePattern the pattern to match against queue names
     * @return list of matching queue configurations
     */
    @Query("SELECT qc FROM MsmqQueueConfig qc WHERE qc.queueName LIKE %:pattern%")
    List<MsmqQueueConfig> findByQueueNameContaining(@Param("pattern") String queueNamePattern);
    
    // =====================================================
    // NEW: Queue Direction and Purpose Query Methods
    // =====================================================
    
    /**
     * Find all queue configurations by direction.
     * 
     * @param direction the queue direction to filter by
     * @return list of queue configurations with the specified direction
     */
    List<MsmqQueueConfig> findByQueueDirection(com.enterprise.msmq.enums.QueueDirection direction);
    
    /**
     * Find all queue configurations by purpose.
     * 
     * @param purpose the queue purpose to filter by
     * @return list of queue configurations with the specified purpose
     */
    List<MsmqQueueConfig> findByQueuePurpose(com.enterprise.msmq.enums.QueuePurpose purpose);
    
    /**
     * Find all active queue configurations by direction.
     * 
     * @param direction the queue direction to filter by
     * @return list of active queue configurations with the specified direction
     */
    List<MsmqQueueConfig> findByQueueDirectionAndIsActiveTrue(com.enterprise.msmq.enums.QueueDirection direction);
    
    /**
     * Find all active queue configurations by purpose.
     * 
     * @param purpose the queue purpose to filter by
     * @return list of active queue configurations with the specified purpose
     */
    List<MsmqQueueConfig> findByQueuePurposeAndIsActiveTrue(com.enterprise.msmq.enums.QueuePurpose purpose);
    
    /**
     * Find all active queue configurations by both direction and purpose.
     * 
     * @param direction the queue direction to filter by
     * @param purpose the queue purpose to filter by
     * @return list of active queue configurations matching both criteria
     */
    List<MsmqQueueConfig> findByQueueDirectionAndQueuePurposeAndIsActiveTrue(
        com.enterprise.msmq.enums.QueueDirection direction, 
        com.enterprise.msmq.enums.QueuePurpose purpose);
    
    /**
     * Count queue configurations by direction.
     * 
     * @param direction the queue direction to count
     * @return count of queue configurations with the specified direction
     */
    long countByQueueDirection(com.enterprise.msmq.enums.QueueDirection direction);
    
    /**
     * Count active queue configurations by direction.
     * 
     * @param direction the queue direction to count
     * @return count of active queue configurations with the specified direction
     */
    long countByQueueDirectionAndIsActiveTrue(com.enterprise.msmq.enums.QueueDirection direction);
    
    /**
     * Count queue configurations by purpose.
     * 
     * @param purpose the queue purpose to count
     * @return count of queue configurations with the specified purpose
     */
    long countByQueuePurpose(com.enterprise.msmq.enums.QueuePurpose purpose);
    
    /**
     * Count active queue configurations by purpose.
     * 
     * @param purpose the queue purpose to count
     * @return count of active queue configurations with the specified purpose
     */
    long countByQueuePurposeAndIsActiveTrue(com.enterprise.msmq.enums.QueuePurpose purpose);

}
