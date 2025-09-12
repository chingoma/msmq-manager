package com.enterprise.msmq.controller;

import com.enterprise.msmq.dto.ApiResponse;
import com.enterprise.msmq.dto.QueueAlert;
import com.enterprise.msmq.enums.AlertSeverity;
import com.enterprise.msmq.enums.ResponseCode;
import com.enterprise.msmq.service.QueueMonitoringService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for MSMQ queue monitoring and alert management.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@RestController
@RequestMapping("/queue-monitoring")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Queue Monitoring", description = "Endpoints for monitoring MSMQ queue health and managing alerts")
public class QueueMonitoringController {

    private final QueueMonitoringService monitoringService;

    /**
     * Get current monitoring statistics and system health.
     * 
     * @return monitoring statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMonitoringStats() {
        try {
            Map<String, Object> stats = monitoringService.getMonitoringStats();
            
            return ResponseEntity.ok(ApiResponse.success("Monitoring statistics retrieved successfully", stats));
                
        } catch (Exception e) {
            log.error("Error retrieving monitoring statistics", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to retrieve monitoring statistics: " + e.getMessage()));
        }
    }

    /**
     * Get recent alerts.
     * 
     * @param limit maximum number of alerts to return (default: 20)
     * @return list of recent alerts
     */
    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<List<QueueAlert>>> getRecentAlerts(
            @RequestParam(defaultValue = "20") int limit) {
        try {
            List<QueueAlert> alerts = monitoringService.getRecentAlerts(limit);
            
            return ResponseEntity.ok(ApiResponse.success("Recent alerts retrieved successfully", alerts));
                
        } catch (Exception e) {
            log.error("Error retrieving recent alerts", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to retrieve recent alerts: " + e.getMessage()));
        }
    }

    /**
     * Get alerts by severity level.
     * 
     * @param severity the severity level (INFO, WARNING, ERROR)
     * @return list of alerts with the specified severity
     */
    @GetMapping("/alerts/{severity}")
    public ResponseEntity<ApiResponse<List<QueueAlert>>> getAlertsBySeverity(
            @PathVariable String severity) {
        try {
            AlertSeverity alertSeverity;
            try {
                alertSeverity = AlertSeverity.valueOf(severity.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ResponseCode.VALIDATION_ERROR, "Invalid severity level: " + severity));
            }
            
            List<QueueAlert> alerts = monitoringService.getAlertsBySeverity(alertSeverity);
            
            return ResponseEntity.ok(ApiResponse.success(
                "Alerts with severity " + severity + " retrieved successfully", alerts));
                
        } catch (Exception e) {
            log.error("Error retrieving alerts by severity: {}", severity, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to retrieve alerts by severity: " + e.getMessage()));
        }
    }

    /**
     * Get system health overview.
     * 
     * @return system health information
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemHealth() {
        try {
            Map<String, Object> stats = monitoringService.getMonitoringStats();
            
            // Extract health-specific information
            Map<String, Object> health = Map.of(
                "status", determineOverallStatus(stats),
                "healthyQueues", stats.get("healthyQueues"),
                "unhealthyQueues", stats.get("unhealthyQueues"),
                "totalAlerts", stats.get("totalAlerts"),
                "errorAlerts", stats.get("errorAlerts"),
                "warningAlerts", stats.get("warningAlerts"),
                "lastUpdate", stats.get("lastUpdate"),
                "alertsEnabled", stats.get("alertsEnabled")
            );
            
            return ResponseEntity.ok(ApiResponse.success("System health retrieved successfully", health));
                
        } catch (Exception e) {
            log.error("Error retrieving system health", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to retrieve system health: " + e.getMessage()));
        }
    }

    /**
     * Get alert summary for dashboard.
     * 
     * @return alert summary information
     */
    @GetMapping("/alerts/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAlertSummary() {
        try {
            Map<String, Object> stats = monitoringService.getMonitoringStats();
            
            // Create alert summary
            Map<String, Object> summary = Map.of(
                "totalAlerts", stats.get("totalAlerts"),
                "errorAlerts", stats.get("errorAlerts"),
                "warningAlerts", stats.get("warningAlerts"),
                "infoAlerts", stats.get("infoAlerts"),
                "alertsEnabled", stats.get("alertsEnabled"),
                "lastUpdate", stats.get("lastUpdate")
            );
            
            return ResponseEntity.ok(ApiResponse.success("Alert summary retrieved successfully", summary));
                
        } catch (Exception e) {
            log.error("Error retrieving alert summary", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to retrieve alert summary: " + e.getMessage()));
        }
    }

    /**
     * Clear old alerts to free up memory.
     * 
     * @return success message
     */
    @PostMapping("/alerts/cleanup")
    public ResponseEntity<ApiResponse<String>> cleanupOldAlerts() {
        try {
            monitoringService.cleanupOldAlerts();
            
            return ResponseEntity.ok(ApiResponse.success("Old alerts cleaned up successfully", "Cleanup completed"));
                
        } catch (Exception e) {
            log.error("Error cleaning up old alerts", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to cleanup old alerts: " + e.getMessage()));
        }
    }

    /**
     * Determine overall system status based on monitoring data.
     * 
     * @param stats monitoring statistics
     * @return overall status (HEALTHY, WARNING, CRITICAL)
     */
    private String determineOverallStatus(Map<String, Object> stats) {
        long errorAlerts = (Long) stats.get("errorAlerts");
        long warningAlerts = (Long) stats.get("warningAlerts");
        long unhealthyQueues = (Long) stats.get("unhealthyQueues");
        
        if (errorAlerts > 0 || unhealthyQueues > 0) {
            return "CRITICAL";
        } else if (warningAlerts > 0) {
            return "WARNING";
        } else {
            return "HEALTHY";
        }
    }
}
