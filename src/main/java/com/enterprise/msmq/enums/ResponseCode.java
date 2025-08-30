package com.enterprise.msmq.enums;

import lombok.Getter;

/**
 * Response Code enumeration for MSMQ Manager API.
 * 
 * All response codes start from 600 as requested by the business requirements.
 * These codes are used to indicate the status of operations within the response body,
 * allowing API consumers to check statusCode for business logic decisions.
 * 
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Getter
public enum ResponseCode {

    // Success Codes (600-609)
    SUCCESS("600", "Operation completed successfully"),
    QUEUE_CREATED("601", "Queue created successfully"),
    MESSAGE_SENT("602", "Message sent to queue successfully"),
    MESSAGE_RECEIVED("603", "Message received from queue successfully"),
    QUEUE_DELETED("604", "Queue deleted successfully"),
    CONNECTION_ESTABLISHED("605", "MSMQ connection established successfully"),
    QUEUE_UPDATED("606", "Queue updated successfully"),
    MESSAGE_PEEKED("607", "Message peeked successfully"),
    QUEUE_PURGED("608", "Queue purged successfully"),

    // Validation Error Codes (610-619)
    VALIDATION_ERROR("610", "Request validation failed"),
    INVALID_QUEUE_NAME("611", "Invalid queue name provided"),
    INVALID_MESSAGE_FORMAT("612", "Invalid message format"),
    MISSING_REQUIRED_FIELD("613", "Required field is missing"),
    INVALID_QUEUE_PATH("614", "Invalid queue path provided"),
    INVALID_PARAMETER("615", "Invalid parameter provided"),
    INVALID_MESSAGE_ID("616", "Invalid message ID provided"),
    INVALID_MESSAGE_PRIORITY("617", "Invalid message priority level"),
    INVALID_TIMEOUT_VALUE("618", "Invalid timeout value provided"),
    VALIDATION_CONSTRAINT_VIOLATED("619", "Validation constraint violated"),

    // Business Error Codes (620-629)
    BUSINESS_ERROR("620", "Business logic error occurred"),
    QUEUE_NOT_FOUND("621", "Specified queue not found"),
    QUEUE_ALREADY_EXISTS("622", "Queue already exists"),
    MESSAGE_TOO_LARGE("623", "Message size exceeds maximum allowed"),
    QUEUE_IS_FULL("624", "Queue is full and cannot accept more messages"),
    QUEUE_ACCESS_DENIED("625", "Access denied to the specified queue"),
    MESSAGE_NOT_FOUND("626", "Specified message not found in queue"),
    QUEUE_TYPE_MISMATCH("627", "Queue type mismatch for operation"),
    MESSAGE_EXPIRED("628", "Message has expired"),
    OPERATION_NOT_SUPPORTED("629", "Operation not supported on this queue type"),

    // System Error Codes (630-639)
    SYSTEM_ERROR("630", "Internal system error occurred"),
    DATABASE_ERROR("631", "Database operation failed"),
    NETWORK_ERROR("632", "Network communication error"),
    TIMEOUT_ERROR("633", "Operation timed out"),
    CONNECTION_ERROR("634", "MSMQ connection failed"),
    SERIALIZATION_ERROR("635", "Message serialization/deserialization failed"),
    THREAD_INTERRUPTION_ERROR("636", "Thread interruption occurred"),
    RESOURCE_CLEANUP_ERROR("637", "Resource cleanup failed"),
    STATE_TRANSITION_ERROR("638", "Invalid state transition"),
    SYSTEM_RESOURCE_ERROR("639", "System resource allocation failed"),

    // Authentication and Authorization Error Codes (640-649)
    AUTHENTICATION_ERROR("640", "Authentication failed"),
    AUTHORIZATION_ERROR("641", "Authorization denied"),
    INVALID_CREDENTIALS("642", "Invalid credentials provided"),
    TOKEN_EXPIRED("643", "Authentication token expired"),
    INSUFFICIENT_PERMISSIONS("644", "Insufficient permissions for operation"),
    SESSION_EXPIRED("645", "Session has expired"),
    INVALID_TOKEN("646", "Invalid authentication token"),
    ACCESS_DENIED("647", "Access denied to resource"),
    AUTHENTICATION_REQUIRED("648", "Authentication required for operation"),
    PERMISSION_REVOKED("649", "User permissions have been revoked"),

    // Resource Error Codes (650-659)
    RESOURCE_NOT_FOUND("650", "Requested resource not found"),
    RESOURCE_CONFLICT("651", "Resource conflict detected"),
    RESOURCE_UNAVAILABLE("652", "Resource is currently unavailable"),
    RESOURCE_LOCKED("653", "Resource is locked by another process"),
    RESOURCE_EXHAUSTED("654", "Resource limit exceeded"),
    RESOURCE_BUSY("655", "Resource is busy"),
    RESOURCE_DELETED("656", "Resource has been deleted"),
    RESOURCE_CORRUPTED("657", "Resource is corrupted"),
    RESOURCE_VERSION_MISMATCH("658", "Resource version mismatch"),
    RESOURCE_DEPENDENCY_ERROR("659", "Resource dependency error"),

    // MSMQ Specific Error Codes (660-669)
    MSMQ_NOT_AVAILABLE("660", "MSMQ service is not available"),
    QUEUE_CORRUPTED("661", "Queue data is corrupted"),
    TRANSACTION_FAILED("662", "MSMQ transaction failed"),
    MESSAGE_PROPERTY_ERROR("663", "Message property error"),
    QUEUE_PERMISSION_DENIED("664", "Queue permission denied"),
    QUEUE_FORMAT_ERROR("665", "Queue format error"),
    MESSAGE_LABEL_ERROR("666", "Message label error"),
    QUEUE_JOURNAL_ERROR("667", "Queue journal error"),
    MESSAGE_AUTHENTICATION_ERROR("668", "Message authentication failed"),
    QUEUE_QUOTA_EXCEEDED("669", "Queue quota exceeded"),

    // Monitoring and Health Error Codes (670-679)
    HEALTH_CHECK_FAILED("670", "Health check failed"),
    METRICS_UNAVAILABLE("671", "Metrics data unavailable"),
    MONITORING_ERROR("672", "Monitoring operation failed"),
    PERFORMANCE_THRESHOLD_EXCEEDED("673", "Performance threshold exceeded"),
    SERVICE_DEGRADED("674", "Service is in degraded state"),
    RESOURCE_THRESHOLD_WARNING("675", "Resource usage threshold warning"),
    CONNECTIVITY_WARNING("676", "Connectivity issues detected"),
    SYSTEM_OVERLOAD("677", "System is overloaded"),
    BACKUP_ERROR("678", "Backup operation failed"),
    MAINTENANCE_REQUIRED("679", "System maintenance required"),

    // Configuration Error Codes (680-689)
    CONFIGURATION_ERROR("680", "Configuration error detected"),
    INVALID_HOST_CONFIG("681", "Invalid host configuration"),
    INVALID_PORT_CONFIG("682", "Invalid port configuration"),
    INVALID_TIMEOUT_CONFIG("683", "Invalid timeout configuration"),
    INVALID_RETRY_CONFIG("684", "Invalid retry configuration"),
    CONFIG_NOT_FOUND("685", "Configuration not found"),
    CONFIG_PARSE_ERROR("686", "Configuration parse error"),
    INVALID_ENVIRONMENT("687", "Invalid environment configuration"),
    CONFIG_UPDATE_ERROR("688", "Configuration update failed"),
    CONFIG_VALIDATION_ERROR("689", "Configuration validation failed"),

    // Connection State Error Codes (690-698)
    CONNECTION_STATE_ERROR("690", "Invalid connection state transition"),
    CONNECTION_INITIALIZATION_ERROR("691", "Connection initialization failed"),
    CONNECTION_SHUTDOWN_ERROR("692", "Connection shutdown failed"),
    CONNECTION_TIMEOUT("693", "Connection operation timed out"),
    CONNECTION_INTERRUPTED("694", "Connection operation interrupted"),
    CONNECTION_RESET("695", "Connection was reset"),
    CONNECTION_REFUSED("696", "Connection was refused"),
    CONNECTION_CLOSED("697", "Connection is closed"),
    CONNECTION_ABORTED("698", "Connection was aborted"),

    // Unknown Error Code
    UNKNOWN_ERROR("699", "Unknown error occurred");

    /**
     * -- GETTER --
     *  Gets the response code string.
     *
     */
    private final String code;
    /**
     * -- GETTER --
     *  Gets the description of the response code.
     *
     */
    private final String description;

    /**
     * Constructor for ResponseCode enum.
     * 
     * @param code the response code string
     * @param description the human-readable description of the code
     */
    ResponseCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Converts a string code to ResponseCode enum value.
     * 
     * @param code the string code to convert
     * @return the corresponding ResponseCode enum value
     * @throws IllegalArgumentException if the code is not found
     */
    public static ResponseCode fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Code cannot be null or empty");
        }
        
        for (ResponseCode responseCode : values()) {
            if (responseCode.getCode().equals(code.trim())) {
                return responseCode;
            }
        }
        
        throw new IllegalArgumentException("Unknown response code: " + code);
    }

    /**
     * Safely converts a string code to ResponseCode enum value.
     * Returns UNKNOWN_ERROR if the code is not found.
     * 
     * @param code the string code to convert
     * @return the corresponding ResponseCode enum value or UNKNOWN_ERROR
     */
    public static ResponseCode fromCodeSafe(String code) {
        try {
            return fromCode(code);
        } catch (IllegalArgumentException e) {
            return UNKNOWN_ERROR;
        }
    }
}
