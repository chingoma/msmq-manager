package com.enterprise.msmq.repository;

import com.enterprise.msmq.model.MsmqQueueConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for MSMQ queue configuration entities.
 * Provides database operations for MSMQ queue management.
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
     * @return optional queue configuration
     */
    Optional<MsmqQueueConfig> findByQueueName(String queueName);

    /**
     * Find all active queue configurations.
     * 
     * @return list of active queue configurations
     */
    List<MsmqQueueConfig> findByIsActiveTrue();

    /**
     * Find queue configuration by queue path.
     * 
     * @param queuePath the path of the queue
     * @return optional queue configuration
     */
    Optional<MsmqQueueConfig> findByQueuePath(String queuePath);

    /**
     * Check if queue exists by name.
     * 
     * @param queueName the name of the queue
     * @return true if queue exists
     */
    boolean existsByQueueName(String queueName);

    /**
     * Delete queue configuration by queue name.
     * 
     * @param queueName the name of the queue to delete
     */
    void deleteByQueueName(String queueName);

    /**
     * Find queues by partial name match.
     * 
     * @param partialName partial queue name to search for
     * @return list of matching queue configurations
     */
    @Query("SELECT q FROM MsmqQueueConfig q WHERE q.queueName LIKE %:partialName%")
    List<MsmqQueueConfig> findByQueueNameContaining(@Param("partialName") String partialName);
}
