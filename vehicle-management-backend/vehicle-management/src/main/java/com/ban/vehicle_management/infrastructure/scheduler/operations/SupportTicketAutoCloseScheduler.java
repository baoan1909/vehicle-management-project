package com.ban.vehicle_management.infrastructure.scheduler.operations;

import com.ban.vehicle_management.application.operations.supportticket.port.in.SupportTicketAutoClosePortIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SupportTicketAutoCloseScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SupportTicketAutoCloseScheduler.class);
    private final SupportTicketAutoClosePortIn supportTicketAutoClosePortIn;

    public SupportTicketAutoCloseScheduler(SupportTicketAutoClosePortIn supportTicketAutoClosePortIn) {
        this.supportTicketAutoClosePortIn = supportTicketAutoClosePortIn;
    }

    @Scheduled(
            fixedDelayString = "${app.operations.support-ticket.auto-close-fixed-delay:1m}",
            initialDelayString = "${app.operations.support-ticket.auto-close-initial-delay:1m}"
    )
    public void closeDueTickets() {
        try {
            int closed = supportTicketAutoClosePortIn.closeDueTickets();
            if (closed > 0) {
                LOGGER.info("Automatically closed {} inactive support tickets", closed);
            }
        } catch (RuntimeException exception) {
            LOGGER.error("Support ticket inactivity auto-close failed", exception);
        }
    }
}
