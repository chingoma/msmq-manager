package com.enterprise.msmq.service;

import com.enterprise.msmq.dto.request.SecuritiesSettlementRequest;
import com.enterprise.msmq.dto.response.SecuritiesSettlementResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for handling securities settlement operations using MSMQ templates.
 * Generates paired RECE and DELI messages for securities transfers.
 * 
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2025-08-30
 */
@Service
public class SecuritiesSettlementService {

    private static final Logger logger = LoggerFactory.getLogger(SecuritiesSettlementService.class);

    private final MsmqMessageTemplateService templateService;

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public SecuritiesSettlementService(MsmqMessageTemplateService templateService) {
        this.templateService = templateService;
    }

    /**
     * Sends paired RECE and DELI messages for securities settlement.
     * 
     * @param settlementRequest the settlement request containing investor and security details
     * @return SecuritiesSettlementResponse with transaction IDs and status information
     */
    public SecuritiesSettlementResponse sendPairedSettlement(SecuritiesSettlementRequest settlementRequest) {
        try {
            logger.info("Starting paired settlement for security: {} with quantity: {}",
                       settlementRequest.getIsinCode(), settlementRequest.getQuantity());

            // Generate unique base transaction ID
            String baseTransactionId = generateBaseTransactionId();
            String receTransactionId = baseTransactionId + "A";
            String deliTransactionId = baseTransactionId + "B";

            // Generate or use provided common reference ID
            String commonReferenceId = generateCommonReferenceId();
            // Generate correlation ID
            String correlationId = generateCorrelationId();

            // Send RECE message (credits seller)
            boolean receSent = sendReceMessage(settlementRequest, 
                                            receTransactionId, deliTransactionId, correlationId, commonReferenceId);

            // Send DELI message (debits buyer)
            boolean deliSent = sendDeliMessage(settlementRequest, 
                                             deliTransactionId, receTransactionId, correlationId, commonReferenceId);

            // Build response
            SecuritiesSettlementResponse response = SecuritiesSettlementResponse.builder()
                .success(receSent && deliSent)
                .baseTransactionId(baseTransactionId)
                .receTransactionId(receTransactionId)
                .deliTransactionId(deliTransactionId)
                .correlationId(correlationId)
                .commonReferenceId(commonReferenceId)
                .queueName(settlementRequest.getQueueName())
                .isinCode(settlementRequest.getIsinCode())
                .securityName(settlementRequest.getSecurityName())
                .quantity(settlementRequest.getQuantity())
                .sellerAccountId(settlementRequest.getSellerAccountId())
                .buyerAccountId(settlementRequest.getBuyerAccountId())
                .buyerBrokerBic(settlementRequest.getBuyerBrokerBic())
                .sellerBrokerBic(settlementRequest.getSellerBrokerBic())
                .processedAt(LocalDateTime.now())
                .receStatus(receSent ? "SENT" : "FAILED")
                .deliStatus(deliSent ? "SENT" : "FAILED")
                .build();

            if (receSent && deliSent) {
                logger.info("✅ Paired settlement completed successfully. RECE: {}, DELI: {}", 
                           receTransactionId, deliTransactionId);
            } else {
                logger.error("❌ Paired settlement failed. RECE: {}, DELI: {}", 
                           receSent, deliSent);
                response.setErrorMessage("Failed to send " + 
                    (receSent ? "" : "RECE ") + 
                    (deliSent ? "" : "DELI ") + "message(s)");
            }

            return response;

        } catch (Exception e) {
            logger.error("❌ Error during paired settlement", e);
            return SecuritiesSettlementResponse.builder()
                .success(false)
                .errorMessage("Error during settlement: " + e.getMessage())
                .processedAt(LocalDateTime.now())
                .build();
        }
    }

