package com.enterprise.msmq.controller;

import com.enterprise.msmq.dto.*;
import com.enterprise.msmq.enums.ResponseCode;
import com.enterprise.msmq.exception.MsmqException;
import com.enterprise.msmq.service.MsmqService;
import com.enterprise.msmq.util.RequestIdGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

/**
 * REST Controller for MSMQ operations.
 * 
 * This controller exposes all MSMQ operations as REST endpoints.
 * All responses return HTTP 200 status code with business status
 * indicated in the response body through statusCode field.
 * 
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/msmq")
@Validated
@CrossOrigin(origins = "*")
public class MsmqController {

    private static final Logger logger = LoggerFactory.getLogger(MsmqController.class);

    private final MsmqService msmqService;

    private final RequestIdGenerator requestIdGenerator;

    /**
     * Constructor for dependency injection.
     * 
     * @param msmqService the MSMQ service
     * @param requestIdGenerator the request ID generator
     */
    public MsmqController(MsmqService msmqService, RequestIdGenerator requestIdGenerator) {
        this.msmqService = msmqService;
        this.requestIdGenerator = requestIdGenerator;
    }

    // Queue Management Endpoints

    /**
     * Creates a new MSMQ queue.
     * 
     * @param queue the queue information
     * @return the created queue
     */
    @PostMapping("/queues")
    public ResponseEntity<ApiResponse<MsmqQueue>> createQueue(@Valid @RequestBody MsmqQueue queue) {
        long startTime = System.currentTimeMillis();
        String requestId = requestIdGenerator.generateRequestId();
        
        try {
            logger.info("Creating queue: {} with request ID: {}", queue.getName(), requestId);
            
            MsmqQueue createdQueue = msmqService.createQueue(queue);
            
            ResponseMetadata metadata = new ResponseMetadata(System.currentTimeMillis() - startTime);
            ApiResponse<MsmqQueue> response = ApiResponse.success("Queue created successfully", createdQueue);
            response.setRequestId(requestId);
            response.setMetadata(metadata);
            
            logger.info("Queue created successfully: {} with request ID: {}", queue.getName(), requestId);
            return ResponseEntity.ok(response);
            
        } catch (MsmqException e) {
            logger.error("Failed to create queue: {} with request ID: {}", queue.getName(), requestId, e);
            ApiResponse<MsmqQueue> response = ApiResponse.error(e.getResponseCode(), e.getMessage());
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Unexpected error creating queue: {} with request ID: {}", queue.getName(), requestId, e);
            ApiResponse<MsmqQueue> response = ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Internal server error");
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Deletes an MSMQ queue.
     * 
     * @param queueName the name of the queue to delete
     * @return success response
     */
    @DeleteMapping("/queues/{queueName}")
    public ResponseEntity<ApiResponse<Void>> deleteQueue(@PathVariable @NotBlank String queueName) {
        long startTime = System.currentTimeMillis();
        String requestId = requestIdGenerator.generateRequestId();
        
        try {
            logger.info("Deleting queue: {} with request ID: {}", queueName, requestId);
            
            msmqService.deleteQueue(queueName);
            
            ResponseMetadata metadata = new ResponseMetadata(System.currentTimeMillis() - startTime);
            ApiResponse<Void> response = ApiResponse.success("Queue deleted successfully");
            response.setRequestId(requestId);
            response.setMetadata(metadata);
            
            logger.info("Queue deleted successfully: {} with request ID: {}", queueName, requestId);
            return ResponseEntity.ok(response);
            
        } catch (MsmqException e) {
            logger.error("Failed to delete queue: {} with request ID: {}", queueName, requestId, e);
            ApiResponse<Void> response = ApiResponse.error(e.getResponseCode(), e.getMessage());
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Unexpected error deleting queue: {} with request ID: {}", queueName, requestId, e);
            ApiResponse<Void> response = ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Internal server error");
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Retrieves information about a specific queue.
     * 
     * @param queueName the name of the queue
     * @return the queue information
     */
    @GetMapping("/queues/{queueName}")
    public ResponseEntity<ApiResponse<MsmqQueue>> getQueue(@PathVariable @NotBlank String queueName) {
        long startTime = System.currentTimeMillis();
        String requestId = requestIdGenerator.generateRequestId();
        
        try {
            logger.debug("Retrieving queue: {} with request ID: {}", queueName, requestId);
            
            MsmqQueue queue = msmqService.getQueue(queueName);
            
            ResponseMetadata metadata = new ResponseMetadata(System.currentTimeMillis() - startTime);
            ApiResponse<MsmqQueue> response = ApiResponse.success("Queue retrieved successfully", queue);
            response.setRequestId(requestId);
            response.setMetadata(metadata);
            
            return ResponseEntity.ok(response);
            
        } catch (MsmqException e) {
            logger.error("Failed to retrieve queue: {} with request ID: {}", queueName, requestId, e);
            ApiResponse<MsmqQueue> response = ApiResponse.error(e.getResponseCode(), e.getMessage());
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Unexpected error retrieving queue: {} with request ID: {}", queueName, requestId, e);
            ApiResponse<MsmqQueue> response = ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Internal server error");
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Lists all available MSMQ queues.
     * 
     * @return list of all queues
     */
    @GetMapping("/queues")
    public ResponseEntity<ApiResponse<List<MsmqQueue>>> listQueues() {
        long startTime = System.currentTimeMillis();
        String requestId = requestIdGenerator.generateRequestId();
        
        try {
            logger.debug("Listing all queues with request ID: {}", requestId);
            
            List<MsmqQueue> queues = msmqService.listQueues();
            
            ResponseMetadata metadata = new ResponseMetadata(System.currentTimeMillis() - startTime);
            ApiResponse<List<MsmqQueue>> response = ApiResponse.success("Queues retrieved successfully", queues);
            response.setRequestId(requestId);
            response.setMetadata(metadata);
            
            return ResponseEntity.ok(response);
            
        } catch (MsmqException e) {
            logger.error("Failed to list queues with request ID: {}", requestId, e);
            ApiResponse<List<MsmqQueue>> response = ApiResponse.error(e.getResponseCode(), e.getMessage());
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Unexpected error listing queues with request ID: {}", requestId, e);
            ApiResponse<List<MsmqQueue>> response = ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Internal server error");
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        }
    }

    // Message Operations Endpoints

    /**
     * Sends a message to a specific queue.
     * 
     * @param queueName the destination queue name
     * @param message the message to send
     * @return the sent message
     */
    @PostMapping("/queues/{queueName}/messages")
    public ResponseEntity<ApiResponse<MsmqMessage>> sendMessage(
            @PathVariable @NotBlank String queueName,
            @Valid @RequestBody MsmqMessage message) {
        long startTime = System.currentTimeMillis();
        String requestId = requestIdGenerator.generateRequestId();
        
        try {
            logger.info("Sending message to queue: {} with request ID: {}", queueName, requestId);
            
            MsmqMessage sentMessage = msmqService.sendMessage(queueName, message);
            
            ResponseMetadata metadata = new ResponseMetadata(System.currentTimeMillis() - startTime);
            ApiResponse<MsmqMessage> response = ApiResponse.success("Message sent successfully", sentMessage);
            response.setRequestId(requestId);
            response.setMetadata(metadata);
            
            logger.info("Message sent successfully to queue: {} with request ID: {}", queueName, requestId);
            return ResponseEntity.ok(response);
            
        } catch (MsmqException e) {
            logger.error("Failed to send message to queue: {} with request ID: {}", queueName, requestId, e);
            ApiResponse<MsmqMessage> response = ApiResponse.error(e.getResponseCode(), e.getMessage());
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Unexpected error sending message to queue: {} with request ID: {}", queueName, requestId, e);
            ApiResponse<MsmqMessage> response = ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Internal server error");
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Receives a message from a specific queue.
     * 
     * @param queueName the source queue name
     * @param timeout the timeout in milliseconds (optional)
     * @return the received message or empty if no message available
     */
    @GetMapping("/queues/{queueName}/messages")
    public ResponseEntity<ApiResponse<MsmqMessage>> receiveMessage(
            @PathVariable @NotBlank String queueName,
            @RequestParam(required = false) Long timeout) {
        long startTime = System.currentTimeMillis();
        String requestId = requestIdGenerator.generateRequestId();
        
        try {
            logger.debug("Receiving message from queue: {} with request ID: {}", queueName, requestId);
            
            Optional<MsmqMessage> receivedMessage;
            if (timeout != null) {
                receivedMessage = msmqService.receiveMessage(queueName, timeout);
            } else {
                receivedMessage = msmqService.receiveMessage(queueName);
            }
            
            ResponseMetadata metadata = new ResponseMetadata(System.currentTimeMillis() - startTime);
            ApiResponse<MsmqMessage> response;
            
            if (receivedMessage.isPresent()) {
                response = ApiResponse.success("Message received successfully", receivedMessage.get());
            } else {
                response = ApiResponse.success("No message available");
            }
            
            response.setRequestId(requestId);
            response.setMetadata(metadata);
            
            return ResponseEntity.ok(response);
            
        } catch (MsmqException e) {
            logger.error("Failed to receive message from queue: {} with request ID: {}", queueName, requestId, e);
            ApiResponse<MsmqMessage> response = ApiResponse.error(e.getResponseCode(), e.getMessage());
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Unexpected error receiving message from queue: {} with request ID: {}", queueName, requestId, e);
            ApiResponse<MsmqMessage> response = ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Internal server error");
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Peeks at a message in the queue without removing it.
     * 
     * @param queueName the queue name
     * @param timeout the timeout in milliseconds (optional)
     * @return the peeked message or empty if no message available
     */
    @GetMapping("/queues/{queueName}/messages/peek")
    public ResponseEntity<ApiResponse<MsmqMessage>> peekMessage(
            @PathVariable @NotBlank String queueName,
            @RequestParam(required = false) Long timeout) {
        long startTime = System.currentTimeMillis();
        String requestId = requestIdGenerator.generateRequestId();
        
        try {
            logger.debug("Peeking message from queue: {} with request ID: {}", queueName, requestId);
            
            Optional<MsmqMessage> peekedMessage;
            if (timeout != null) {
                peekedMessage = msmqService.peekMessage(queueName, timeout);
            } else {
                peekedMessage = msmqService.peekMessage(queueName);
            }
            
            ResponseMetadata metadata = new ResponseMetadata(System.currentTimeMillis() - startTime);
            ApiResponse<MsmqMessage> response;
            
            if (peekedMessage.isPresent()) {
                response = ApiResponse.success("Message peeked successfully", peekedMessage.get());
            } else {
                response = ApiResponse.success("No message available");
            }
            
            response.setRequestId(requestId);
            response.setMetadata(metadata);
            
            return ResponseEntity.ok(response);
            
        } catch (MsmqException e) {
            logger.error("Failed to peek message from queue: {} with request ID: {}", queueName, requestId, e);
            ApiResponse<MsmqMessage> response = ApiResponse.error(e.getResponseCode(), e.getMessage());
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Unexpected error peeking message from queue: {} with request ID: {}", queueName, requestId, e);
            ApiResponse<MsmqMessage> response = ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Internal server error");
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        }
    }

    // Connection Management Endpoints

    /**
     * Gets the current connection status.
     * 
     * @return connection status information
     */
    @GetMapping("/connection/status")
    public ResponseEntity<ApiResponse<ConnectionStatus>> getConnectionStatus() {
        long startTime = System.currentTimeMillis();
        String requestId = requestIdGenerator.generateRequestId();
        
        try {
            logger.debug("Getting connection status with request ID: {}", requestId);
            
            ConnectionStatus status = msmqService.getConnectionStatus();
            
            ResponseMetadata metadata = new ResponseMetadata(System.currentTimeMillis() - startTime);
            ApiResponse<ConnectionStatus> response = ApiResponse.success("Connection status retrieved successfully", status);
            response.setRequestId(requestId);
            response.setMetadata(metadata);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Unexpected error getting connection status with request ID: {}", requestId, e);
            ApiResponse<ConnectionStatus> response = ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Internal server error");
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Establishes a connection to the MSMQ service.
     * 
     * @return success response
     */
    @PostMapping("/connection/connect")
    public ResponseEntity<ApiResponse<Void>> connect() {
        long startTime = System.currentTimeMillis();
        String requestId = requestIdGenerator.generateRequestId();
        
        try {
            logger.info("Establishing MSMQ connection with request ID: {}", requestId);
            
            msmqService.connect();
            
            ResponseMetadata metadata = new ResponseMetadata(System.currentTimeMillis() - startTime);
            ApiResponse<Void> response = ApiResponse.success("Connection established successfully");
            response.setRequestId(requestId);
            response.setMetadata(metadata);
            
            logger.info("MSMQ connection established successfully with request ID: {}", requestId);
            return ResponseEntity.ok(response);
            
        } catch (MsmqException e) {
            logger.error("Failed to establish MSMQ connection with request ID: {}", requestId, e);
            ApiResponse<Void> response = ApiResponse.error(e.getResponseCode(), e.getMessage());
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Unexpected error establishing MSMQ connection with request ID: {}", requestId, e);
            ApiResponse<Void> response = ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Internal server error");
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        }
    }

    // Health and Monitoring Endpoints

    /**
     * Performs a health check on the MSMQ service.
     * 
     * @return health check result
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<HealthCheckResult>> performHealthCheck() {
        long startTime = System.currentTimeMillis();
        String requestId = requestIdGenerator.generateRequestId();
        
        try {
            logger.debug("Performing health check with request ID: {}", requestId);
            
            HealthCheckResult healthCheck = msmqService.performHealthCheck();
            
            ResponseMetadata metadata = new ResponseMetadata(System.currentTimeMillis() - startTime);
            ApiResponse<HealthCheckResult> response = ApiResponse.success("Health check completed successfully", healthCheck);
            response.setRequestId(requestId);
            response.setMetadata(metadata);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Unexpected error performing health check with request ID: {}", requestId, e);
            ApiResponse<HealthCheckResult> response = ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Internal server error");
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Gets performance metrics for the MSMQ service.
     * 
     * @return performance metrics
     */
    @GetMapping("/metrics/performance")
    public ResponseEntity<ApiResponse<PerformanceMetrics>> getPerformanceMetrics() {
        long startTime = System.currentTimeMillis();
        String requestId = requestIdGenerator.generateRequestId();
        
        try {
            logger.debug("Getting performance metrics with request ID: {}", requestId);
            
            PerformanceMetrics metrics = msmqService.getPerformanceMetrics();
            
            ResponseMetadata metadata = new ResponseMetadata(System.currentTimeMillis() - startTime);
            ApiResponse<PerformanceMetrics> response = ApiResponse.success("Performance metrics retrieved successfully", metrics);
            response.setRequestId(requestId);
            response.setMetadata(metadata);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Unexpected error getting performance metrics with request ID: {}", requestId, e);
            ApiResponse<PerformanceMetrics> response = ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Internal server error");
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        }
    }
}
