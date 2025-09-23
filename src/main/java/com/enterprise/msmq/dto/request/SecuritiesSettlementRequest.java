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

/**
 * Request DTO for securities settlement operations.
 * Contains all necessary information to generate paired RECE and DELI messages.
 * 
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2025-08-30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request for securities settlement operation", requiredProperties = {"isinCode", "securityName", "quantity", "sellerAccountId", "buyerAccountId", "sellerName", "buyerName", "tradeDate", "settlementDate", "queueName", "commonReferenceId", "priority","sellerCustodianBic","buyerCustodianBic", "sellerBrokerBic","buyerBrokerBic"})
public class SecuritiesSettlementRequest {

    @Schema(
        description = "ISIN code of the security being transferred",
        example = "TZ1996100214"
    )
    @NotBlank(message = "ISIN code is required")
    @JsonProperty("isin_code")
    private String isinCode;

    @Schema(
        description = "Name of the security being transferred",
        example = "DCB"
    )
    @NotBlank(message = "Security name is required")
    @JsonProperty("security_name")
    private String securityName;

    @Schema(
        description = "Quantity of securities to transfer",
        example = "10",
        minimum = "1"
    )
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    @JsonProperty("quantity")
    private int quantity;

    @Schema(
        description = "Seller's account identifier",
        example = "588990"
    )
    @NotBlank(message = "Seller account ID is required")
    @JsonProperty("seller_account_id")
    private String sellerAccountId;

    @Schema(
        description = "Buyer's account identifier",
        example = "593129"
    )
    @NotBlank(message = "Buyer account ID is required")
    @JsonProperty("buyer_account_id")
    private String buyerAccountId;

    @Schema(
        description = "Destination queue name for settlement messages",
        example = "securities-settlement-queue"
    )
    @NotBlank(message = "Queue name is required")
    @JsonProperty("queue_name")
    private String queueName;

    @Schema(description = "BIC of the buyer's broker", example = "BUYERBICXXX")
    @JsonProperty("buyer_broker_bic")
    private String buyerBrokerBic;

    @Schema(description = "BIC of the seller's broker", example = "SELLERBICXXX")
    @JsonProperty("seller_broker_bic")
    private String sellerBrokerBic;

    @JsonProperty("seller_custodian_bic")
    @Schema(description = "BIC of the seller's custodian", example = "SELLERCUSTXXX")
    private String sellerCustodianBic;

    @JsonProperty("buyer_custodian_bic")
    @Schema(description = "BIC of the buyer's custodian", example = "BUYERCUSTXXX")
    private String buyerCustodianBic;

}
