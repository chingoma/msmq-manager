package com.enterprise.msmq.service;

import com.enterprise.msmq.dto.request.PledgeInstructionRequest;
import com.enterprise.msmq.dto.response.PledgeInstructionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for handling pledge instruction operations using MSMQ templates.
 * Generates messages for pledge balance (COLI) and pledge release (COLO) operations.
 * <p>
 * This service:
 * <ul>
 *   <li>Generates unique transaction IDs and processing IDs when not provided.</li>
 *   <li>Sends messages using the appropriate template based on operation type.</li>
 *   <li>Handles both pledge balance and pledge release operations.</li>
 *   <li>Returns a detailed response with transaction status and error messages if any.</li>
 * </ul>
 *
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2025-09-26
 */
@Service
public class PledgeInstructionService {

    private static final Logger logger = LoggerFactory.getLogger(PledgeInstructionService.class);

    private final MsmqMessageTemplateService templateService;

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public PledgeInstructionService(MsmqMessageTemplateService templateService) {
        this.templateService = templateService;
    }

    /**
     * Sends a pledge instruction message (balance or release).
     *
     * @param request the pledge instruction request
     * @param environment the environment (local or remote)
     * @return PledgeInstructionResponse with transaction details and status
     */
    public PledgeInstructionResponse sendPledgeInstruction(PledgeInstructionRequest request, String environment) {
        try {
            // Validate operation type - default to BALANCE if not provided
            if (request.getOperationType() == null || request.getOperationType().trim().isEmpty()) {
                request.setOperationType("BALANCE"); // Default to BALANCE if not specified
                logger.info("No operation type provided, defaulting to BALANCE");
            }
            
            logger.info("Starting pledge {} operation for security: {} with quantity: {}",
                       request.getOperationType(), request.getSecurityIsin(), request.getQuantity());

            // Generate transaction ID if not provided
            String transactionId = request.getTransactionId() != null ? 
                request.getTransactionId() : generateTransactionId();
                
            // Generate processing ID that matches the format from documentation: 0000207 (7-digit number)
            String processingId = request.getProcessingId() != null ? 
                request.getProcessingId() : 
                String.format("%07d", (int) (Math.random() * 10000000));

            // Set default dates if not provided
            LocalDate tradeDate = request.getTradeDate() != null ? 
                request.getTradeDate() : LocalDate.now();
                
            LocalDate settlementDate = request.getSettlementDate() != null ? 
                request.getSettlementDate() : LocalDate.now();

            // Select template based on operation type
            String templateName = "BALANCE".equalsIgnoreCase(request.getOperationType()) ? 
                "SWIFT_PLEDGE_BALANCE_INSTRUCTION" : "SWIFT_PLEDGE_RELEASE_INSTRUCTION";

            // Build parameters
            Map<String, String> parameters = buildPledgeParameters(request, transactionId, processingId, 
                                                                 tradeDate, settlementDate);

            // Send message
            boolean success = templateService.sendMessageUsingTemplate(
                templateName, 
                request.getQueueName(), 
                parameters,
                environment,
                1,
                null
            );

            // Build response
            PledgeInstructionResponse response = PledgeInstructionResponse.builder()
                .success(success)
                .transactionId(transactionId)
                .processingId(processingId)
                .securityIsin(request.getSecurityIsin())
                .securityDesc(request.getSecurityDesc())
                .quantity(request.getQuantity())
                .brokerCode(request.getBrokerCode())
                .csdAccount(request.getCsdAccount())
                .pledgeeBpid(request.getPledgeeBpid())
                .operationType(request.getOperationType())
                .queueName(request.getQueueName())
                .status(success ? "SENT" : "FAILED")
                .processedAt(LocalDateTime.now())
                .build();

            if (success) {
                logger.info("✅ Pledge {} instruction sent successfully. Transaction ID: {}", 
                           request.getOperationType(), transactionId);
            } else {
                logger.error("❌ Pledge {} instruction failed. Transaction ID: {}", 
                           request.getOperationType(), transactionId);
                response.setErrorMessage("Failed to send pledge " + request.getOperationType() + " instruction");
            }

            return response;

        } catch (Exception e) {
            logger.error("❌ Error during pledge instruction processing", e);
            return PledgeInstructionResponse.builder()
                .success(false)
                .errorMessage("Error processing pledge instruction: " + e.getMessage())
                .operationType(request.getOperationType())
                .processedAt(LocalDateTime.now())
                .build();
        }
    }

