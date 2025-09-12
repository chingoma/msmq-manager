package com.enterprise.msmq.controller;

import com.enterprise.msmq.dto.ApiResponse;
import com.enterprise.msmq.dto.request.PairedSettlementRequest;
import com.enterprise.msmq.entity.MsmqMessageTemplate;
import com.enterprise.msmq.service.MsmqMessageTemplateService;
import com.enterprise.msmq.enums.ResponseCode;
import com.enterprise.msmq.util.DateTimeUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for MSMQ Message Templates.
 * 
 * This controller provides comprehensive template management capabilities including:
 * - Creating and managing message templates with parameter placeholders
 * - Sending messages using templates with dynamic parameter substitution
 * - Validating template parameters before message sending
 * - Full CRUD operations for template lifecycle management
 * 
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/msmq/templates")
@Tag(name = "MSMQ Message Templates", description = "Operations for managing and using MSMQ message templates")
public class MsmqMessageTemplateController {

    private static final Logger logger = LoggerFactory.getLogger(MsmqMessageTemplateController.class);

    @Autowired
    private MsmqMessageTemplateService templateService;

    /**
     * Create a new message template.
     */
    @Operation(
        summary = "Create Message Template",
        description = "Creates a new message template with parameter placeholders for dynamic message generation",
        tags = {"MSMQ Message Templates"}
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Template created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Success Response",
                    value = """
                        {
                          "error": false,
                          "success": true,
                          "statusCode": "600",
                          "message": "Template created successfully",
                          "timestamp": "2025-08-27T12:36:33.123Z",
                          "requestId": "req-1756287393123-456",
                          "data": {
                            "id": 1,
                            "templateName": "SWIFT_SHARE_TRANSFER_DETAILED",
                            "templateType": "SWIFT",
                            "templateContent": "<RequestPayload>...</RequestPayload>",
                            "description": "Detailed SWIFT Share Transfer Instruction Template",
                            "parameters": {
                              "FROM_BIC": "string",
                              "TO_BIC": "string",
                              "TRANSACTION_ID": "string"
                            },
                            "isActive": true,
                            "createdAt": "2025-08-27T12:36:33",
                            "createdBy": "System"
                          }
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad request - validation error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        )
    })
    @PostMapping
    public ResponseEntity<ApiResponse<MsmqMessageTemplate>> createTemplate(
        @Parameter(
            description = "Message template to create",
            required = true,
            content = @Content(
                examples = @ExampleObject(
                    name = "SWIFT Template Example",
                    value = """
                        {
                          "templateName": "SWIFT_SHARE_TRANSFER_DETAILED",
                          "templateType": "SWIFT",
                          "templateContent": "<RequestPayload xmlns=\\"SWIFTNetBusinessEnvelope\\"><AppHdr xmlns=\\"urn:iso:std:iso:20022:tech:xsd:head.001.001.01\\"><Fr><OrgId><Id><OrgId><AnyBIC>{{FROM_BIC}}</AnyBIC></OrgId></Id></OrgId></Fr><To><OrgId><Id><OrgId><AnyBIC>{{TO_BIC}}</AnyBIC></OrgId></Id></OrgId></To><BizMsgIdr>{{MESSAGE_TYPE}}</BizMsgIdr><MsgDefIdr>{{MSG_DEF_ID}}</MsgDefIdr><CreDt>{{CREATION_DATE}}</CreDt></AppHdr><Document xmlns=\\"urn:iso:std:iso:20022:tech:xsd:sese.023.001.06\\"><SctiesSttlmTxInstr><TxId><AcctOwnrTxId>{{TRANSACTION_ID}}</AcctOwnrTxId></TxId><SttlmParams><SttlmDt>{{SETTLEMENT_DATE}}</SttlmDt><SctiesTxTp><Cd>{{TRADE_TYPE}}</Cd></SctiesTxTp><SttlmTxCond><Cd>{{SETTLEMENT_CONDITION}}</Cd></SttlmTxCond></SttlmParams><TradDtls><TradDt>{{TRADE_DATE}}</TradDt><SctiesMvmntTp>{{MOVEMENT_TYPE}}</SctiesMvmntTp><Pmt>{{PAYMENT_TYPE}}</Pmt></TradDtls><FinInstrmId><ISIN>{{ISIN_CODE}}</ISIN><Nm>{{SECURITY_NAME}}</Nm></FinInstrmId><QtyAndAcctDtls><SttlmQty><Qty><Unit>{{QUANTITY}}</Unit></Qty></SttlmQty><SfkpgAcct><Id>{{ACCOUNT_ID}}</Id></SfkpgAcct></QtyAndAcctDtls><SttlmPtiesSts><Pty1><Pty><Nm>{{PARTY1_NAME}}</Nm></Pty><Acct><Id>{{PARTY1_ACCOUNT}}</Id></Acct></Pty1><Pty2><Pty><Nm>{{PARTY2_NAME}}</Nm></Pty><Acct><Id>{{PARTY2_ACCOUNT}}</Id></Acct></Pty2></SttlmPtiesSts><AddtlTxDtls><TxDesc>{{TRANSACTION_DESCRIPTION}}</TxDesc></AddtlTxDtls></SctiesSttlmTxInstr></Document></RequestPayload>",
                          "description": "Detailed SWIFT Share Transfer Instruction Template with all parameters",
                          "parameters": {
                            "FROM_BIC": "string",
                            "TO_BIC": "string",
                            "MESSAGE_TYPE": "string",
                            "MSG_DEF_ID": "string",
                            "CREATION_DATE": "datetime",
                            "TRANSACTION_ID": "string",
                            "SETTLEMENT_DATE": "date",
                            "TRADE_TYPE": "string",
                            "SETTLEMENT_CONDITION": "string",
                            "TRADE_DATE": "date",
                            "MOVEMENT_TYPE": "string",
                            "PAYMENT_TYPE": "string",
                            "ISIN_CODE": "string",
                            "SECURITY_NAME": "string",
                            "QUANTITY": "number",
                            "ACCOUNT_ID": "string",
                            "PARTY1_NAME": "string",
                            "PARTY1_ACCOUNT": "string",
                            "PARTY2_NAME": "string",
                            "PARTY2_ACCOUNT": "string",
                            "TRANSACTION_DESCRIPTION": "string"
                          },
                          "isActive": true,
                          "createdBy": "System"
                        }
                        """
                )
            )
        )
        @RequestBody MsmqMessageTemplate template) {
        try {
            MsmqMessageTemplate createdTemplate = templateService.createTemplate(template);
            
            ApiResponse<MsmqMessageTemplate> response = ApiResponse.success("Template created successfully", createdTemplate);
            response.setRequestId(generateRequestId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            logger.error("Error creating template: {}", e.getMessage(), e);
            
            ApiResponse<MsmqMessageTemplate> response = ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to create template: " + e.getMessage());
            response.setRequestId(generateRequestId());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get all active templates.
     */
    @Operation(
        summary = "Get All Active Templates",
        description = "Retrieves all active message templates from the system",
        tags = {"MSMQ Message Templates"}
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Templates retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Success Response",
                    value = """
                        {
                          "error": false,
                          "success": true,
                          "statusCode": "600",
                          "message": "Templates retrieved successfully",
                          "timestamp": "2025-08-27T12:36:40.793Z",
                          "requestId": "req-1756287400793-128",
                          "data": [
                            {
                              "id": 1,
                              "templateName": "SWIFT_SHARE_TRANSFER_DETAILED",
                              "templateType": "SWIFT",
                              "templateContent": "<RequestPayload>...</RequestPayload>",
                              "description": "Detailed SWIFT Share Transfer Instruction Template",
                              "parameters": {
                                "FROM_BIC": "string",
                                "TO_BIC": "string",
                                "TRANSACTION_ID": "string"
                              },
                              "isActive": true,
                              "createdAt": "2025-08-27T12:36:33",
                              "createdBy": "System"
                            }
                          ]
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        )
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<MsmqMessageTemplate>>> getAllTemplates() {
        try {
            List<MsmqMessageTemplate> templates = templateService.getAllActiveTemplates();
            
            ApiResponse<List<MsmqMessageTemplate>> response = ApiResponse.success("Templates retrieved successfully", templates);
            response.setRequestId(generateRequestId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving templates: {}", e.getMessage(), e);
            
            ApiResponse<List<MsmqMessageTemplate>> response = ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to retrieve templates: " + e.getMessage());
            response.setRequestId(generateRequestId());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get template by name.
     */
    @Operation(
        summary = "Get Template by Name",
        description = "Retrieves a specific message template by its name",
        tags = {"MSMQ Message Templates"}
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Template retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Success Response",
                    value = """
                        {
                          "error": false,
                          "success": true,
                          "statusCode": "600",
                          "message": "Template retrieved successfully",
                          "timestamp": "2025-08-27T12:36:45.123Z",
                          "requestId": "req-1756287405123-789",
                          "data": {
                            "id": 1,
                            "templateName": "SWIFT_SHARE_TRANSFER_DETAILED",
                            "templateType": "SWIFT",
                            "templateContent": "<RequestPayload>...</RequestPayload>",
                            "description": "Detailed SWIFT Share Transfer Instruction Template",
                            "parameters": {
                              "FROM_BIC": "string",
                              "TO_BIC": "string",
                              "TRANSACTION_ID": "string"
                            },
                            "isActive": true,
                            "createdAt": "2025-08-27T12:36:33",
                            "createdBy": "System"
                          }
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Template not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Not Found Response",
                    value = """
                        {
                          "error": true,
                          "success": false,
                          "statusCode": "606",
                          "message": "Template not found: NON_EXISTENT_TEMPLATE",
                          "timestamp": "2025-08-27T12:36:45.123Z",
                          "requestId": "req-1756287405123-789",
                          "data": null
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        )
    })
    @GetMapping("/{templateName}")
    public ResponseEntity<ApiResponse<MsmqMessageTemplate>> getTemplateByName(
            @Parameter(description = "Name of the template to retrieve", example = "SWIFT_SHARE_TRANSFER_DETAILED")
            @PathVariable String templateName) {
        try {
            return templateService.getTemplateByName(templateName)
                .map(template -> {
                    ApiResponse<MsmqMessageTemplate> response = ApiResponse.success("Template retrieved successfully", template);
                    response.setRequestId(generateRequestId());
                    
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    ApiResponse<MsmqMessageTemplate> response = ApiResponse.error(ResponseCode.RESOURCE_NOT_FOUND, "Template not found: " + templateName);
                    response.setRequestId(generateRequestId());
                    
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                });
                
        } catch (Exception e) {
            logger.error("Error retrieving template: {}", e.getMessage(), e);
            
            ApiResponse<MsmqMessageTemplate> response = ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to retrieve template: " + e.getMessage());
            response.setRequestId(generateRequestId());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get templates by type.
     */
    @Operation(
        summary = "Get Templates by Type",
        description = "Retrieves all active templates of a specific type (e.g., SWIFT, JSON, XML)",
        tags = {"MSMQ Message Templates"}
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Templates retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Success Response",
                    value = """
                        {
                          "error": false,
                          "success": true,
                          "statusCode": "600",
                          "message": "Templates retrieved successfully",
                          "timestamp": "2025-08-27T12:36:58.123Z",
                          "requestId": "req-1756287418123-456",
                          "data": [
                            {
                              "id": 1,
                              "templateName": "SWIFT_SHARE_TRANSFER_DETAILED",
                              "templateType": "SWIFT",
                              "description": "SWIFT Share Transfer Template",
                              "isActive": true
                            }
                          ]
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        )
    })
    @GetMapping("/type/{templateType}")
    public ResponseEntity<ApiResponse<List<MsmqMessageTemplate>>> getTemplatesByType(
            @Parameter(description = "Type of templates to retrieve", example = "SWIFT")
            @PathVariable String templateType) {
        try {
            List<MsmqMessageTemplate> templates = templateService.getTemplatesByType(templateType);
            
            ApiResponse<List<MsmqMessageTemplate>> response = ApiResponse.success("Templates retrieved successfully", templates);
            response.setRequestId(generateRequestId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving templates by type: {}", e.getMessage(), e);
            
            ApiResponse<List<MsmqMessageTemplate>> response = ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to retrieve templates: " + e.getMessage());
            response.setRequestId(generateRequestId());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Search templates by name.
     */
    @Operation(
        summary = "Search Templates by Name",
        description = "Searches for templates by name using case-insensitive partial matching",
        tags = {"MSMQ Message Templates"}
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Template search completed successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Success Response",
                    value = """
                        {
                          "error": false,
                          "success": true,
                          "statusCode": "600",
                          "message": "Template search completed successfully",
                          "timestamp": "2025-08-27T12:37:00.123Z",
                          "requestId": "req-1756287420123-789",
                          "data": [
                            {
                              "id": 1,
                              "templateName": "SWIFT_SHARE_TRANSFER_DETAILED",
                              "templateType": "SWIFT",
                              "description": "SWIFT Share Transfer Template",
                              "isActive": true
                            }
                          ]
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        )
    })
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<MsmqMessageTemplate>>> searchTemplates(
            @Parameter(description = "Search term for template names", example = "SWIFT")
            @RequestParam String name) {
        try {
            List<MsmqMessageTemplate> templates = templateService.searchTemplatesByName(name);
            
            ApiResponse<List<MsmqMessageTemplate>> response = ApiResponse.success("Templates search completed successfully", templates);
            response.setRequestId(generateRequestId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error searching templates: {}", e.getMessage(), e);
            
            ApiResponse<List<MsmqMessageTemplate>> response = ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to search templates: " + e.getMessage());
            response.setRequestId(generateRequestId());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Update an existing template.
     */
    @Operation(
        summary = "Update Template",
        description = "Updates an existing message template with new content, parameters, or metadata",
        tags = {"MSMQ Message Templates"}
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Template updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Success Response",
                    value = """
                        {
                          "error": false,
                          "success": true,
                          "statusCode": "600",
                          "message": "Template updated successfully",
                          "timestamp": "2025-08-27T12:36:50.123Z",
                          "requestId": "req-1756287410123-456",
                          "data": {
                            "id": 1,
                            "templateName": "SWIFT_SHARE_TRANSFER_DETAILED",
                            "templateType": "SWIFT",
                            "templateContent": "<RequestPayload>Updated content...</RequestPayload>",
                            "description": "Updated SWIFT Template Description",
                            "parameters": {
                              "FROM_BIC": "string",
                              "TO_BIC": "string"
                            },
                            "isActive": true,
                            "updatedAt": "2025-08-27T12:36:50",
                            "updatedBy": "admin"
                          }
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad request - template not found or validation error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MsmqMessageTemplate>> updateTemplate(
            @Parameter(description = "ID of the template to update", example = "1")
            @PathVariable Long id,
            @Parameter(
                description = "Updated template data",
                required = true,
                content = @Content(
                    examples = @ExampleObject(
                        name = "Update Template Example",
                        value = """
                            {
                              "templateContent": "<RequestPayload>Updated content...</RequestPayload>",
                              "description": "Updated SWIFT Template Description",
                              "parameters": {
                                "FROM_BIC": "string",
                                "TO_BIC": "string"
                              },
                              "updatedBy": "admin"
                            }
                            """
                    )
                )
            )
            @RequestBody MsmqMessageTemplate template) {
        try {
            MsmqMessageTemplate updatedTemplate = templateService.updateTemplate(id, template);
            
            ApiResponse<MsmqMessageTemplate> response = ApiResponse.success("Template updated successfully", updatedTemplate);
            response.setRequestId(generateRequestId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error updating template: {}", e.getMessage(), e);
            
            ApiResponse<MsmqMessageTemplate> response = ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to update template: " + e.getMessage());
            response.setRequestId(generateRequestId());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Delete a template.
     */
    @Operation(
        summary = "Delete Template",
        description = "Soft deletes a message template by setting it as inactive",
        tags = {"MSMQ Message Templates"}
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Template deleted successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Success Response",
                    value = """
                        {
                          "error": false,
                          "success": true,
                          "statusCode": "600",
                          "message": "Template deleted successfully",
                          "timestamp": "2025-08-27T12:36:55.123Z",
                          "requestId": "req-1756287415123-789",
                          "data": null
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad request - template not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(
            @Parameter(description = "ID of the template to delete", example = "1")
            @PathVariable Long id) {
        try {
            templateService.deleteTemplate(id);
            
            ApiResponse<Void> response = ApiResponse.success("Template deleted successfully");
            response.setRequestId(generateRequestId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error deleting template: {}", e.getMessage(), e);
            
            ApiResponse<Void> response = ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to delete template: " + e.getMessage());
            response.setRequestId(generateRequestId());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Send message using template.
     */
    @Operation(
        summary = "Send Message Using Template",
        description = "Sends a message to MSMQ using a template with parameter substitution. The template content is merged with the provided parameters before sending.",
        tags = {"MSMQ Message Templates"}
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Message sent successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Success Response",
                    value = """
                        {
                          "error": false,
                          "success": true,
                          "statusCode": "600",
                          "message": "Message sent successfully using template",
                          "timestamp": "2025-08-27T12:36:48.123Z",
                          "requestId": "req-1756287408123-789",
                          "data": true
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad request - template not found or validation error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal server error or MSMQ operation failed",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        )
    })
    @PostMapping("/{templateName}/send")
    public ResponseEntity<ApiResponse<Boolean>> sendMessageUsingTemplate(
            @Parameter(description = "Name of the template to use", example = "SWIFT_SHARE_TRANSFER_DETAILED")
            @PathVariable String templateName,
            @Parameter(description = "Name of the MSMQ queue to send the message to", example = "testqueue")
            @RequestParam String queueName,
            @Parameter(
                description = "Parameters to substitute in the template",
                required = true,
                content = @Content(
                    examples = @ExampleObject(
                        name = "SWIFT Parameters Example",
                        value = """
                            {
                              "FROM_BIC": "588990",
                              "TO_BIC": "593129",
                              "MESSAGE_TYPE": "ShareTransferInstruction",
                              "MSG_DEF_ID": "sese.023.001.11.xsd",
                              "CREATION_DATE": "2025-08-14T22:22:38.601927660Z",
                              "TRANSACTION_ID": "TX20250808-0002",
                              "SETTLEMENT_DATE": "2025-08-15",
                              "TRADE_TYPE": "TRAD",
                              "SETTLEMENT_CONDITION": "NOMC",
                              "TRADE_DATE": "2025-08-15",
                              "MOVEMENT_TYPE": "DELIV",
                              "PAYMENT_TYPE": "APMT",
                              "ISIN_CODE": "GB0002634946",
                              "SECURITY_NAME": "CRDB",
                              "QUANTITY": "30",
                              "ACCOUNT_ID": "1",
                              "PARTY1_NAME": "ALI OMAR OTHMAN",
                              "PARTY1_ACCOUNT": "1",
                              "PARTY2_NAME": "CHRISTIAN KINDOLE",
                              "PARTY2_ACCOUNT": "ACC-REC-2020",
                              "TRANSACTION_DESCRIPTION": "Settlement against payment"
                            }
                            """
                    )
                )
            )
            @RequestBody Map<String, String> parameters) {
        try {

            String creationDate = DateTimeUtil.dateTime();

            Map<String, String> msgParameter = new HashMap<>();
            msgParameter.put("FROM_BIC", "588990");
            msgParameter.put("TO_BIC", "593129");
            msgParameter.put("MESSAGE_TYPE", "ShareTransferInstruction");
            msgParameter.put("MSG_DEF_ID", "sese.023.001.06.xsd");
            msgParameter.put("CREATION_DATE", creationDate);
            msgParameter.put("TRANSACTION_ID", "TX20250808-0002");
            msgParameter.put("SETTLEMENT_DATE", "2025-08-15");
            msgParameter.put("TRADE_TYPE", "TRAD");
            msgParameter.put("SETTLEMENT_CONDITION", "NOMC");
            msgParameter.put("TRADE_DATE", "2025-08-15");
            msgParameter.put("MOVEMENT_TYPE", "DELIV");
            msgParameter.put("PAYMENT_TYPE", "APMT");
            msgParameter.put("ISIN_CODE", "GB0002634946");
            msgParameter.put("SECURITY_NAME", "CRDB");
            msgParameter.put("QUANTITY", "30");
            msgParameter.put("ACCOUNT_ID", "1");
            msgParameter.put("PARTY1_NAME", "ALI OMAR OTHMAN");
            msgParameter.put("PARTY1_ACCOUNT", "1");
            msgParameter.put("PARTY2_NAME", "CHRISTIAN KINDOLE");
            msgParameter.put("PARTY2_ACCOUNT", "ACC-REC-2020");
            msgParameter.put("TRANSACTION_DESCRIPTION", "Settlement against payment");

            boolean success = templateService.sendMessageUsingTemplate(
                    templateName,
                    queueName,
                    msgParameter,
                    1,
                    null
            );
            
            ApiResponse<Boolean> response = ApiResponse.success(success ? "Message sent successfully using template" : "Failed to send message using template", success);
            response.setRequestId(generateRequestId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error sending message using template: {}", e.getMessage(), e);
            
            ApiResponse<Boolean> response = ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to send message using template: " + e.getMessage());
            response.setRequestId(generateRequestId());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Send paired (RECE/DELI) settlement messages using a template.
     */
    @Operation(
        summary = "Send Paired (RECE/DELI) Settlement Messages Using Template",
        description = "Sends both legs (RECE and DELI) of a securities settlement using the specified template, with correct cross-referencing.",
        tags = {"MSMQ Message Templates"}
    )
    @PostMapping("/{templateName}/send-paired")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> sendPairedSettlement(
            @PathVariable String templateName,
            @RequestBody PairedSettlementRequest request
    ) {
        try {
            // Use only baseParameters for both legs (no overrides)
            Map<String, String> base = request.getBaseParameters();
            Map<String, String> rece = base == null ? new java.util.HashMap<>() : new java.util.HashMap<>(base);
            Map<String, String> deli = base == null ? new java.util.HashMap<>() : new java.util.HashMap<>(base);

            // Generate a common reference ID for cross-linking
            String commonRef = rece.getOrDefault("COMMON_REFERENCE_ID", java.util.UUID.randomUUID().toString());
            rece.put("COMMON_REFERENCE_ID", commonRef);
            deli.put("COMMON_REFERENCE_ID", commonRef);

            // Generate unique transaction IDs for each leg if not provided
            String receTxId = rece.getOrDefault("TRANSACTION_ID", "RECE-" + java.util.UUID.randomUUID());
            String deliTxId = deli.getOrDefault("TRANSACTION_ID", "DELI-" + java.util.UUID.randomUUID());
            rece.put("TRANSACTION_ID", receTxId);
            deli.put("TRANSACTION_ID", deliTxId);

            // Set cross-referencing fields
            rece.put("LINKED_TRANSACTION_ID", deliTxId);
            deli.put("LINKED_TRANSACTION_ID", receTxId);

            // Set MOVEMENT_TYPE for each leg
            rece.put("MOVEMENT_TYPE", "RECE");
            deli.put("MOVEMENT_TYPE", "DELI");

            // Overwrite constant and generated fields for both legs
            String nowIso = java.time.Instant.now().toString();
            rece.put("MESSAGE_TYPE", "ShareTransferInstruction");
            deli.put("MESSAGE_TYPE", "ShareTransferInstruction");
            rece.put("MSG_DEF_ID", "sese.023.001.11.xsd");
            deli.put("MSG_DEF_ID", "sese.023.001.11.xsd");
            rece.put("CREATION_DATE", nowIso);
            deli.put("CREATION_DATE", nowIso);


            // Use provided or default priority/correlationId
            int priority = request.getPriority() != null ? request.getPriority() : 1;
            String correlationId = request.getCorrelationId();

            // Send both legs
            boolean receResult = templateService.sendMessageUsingTemplate(templateName, request.getQueueName(), rece, priority, correlationId);
            boolean deliResult = templateService.sendMessageUsingTemplate(templateName, request.getQueueName(), deli, priority, correlationId);

            Map<String, Boolean> result = new java.util.HashMap<>();
            result.put("RECE", receResult);
            result.put("DELI", deliResult);

            ApiResponse<Map<String, Boolean>> response = ApiResponse.success(
                (receResult && deliResult) ? "Both legs sent successfully" : "One or both legs failed to send",
                result
            );
            response.setRequestId(generateRequestId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error sending paired settlement messages: {}", e.getMessage(), e);
            ApiResponse<Map<String, Boolean>> response = ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to send paired settlement messages: " + e.getMessage());
            response.setRequestId(generateRequestId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Validate template parameters.
     */
    @Operation(
        summary = "Validate Template Parameters",
        description = "Validates that all required parameters for a template are provided before sending a message",
        tags = {"MSMQ Message Templates"}
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Parameters validation completed",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Valid Parameters Response",
                    value = """
                        {
                          "error": false,
                          "success": true,
                          "statusCode": "600",
                          "message": "Parameters are valid",
                          "timestamp": "2025-08-27T12:36:45.123Z",
                          "requestId": "req-1756287405123-456",
                          "data": true
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad request - template not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        )
    })
    @PostMapping("/{templateName}/validate")
    public ResponseEntity<ApiResponse<Boolean>> validateTemplateParameters(
            @Parameter(description = "Name of the template to validate parameters for", example = "SWIFT_SHARE_TRANSFER_DETAILED")
            @PathVariable String templateName,
            @Parameter(
                description = "Parameters to validate against the template",
                required = true,
                content = @Content(
                    examples = @ExampleObject(
                        name = "Validation Parameters Example",
                        value = """
                            {
                              "FROM_BIC": "588990",
                              "TO_BIC": "593129",
                              "TRANSACTION_ID": "TX20250808-0002",
                              "QUANTITY": "30"
                            }
                            """
                    )
                )
            )
            @RequestBody Map<String, String> parameters) {
        try {
            boolean isValid = templateService.validateTemplateParameters(templateName, parameters);
            
            ApiResponse<Boolean> response = ApiResponse.success(isValid ? "Parameters are valid" : "Parameters are invalid", isValid);
            response.setRequestId(generateRequestId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error validating template parameters: {}", e.getMessage(), e);
            
            ApiResponse<Boolean> response = ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to validate parameters: " + e.getMessage());
            response.setRequestId(generateRequestId());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private String generateRequestId() {
        return "req-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
}
