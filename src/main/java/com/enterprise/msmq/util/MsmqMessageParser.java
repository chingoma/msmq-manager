package com.enterprise.msmq.util;

import com.enterprise.msmq.dto.MsmqMessage;
import com.enterprise.msmq.exception.MsmqException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;
import com.enterprise.msmq.enums.ResponseCode;

/**
 * MSMQ Message Parser utility class.
 * 
 * This class handles parsing, validation, and transformation of
 * MSMQ messages between API and MSMQ formats.
 * 
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
public class MsmqMessageParser {

    private static final Logger logger = LoggerFactory.getLogger(MsmqMessageParser.class);

    /**
     * Parses and validates an outgoing message for MSMQ.
     * 
     * @param message the message to parse
     * @return the parsed and validated message
     * @throws MsmqException if message parsing fails
     */
    public MsmqMessage parseOutgoingMessage(MsmqMessage message) throws MsmqException {
        try {
            logger.debug("Parsing outgoing message: {}", message.getMessageId());
            
            // Validate message
            validateMessage(message);
            
            // Generate message ID if not provided
            if (message.getMessageId() == null) {
                message.setMessageId(UUID.randomUUID().toString());
            }
            
            // Set default values
            if (message.getCreatedTime() == null) {
                message.setCreatedTime(LocalDateTime.now());
            }
            
            if (message.getPriority() == null) {
                message.setPriority(3); // Default normal priority
            }
            
            if (message.getDeliveryCount() == null) {
                message.setDeliveryCount(0);
            }
            
            if (message.getMaxDeliveryCount() == null) {
                message.setMaxDeliveryCount(5);
            }
            
            if (message.getStatus() == null) {
                message.setStatus("PENDING");
            }
            
            // Validate priority range
            if (message.getPriority() < 0 || message.getPriority() > 7) {
                throw new MsmqException(ResponseCode.fromCode("612"), "Message priority must be between 0 and 7");
            }
            
            // Validate message size
            if (message.getBody() != null) {
                long messageSize = message.getBody().getBytes().length;
                message.setSize(messageSize);
                
                // Check if message size exceeds maximum (4MB default)
                if (messageSize > 4 * 1024 * 1024) {
                    throw new MsmqException(ResponseCode.fromCode("623"), "Message size exceeds maximum allowed size");
                }
            }
            
            logger.debug("Successfully parsed outgoing message: {}", message.getMessageId());
            return message;
            
        } catch (MsmqException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to parse outgoing message: {}", message.getMessageId(), e);
            throw new MsmqException(ResponseCode.fromCode("612"), "Failed to parse outgoing message", e);
        }
    }

    /**
     * Parses and validates an incoming message from MSMQ.
     * 
     * @param message the message to parse
     * @return the parsed and validated message
     * @throws MsmqException if message parsing fails
     */
    public MsmqMessage parseIncomingMessage(MsmqMessage message) throws MsmqException {
        try {
            logger.debug("Parsing incoming message: {}", message.getMessageId());
            
            // Validate message
            validateMessage(message);
            
            // Set received time if not provided
            if (message.getReceivedTime() == null) {
                message.setReceivedTime(LocalDateTime.now());
            }
            
            // Update status if not set
            if (message.getStatus() == null) {
                message.setStatus("RECEIVED");
            }
            
            // Validate message format
            if (message.getBody() == null || message.getBody().trim().isEmpty()) {
                logger.warn("Received message with empty body: {}", message.getMessageId());
            }
            
            logger.debug("Successfully parsed incoming message: {}", message.getMessageId());
            return message;
            
        } catch (MsmqException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to parse incoming message: {}", message.getMessageId(), e);
            throw new MsmqException(ResponseCode.fromCode("612"), "Failed to parse incoming message", e);
        }
    }

    /**
     * Validates a message for basic requirements.
     * 
     * @param message the message to validate
     * @throws MsmqException if validation fails
     */
    private void validateMessage(MsmqMessage message) throws MsmqException {
        if (message == null) {
            throw new MsmqException(ResponseCode.fromCode("612"), "Message cannot be null");
        }
        
        // Validate message body
        if (message.getBody() == null || message.getBody().trim().isEmpty()) {
            throw new MsmqException(ResponseCode.fromCode("612"), "Message content cannot be null or empty");
        }
        
        // Validate label if provided
        if (message.getLabel() != null && message.getLabel().trim().isEmpty()) {
            throw new MsmqException(ResponseCode.fromCode("612"), "Message label cannot be empty if provided");
        }
        
        // Validate correlation ID format if provided
        if (message.getCorrelationId() != null && message.getCorrelationId().trim().isEmpty()) {
            throw new MsmqException(ResponseCode.fromCode("612"), "Message correlation ID cannot be empty if provided");
        }
        
        // Validate message type if provided
        if (message.getMessageType() != null && message.getMessageType().trim().isEmpty()) {
            throw new MsmqException(ResponseCode.fromCode("612"), "Message type cannot be empty if provided");
        }
    }

    /**
     * Converts a message to MSMQ format.
     * 
     * @param message the message to convert
     * @return the converted message
     * @throws MsmqException if conversion fails
     */
    public MsmqMessage convertToMsmqFormat(MsmqMessage message) throws MsmqException {
        try {
            logger.debug("Converting message to MSMQ format: {}", message.getMessageId());
            
            // This method would contain the actual conversion logic to MSMQ format
            // For now, we just return the parsed message
            
            return parseOutgoingMessage(message);
            
        } catch (Exception e) {
            logger.error("Failed to convert message to MSMQ format: {}", message.getMessageId(), e);
            throw new MsmqException(ResponseCode.fromCode("612"), "Failed to convert message to MSMQ format", e);
        }
    }

    /**
     * Converts a message from MSMQ format.
     * 
     * @param message the message to convert
     * @return the converted message
     * @throws MsmqException if conversion fails
     */
    public MsmqMessage convertFromMsmqFormat(MsmqMessage message) throws MsmqException {
        try {
            logger.debug("Converting message from MSMQ format: {}", message.getMessageId());
            
            // This method would contain the actual conversion logic from MSMQ format
            // For now, we just return the parsed message
            
            return parseIncomingMessage(message);
            
        } catch (Exception e) {
            logger.error("Failed to convert message from MSMQ format: {}", message.getMessageId(), e);
            throw new MsmqException(ResponseCode.fromCode("612"), "Failed to convert message from MSMQ format", e);
        }
    }

    /**
     * Validates message properties.
     * 
     * @param message the message to validate
     * @throws MsmqException if validation fails
     */
    public void validateMessageProperties(MsmqMessage message) throws MsmqException {
        try {
            logger.debug("Validating message properties: {}", message.getMessageId());
            
            // Validate priority
            if (message.getPriority() != null && (message.getPriority() < 0 || message.getPriority() > 7)) {
                throw new MsmqException(ResponseCode.fromCode("612"), "Message priority must be between 0 and 7");
            }
            
            // Validate delivery count
            if (message.getDeliveryCount() != null && message.getDeliveryCount() < 0) {
                throw new MsmqException(ResponseCode.fromCode("612"), "Message delivery count cannot be negative");
            }
            
            // Validate max delivery count
            if (message.getMaxDeliveryCount() != null && message.getMaxDeliveryCount() <= 0) {
                throw new MsmqException(ResponseCode.fromCode("612"), "Message max delivery count must be positive");
            }
            
            // Validate expiration time if provided
            if (message.getExpirationTime() != null && message.getExpirationTime().isBefore(LocalDateTime.now())) {
                throw new MsmqException(ResponseCode.fromCode("612"), "Message expiration time cannot be in the past");
            }
            
            logger.debug("Successfully validated message properties: {}", message.getMessageId());
            
        } catch (MsmqException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to validate message properties: {}", message.getMessageId(), e);
            throw new MsmqException(ResponseCode.fromCode("612"), "Failed to validate message properties", e);
        }
    }
}
