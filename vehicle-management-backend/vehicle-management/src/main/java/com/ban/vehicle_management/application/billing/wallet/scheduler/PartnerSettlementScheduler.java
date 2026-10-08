package com.ban.vehicle_management.application.billing.wallet.scheduler;

import com.ban.vehicle_management.application.billing.wallet.port.in.SettlementPortIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Multi-instance safe settlement release via DB claim (FOR UPDATE SKIP LOCKED).
 */
@Component
public class PartnerSettlementScheduler {

    private static final Logger log = LoggerFactory.getLogger(PartnerSettlementScheduler.class);
    private final SettlementPortIn settlementPortIn;

    public PartnerSettlementScheduler(SettlementPortIn settlementPortIn) {
        this.settlementPortIn = settlementPortIn;
    }

    @Scheduled(fixedDelayString = "${app.wallet.settlement-interval-ms:300000}")
    public void releaseDue() {
        try {
            int released = settlementPortIn.releaseDueSettlements(200);
            if (released > 0) {
                log.info("Released {} due partner settlements", released);
            }
        } catch (Exception exception) {
            log.warn("Partner settlement sweep failed: {}", exception.getMessage());
        }
    }
}
