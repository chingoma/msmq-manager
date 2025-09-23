package com.enterprise.msmq.controller;

import com.enterprise.msmq.service.DefaultConfigurationInitializerService;
import com.enterprise.msmq.dto.ApiResponse;
import com.enterprise.msmq.enums.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for managing startup configuration and default settings.
 * Provides endpoints for checking initialization status and manually triggering configuration setup.
 *
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/v1/startup-config")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Startup Configuration", description = "Manage startup configuration and default settings")
public class StartupConfigurationController {

    private final DefaultConfigurationInitializerService defaultConfigInitializerService;

    /**
     * Get the current initialization status of default configurations.
     *
     * @return Status information about default configurations
     */
    @GetMapping("/status")
    @Operation(summary = "Get initialization status",
               description = "Returns the current status of default email configuration and mailing list initialization")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "600", description = "Status retrieved successfully")
    })
    public ResponseEntity<ApiResponse<DefaultConfigurationInitializerService.InitializationStatus>> getInitializationStatus() {
        try {
            DefaultConfigurationInitializerService.InitializationStatus status =
                defaultConfigInitializerService.getInitializationStatus();

            return ResponseEntity.ok(ApiResponse.success("Initialization status retrieved successfully", status));

        } catch (Exception e) {
            log.error("Error retrieving initialization status", e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to retrieve initialization status: " + e.getMessage()));
        }
    }

    /**
     * Manually trigger the initialization of default configurations.
     * This can be used to re-initialize or update configurations after startup.
     *
     * @return Result of the initialization process
     */
    @PostMapping("/initialize")
    @Operation(summary = "Initialize default configurations",
               description = "Manually trigger initialization of default email configuration and mailing lists")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "600", description = "Initialization completed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "603", description = "Initialization failed")
    })
    public ResponseEntity<ApiResponse<String>> initializeDefaultConfigurations() {
        try {
            log.info("Manual initialization of default configurations requested");

            defaultConfigInitializerService.initializeDefaultConfigurations();

            return ResponseEntity.ok(ApiResponse.success("Default configurations initialized successfully"));

        } catch (Exception e) {
            log.error("Error during manual initialization of default configurations", e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR,
                    "Failed to initialize default configurations: " + e.getMessage()));
        }
    }
}
