package com.enterprise.msmq.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for sending paired (RECE/DELI) settlement messages using a template.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request for sending paired (RECE/DELI) settlement messages using a template")
public class PairedSettlementRequest {
    @Schema(description = "Name of the MSMQ queue to send the messages to", example = "testqueue", required = true)
    private String queueName;

    @Schema(description = "Base parameters common to both legs", required = true)
    private Map<String, String> baseParameters;

    @Schema(description = "Message priority (optional)")
    private Integer priority;

    @Schema(description = "Correlation ID for the messages (optional)")
    private String correlationId;
}