    /**
     * Sends RECE message (credits seller's account).
     */
    private boolean sendReceMessage(SecuritiesSettlementRequest request, 
                                  String receTxId, String deliTxId, String correlationId, String commonReferenceId) {
        try {
            Map<String, String> parameters = buildReceParameters(request, receTxId, deliTxId, correlationId, commonReferenceId);

            // Use existing template service to send message
            boolean success = templateService.sendMessageUsingTemplate(
                "SWIFT_SECURITIES_SETTLEMENT", 
                request.getQueueName(), 
                parameters, 
                1,
                correlationId
            );

            if (success) {
                logger.info("✅ RECE message sent successfully. Transaction ID: {}", receTxId);
            } else {
                logger.error("❌ Failed to send RECE message. Transaction ID: {}", receTxId);
            }

            return success;

        } catch (Exception e) {
            logger.error("❌ Error sending RECE message", e);
            return false;
        }
    }

    /**
     * Sends DELI message (debits buyer's account).
     */
    private boolean sendDeliMessage(SecuritiesSettlementRequest request, 
                                  String deliTxId, String receTxId, String correlationId, String commonReferenceId) {
        try {
            Map<String, String> parameters = buildDeliParameters(request, deliTxId, receTxId, correlationId, commonReferenceId);

            // Use existing template service to send message
            boolean success = templateService.sendMessageUsingTemplate(
                "SWIFT_SECURITIES_SETTLEMENT", 
                request.getQueueName(), 
                parameters, 
                1,
                correlationId
            );

            if (success) {
                logger.info("✅ DELI message sent successfully. Transaction ID: {}", deliTxId);
            } else {
                logger.error("❌ Failed to send DELI message. Transaction ID: {}", deliTxId);
            }

            return success;

        } catch (Exception e) {
            logger.error("❌ Error sending DELI message", e);
            return false;
        }
    }

