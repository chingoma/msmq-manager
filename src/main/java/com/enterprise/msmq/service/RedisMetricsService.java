package com.enterprise.msmq.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis service for storing and retrieving performance metrics.
 * Replaces in-memory storage with Redis for better scalability and persistence.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisMetricsService {

    private final RedisTemplate<String, Object> redisTemplate;
    
    // Redis key prefixes for different metric types
    private static final String OPERATION_TIMINGS_PREFIX = "msmq:metrics:timings:";
    private static final String ERROR_COUNTS_PREFIX = "msmq:metrics:errors:";
    private static final String TOTAL_MESSAGES_PREFIX = "msmq:metrics:total:";
    private static final String CACHE_TTL_HOURS = "msmq:cache:ttl";
    
    // TTL for metrics (24 hours)
    private static final long METRICS_TTL_HOURS = 24;

    /**
     * Store operation timing metric in Redis.
     * 
     * @param operation the operation name
     * @param duration the operation duration in milliseconds
     */
    public void storeOperationTiming(String operation, long duration) {
        try {
            String key = OPERATION_TIMINGS_PREFIX + operation;
            redisTemplate.opsForValue().set(key, duration, METRICS_TTL_HOURS, TimeUnit.HOURS);
            log.debug("Stored operation timing for {}: {}ms", operation, duration);
        } catch (Exception e) {
            log.warn("Failed to store operation timing in Redis for {}: {}", operation, e.getMessage());
        }
    }

    /**
     * Get operation timing metric from Redis.
     * 
     * @param operation the operation name
     * @return the operation duration in milliseconds, or null if not found
     */
    public Long getOperationTiming(String operation) {
        try {
            String key = OPERATION_TIMINGS_PREFIX + operation;
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? (Long) value : null;
        } catch (Exception e) {
            log.warn("Failed to get operation timing from Redis for {}: {}", operation, e.getMessage());
            return null;
        }
    }

    /**
     * Increment error count for an operation in Redis.
     * 
     * @param operation the operation name
     */
    public void incrementErrorCount(String operation) {
        try {
            String key = ERROR_COUNTS_PREFIX + operation;
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, METRICS_TTL_HOURS, TimeUnit.HOURS);
            log.debug("Incremented error count for operation: {}", operation);
        } catch (Exception e) {
            log.warn("Failed to increment error count in Redis for {}: {}", operation, e.getMessage());
        }
    }

    /**
     * Get error count for an operation from Redis.
     * 
     * @param operation the operation name
     * @return the error count, or 0 if not found
     */
    public Long getErrorCount(String operation) {
        try {
            String key = ERROR_COUNTS_PREFIX + operation;
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? (Long) value : 0L;
        } catch (Exception e) {
            log.warn("Failed to get error count from Redis for {}: {}", operation, e.getMessage());
            return 0L;
        }
    }

    /**
     * Store total message count in Redis.
     * 
     * @param messageType the type of message (sent/received)
     * @param count the total count
     */
    public void storeTotalMessageCount(String messageType, long count) {
        try {
            String key = TOTAL_MESSAGES_PREFIX + messageType;
            redisTemplate.opsForValue().set(key, count);
            log.debug("Stored total message count for {}: {}", messageType, count);
        } catch (Exception e) {
            log.warn("Failed to store total message count in Redis for {}: {}", messageType, e.getMessage());
        }
    }

    /**
     * Get total message count from Redis.
     * 
     * @param messageType the type of message (sent/received)
     * @return the total count, or 0 if not found
     */
    public Long getTotalMessageCount(String messageType) {
        try {
            String key = TOTAL_MESSAGES_PREFIX + messageType;
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? (Long) value : 0L;
        } catch (Exception e) {
            log.warn("Failed to get total message count from Redis for {}: {}", messageType, e.getMessage());
            return 0L;
        }
    }

    /**
     * Increment total message count in Redis.
     * 
     * @param messageType the type of message (sent/received)
     */
    public void incrementTotalMessageCount(String messageType) {
        try {
            String key = TOTAL_MESSAGES_PREFIX + messageType;
            redisTemplate.opsForValue().increment(key);
            log.debug("Incremented total message count for: {}", messageType);
        } catch (Exception e) {
            log.warn("Failed to increment total message count in Redis for {}: {}", messageType, e.getMessage());
        }
    }

    /**
     * Get all operation timings from Redis.
     * 
     * @return map of operation names to durations
     */
    public Map<String, Long> getAllOperationTimings() {
        try {
            // This would need to be implemented with Redis SCAN or similar
            // For now, return empty map - can be enhanced later
            log.debug("Getting all operation timings from Redis");
            return Map.of();
        } catch (Exception e) {
            log.warn("Failed to get all operation timings from Redis: {}", e.getMessage());
            return Map.of();
        }
    }

    /**
     * Get all error counts from Redis.
     * 
     * @return map of operation names to error counts
     */
    public Map<String, Long> getAllErrorCounts() {
        try {
            // This would need to be implemented with Redis SCAN or similar
            // For now, return empty map - can be enhanced later
            log.debug("Getting all error counts from Redis");
            return Map.of();
        } catch (Exception e) {
            log.warn("Failed to get all error counts from Redis: {}", e.getMessage());
            return Map.of();
        }
    }

    /**
     * Clear all metrics from Redis.
     */
    public void clearAllMetrics() {
        try {
            // This would need to be implemented with Redis SCAN and DEL
            // For now, just log the intention
            log.info("Clearing all metrics from Redis");
        } catch (Exception e) {
            log.warn("Failed to clear metrics from Redis: {}", e.getMessage());
        }
    }
}
