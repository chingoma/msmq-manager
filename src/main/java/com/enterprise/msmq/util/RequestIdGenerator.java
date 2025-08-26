package com.enterprise.msmq.util;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Utility class for generating unique request identifiers.
 * 
 * This class provides methods to generate unique identifiers for
 * tracking requests throughout the system.
 * 
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
public class RequestIdGenerator {

        /**
     * Generates a unique request identifier.
     *
     * @return a unique request ID string
     */
    public static String generateRequestId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generates a unique request identifier with a custom prefix.
     * 
     * @param prefix the prefix to add to the request ID
     * @return a unique request ID string with prefix
     */
    public String generateRequestId(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return generateRequestId();
        }
        return prefix + "-" + UUID.randomUUID().toString();
    }

    /**
     * Generates a short unique request identifier.
     * 
     * @return a short unique request ID string
     */
    public String generateShortRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