    /**
     * Builds parameters for RECE message (credits seller).
     */
    private Map<String, String> buildReceParameters(SecuritiesSettlementRequest request, 
                                                   String receTxId, String deliTxId, String correlationId, String commonReferenceId) {
        Map<String, String> parameters = new HashMap<>();
        
        // Basic SWIFT header parameters (based on your rece.xml)
        parameters.put("FROM_BIC", "SAFMXXXXXXX");
        parameters.put("TO_BIC", "DSTXTZTZXXX");
        parameters.put("MESSAGE_TYPE", "Matched Deal Report");
        parameters.put("MSG_DEF_ID", "sese.023.001.11.xsd");
        parameters.put("CREATION_DATE", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        
        // Transaction and settlement parameters
        parameters.put("TRANSACTION_ID", receTxId);
        parameters.put("MOVEMENT_TYPE", "RECE");
        parameters.put("PAYMENT_TYPE", "FREE");
        parameters.put("COMMON_REFERENCE_ID", commonReferenceId);
        parameters.put("CURRENT_INSTRUCTION_NUMBER", "001");
        parameters.put("TOTAL_LINKED_INSTRUCTIONS", "002");
        parameters.put("LINKED_TRANSACTION_ID", deliTxId);
        
        // Trade details
        parameters.put("MARKET_IDENTIFIER", "SAFM");
        parameters.put("MARKET_TYPE", "OTCO");
        parameters.put("TRADE_DATE_TIME", request.getTradeDate() + "T15:59:37");
        parameters.put("SETTLEMENT_DATE", request.getSettlementDate());
        parameters.put("TRADE_TRANSACTION_CONDITION", "MAPR");
        parameters.put("TRADE_ORIGINATOR_ROLE", "MNOn");
        parameters.put("TRADE_ORIGINATOR_ISSUER", "DSTXTZTZXXX");
        parameters.put("TRADE_ORIGINATOR_SCHEME", "ORDER PLACEMENT PLATFORM");
        parameters.put("MATCHING_STATUS", "MACH");
        
        // Security details
        parameters.put("ISIN_CODE", request.getIsinCode());
        parameters.put("SECURITY_DESCRIPTION", request.getSecurityName());
        parameters.put("QUANTITY", String.valueOf(request.getQuantity()));
        
        // Account and settlement details
        parameters.put("ACCOUNT_OWNER_ID", request.getSellerAccountId());
        parameters.put("ACCOUNT_OWNER_ISSUER", "CSD");
        parameters.put("ACCOUNT_OWNER_SCHEME", "SOR ACCOUNT");
        parameters.put("SAFEEPING_ACCOUNT_ID", request.getSellerBrokerBic()+"/C");
        parameters.put("SAFEEPING_PLACE_TYPE", "CUST");
        parameters.put("SAFEEPING_PLACE_ID", "DSTXTZTZXXX");
        parameters.put("SECURITIES_TRANSACTION_TYPE", "TRAD");
        parameters.put("SETTLEMENT_SYSTEM_METHOD", "NSET");
        
        // Party details (Delivering and Receiving)
        parameters.put("DEPOSITORY_BIC", "DSTXTZTZ");
        parameters.put("DELIVERING_PARTY1_ID", request.getBuyerBrokerBic()+"B02/B");
        parameters.put("DELIVERING_PARTY1_ISSUER", "CSD");
        parameters.put("DELIVERING_PARTY1_SCHEME", "TRADING PARTY");
        parameters.put("PROCESSING_ID", receTxId);
        parameters.put("DELIVERING_PARTY2_ID", request.getSellerAccountId());
        parameters.put("DELIVERING_PARTY2_ISSUER", "CSD");
        parameters.put("DELIVERING_PARTY2_SCHEME", "SOR ACCOUNT");
        parameters.put("DELIVERING_PARTY3_ID", request.getBuyerBrokerBic()+"/C");
        parameters.put("DELIVERING_PARTY3_ISSUER", "CSD");
        parameters.put("DELIVERING_PARTY3_SCHEME", "MB SCA");
        
        parameters.put("RECEIVING_DEPOSITORY_BIC", "SAFMXXXX");
        parameters.put("RECEIVING_PARTY1_ID", request.getSellerBrokerBic()+"/B");
        parameters.put("RECEIVING_PARTY1_ISSUER", "CSD");
        parameters.put("RECEIVING_PARTY1_SCHEME", "TRADING PARTY");
        parameters.put("RECEIVING_PARTY2_ID", request.getSellerAccountId());
        parameters.put("RECEIVING_PARTY2_ISSUER", "CSD");
        parameters.put("RECEIVING_PARTY2_SCHEME", "SOR ACCOUNT");
        parameters.put("RECEIVING_PARTY3_ID", request.getSellerBrokerBic()+"/C");
        parameters.put("RECEIVING_PARTY3_ISSUER", "CSD");
        parameters.put("RECEIVING_PARTY3_SCHEME", "MB SCA");
        
        // Settlement amount and credit/debit indicator
        parameters.put("CREDIT_DEBIT_INDICATOR", "DBIT"); // RECE debits the seller
        
        // Termination date
        parameters.put("TERMINATION_DATE", request.getTradeDate());
        
        return parameters;
    }

    /**
     * Builds parameters for DELI message (debits buyer).
     */
    private Map<String, String> buildDeliParameters(SecuritiesSettlementRequest request, 
                                                   String deliTxId, String receTxId, String correlationId, String commonReferenceId) {
        Map<String, String> parameters = new HashMap<>();
        
        // Basic SWIFT header parameters (based on your deli.xml)
        parameters.put("FROM_BIC", "SAFMXXXXXXX");
        parameters.put("TO_BIC", "DSTXTZTZXXX");
        parameters.put("MESSAGE_TYPE", "Matched Deal Report");
        parameters.put("MSG_DEF_ID", "sese.023.001.11.xsd");
        parameters.put("CREATION_DATE", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        
        // Transaction and settlement parameters
        parameters.put("TRANSACTION_ID", deliTxId);
        parameters.put("MOVEMENT_TYPE", "DELI");
        parameters.put("PAYMENT_TYPE", "FREE");
        parameters.put("COMMON_REFERENCE_ID", commonReferenceId);
        parameters.put("CURRENT_INSTRUCTION_NUMBER", "002");
        parameters.put("TOTAL_LINKED_INSTRUCTIONS", "002");
        parameters.put("LINKED_TRANSACTION_ID", receTxId);
        
        // Trade details
        parameters.put("MARKET_IDENTIFIER", "SAFM");
        parameters.put("MARKET_TYPE", "OTCO");
        parameters.put("TRADE_DATE_TIME", request.getTradeDate() + "T15:59:37");
        parameters.put("SETTLEMENT_DATE", request.getSettlementDate());
        parameters.put("TRADE_TRANSACTION_CONDITION", "MAPR");
        parameters.put("TRADE_ORIGINATOR_ROLE", "MNOn");
        parameters.put("TRADE_ORIGINATOR_ISSUER", "DSTXTZTZXXX");
        parameters.put("TRADE_ORIGINATOR_SCHEME", "ORDER PLACEMENT PLATFORM");
        parameters.put("MATCHING_STATUS", "MACH");
        
        // Security details
        parameters.put("ISIN_CODE", request.getIsinCode());
        parameters.put("SECURITY_DESCRIPTION", request.getSecurityName());
        parameters.put("QUANTITY", String.valueOf(request.getQuantity()));
        
        // Account and settlement details
        parameters.put("ACCOUNT_OWNER_ID", request.getBuyerAccountId());
        parameters.put("ACCOUNT_OWNER_ISSUER", "CSD");
        parameters.put("ACCOUNT_OWNER_SCHEME", "SOR ACCOUNT");
        parameters.put("SAFEEPING_ACCOUNT_ID", request.getBuyerBrokerBic()+"/C");
        parameters.put("SAFEEPING_PLACE_TYPE", "CUST");
        parameters.put("SAFEEPING_PLACE_ID", "DSTXTZTZXXX");
        parameters.put("SECURITIES_TRANSACTION_TYPE", "TRAD");
        parameters.put("SETTLEMENT_SYSTEM_METHOD", "NSET");
        
        // Party details (Delivering and Receiving)
        parameters.put("DEPOSITORY_BIC", "DSTXTZTZ");
        parameters.put("DELIVERING_PARTY1_ID", request.getBuyerBrokerBic()+"/B");
        parameters.put("DELIVERING_PARTY1_ISSUER", "CSD");
        parameters.put("DELIVERING_PARTY1_SCHEME", "TRADING PARTY");
        parameters.put("PROCESSING_ID", deliTxId);
        parameters.put("DELIVERING_PARTY2_ID", request.getBuyerAccountId());
        parameters.put("DELIVERING_PARTY2_ISSUER", "CSD");
        parameters.put("DELIVERING_PARTY2_SCHEME", "SOR ACCOUNT");
        parameters.put("DELIVERING_PARTY3_ID", request.getBuyerBrokerBic()+"/C");
        parameters.put("DELIVERING_PARTY3_ISSUER", "CSD");
        parameters.put("DELIVERING_PARTY3_SCHEME", "MB SCA");
        
        parameters.put("RECEIVING_DEPOSITORY_BIC", "SAFMXXXX");
        parameters.put("RECEIVING_PARTY1_ID", request.getSellerBrokerBic()+"/B");
        parameters.put("RECEIVING_PARTY1_ISSUER", "CSD");
        parameters.put("RECEIVING_PARTY1_SCHEME", "TRADING PARTY");
        parameters.put("RECEIVING_PARTY2_ID", request.getBuyerAccountId());
        parameters.put("RECEIVING_PARTY2_ISSUER", "CSD");
        parameters.put("RECEIVING_PARTY2_SCHEME", "SOR ACCOUNT");
        parameters.put("RECEIVING_PARTY3_ID", request.getSellerBrokerBic()+"/C");
        parameters.put("RECEIVING_PARTY3_ISSUER", "CSD");
        parameters.put("RECEIVING_PARTY3_SCHEME", "MB SCA");
        
        // Settlement amount and credit/debit indicator
        parameters.put("CREDIT_DEBIT_INDICATOR", "CRDT"); // DELI credits the buyer
        
        // Termination date
        parameters.put("TERMINATION_DATE", request.getTradeDate());
        
        return parameters;
    }

    /**
     * Generates a unique base transaction ID.
     */
    private String generateBaseTransactionId() {
        // Generate format: YYMMDD + random 6 chars
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return datePart + randomPart;
    }

    /**
     * Generates a correlation ID if none provided.
     */
    private String generateCorrelationId() {
        return "CORR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Generates a unique 9-character common reference ID (e.g., 616964F32).
     * The first character is a digit, followed by 8 alphanumeric characters.
     */
    private String generateCommonReferenceId() {
        StringBuilder sb = new StringBuilder(9);
        // First character: digit
        sb.append(RANDOM.nextInt(10));
        // Next 8: alphanumeric
        for (int i = 0; i < 8; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }


}
