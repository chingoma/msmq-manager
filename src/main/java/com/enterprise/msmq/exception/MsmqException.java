package com.enterprise.msmq.exception;

import com.enterprise.msmq.enums.ResponseCode;

/**
 * Custom exception for MSMQ-related errors.
 * Provides structured error reporting with response codes.
 *
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
public class MsmqException extends RuntimeException {

    private final ResponseCode responseCode;

    /**
     * Constructs a new MSMQ exception with the specified response code and message.
     *
     * @param responseCode the response code for this exception
     * @param message the detail message
     */
    public MsmqException(ResponseCode responseCode, String message) {
        super(message);
        this.responseCode = responseCode;
    }

    /**
     * Constructs a new MSMQ exception with the specified response code, message, and cause.
     *
     * @param responseCode the response code for this exception
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public MsmqException(ResponseCode responseCode, String message, Throwable cause) {
        super(message, cause);
        this.responseCode = responseCode;
    }

    /**
     * Gets the response code associated with this exception.
     *
     * @return the response code
     */
    public ResponseCode getResponseCode() {
        return responseCode;
    }

    /**
     * Gets the response code string associated with this exception.
     *
     * @return the response code string
     */
    public String getResponseCodeString() {
        return responseCode != null ? responseCode.getCode() : "699";
    }
}
