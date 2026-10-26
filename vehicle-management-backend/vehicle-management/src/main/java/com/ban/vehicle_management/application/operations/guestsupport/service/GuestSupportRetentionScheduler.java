package com.ban.vehicle_management.application.operations.guestsupport.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GuestSupportRetentionScheduler {
    private static final Logger log = LoggerFactory.getLogger(GuestSupportRetentionScheduler.class);
    private final GuestSupportService service;

    public GuestSupportRetentionScheduler(GuestSupportService service) {
        this.service = service;
    }

    @Scheduled(cron = "${app.public-support.retention-cleanup-cron:0 30 3 * * *}")
    public void purgeExpiredRequests() {
        long deleted = service.purgeExpired();
        if (deleted > 0) log.info("Purged {} expired guest support requests", deleted);
    }
}
