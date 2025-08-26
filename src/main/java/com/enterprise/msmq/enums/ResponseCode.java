package com.enterprise.msmq.enums;

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
public enum ResponseCode {

    // Success Codes (600-609)
    SUCCESS("600", "Operation completed successfully"),
    QUEUE_CREATED("601", "Queue created successfully"),
    MESSAGE_SENT("602", "Message sent to queue successfully"),
    MESSAGE_RECEIVED("603", "Message received from queue successfully"),
    QUEUE_DELETED("604", "Queue deleted successfully"),
    CONNECTION_ESTABLISHED("605", "MSMQ connection established successfully"),

    // Validation Error Codes (610-619)
    VALIDATION_ERROR("610", "Request validation failed"),
    INVALID_QUEUE_NAME("611", "Invalid queue name provided"),
    INVALID_MESSAGE_FORMAT("612", "Invalid message format"),
    MISSING_REQUIRED_FIELD("613", "Required field is missing"),
    INVALID_QUEUE_PATH("614", "Invalid queue path provided"),

    // Business Error Codes (620-629)
    BUSINESS_ERROR("620", "Business logic error occurred"),
    QUEUE_NOT_FOUND("621", "Specified queue not found"),
    QUEUE_ALREADY_EXISTS("622", "Queue already exists"),
    MESSAGE_TOO_LARGE("623", "Message size exceeds maximum allowed"),
    QUEUE_IS_FULL("624", "Queue is full and cannot accept more messages"),
    QUEUE_ACCESS_DENIED("625", "Access denied to the specified queue"),

    // System Error Codes (630-639)
    SYSTEM_ERROR("630", "Internal system error occurred"),
    DATABASE_ERROR("631", "Database operation failed"),
    NETWORK_ERROR("632", "Network communication error"),
    TIMEOUT_ERROR("633", "Operation timed out"),
    CONNECTION_ERROR("634", "MSMQ connection failed"),
    SERIALIZATION_ERROR("635", "Message serialization/deserialization failed"),

    // Authentication and Authorization Error Codes (640-649)
    AUTHENTICATION_ERROR("640", "Authentication failed"),
    AUTHORIZATION_ERROR("641", "Authorization denied"),
    INVALID_CREDENTIALS("642", "Invalid credentials provided"),
    TOKEN_EXPIRED("643", "Authentication token expired"),
    INSUFFICIENT_PERMISSIONS("644", "Insufficient permissions for operation"),

    // Resource Error Codes (650-659)
    RESOURCE_NOT_FOUND("650", "Requested resource not found"),
    RESOURCE_CONFLICT("651", "Resource conflict detected"),
    RESOURCE_UNAVAILABLE("652", "Resource is currently unavailable"),
    RESOURCE_LOCKED("653", "Resource is locked by another process"),

    // MSMQ Specific Error Codes (660-669)
    MSMQ_NOT_AVAILABLE("660", "MSMQ service is not available"),
    QUEUE_CORRUPTED("661", "Queue data is corrupted"),
    TRANSACTION_FAILED("662", "MSMQ transaction failed"),
    MESSAGE_PROPERTY_ERROR("663", "Message property error"),
    QUEUE_PERMISSION_DENIED("664", "Queue permission denied"),

    // Monitoring and Health Error Codes (670-679)
    HEALTH_CHECK_FAILED("670", "Health check failed"),
    METRICS_UNAVAILABLE("671", "Metrics data unavailable"),
    MONITORING_ERROR("672", "Monitoring operation failed"),

    // Unknown Error Code
    UNKNOWN_ERROR("699", "Unknown error occurred");

    private final String code;
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
     * Gets the response code string.
     * 
     * @return the response code
     */
    public String getCode() {
        return code;
    }

    /**
     * Gets the description of the response code.
     * 
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the ResponseCode enum by its code string.
     * 
     * @param code the response code string to search for
     * @return the ResponseCode enum or null if not found
     */
    public static ResponseCode fromCode(String code) {
        for (ResponseCode responseCode : values()) {
            if (responseCode.code.equals(code)) {
                return responseCode;
            }
        }
        return null;
    }

    /**
     * Checks if the response code indicates success.
     * 
     * @return true if the code is in the success range (600-609)
     */
    public boolean isSuccess() {
        return this.code.compareTo("600") >= 0 && this.code.compareTo("609") <= 0;
    }

    /**
     * Checks if the response code indicates an error.
     * 
     * @return true if the code is not in the success range
     */
    public boolean isError() {
        return !isSuccess();
    }

    @Override
    public String toString() {
        return code + " - " + description;
    }
}
