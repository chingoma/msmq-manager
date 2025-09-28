package com.enterprise.msmq.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for pledge instructions (balance and release).
 * Contains all necessary information to generate pledge balance and release messages.
 * 
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2025-09-26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request for pledge balance or release operations", 
        requiredProperties = {"securityIsin", "securityDesc", "quantity", "brokerCode", "csdAccount", "pledgeeBpid", "queueName"})
public class PledgeInstructionRequest {

    @Schema(
        description = "ISIN code of the security being pledged",
        example = "TZ0000000001",
        required = true
    )
    @NotBlank(message = "Security ISIN is required")
    @JsonProperty("security_isin")
    private String securityIsin;

    @Schema(
        description = "Description of the security",
        example = "Government Bond 2025",
        required = true
    )
    @NotBlank(message = "Security description is required")
    @JsonProperty("security_desc")
    private String securityDesc;

    @Schema(
        description = "Quantity of securities to pledge or release",
        example = "1000",
        required = true
    )
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;

    @Schema(
        description = "Broker code",
        example = "BRK001",
        required = true
    )
    @NotBlank(message = "Broker code is required")
    @JsonProperty("broker_code")
    private String brokerCode;

    @Schema(
        description = "CSD account",
        example = "123456",
        required = true
    )
    @NotBlank(message = "CSD account is required")
    @JsonProperty("csd_account")
    private String csdAccount;

    @Schema(
        description = "Pledgee BP ID",
        example = "PLEDGEE001",
        required = true
    )
    @NotBlank(message = "Pledgee BP ID is required")
    @JsonProperty("pledgee_bpid")
    private String pledgeeBpid;
    
    @Schema(
        description = "Trade date of the pledge operation",
        example = "2025-09-26"
    )
    @JsonProperty("trade_date")
    private LocalDate tradeDate;

    @Schema(
        description = "Settlement date of the pledge operation",
        example = "2025-09-26"
    )
    @JsonProperty("settlement_date")
    private LocalDate settlementDate;

    @Schema(
        description = "Holding number information",
        example = "HOLD123456"
    )
    @JsonProperty("holding_number_info")
    private String holdingNumberInfo;

    @Schema(
        description = "Client BP ID",
        example = "CLIENT001"
    )
    @JsonProperty("client_bpid")
    private String clientBpid;

    @Schema(
        description = "Type of operation: BALANCE or RELEASE. Will be automatically set based on endpoint if not provided.",
        example = "BALANCE",
        allowableValues = {"BALANCE", "RELEASE"},
        required = false
    )
    @JsonProperty("operation_type")
    private String operationType;

    @Schema(
        description = "Transaction ID for the pledge operation (will be auto-generated if not provided)",
        example = "202002340233"
    )
    @JsonProperty("transaction_id")
    private String transactionId;

    @Schema(
        description = "Processing reference ID (will be auto-generated if not provided)",
        example = "REF000001"
    )
    @JsonProperty("processing_id")
    private String processingId;
    
    @Schema(
        description = "Name of the queue to send the message to",
        example = "CSD.PLEDGE.QUEUE",
        required = true
    )
    @NotBlank(message = "Queue name is required")
    @JsonProperty("queue_name")
    private String queueName;
}