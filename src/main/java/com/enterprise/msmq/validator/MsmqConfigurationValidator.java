package com.enterprise.msmq.validator;

import com.enterprise.msmq.exception.MsmqException;
import com.enterprise.msmq.enums.ResponseCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Validator for MSMQ configuration values.
 * Validates all configuration parameters for MSMQ connection and operations.
 */
@Component
public class MsmqConfigurationValidator {
    private static final Logger logger = LoggerFactory.getLogger(MsmqConfigurationValidator.class);

    /**
     * Validates the hostname configuration.
     *
     * @param hostname the hostname to validate
     * @throws MsmqException if hostname is invalid
     */
    public void validateHostname(String hostname) throws MsmqException {
        if (hostname == null || hostname.trim().isEmpty()) {
            logger.error("Invalid hostname configuration: hostname is null or empty");
            throw new MsmqException(ResponseCode.INVALID_HOST_CONFIG, "MSMQ hostname cannot be null or empty");
        }
        if (!isValidHostname(hostname)) {
            logger.error("Invalid hostname configuration: invalid hostname format {}", hostname);
            throw new MsmqException(ResponseCode.INVALID_HOST_CONFIG, "Invalid hostname format");
        }
    }

    /**
     * Validates the port configuration.
     *
     * @param port the port to validate
     * @throws MsmqException if port is invalid
     */
    public void validatePort(int port) throws MsmqException {
        if (port <= 0 || port > 65535) {
            logger.error("Invalid port configuration: port {} is out of valid range", port);
            throw new MsmqException(ResponseCode.INVALID_PORT_CONFIG, "MSMQ port must be between 1 and 65535");
        }
    }

    /**
     * Validates the timeout configuration.
     *
     * @param timeout the timeout value to validate (in milliseconds)
     * @throws MsmqException if timeout is invalid
     */
    public void validateTimeout(int timeout) throws MsmqException {
        if (timeout < 0) {
            logger.error("Invalid timeout configuration: timeout {} is negative", timeout);
            throw new MsmqException(ResponseCode.INVALID_TIMEOUT_CONFIG, "Timeout cannot be negative");
        }
        if (timeout == 0) {
            logger.warn("Timeout is set to 0, this means no timeout will be applied");
        }
        if (timeout > 300000) { // 5 minutes
            logger.warn("Timeout {} ms is longer than recommended maximum (300000 ms)", timeout);
        }
    }

    /**
     * Validates the retry attempts configuration.
     *
     * @param retryAttempts the number of retry attempts to validate
     * @throws MsmqException if retry attempts value is invalid
     */
    public void validateRetryAttempts(int retryAttempts) throws MsmqException {
        if (retryAttempts < 0) {
            logger.error("Invalid retry configuration: retry attempts {} is negative", retryAttempts);
            throw new MsmqException(ResponseCode.INVALID_RETRY_CONFIG, "Retry attempts cannot be negative");
        }
        if (retryAttempts == 0) {
            logger.warn("Retry attempts is set to 0, no retries will be attempted on failure");
        }
        if (retryAttempts > 10) {
            logger.warn("Retry attempts {} is higher than recommended maximum (10)", retryAttempts);
        }
    }

    /**
     * Validates if a hostname string is in a valid format.
     * Accepts IP addresses and hostnames.
     *
     * @param hostname the hostname to validate
     * @return true if hostname is valid, false otherwise
     */
    private boolean isValidHostname(String hostname) {
        // Accept localhost
        if ("localhost".equalsIgnoreCase(hostname)) {
            return true;
        }

        // IP address pattern
        String ipPattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        if (hostname.matches(ipPattern)) {
            return true;
        }

        // Hostname pattern
        String hostnamePattern = "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$";
        return hostname.matches(hostnamePattern);
    }
}
