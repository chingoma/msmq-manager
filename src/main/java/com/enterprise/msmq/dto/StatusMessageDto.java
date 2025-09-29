package com.enterprise.msmq.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for parsing SWIFT status messages received from MSMQ queues.
 * Handles sese.024.001.05 (Securities Settlement Transaction Status Advice) format.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusMessageDto {
    
    // Message Header Information
    private String messageId;
    private String businessMessageId;
    private String messageDefinitionId;
    private LocalDateTime creationDate;
    private String fromBic;
    private String toBic;
    
    // Transaction Identification
    private String accountOwnerTransactionId;
    private String accountServicerTransactionId;
    private String commonId;
    private List<String> tradeIds;
    
    // Status Information
    private String statusCode;
    private String statusIssuer;
    private String additionalReasonInfo;
    private String processingStatus;
    
    // Transaction Details
    private String isin;
    private String instrumentDescription;
    private Long settlementQuantity;
    private String settlementAmount;
    private String currency;
    private String creditDebitIndicator;
    private LocalDateTime expectedSettlementDate;
    private LocalDateTime expectedValueDate;
    private LocalDateTime settlementDate;
    private LocalDateTime tradeDate;
    
    // Movement Type (RECE, DELI, etc.)
    private String securitiesMovementType;
    private String paymentType;
    
    // Settlement Parties
    private String receivingDepository;
    private String receivingProcessingId;
    private String deliveringDepository;
    private String deliveringProcessingId;
    
    // Account Information
    private String accountOwnerId;
    private String accountOwnerIssuer;
    private String accountOwnerScheme;
    private String safekeepingAccountId;
    private String safekeepingAccountName;
    
    // Raw message content for audit
    private String rawMessageContent;
    
    // Processing metadata
    private LocalDateTime receivedAt;
    private String sourceQueue;
    private String processingStatusInternal;
}
