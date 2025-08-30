package com.enterprise.msmq.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Service for storing and managing MSMQ metrics in Redis.
 */
@Service
public class RedisMetricsService {
    private static final Logger logger = LoggerFactory.getLogger(RedisMetricsService.class);
    private static final String METRICS_PREFIX = "msmq:metrics:";
    private static final String ERROR_PREFIX = "msmq:errors:";
    private static final Duration METRICS_TTL = Duration.ofDays(7); // Keep metrics for 7 days

    private final RedisTemplate<String, String> redisTemplate;

    public RedisMetricsService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Stores operation timing information.
     *
     * @param operation the operation name
     * @param duration the operation duration in milliseconds
     */
    public void storeOperationTiming(String operation, long duration) {
        try {
            String key = METRICS_PREFIX + "timing:" + operation;
            redisTemplate.opsForValue().set(key, String.valueOf(duration), METRICS_TTL);

            // Store in time series for trending
            String timeSeriesKey = METRICS_PREFIX + "timing:" + operation + ":" + System.currentTimeMillis();
            redisTemplate.opsForValue().set(timeSeriesKey, String.valueOf(duration), METRICS_TTL);
        } catch (Exception e) {
            logger.warn("Failed to store operation timing: {}", e.getMessage());
        }
    }

    /**
     * Stores error information for an operation.
     *
     * @param operation the operation name
     * @param errorMessage the error message
     */
    public void storeLastError(String operation, String errorMessage) {
        try {
            String key = ERROR_PREFIX + "last:" + operation;
            redisTemplate.opsForValue().set(key, errorMessage, METRICS_TTL);

            // Store error timestamp
            String timestampKey = ERROR_PREFIX + "timestamp:" + operation;
            redisTemplate.opsForValue().set(timestampKey, String.valueOf(System.currentTimeMillis()), METRICS_TTL);

            // Increment error count
            String countKey = ERROR_PREFIX + "count:" + operation;
            redisTemplate.opsForValue().increment(countKey, 1);
            redisTemplate.expire(countKey, METRICS_TTL);
        } catch (Exception e) {
            logger.warn("Failed to store error information: {}", e.getMessage());
        }
    }

    /**
     * Increments the total message count for a specific operation type.
     *
     * @param operationType the type of operation (e.g., "sent", "received")
     */
    public void incrementTotalMessageCount(String operationType) {
        try {
            String key = METRICS_PREFIX + "messages:" + operationType + ":total";
            redisTemplate.opsForValue().increment(key, 1);
            redisTemplate.expire(key, METRICS_TTL);

            // Store rate information
            String rateKey = METRICS_PREFIX + "messages:" + operationType + ":rate:" + System.currentTimeMillis();
            redisTemplate.opsForValue().set(rateKey, "1", Duration.ofMinutes(5));
        } catch (Exception e) {
            logger.warn("Failed to increment message count: {}", e.getMessage());
        }
    }

    /**
     * Increments the error count for an operation.
     *
     * @param operation the operation name
     */
    public void incrementErrorCount(String operation) {
        try {
            String key = ERROR_PREFIX + "count:" + operation;
            redisTemplate.opsForValue().increment(key, 1);
            redisTemplate.expire(key, METRICS_TTL);
        } catch (Exception e) {
            logger.warn("Failed to increment error count: {}", e.getMessage());
        }
    }

    /**
     * Gets the error count for an operation.
     *
     * @param operation the operation name
     * @return the error count
     */
    public long getErrorCount(String operation) {
        try {
            String key = ERROR_PREFIX + "count:" + operation;
            String count = redisTemplate.opsForValue().get(key);
            return count != null ? Long.parseLong(count) : 0;
        } catch (Exception e) {
            logger.warn("Failed to get error count: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Gets the last error message for an operation.
     *
     * @param operation the operation name
     * @return the last error message or null if none exists
     */
    public String getLastError(String operation) {
        try {
            String key = ERROR_PREFIX + "last:" + operation;
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            logger.warn("Failed to get last error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Gets the total message count for a specific operation type.
     *
     * @param operationType the type of operation (e.g., "sent", "received")
     * @return the total message count
     */
    public long getTotalMessageCount(String operationType) {
        try {
            String key = METRICS_PREFIX + "messages:" + operationType + ":total";
            String count = redisTemplate.opsForValue().get(key);
            return count != null ? Long.parseLong(count) : 0;
        } catch (Exception e) {
            logger.warn("Failed to get total message count: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Gets the average operation timing for the specified operation.
     *
     * @param operation the operation name
     * @return the average duration in milliseconds
     */
    public double getAverageOperationTiming(String operation) {
        try {
            String pattern = METRICS_PREFIX + "timing:" + operation + ":*";
            long total = 0;
            long count = 0;

            for (String key : redisTemplate.keys(pattern)) {
                String value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    total += Long.parseLong(value);
                    count++;
                }
            }

            return count > 0 ? (double) total / count : 0;
        } catch (Exception e) {
            logger.warn("Failed to get average operation timing: {}", e.getMessage());
            return 0;
        }
    }
}
