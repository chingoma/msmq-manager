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
//    @EventListener(ApplicationReadyEvent.class)
    public void testSecuritiesSettlement() {
        try {
            log.info("🧪 Starting securities settlement test...");

            // Create a test settlement request
            SecuritiesSettlementRequest request = SecuritiesSettlementRequest.builder()
                .securityName("CRDB")
                .quantity(10)
                .sellerAccountId("639535")
                .buyerAccountId("575883")
                .sellerName("John Doe")
                .buyerName("Jane Smith")
                .tradeDate("2025-08-29")
                .settlementDate("2025-09-03")
                .queueName("FormatName:DIRECT=TCP:192.168.2.170\\private$\\crdb_to_dse")  // Fixed FormatName format
                .buyerBrokerBic("B05/B")
                .buyerCustodianBic("B05/C")
                .sellerBrokerBic("B02/B")
                .sellerCustodianBic("B02/C")
                .isinCode("TZ1996100214")
                .build();

            log.info("📋 Test settlement request created:");
            log.info("   Security: {}", request.getSecurityName());
            log.info("   Quantity: {}", request.getQuantity());
            log.info("   Seller: {} ({})", request.getSellerName(), request.getSellerAccountId());
            log.info("   Buyer: {} ({})", request.getBuyerName(), request.getBuyerAccountId());
            log.info("   Queue: {}", request.getQueueName());

            // Execute the settlement remotely
            SecuritiesSettlementResponse responseRemotely = settlementService.sendPairedSettlement(request,"remote");

            // Log the results
            if (responseRemotely.isSuccess()) {
                log.info("✅ [Remotely] Securities settlement test completed successfully!");
            } else {
                log.error("❌ [Remotely] Securities settlement test failed!");
            }

//            // Execute the settlement locally
//            SecuritiesSettlementResponse responseLocally = settlementService.sendPairedSettlement(request,"local");
//
//            // Log the results
//            if (responseLocally.isSuccess()) {
//                log.info("✅ [Locally] Securities settlement test completed successfully!");
//            } else {
//                log.error("❌ [Locally] Securities settlement test failed!");
//            }

        } catch (Exception e) {
            log.error("❌ Error during securities settlement test", e);
        }
    }
}
