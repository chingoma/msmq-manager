package com.enterprise.msmq.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Response DTO for securities settlement operations.
 * Contains transaction IDs and status information for both RECE and DELI messages.
 * 
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2025-08-30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response for securities settlement operation")
public class SecuritiesSettlementResponse {

    @Schema(
        description = "Whether the paired settlement was successful",
        example = "true"
    )
    @JsonProperty("success")
    private boolean success;

    @Schema(
        description = "Base transaction ID (without A/B suffix)",
        example = "250830A1B2C3"
    )
    @JsonProperty("base_transaction_id")
    private String baseTransactionId;

    @Schema(
        description = "RECE transaction ID (credits seller)",
        example = "250830A1B2C3A"
    )
    @JsonProperty("rece_transaction_id")
    private String receTransactionId;

    @Schema(
        description = "DELI transaction ID (debits buyer)",
        example = "250830A1B2C3B"
    )
    @JsonProperty("deli_transaction_id")
    private String deliTransactionId;

    @Schema(
        description = "Correlation ID linking both messages",
        example = "CORR-A1B2C3D4"
    )
    @JsonProperty("correlation_id")
    private String correlationId;

    @Schema(
        description = "Common reference ID from the request",
        example = "616964F32"
    )
    @JsonProperty("common_reference_id")
    private String commonReferenceId;

    @Schema(
        description = "Destination queue name",
        example = "securities-settlement-queue"
    )
    @JsonProperty("queue_name")
    private String queueName;

    @Schema(
        description = "Security ISIN code",
        example = "TZ1996100214"
    )
    @JsonProperty("isin_code")
    private String isinCode;

    @Schema(
        description = "Security name",
        example = "DCB"
    )
    @JsonProperty("security_name")
    private String securityName;

    @Schema(
        description = "Quantity transferred",
        example = "10"
    )
    @JsonProperty("quantity")
    private Long quantity;

    @Schema(
        description = "Seller account ID",
        example = "588990"
    )
    @JsonProperty("seller_account_id")
    private String sellerAccountId;

    @Schema(
        description = "Buyer account ID",
        example = "593129"
    )
    @JsonProperty("buyer_account_id")
    private String buyerAccountId;

    @Schema(
        description = "Timestamp when settlement was processed",
        example = "2025-08-30T17:30:00"
    )
    @JsonProperty("processed_at")
    private LocalDateTime processedAt;

    @Schema(
        description = "Error message if settlement failed",
        example = "Failed to send DELI message"
    )
    @JsonProperty("error_message")
    private String errorMessage;

    @Schema(
        description = "Detailed status for RECE message",
        example = "SENT"
    )
    @JsonProperty("rece_status")
    private String receStatus;

    @Schema(
        description = "Detailed status for DELI message",
        example = "SENT"
    )
    @JsonProperty("deli_status")
    private String deliStatus;

    @Schema(description = "BIC of the buyer's broker", example = "BUYERBICXXX")
    @JsonProperty("buyer_broker_bic")
    private String buyerBrokerBic;

    @Schema(description = "BIC of the seller's broker", example = "SELLERBICXXX")
    @JsonProperty("seller_broker_bic")
    private String sellerBrokerBic;
}
