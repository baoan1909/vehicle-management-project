package com.ban.vehicle_management.application.billing.wallet.scheduler;

import com.ban.vehicle_management.application.billing.wallet.port.in.WalletTopupPortIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Multi-instance safe top-up expiry via DB claim (no local timers).
 */
@Component
public class WalletTopupExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(WalletTopupExpiryScheduler.class);
    private final WalletTopupPortIn walletTopupPortIn;

    public WalletTopupExpiryScheduler(WalletTopupPortIn walletTopupPortIn) {
        this.walletTopupPortIn = walletTopupPortIn;
    }

    @Scheduled(fixedDelayString = "${app.wallet.topup-expiry-interval-ms:60000}")
    public void expireOverdue() {
        try {
            int expired = walletTopupPortIn.expireOverdueTopups(200);
            if (expired > 0) {
                log.info("Expired {} overdue wallet top-up orders", expired);
            }
        } catch (Exception exception) {
            log.warn("Wallet top-up expiry sweep failed: {}", exception.getMessage());
        }
    }
}
