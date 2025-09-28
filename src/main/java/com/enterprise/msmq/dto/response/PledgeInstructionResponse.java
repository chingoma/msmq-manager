package com.enterprise.msmq.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for pledge instruction operations.
 * Contains transaction details and status information.
 * 
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2025-09-26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PledgeInstructionResponse {

    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("transaction_id")
    private String transactionId;
    
    @JsonProperty("processing_id")
    private String processingId;
    
    @JsonProperty("security_isin")
    private String securityIsin;
    
    @JsonProperty("security_desc")
    private String securityDesc;
    
    @JsonProperty("quantity")
    private BigDecimal quantity;
    
    @JsonProperty("broker_code")
    private String brokerCode;
    
    @JsonProperty("csd_account")
    private String csdAccount;
    
    @JsonProperty("pledgee_bpid")
    private String pledgeeBpid;
    
    @JsonProperty("operation_type")
    private String operationType;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("queue_name")
    private String queueName;
    
    @JsonProperty("processed_at")
    private LocalDateTime processedAt;
    
    @JsonProperty("error_message")
    private String errorMessage;
}