package com.enterprise.msmq.service;

import com.enterprise.msmq.dto.request.SecuritiesSettlementRequest;
import com.enterprise.msmq.dto.response.SecuritiesSettlementResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Test service for demonstrating securities settlement functionality.
 * Runs automatically when the application starts.
 * 
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2025-08-30
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecuritiesSettlementTestService {

    private final SecuritiesSettlementService settlementService;

    /**
     * Runs a test securities settlement when the application starts.
     * This demonstrates the paired RECE and DELI message generation.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void testSecuritiesSettlement() {
        try {
            log.info("🧪 Starting securities settlement test...");

            // Create a test settlement request
            SecuritiesSettlementRequest request = SecuritiesSettlementRequest.builder()
                .securityName("DCB")
                .quantity(10L)
                .sellerAccountId("588990")
                .buyerAccountId("593129")
                .sellerName("John Doe")
                .buyerName("Jane Smith")
                .tradeDate("2025-08-29")
                .settlementDate("2025-09-03")
                .queueName("nmb_to_dse")
                .buyerBrokerBic("BUYERBICXXX")
                .sellerBrokerBic("SELLERBICXXX")
                .build();

            log.info("📋 Test settlement request created:");
            log.info("   Security: {}", request.getSecurityName());
            log.info("   Quantity: {}", request.getQuantity());
            log.info("   Seller: {} ({})", request.getSellerName(), request.getSellerAccountId());
            log.info("   Buyer: {} ({})", request.getBuyerName(), request.getBuyerAccountId());
            log.info("   Queue: {}", request.getQueueName());

            // Execute the settlement
            SecuritiesSettlementResponse response = settlementService.sendPairedSettlement(request);

            // Log the results
            if (response.isSuccess()) {
                log.info("✅ Securities settlement test completed successfully!");
                log.info("   Base Transaction ID: {}", response.getBaseTransactionId());
                log.info("   RECE Transaction ID: {}", response.getReceTransactionId());
                log.info("   DELI Transaction ID: {}", response.getDeliTransactionId());
                log.info("   Correlation ID: {}", response.getCorrelationId());
                log.info("   RECE Status: {}", response.getReceStatus());
                log.info("   DELI Status: {}", response.getDeliStatus());
                log.info("   Processed At: {}", response.getProcessedAt());
            } else {
                log.error("❌ Securities settlement test failed!");
                log.error("   Error: {}", response.getErrorMessage());
                log.error("   RECE Status: {}", response.getReceStatus());
                log.error("   DELI Status: {}", response.getDeliStatus());
            }

        } catch (Exception e) {
            log.error("❌ Error during securities settlement test", e);
        }
    }
}
