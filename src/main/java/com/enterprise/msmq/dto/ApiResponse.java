package com.enterprise.msmq.dto;

import com.enterprise.msmq.enums.ResponseCode;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Generic API Response structure for MSMQ Manager API.
 * 
 * This class ensures that all API responses return HTTP 200 status code,
 * with the actual business status indicated in the response body through
 * the statusCode field. This allows API consumers to check the statusCode
 * for business logic decisions.
 * 
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2024-01-01
 * 
 * @param <T> the type of data payload
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * Response status code indicating business operation result.
     * Always starts from 600 as per business requirements.
     */
    @JsonProperty("statusCode")
    private String statusCode;

    /**
     * Human-readable message describing the response.
     */
    @JsonProperty("message")
    private String message;

    /**
     * Timestamp when the response was generated.
     */
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime timestamp;

    /**
     * Request identifier for tracking and debugging purposes.
     */
    @JsonProperty("requestId")
    private String requestId;

    /**
     * The actual data payload of the response.
     */
    @JsonProperty("data")
    private T data;

    /**
     * List of validation errors if any occurred.
     */
    @JsonProperty("errors")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<ValidationError> errors;

    /**
     * Additional metadata about the response.
     */
    @JsonProperty("metadata")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ResponseMetadata metadata;

    /**
     * Default constructor.
     */
    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructor with status code and message.
     * 
     * @param statusCode the response status code
     * @param message the response message
     */
    public ApiResponse(String statusCode, String message) {
        this();
        this.statusCode = statusCode;
        this.message = message;
    }

    /**
     * Constructor with ResponseCode enum.
     * 
     * @param responseCode the ResponseCode enum
     */
    public ApiResponse(ResponseCode responseCode) {
        this();
        this.statusCode = responseCode.getCode();
        this.message = responseCode.getDescription();
    }

    /**
     * Constructor with ResponseCode enum and custom message.
     * 
     * @param responseCode the ResponseCode enum
     * @param message the custom message
     */
    public ApiResponse(ResponseCode responseCode, String message) {
        this();
        this.statusCode = responseCode.getCode();
        this.message = message;
    }

    /**
     * Constructor with ResponseCode enum and data.
     * 
     * @param responseCode the ResponseCode enum
     * @param data the response data
     */
    public ApiResponse(ResponseCode responseCode, T data) {
        this(responseCode);
        this.data = data;
    }

    /**
     * Constructor with ResponseCode enum, message, and data.
     * 
     * @param responseCode the ResponseCode enum
     * @param message the response message
     * @param data the response data
     */
    public ApiResponse(ResponseCode responseCode, String message, T data) {
        this(responseCode, message);
        this.data = data;
    }

    /**
     * Creates a success response with data.
     * 
     * @param data the response data
     * @param <T> the type of data
     * @return ApiResponse with success status
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResponseCode.SUCCESS, data);
    }

    /**
     * Creates a success response with message.
     * 
     * @param message the success message
     * @param <T> the type of data
     * @return ApiResponse with success status
     */
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(ResponseCode.SUCCESS, message);
    }

    /**
     * Creates a success response with message and data.
     * 
     * @param message the success message
     * @param data the response data
     * @param <T> the type of data
     * @return ApiResponse with success status
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(ResponseCode.SUCCESS, message, data);
    }

    /**
     * Creates an error response with ResponseCode.
     * 
     * @param responseCode the error ResponseCode
     * @param <T> the type of data
     * @return ApiResponse with error status
     */
    public static <T> ApiResponse<T> error(ResponseCode responseCode) {
        return new ApiResponse<>(responseCode);
    }

    /**
     * Creates an error response with ResponseCode and custom message.
     * 
     * @param responseCode the error ResponseCode
     * @param message the custom error message
     * @param <T> the type of data
     * @return ApiResponse with error status
     */
    public static <T> ApiResponse<T> error(ResponseCode responseCode, String message) {
        return new ApiResponse<>(responseCode, message);
    }

    /**
     * Creates a validation error response with errors list.
     * 
     * @param errors the list of validation errors
     * @param <T> the type of data
     * @return ApiResponse with validation error status
     */
    public static <T> ApiResponse<T> validationError(List<ValidationError> errors) {
        ApiResponse<T> response = new ApiResponse<>(ResponseCode.VALIDATION_ERROR);
        response.setErrors(errors);
        return response;
    }

    // Getters and Setters
    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public void setErrors(List<ValidationError> errors) {
        this.errors = errors;
    }

    public ResponseMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ResponseMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Checks if the response indicates success.
     * 
     * @return true if statusCode is in the success range (600-609)
     */
    public boolean isSuccess() {
        return statusCode != null && statusCode.compareTo("600") >= 0 && statusCode.compareTo("609") <= 0;
    }

    /**
     * Checks if the response indicates an error.
     * 
     * @return true if statusCode is not in the success range
     */
    public boolean isError() {
        return !isSuccess();
    }

    @Override
    public String toString() {
        return "ApiResponse{" +
                "statusCode='" + statusCode + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                ", requestId='" + requestId + '\'' +
                ", data=" + data +
                ", errors=" + errors +
                ", metadata=" + metadata +
                '}';
    }
}
