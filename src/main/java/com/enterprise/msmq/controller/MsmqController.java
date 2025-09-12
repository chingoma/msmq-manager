package com.enterprise.msmq.controller;

import com.enterprise.msmq.dto.*;
import com.enterprise.msmq.dto.request.CreateQueueRequest;
import com.enterprise.msmq.dto.request.SendMessageRequest;
import com.enterprise.msmq.dto.response.QueueResponse;
import com.enterprise.msmq.dto.response.MessageResponse;
import com.enterprise.msmq.enums.QueueDirection;
import com.enterprise.msmq.enums.QueuePurpose;
import com.enterprise.msmq.enums.ResponseCode;
import com.enterprise.msmq.exception.MsmqException;
import com.enterprise.msmq.service.contracts.IMsmqService;
import com.enterprise.msmq.service.MsmqService;
import com.enterprise.msmq.util.RequestIdGenerator;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for MSMQ operations.
 * <p>
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
@Tag(name = "MSMQ Management API", description = "API for managing MSMQ operations")
public class MsmqController {

    private static final Logger logger = LoggerFactory.getLogger(MsmqController.class);

    private final IMsmqService IMsmqService;

    /**
     * Constructor for dependency injection.
     * 
     * @param IMsmqService the MSMQ service
     */
    public MsmqController(IMsmqService IMsmqService) {
        this.IMsmqService = IMsmqService;
    }

    // Queue Management Endpoints

