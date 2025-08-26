package com.enterprise.msmq.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Validation error details for API responses.
 * 
 * This class represents individual validation errors that occur during
 * request processing, providing detailed information about what went wrong.
 * 
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationError {

    /**
     * The field that failed validation.
     */
    @JsonProperty("field")
    private String field;

    /**
     * The value that caused the validation failure.
     */
    @JsonProperty("value")
    private Object value;

    /**
     * The validation error message.
     */
    @JsonProperty("message")
    private String message;

    /**
     * The validation error code.
     */
    @JsonProperty("code")
    private String code;

    /**
     * Default constructor.
     */
    public ValidationError() {
    }

    /**
     * Constructor with field and message.
     * 
     * @param field the field that failed validation
     * @param message the validation error message
     */
    public ValidationError(String field, String message) {
        this.field = field;
        this.message = message;
    }

    /**
     * Constructor with all fields.
     * 
     * @param field the field that failed validation
     * @param value the value that caused the validation failure
     * @param message the validation error message
     * @param code the validation error code
     */
    public ValidationError(String field, Object value, String message, String code) {
        this.field = field;
        this.value = value;
        this.message = message;
        this.code = code;
    }

    // Getters and Setters
    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "ValidationError{" +
                "field='" + field + '\'' +
                ", value=" + value +
                ", message='" + message + '\'' +
                ", code='" + code + '\'' +
                '}';
    }
}