    /**
     * Builds parameters map for pledge instruction templates.
     *
     * @param request the pledge instruction request
     * @param transactionId generated or provided transaction ID
     * @param processingId generated or provided processing ID
     * @param tradeDate trade date
     * @param settlementDate settlement date
     * @return map of parameters for the template
     */
    private Map<String, String> buildPledgeParameters(PledgeInstructionRequest request, 
                                                    String transactionId, String processingId,
                                                    LocalDate tradeDate, LocalDate settlementDate) {
        Map<String, String> parameters = new HashMap<>();
        
        // Basic SWIFT header parameters
        parameters.put("FROM_BIC", "TANZTZTXCSD");
        parameters.put("TO_BIC", "DSTXTZTZXXX");
        parameters.put("MESSAGE_TYPE", "Pledge Transaction");
        parameters.put("MSG_DEF_ID", "sese.023.001.06.xsd");  // Changed from 09 to 06 to match vendor format
        
        // Format: 2025-09-26T15:30:56.1234567Z (ISO format with milliseconds)
        LocalDateTime now = LocalDateTime.now();
        String creationDate = now.atZone(ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'"));
        parameters.put("CREATION_DATE", creationDate);
        
        // Transaction details - use shorter transaction ID format to match vendor sample
        // The vendor uses format like "2025092613495"
        String shortTxId = transactionId;
        if (transactionId.length() > 13) {
            shortTxId = transactionId.substring(0, 13);
        }
        parameters.put("TRANSACTION_ID", shortTxId);
        
        // Movement and payment types
        if ("BALANCE".equalsIgnoreCase(request.getOperationType())) {
            parameters.put("MOVEMENT_TYPE", "DELI");
            parameters.put("SECURITIES_TX_TYPE", "COLI");
        } else {
            parameters.put("MOVEMENT_TYPE", "RECE");
            parameters.put("SECURITIES_TX_TYPE", "COLO");
        }
        parameters.put("PAYMENT_TYPE", "FREE");
        
        // Date formats: ISO date (YYYY-MM-DD)
        parameters.put("TRADE_DATE", tradeDate.format(DateTimeFormatter.ISO_DATE));
        parameters.put("SETTLEMENT_DATE", settlementDate.format(DateTimeFormatter.ISO_DATE));
        
        // Status and additional info
        parameters.put("MATCHING_STATUS", "MACH");
        // Remove SttlmInstrPrcgAddtlDtls as it's not in the vendor's format
        // parameters.put("HOLDING_NUMBER_INFO", 
        //    request.getHoldingNumberInfo() != null ? request.getHoldingNumberInfo() : "");
        
        // Security details
        parameters.put("SECURITY_ISIN", request.getSecurityIsin());
        parameters.put("SECURITY_DESC", request.getSecurityDesc());
        parameters.put("QUANTITY", request.getQuantity().toString());
        
        // Account details - Format BPID as "XXXXX//XXXXX" to match vendor's format
        String brokerCode = request.getBrokerCode();
         parameters.put("BROKER_CODE",brokerCode); 
        parameters.put("CLIENT_BPID", request.getCsdAccount() + "//" + request.getCsdAccount());
        parameters.put("ACCOUNT_ISSUER", "BANK OF TANZANA");
        parameters.put("ACCOUNT_SCHEME", "SOR ACCOUNT");
        parameters.put("CSD_ACCOUNT", request.getCsdAccount());
        
        // Safekeeping place
        parameters.put("SAFEKEEPING_PLACE_TYPE", "NCSD");
        parameters.put("SAFEKEEPING_PLACE_ID", "TANZTZTXCSD");
        
        // Depository details
        parameters.put("DEPOSITORY_BIC", "TANZTZTXCSD");
        // Format Pledgee BPID with double-slash format to match vendor's format
        String pledgeeBPID = request.getPledgeeBpid();
        parameters.put("PLEDGEE_BPID", pledgeeBPID + "//" + pledgeeBPID);
        parameters.put("PLEDGEE_ISSUER", "CSD");
        parameters.put("PLEDGEE_SCHEME", "PLEDGEE");
        
        // Keep processing ID short as per vendor example
        // Vendor uses simple numeric values like "21"
        String shortProcessingId = processingId;
        if (processingId.length() > 2) {
            try {
                // Try to convert to integer and get a simple value
                int processId = Integer.parseInt(processingId) % 100; // Get last 2 digits
                shortProcessingId = String.valueOf(processId);
            } catch (NumberFormatException e) {
                // If conversion fails, just use the first 2 chars
                shortProcessingId = processingId.substring(0, 2);
            }
        }
        parameters.put("PROCESSING_ID", shortProcessingId);
        
        return parameters;
    }

    /**
     * Generates a transaction ID in the format YYYYMMDDHHMMSSmmm + random suffix
     *
     * @return transaction ID
     */
    private String generateTransactionId() {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        
        // Add a 3-character random alphanumeric suffix for uniqueness
        StringBuilder suffix = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            suffix.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        
        return timestamp + suffix.toString();
    }
}