    /**
     * Creates a new MSMQ queue.
     * 
     * @param request the queue creation request
     * @return the created queue response
     */
    @PostMapping("/queues")
    public ResponseEntity<ApiResponse<QueueResponse>> createQueue(@Valid @RequestBody CreateQueueRequest request) {
        long startTime = System.currentTimeMillis();
        String requestId = RequestIdGenerator.generateRequestId();
        
        try {
            logger.info("Creating queue: {} with request ID: {}", request.getName(), requestId);
            
            // TODO: Update service to accept CreateQueueRequest and return QueueResponse
            // For now, we'll need to map the request to the legacy format
            MsmqQueue queue = mapToLegacyQueue(request);
            MsmqQueue createdQueue = IMsmqService.createQueue(queue);
            
            // TODO: Map the legacy response to QueueResponse
            QueueResponse queueResponse = mapToQueueResponse(createdQueue);
            
            ResponseMetadata metadata = new ResponseMetadata(System.currentTimeMillis() - startTime);
            ApiResponse<QueueResponse> response = ApiResponse.success("Queue created successfully", queueResponse);
            response.setRequestId(requestId);
            response.setMetadata(metadata);
            
            logger.info("Queue created successfully: {} with request ID: {}", request.getName(), requestId);
            return ResponseEntity.ok(response);
            
        } catch (MsmqException e) {
            logger.error("Failed to create queue: {} with request ID: {}", request.getName(), requestId, e);
            ApiResponse<QueueResponse> response = ApiResponse.error(e.getResponseCode(), e.getMessage());
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Unexpected error creating queue: {} with request ID: {}", request.getName(), requestId, e);
            ApiResponse<QueueResponse> response = ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Internal server error");
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Maps CreateQueueRequest to legacy MsmqQueue format.
     * TODO: Remove this method once service layer is updated
     */
    private MsmqQueue mapToLegacyQueue(CreateQueueRequest request) {
        MsmqQueue queue = new MsmqQueue();
        queue.setName(request.getName());
        queue.setPath(request.getPath());
        queue.setType(request.getType());
        queue.setDescription(request.getDescription());
        queue.setMaxMessageCount(request.getMaxMessageCount());
        queue.setMaxSize(request.getMaxSize());
        queue.setTransactional(request.getTransactional());
        queue.setJournaled(request.getJournaled());
        queue.setAuthenticated(request.getAuthenticated());
        queue.setEncrypted(request.getEncrypted());
        queue.setOwner(request.getOwner());
        queue.setPermissions(request.getPermissions());
        return queue;
    }

    /**
     * Maps legacy MsmqQueue to QueueResponse format.
     * TODO: Remove this method once service layer is updated
     */
    private QueueResponse mapToQueueResponse(MsmqQueue queue) {
        return QueueResponse.builder()
                .id(null) // Will be set by service
                .name(queue.getName())
                .path(queue.getPath())
                .type(queue.getType())
                .status(queue.getStatus())
                .messageCount(queue.getMessageCount())
                .maxMessageCount(queue.getMaxMessageCount())
                .size(queue.getSize())
                .maxSize(queue.getMaxSize())
                .createdTime(queue.getCreatedTime())
                .modifiedTime(queue.getModifiedTime())
                .lastAccessTime(queue.getLastAccessTime())
                .description(queue.getDescription())
                .transactional(queue.getTransactional())
                .journaled(queue.getJournaled())
                .authenticated(queue.getAuthenticated())
                .encrypted(queue.getEncrypted())
                .owner(queue.getOwner())
                .permissions(queue.getPermissions())
                .errorMessage(queue.getErrorMessage())
                .queueDirection(null) // Will be set by service
                .queuePurpose(null) // Will be set by service
                .isActive(true) // Default for new queues
                .build();
    }

    /**
     * Maps SendMessageRequest to legacy MsmqMessage format.
     * TODO: Remove this method once service layer is updated
     */
    private MsmqMessage mapToLegacyMessage(SendMessageRequest request) {
        MsmqMessage message = new MsmqMessage();
        message.setBody(request.getBody());
        message.setLabel(request.getLabel());
        message.setPriority(request.getPriority());
        message.setCorrelationId(request.getCorrelationId());
        message.setMessageType(request.getMessageType());
        message.setDestinationQueue(request.getDestinationQueue());
        message.setSourceQueue(request.getSourceQueue());
        message.setProperties(request.getProperties());
        return message;
    }

    /**
     * Maps legacy MsmqMessage to MessageResponse format.
     * TODO: Remove this method once service layer is updated
     */
    private MessageResponse mapToMessageResponse(MsmqMessage message) {
        return MessageResponse.builder()
                .messageId(message.getMessageId())
                .body(message.getBody())
                .label(message.getLabel())
                .priority(message.getPriority())
                .correlationId(message.getCorrelationId())
                .messageType(message.getMessageType())
                .createdTime(message.getCreatedTime())
                .sentTime(message.getSentTime())
                .receivedTime(message.getReceivedTime())
                .sourceQueue(message.getSourceQueue())
                .destinationQueue(message.getDestinationQueue())
                .size(message.getSize())
                .deliveryCount(message.getDeliveryCount())
                .state(message.getStatus()) // Using status instead of state
                .transactional(false) // Default value since not available in legacy
                .requiresAck(false) // Default value since not available in legacy
                .properties(message.getProperties())
                .errorMessage(message.getErrorMessage())
                .result(message.getStatus()) // Using status as result
                .processingTimeMs(null) // Not available in legacy
                .templateName(null) // Not available in legacy
                .templateParameters(null) // Not available in legacy
                .build();
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
        String requestId = RequestIdGenerator.generateRequestId();
        
        try {
            logger.info("Deleting queue: {} with request ID: {}", queueName, requestId);
            
            IMsmqService.deleteQueue(queueName);
            
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
    public ResponseEntity<ApiResponse<QueueResponse>> getQueue(@PathVariable @NotBlank String queueName) {
        long startTime = System.currentTimeMillis();
        String requestId = RequestIdGenerator.generateRequestId();
        
        try {
            logger.debug("Retrieving queue: {} with request ID: {}", queueName, requestId);
            
            MsmqQueue queue = IMsmqService.getQueue(queueName);
            
            // TODO: Map the legacy response to QueueResponse
            QueueResponse queueResponse = mapToQueueResponse(queue);
            
            ResponseMetadata metadata = new ResponseMetadata(System.currentTimeMillis() - startTime);
            ApiResponse<QueueResponse> response = ApiResponse.success("Queue retrieved successfully", queueResponse);
            response.setRequestId(requestId);
            response.setMetadata(metadata);
            
            return ResponseEntity.ok(response);
            
        } catch (MsmqException e) {
            logger.error("Failed to retrieve queue: {} with request ID: {}", queueName, requestId, e);
            ApiResponse<QueueResponse> response = ApiResponse.error(e.getResponseCode(), e.getMessage());
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Unexpected error retrieving queue: {} with request ID: {}", queueName, requestId, e);
            ApiResponse<QueueResponse> response = ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Internal server error");
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
    public ResponseEntity<ApiResponse<List<QueueResponse>>> listQueues() {
        long startTime = System.currentTimeMillis();
        String requestId = RequestIdGenerator.generateRequestId();
        
        try {
            logger.debug("Listing all queues with request ID: {}", requestId);
            
            List<MsmqQueue> queues = IMsmqService.listQueues();
            
            // TODO: Map the legacy responses to QueueResponse
            List<QueueResponse> queueResponses = queues.stream()
                    .map(this::mapToQueueResponse)
                    .toList();
            
            ResponseMetadata metadata = new ResponseMetadata(System.currentTimeMillis() - startTime);
            ApiResponse<List<QueueResponse>> response = ApiResponse.success("Queues retrieved successfully", queueResponses);
            response.setRequestId(requestId);
            response.setMetadata(metadata);
            
            return ResponseEntity.ok(response);
            
        } catch (MsmqException e) {
            logger.error("Failed to list queues with request ID: {}", requestId, e);
            ApiResponse<List<QueueResponse>> response = ApiResponse.error(e.getResponseCode(), e.getMessage());
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Unexpected error listing queues with request ID: {}", requestId, e);
            ApiResponse<List<QueueResponse>> response = ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Internal server error");
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        }
    }

    // Message Operations Endpoints

    /**
     * Sends a message to a specific queue.
     * 
     * @param queueName the destination queue name
     * @param request the message request
     * @return the sent message response
     */
    @PostMapping("/queues/{queueName}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @PathVariable @NotBlank String queueName,
            @Valid @RequestBody SendMessageRequest request) {
        long startTime = System.currentTimeMillis();
        String requestId = RequestIdGenerator.generateRequestId();
        
        try {
            logger.info("Sending message to queue: {} with request ID: {}", queueName, requestId);
            
            // TODO: Update service to accept SendMessageRequest and return MessageResponse
            // For now, we'll need to map the request to the legacy format
            MsmqMessage message = mapToLegacyMessage(request);
            MsmqMessage sentMessage = IMsmqService.sendMessage(queueName, message);
            
            // TODO: Map the legacy response to MessageResponse
            MessageResponse messageResponse = mapToMessageResponse(sentMessage);
            
            ResponseMetadata metadata = new ResponseMetadata(System.currentTimeMillis() - startTime);
            ApiResponse<MessageResponse> response = ApiResponse.success("Message sent successfully", messageResponse);
            response.setRequestId(requestId);
            response.setMetadata(metadata);
            
            logger.info("Message sent successfully to queue: {} with request ID: {}", queueName, requestId);
            return ResponseEntity.ok(response);
            
        } catch (MsmqException e) {
            logger.error("Failed to send message to queue: {} with request ID: {}", queueName, requestId, e);
            ApiResponse<MessageResponse> response = ApiResponse.error(e.getResponseCode(), e.getMessage());
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Unexpected error sending message to queue: {} with request ID: {}", queueName, requestId, e);
            ApiResponse<MessageResponse> response = ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Internal server error");
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
        String requestId = RequestIdGenerator.generateRequestId();
        
        try {
            logger.debug("Receiving message from queue: {} with request ID: {}", queueName, requestId);
            
            Optional<MsmqMessage> receivedMessage;
            if (timeout != null) {
                receivedMessage = IMsmqService.receiveMessage(queueName, timeout);
            } else {
                receivedMessage = IMsmqService.receiveMessage(queueName);
            }
            
            ResponseMetadata metadata = new ResponseMetadata(System.currentTimeMillis() - startTime);
            ApiResponse<MsmqMessage> response;

            response = receivedMessage.map(msmqMessage -> ApiResponse.success("Message received successfully", msmqMessage)).orElseGet(() -> ApiResponse.success("No message available"));
            
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
        String requestId = RequestIdGenerator.generateRequestId();
        
        try {
            logger.debug("Peeking message from queue: {} with request ID: {}", queueName, requestId);
            
            Optional<MsmqMessage> peekedMessage;
            if (timeout != null) {
                peekedMessage = IMsmqService.peekMessage(queueName, timeout);
            } else {
                peekedMessage = IMsmqService.peekMessage(queueName);
            }
            
            ResponseMetadata metadata = new ResponseMetadata(System.currentTimeMillis() - startTime);
            ApiResponse<MsmqMessage> response;

            response = peekedMessage.map(msmqMessage -> ApiResponse.success("Message peeked successfully", msmqMessage)).orElseGet(() -> ApiResponse.success("No message available"));
            
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
        String requestId = RequestIdGenerator.generateRequestId();
        
        try {
            logger.debug("Getting connection status with request ID: {}", requestId);
            
            ConnectionStatus status = IMsmqService.getConnectionStatus();
            
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
        String requestId = RequestIdGenerator.generateRequestId();
        
        try {
            logger.info("Establishing MSMQ connection with request ID: {}", requestId);
            
            IMsmqService.connect();
            
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
        String requestId = RequestIdGenerator.generateRequestId();
        
        try {
            logger.debug("Performing health check with request ID: {}", requestId);
            
            HealthCheckResult healthCheck = IMsmqService.performHealthCheck();
            
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
        String requestId = RequestIdGenerator.generateRequestId();
        
        try {
            logger.debug("Getting performance metrics with request ID: {}", requestId);
            
            PerformanceMetrics metrics = IMsmqService.getPerformanceMetrics();
            
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
    
    // =====================================================
    // NEW: Queue Direction and Purpose Endpoints
    // =====================================================
    
    /**
     * Gets all queue names by direction.
     * 
     * @param direction the queue direction (INCOMING_ONLY, OUTGOING_ONLY, BIDIRECTIONAL)
     * @return list of queue names with the specified direction
     */
    @GetMapping("/queues/direction/{direction}")
    public ResponseEntity<ApiResponse<List<String>>> getQueuesByDirection(
            @PathVariable @NotBlank String direction) {
        long startTime = System.currentTimeMillis();
        String requestId = RequestIdGenerator.generateRequestId();
        
        try {
            logger.info("Getting queues by direction: {} with request ID: {}", direction, requestId);
            
            // Parse the direction enum
            QueueDirection queueDirection;
            try {
                queueDirection = QueueDirection.valueOf(direction.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid direction parameter: {} with request ID: {}", direction, requestId);
                ApiResponse<List<String>> response = ApiResponse.error(
                    ResponseCode.VALIDATION_ERROR, 
                    "Invalid direction. Must be one of: INCOMING_ONLY, OUTGOING_ONLY, BIDIRECTIONAL");
                response.setRequestId(requestId);
                return ResponseEntity.ok(response);
            }
            
            // Cast to concrete service to access direction methods
            if (IMsmqService instanceof MsmqService) {
                MsmqService msmqService = (MsmqService) IMsmqService;
                List<String> queueNames = msmqService.getQueueNamesByDirection(queueDirection);
                
                ResponseMetadata metadata = new ResponseMetadata(System.currentTimeMillis() - startTime);
                ApiResponse<List<String>> response = ApiResponse.success(
                    "Queues retrieved successfully by direction: " + direction, queueNames);
                response.setRequestId(requestId);
                response.setMetadata(metadata);
                
                logger.info("Retrieved {} queues with direction {} with request ID: {}", 
                           queueNames.size(), direction, requestId);
                return ResponseEntity.ok(response);
            } else {
                throw new IllegalStateException("Service implementation does not support direction operations");
            }
            
        } catch (MsmqException e) {
            logger.error("Failed to get queues by direction: {} with request ID: {}", direction, requestId, e);
            ApiResponse<List<String>> response = ApiResponse.error(e.getResponseCode(), e.getMessage());
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Unexpected error getting queues by direction: {} with request ID: {}", direction, requestId, e);
            ApiResponse<List<String>> response = ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Internal server error");
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * Gets all queue names by purpose.
     * 
     * @param purpose the queue purpose (GENERAL, SWIFT_MESSAGES, SYSTEM_NOTIFICATIONS, etc.)
     * @return list of queue names with the specified purpose
     */
    @GetMapping("/queues/purpose/{purpose}")
    public ResponseEntity<ApiResponse<List<String>>> getQueuesByPurpose(
            @PathVariable @NotBlank String purpose) {
        long startTime = System.currentTimeMillis();
        String requestId = RequestIdGenerator.generateRequestId();
        
        try {
            logger.info("Getting queues by purpose: {} with request ID: {}", purpose, requestId);
            
            // Parse the purpose enum
            QueuePurpose queuePurpose;
            try {
                queuePurpose = QueuePurpose.valueOf(purpose.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid purpose parameter: {} with request ID: {}", purpose, requestId);
                ApiResponse<List<String>> response = ApiResponse.error(
                    ResponseCode.VALIDATION_ERROR, 
                    "Invalid purpose. Must be one of: GENERAL, SWIFT_MESSAGES, SYSTEM_NOTIFICATIONS, ERROR_HANDLING, AUDIT_LOGS, DATA_SYNC, BATCH_PROCESSING, URGENT_MESSAGES");
                response.setRequestId(requestId);
                return ResponseEntity.ok(response);
            }
            
            // Cast to concrete service to access purpose methods
            if (IMsmqService instanceof MsmqService) {
                MsmqService msmqService = (MsmqService) IMsmqService;
                List<String> queueNames = msmqService.getQueueNamesByPurpose(queuePurpose);
                
                ResponseMetadata metadata = new ResponseMetadata(System.currentTimeMillis() - startTime);
                ApiResponse<List<String>> response = ApiResponse.success(
                    "Queues retrieved successfully by purpose: " + purpose, queueNames);
                response.setRequestId(requestId);
                response.setMetadata(metadata);
                
                logger.info("Retrieved {} queues with purpose {} with request ID: {}", 
                           queueNames.size(), purpose, requestId);
                return ResponseEntity.ok(response);
            } else {
                throw new IllegalStateException("Service implementation does not support purpose operations");
            }
            
        } catch (MsmqException e) {
            logger.error("Failed to get queues by purpose: {} with request ID: {}", purpose, requestId, e);
            ApiResponse<List<String>> response = ApiResponse.error(e.getResponseCode(), e.getMessage());
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Unexpected error getting queues by purpose: {} with request ID: {}", purpose, requestId, e);
            ApiResponse<List<String>> response = ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Internal server error");
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * Gets comprehensive queue information including direction and purpose.
     * 
     * @param queueName the name of the queue
     * @return comprehensive queue information
     */
    @GetMapping("/queues/{queueName}/info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getQueueInfo(
            @PathVariable @NotBlank String queueName) {
        long startTime = System.currentTimeMillis();
        String requestId = RequestIdGenerator.generateRequestId();
        
        try {
            logger.info("Getting comprehensive info for queue: {} with request ID: {}", queueName, requestId);
            
            // Cast to concrete service to access info method
            if (IMsmqService instanceof MsmqService) {
                MsmqService msmqService = (MsmqService) IMsmqService;
                Map<String, Object> queueInfo = msmqService.getQueueInfo(queueName);
                
                ResponseMetadata metadata = new ResponseMetadata(System.currentTimeMillis() - startTime);
                ApiResponse<Map<String, Object>> response = ApiResponse.success(
                    "Queue information retrieved successfully", queueInfo);
                response.setRequestId(requestId);
                response.setMetadata(metadata);
                
                logger.info("Retrieved comprehensive info for queue: {} with request ID: {}", queueName, requestId);
                return ResponseEntity.ok(response);
            } else {
                throw new IllegalStateException("Service implementation does not support info operations");
            }
            
        } catch (MsmqException e) {
            logger.error("Failed to get queue info: {} with request ID: {}", queueName, requestId, e);
            ApiResponse<Map<String, Object>> response = ApiResponse.error(e.getResponseCode(), e.getMessage());
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Unexpected error getting queue info: {} with request ID: {}", queueName, requestId, e);
            ApiResponse<Map<String, Object>> response = ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Internal server error");
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * Gets available queue directions for reference.
     * 
     * @return list of available queue directions
     */
    @GetMapping("/queues/directions")
    public ResponseEntity<ApiResponse<List<String>>> getAvailableDirections() {
        long startTime = System.currentTimeMillis();
        String requestId = RequestIdGenerator.generateRequestId();
        
        try {
            logger.debug("Getting available queue directions with request ID: {}", requestId);
            
            List<String> directions = List.of(
                "INCOMING_ONLY",
                "OUTGOING_ONLY", 
                "BIDIRECTIONAL"
            );
            
            ResponseMetadata metadata = new ResponseMetadata(System.currentTimeMillis() - startTime);
            ApiResponse<List<String>> response = ApiResponse.success(
                "Available queue directions retrieved successfully", directions);
            response.setRequestId(requestId);
            response.setMetadata(metadata);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Unexpected error getting available directions with request ID: {}", requestId, e);
            ApiResponse<List<String>> response = ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Internal server error");
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * Gets available queue purposes for reference.
     * 
     * @return list of available queue purposes
     */
    @GetMapping("/queues/purposes")
    public ResponseEntity<ApiResponse<List<String>>> getAvailablePurposes() {
        long startTime = System.currentTimeMillis();
        String requestId = RequestIdGenerator.generateRequestId();
        
        try {
            logger.debug("Getting available queue purposes with request ID: {}", requestId);
            
            List<String> purposes = List.of(
                "GENERAL",
                "SWIFT_MESSAGES",
                "SYSTEM_NOTIFICATIONS",
                "ERROR_HANDLING",
                "AUDIT_LOGS",
                "DATA_SYNC",
                "BATCH_PROCESSING",
                "URGENT_MESSAGES"
            );
            
            ResponseMetadata metadata = new ResponseMetadata(System.currentTimeMillis() - startTime);
            ApiResponse<List<String>> response = ApiResponse.success(
                "Available queue purposes retrieved successfully", purposes);
            response.setRequestId(requestId);
            response.setMetadata(metadata);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Unexpected error getting available purposes with request ID: {}", requestId, e);
            ApiResponse<List<String>> response = ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Internal server error");
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        }
    }
}
