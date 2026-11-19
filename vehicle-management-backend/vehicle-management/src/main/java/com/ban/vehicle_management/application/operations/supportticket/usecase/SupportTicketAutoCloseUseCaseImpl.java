package com.ban.vehicle_management.application.operations.supportticket.usecase;

import com.ban.vehicle_management.application.notification.notification.model.SendNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.application.operations.supportticket.port.in.SupportTicketAutoClosePortIn;
import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketPortOut;
import com.ban.vehicle_management.application.operations.supportticket.service.SupportTicketConversationService;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.domain.operations.supportticket.policy.SupportTicketPolicy;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketEventType;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupportTicketAutoCloseUseCaseImpl implements SupportTicketAutoClosePortIn {

    private final SupportTicketPortOut supportTicketPortOut;
    private final SupportTicketConversationService ticketConversationService;
    private final NotificationPortIn notificationPortIn;
    private final Clock clock;
    private final int batchSize;
    private final SupportTicketPolicy supportTicketPolicy = new SupportTicketPolicy();

    public SupportTicketAutoCloseUseCaseImpl(
            SupportTicketPortOut supportTicketPortOut,
            SupportTicketConversationService ticketConversationService,
            NotificationPortIn notificationPortIn,
            Clock clock,
            @Value("${app.operations.support-ticket.auto-close-batch-size:50}") int batchSize
    ) {
        this.supportTicketPortOut = supportTicketPortOut;
        this.ticketConversationService = ticketConversationService;
        this.notificationPortIn = notificationPortIn;
        this.clock = clock;
        this.batchSize = Math.max(1, batchSize);
    }

    @Override
    @Transactional
    public int closeDueTickets() {
        Instant now = Instant.now(clock);
        int closed = 0;
        for (SupportTicket ticket : supportTicketPortOut.findDueForAutoClose(now, batchSize)) {
            SupportTicketStatus previousStatus = ticket.getStatus();
            supportTicketPolicy.autoCloseForInactivity(ticket, now);
            SupportTicket savedTicket = supportTicketPortOut.save(ticket);
            ticketConversationService.postSystemTicketStatusCard(
                    savedTicket,
                    SupportTicketEventType.CLOSED,
                    previousStatus,
                    lifecycleEventKey(savedTicket)
            );
            ticketConversationService.postAssistantTicketUpdate(
                    savedTicket,
                    "Yêu cầu hỗ trợ đã tự động đóng sau 24 giờ không phát sinh trao đổi mới."
            );
            notifyRequester(savedTicket);
            closed++;
        }
        return closed;
    }

    private String lifecycleEventKey(SupportTicket ticket) {
        int reopenCount = ticket.getReopenCount() == null ? 0 : ticket.getReopenCount();
        return "auto-closed:" + ticket.getSupportTicketId() + ":" + reopenCount;
    }

    private void notifyRequester(SupportTicket ticket) {
        if (notificationPortIn == null) {
            return;
        }
        notificationPortIn.sendWebNotification(new SendNotificationCommand(
                ticket.getRequesterAccountId(),
                NotificationType.SUPPORT_TICKET_CLOSED,
                "Ticket đã tự động đóng",
                "Ticket hỗ trợ đã đóng sau 24 giờ không phát sinh trao đổi mới.",
                "operations",
                "support_tickets",
                ticket.getSupportTicketId()
        ));
    }
}
