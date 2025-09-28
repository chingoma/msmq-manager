package com.enterprise.msmq.controller;

import com.enterprise.msmq.dto.ApiResponse;
import com.enterprise.msmq.dto.request.PledgeInstructionRequest;
import com.enterprise.msmq.dto.response.PledgeInstructionResponse;
import com.enterprise.msmq.enums.ResponseCode;
import com.enterprise.msmq.service.PledgeInstructionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for pledge instruction operations.
 * Handles pledge balance (COLI) and pledge release (COLO) message generation.
 * 
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2025-09-26
 */
@RestController
@RequestMapping("/pledge-instructions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pledge Instructions", description = "Operations for pledge balance and release using ISO20022 messages")
public class PledgeInstructionController {

    private final PledgeInstructionService pledgeInstructionService;

    /**
     * Initiates a pledge balance operation.
     * Generates a pledge balance (COLI) message.
     * 
     * @param request the pledge instruction request
     * @return response with transaction ID and status information
     */
    @PostMapping("/balance")
    @Operation(
        summary = "Send Pledge Balance Instruction",
        description = "Creates a pledge balance instruction message (collateral in). " +
                    "BALANCE operation creates a COLI (collateral in) message. " +
                    "Messages are sent using ISO20022 sese.023.001.09 format."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "Pledge instruction processed",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "Invalid input provided",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500", 
            description = "Server error processing pledge instruction",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    public ResponseEntity<ApiResponse<PledgeInstructionResponse>> sendPledgeBalanceInstruction(
            @Valid @RequestBody PledgeInstructionRequest request,
            @Parameter(description = "Environment to use (local or remote)", example = "remote")
            @RequestParam(required = false, defaultValue = "remote") String environment) {
        
        try {
            log.info("Received pledge balance instruction request for security: {}", 
                     request.getSecurityIsin());
            
            // Force operation type to BALANCE
            request.setOperationType("BALANCE");
            
            // Process pledge instruction
            PledgeInstructionResponse response = pledgeInstructionService.sendPledgeInstruction(request, environment);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(
                    ApiResponse.success(
                        "Pledge balance instruction sent successfully",
                        response
                    )
                );
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                        ResponseCode.SYSTEM_ERROR,
                        "Failed to send pledge balance instruction"
                    ));
            }
            
        } catch (Exception e) {
            log.error("Error processing pledge balance instruction request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                    ResponseCode.SYSTEM_ERROR,
                    "Error processing pledge balance instruction: " + e.getMessage()
                ));
        }
    }
    
    /**
     * Initiates a pledge release operation.
     * Generates a pledge release (COLO) message.
     * 
     * @param request the pledge instruction request
     * @return response with transaction ID and status information
     */
    @PostMapping("/release")
    @Operation(
        summary = "Send Pledge Release Instruction",
        description = "Creates a pledge release instruction message (collateral out). " +
                    "RELEASE operation creates a COLO (collateral out) message. " +
                    "Messages are sent using ISO20022 sese.023.001.09 format."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "Pledge release instruction processed",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "Invalid input provided",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500", 
            description = "Server error processing pledge release instruction",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    public ResponseEntity<ApiResponse<PledgeInstructionResponse>> sendPledgeReleaseInstruction(
            @Valid @RequestBody PledgeInstructionRequest request,
            @Parameter(description = "Environment to use (local or remote)", example = "remote")
            @RequestParam(required = false, defaultValue = "remote") String environment) {
        
        try {
            log.info("Received pledge release instruction request for security: {}", 
                     request.getSecurityIsin());
            
            // Force operation type to RELEASE
            request.setOperationType("RELEASE");
            
            // Process pledge instruction
            PledgeInstructionResponse response = pledgeInstructionService.sendPledgeInstruction(request, environment);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(
                    ApiResponse.success(
                        "Pledge release instruction sent successfully",
                        response
                    )
                );
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                        ResponseCode.SYSTEM_ERROR,
                        "Failed to send pledge release instruction"
                    ));
            }
            
        } catch (Exception e) {
            log.error("Error processing pledge release instruction request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                    ResponseCode.SYSTEM_ERROR,
                    "Error processing pledge release instruction: " + e.getMessage()
                ));
        }
    }
}