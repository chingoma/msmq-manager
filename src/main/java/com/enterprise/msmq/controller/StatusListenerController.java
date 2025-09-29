package com.enterprise.msmq.controller;

import com.enterprise.msmq.dto.ApiResponse;
import com.enterprise.msmq.enums.ResponseCode;
import com.enterprise.msmq.service.MsmqStatusListenerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for managing MSMQ status listeners.
 * Provides endpoints to start, stop, and monitor status queue listeners.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@RestController
@RequestMapping("/v1/msmq/listeners")
@RequiredArgsConstructor
@Slf4j
public class StatusListenerController {
    
    private final MsmqStatusListenerService statusListenerService;
    
    /**
     * Get the status of all listeners.
     * 
     * @return map of queue names to listener status
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getListenerStatus() {
        try {
            Map<String, Boolean> status = statusListenerService.getListenerStatus();
            return ResponseEntity.ok(ApiResponse.success("Listener status retrieved successfully", status));
        } catch (Exception e) {
            log.error("Error getting listener status", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to get listener status: " + e.getMessage()));
        }
    }
    
    /**
     * Get retry counters for all listeners.
     * 
     * @return map of queue names to retry counts
     */
    @GetMapping("/retry-counters")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getRetryCounters() {
        try {
            Map<String, Integer> counters = statusListenerService.getRetryCounters();
            return ResponseEntity.ok(ApiResponse.success("Retry counters retrieved successfully", counters));
        } catch (Exception e) {
            log.error("Error getting retry counters", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to get retry counters: " + e.getMessage()));
        }
    }
    
    /**
     * Start all listeners.
     * 
     * @return success response
     */
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<String>> startAllListeners() {
        try {
            statusListenerService.startAllListeners();
            return ResponseEntity.ok(ApiResponse.success("All listeners started successfully"));
        } catch (Exception e) {
            log.error("Error starting all listeners", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to start all listeners: " + e.getMessage()));
        }
    }
    
    /**
     * Stop all listeners.
     * 
     * @return success response
     */
    @PostMapping("/stop")
    public ResponseEntity<ApiResponse<String>> stopAllListeners() {
        try {
            statusListenerService.stopAllListeners();
            return ResponseEntity.ok(ApiResponse.success("All listeners stopped successfully"));
        } catch (Exception e) {
            log.error("Error stopping all listeners", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to stop all listeners: " + e.getMessage()));
        }
    }
    
    /**
     * Start a specific listener by queue name.
     * 
     * @param queueName the queue name to start listening to
     * @return success response
     */
    @PostMapping("/start/{queueName}")
    public ResponseEntity<ApiResponse<String>> startListener(@PathVariable String queueName) {
        try {
            statusListenerService.startListener(queueName);
            return ResponseEntity.ok(ApiResponse.success("Listener for queue " + queueName + " started successfully"));
        } catch (Exception e) {
            log.error("Error starting listener for queue: {}", queueName, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to start listener for queue " + queueName + ": " + e.getMessage()));
        }
    }
    
    /**
     * Stop a specific listener by queue name.
     * 
     * @param queueName the queue name to stop listening to
     * @return success response
     */
    @PostMapping("/stop/{queueName}")
    public ResponseEntity<ApiResponse<String>> stopListener(@PathVariable String queueName) {
        try {
            statusListenerService.stopListener(queueName);
            return ResponseEntity.ok(ApiResponse.success("Listener for queue " + queueName + " stopped successfully"));
        } catch (Exception e) {
            log.error("Error stopping listener for queue: {}", queueName, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to stop listener for queue " + queueName + ": " + e.getMessage()));
        }
    }
    
    /**
     * Restart a specific listener by queue name.
     * 
     * @param queueName the queue name to restart listening to
     * @return success response
     */
    @PostMapping("/restart/{queueName}")
    public ResponseEntity<ApiResponse<String>> restartListener(@PathVariable String queueName) {
        try {
            statusListenerService.restartListener(queueName);
            return ResponseEntity.ok(ApiResponse.success("Listener for queue " + queueName + " restarted successfully"));
        } catch (Exception e) {
            log.error("Error restarting listener for queue: {}", queueName, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to restart listener for queue " + queueName + ": " + e.getMessage()));
        }
    }
    
    /**
     * Check if a specific listener is running.
     * 
     * @param queueName the queue name to check
     * @return boolean indicating if listener is running
     */
    @GetMapping("/status/{queueName}")
    public ResponseEntity<ApiResponse<Boolean>> isListenerRunning(@PathVariable String queueName) {
        try {
            boolean isRunning = statusListenerService.isListenerRunning(queueName);
            return ResponseEntity.ok(ApiResponse.success("Listener for queue " + queueName + " is " + (isRunning ? "running" : "stopped"), isRunning));
        } catch (Exception e) {
            log.error("Error checking listener status for queue: {}", queueName, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to check listener status for queue " + queueName + ": " + e.getMessage()));
        }
    }
}
