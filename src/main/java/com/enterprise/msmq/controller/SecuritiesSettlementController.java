package com.enterprise.msmq.controller;

import com.enterprise.msmq.dto.request.SecuritiesSettlementRequest;
import com.enterprise.msmq.dto.response.SecuritiesSettlementResponse;
import com.enterprise.msmq.service.SecuritiesSettlementService;
import com.enterprise.msmq.dto.ApiResponse;
import com.enterprise.msmq.enums.ResponseCode;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for securities settlement operations.
 * Handles paired RECE and DELI message generation for securities transfers.
 * 
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2025-08-30
 */
@RestController
@RequestMapping("/v1/securities-settlement")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Securities Settlement", description = "Operations for securities settlement using paired RECE and DELI messages")
public class SecuritiesSettlementController {

    private final SecuritiesSettlementService settlementService;

    /**
     * Initiates a securities settlement operation.
     * Generates paired RECE and DELI messages for the specified security transfer.
     * 
     * @param request the settlement request containing security and account details
     * @return response with transaction IDs and status information
     */
    @PostMapping("/settle")
    @Operation(
        summary = "Initiate Securities Settlement",
        description = "Creates paired RECE and DELI messages for securities settlement. " +
                    "RECE credits the seller's account, DELI debits the buyer's account. " +
                    "Both messages are automatically cross-referenced and sent to the specified queue."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Settlement initiated successfully",
            content = @Content(schema = @Schema(implementation = SecuritiesSettlementResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal server error during settlement",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    public ResponseEntity<ApiResponse<SecuritiesSettlementResponse>> initiateSettlement(
            @Parameter(description = "Securities settlement request", required = true)
            @Valid @RequestBody SecuritiesSettlementRequest request) {
        
        try {
            SecuritiesSettlementResponse response = settlementService.sendPairedSettlement(request);

            if (response.isSuccess()) {
                log.info("✅ Securities settlement completed successfully. RECE: {}, DELI: {}", 
                        response.getReceTransactionId(), response.getDeliTransactionId());
                
                return ResponseEntity.ok(ApiResponse.success(
                    "Securities settlement completed successfully", 
                    response
                ));
            } else {
                log.error("❌ Securities settlement failed. Error: {}", response.getErrorMessage());
                
                return ResponseEntity.badRequest().body(ApiResponse.error(
                    ResponseCode.BUSINESS_ERROR,
                    "Securities settlement failed: " + response.getErrorMessage()
                ));
            }

        } catch (Exception e) {
            log.error("❌ Error during securities settlement", e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(
                ResponseCode.SYSTEM_ERROR,
                "Internal error during securities settlement: " + e.getMessage()
            ));
        }
    }

    /**
     * Health check endpoint for securities settlement service.
     * 
     * @return simple status response
     */
    @GetMapping("/health")
    @Operation(
        summary = "Securities Settlement Service Health Check",
        description = "Simple health check to verify the service is operational"
    )
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success(
            "Securities settlement service is operational", 
            "OK"
        ));
    }

    /**
     * Get information about the securities settlement process.
     * 
     * @return information about the settlement workflow
     */
    @GetMapping("/info")
    @Operation(
        summary = "Securities Settlement Information",
        description = "Provides information about the securities settlement workflow and message structure"
    )
    public ResponseEntity<ApiResponse<String>> getSettlementInfo() {
        String info = """
            Securities Settlement Workflow:
            
            1. RECE Message (Credits Seller):
               - Movement Type: RECE
               - Credit/Debit: CRDT
               - Account: Seller Account
               - Links to: DELI Message
            
            2. DELI Message (Debits Buyer):
               - Movement Type: DELI
               - Credit/Debit: DBIT
               - Account: Buyer Account
               - Links to: RECE Message
            
            3. Auto-Generated IDs:
               - Base Transaction ID: YYMMDD + Random (e.g., 250830A1B2C3)
               - RECE Transaction ID: Base + A (e.g., 250830A1B2C3A)
               - DELI Transaction ID: Base + B (e.g., 250830A1B2C3B)
            
            4. Cross-Referencing:
               - Each message contains the other's transaction ID
               - Common correlation ID links both messages
               - ATS can match RECE and DELI for settlement
            
            5. Queue Strategy:
               - Both messages sent to same queue
               - SWIFT processor picks up both messages
               - ATS receives messages separately for matching
            """;
        
        return ResponseEntity.ok(ApiResponse.success(
            "Securities settlement workflow information", 
            info
        ));
    }
}
