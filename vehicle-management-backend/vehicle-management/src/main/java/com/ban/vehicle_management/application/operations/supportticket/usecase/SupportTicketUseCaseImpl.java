package com.ban.vehicle_management.application.operations.supportticket.usecase;

import com.ban.vehicle_management.application.operations.supportticket.authorization.SupportTicketAccessGuard;
import com.ban.vehicle_management.application.operations.supportticket.port.in.SupportTicketPortIn;
import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketPortOut;
import com.ban.vehicle_management.application.operations.supportticket.service.SupportTicketConversationService;
import com.ban.vehicle_management.application.notification.notification.model.BroadcastNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.model.NotificationAudience;
import com.ban.vehicle_management.application.notification.notification.model.SendNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.application.people.customer.port.out.CustomerPortOut;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.domain.operations.supportticket.policy.SupportTicketPolicy;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupportTicketUseCaseImpl implements SupportTicketPortIn {

    private final SupportTicketPortOut supportTicketPortOut;
    private final SupportTicketAccessGuard accessGuard;
    private final CustomerPortOut customerPortOut;
    private final NotificationPortIn notificationPortIn;
    private final SupportTicketConversationService ticketConversationService;
    private final SupportTicketPolicy supportTicketPolicy = new SupportTicketPolicy();

    public SupportTicketUseCaseImpl(
            SupportTicketPortOut supportTicketPortOut,
            SupportTicketAccessGuard accessGuard,
            CustomerPortOut customerPortOut,
            NotificationPortIn notificationPortIn,
            SupportTicketConversationService ticketConversationService
    ) {
        this.supportTicketPortOut = supportTicketPortOut;
        this.accessGuard = accessGuard;
        this.customerPortOut = customerPortOut;
        this.notificationPortIn = notificationPortIn;
        this.ticketConversationService = ticketConversationService;
    }

    @Override
    @Transactional
    public SupportTicket createTicket(SupportTicket supportTicket) {
        UUID customerId = accessGuard.resolveCustomerIdForCreate();
        supportTicket.setCustomerId(customerId);

        validateActiveCategory(supportTicket.getCategoryId());

        if (supportTicketPortOut.existsActiveWorkflowByCustomerIdAndCategoryId(
                customerId,
                supportTicket.getCategoryId()
        )) {
            throw new ConflictException("Customer already has an active support ticket in this category");
        }

        supportTicketPolicy.initialize(supportTicket);
        supportTicket.setSupportTicketId(UUID.randomUUID());

        SupportTicket savedTicket = supportTicketPortOut.save(supportTicket);
        notifyTicketCreated(savedTicket);
        return savedTicket;
    }

    @Override
    @Transactional
    public SupportTicket createTicketFromConversation(SupportTicket supportTicket, UUID conversationId) {
        UUID customerId = accessGuard.resolveCustomerIdForCreateFromChat();
        UUID customerAccountId = accessGuard.currentAccountId();
        ChatConversation conversation = ticketConversationService.getPrivateCustomerConversation(conversationId, customerAccountId);

        UUID assignedAccountId = ticketConversationService.resolveCounterpartAccountId(conversation, customerAccountId);
        supportTicket.setCustomerId(customerId);
        validateActiveCategory(supportTicket.getCategoryId());
        if (supportTicketPortOut.existsActiveWorkflowByCustomerIdAndCategoryId(customerId, supportTicket.getCategoryId())) {
            throw new ConflictException("Customer already has an active support ticket in this category");
        }
        supportTicketPolicy.initialize(supportTicket);
        // initialize() deliberately clears workflow fields for a new ticket; direct-chat tickets
        // must restore their resolved counterpart afterwards so assignment and notification agree.
        supportTicket.setAssignedTo(assignedAccountId);
        supportTicket.setSupportTicketId(UUID.randomUUID());

        SupportTicket savedTicket = supportTicketPortOut.save(supportTicket);
        ticketConversationService.postTicketCardFromCustomer(savedTicket, conversation, customerAccountId);
        notifyTicketCreated(savedTicket);
        notifyTicketAssigned(savedTicket);
        return savedTicket;
    }

    @Override
    @Transactional(readOnly = true)
    public SupportTicket getTicketById(UUID supportTicketId) {
        SupportTicket supportTicket = findTicketOrThrow(supportTicketId);
        accessGuard.ensureCanRead(supportTicket);
        return supportTicket;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicket> getTickets(
            UUID customerId,
            UUID categoryId,
            UUID assignedTo,
            SupportTicketStatus status,
            SupportTicketCategoryPriority priority,
            String keyword
    ) {
        return supportTicketPortOut.findAll(customerId, categoryId, assignedTo, status, priority, normalizeKeyword(keyword))
                .stream()
                .filter(ticket -> {
                    try {
                        accessGuard.ensureCanRead(ticket);
                        return true;
                    } catch (RuntimeException exception) {
                        return false;
                    }
                })
                .toList();
    }

    @Override
    @Transactional
    public SupportTicket updateTicket(UUID supportTicketId, SupportTicket supportTicket) {
        SupportTicket existingTicket = findTicketOrThrow(supportTicketId);
        accessGuard.ensureCanUpdate(existingTicket);

        if (existingTicket.getStatus() != SupportTicketStatus.OPEN) {
            throw new ConflictException("Only open support ticket can be updated");
        }

        validateActiveCategory(supportTicket.getCategoryId());

        existingTicket.setCategoryId(supportTicket.getCategoryId());
        existingTicket.setTitle(supportTicket.getTitle());
        existingTicket.setContent(supportTicket.getContent());

        supportTicketPolicy.validateState(existingTicket);
        return supportTicketPortOut.save(existingTicket);
    }

    @Override
    @Transactional
    public SupportTicket assignTicket(UUID supportTicketId, UUID assignedTo) {
        accessGuard.ensureCanAssign();

        SupportTicket existingTicket = findTicketOrThrow(supportTicketId);

        if (!supportTicketPortOut.existsAssignableAccountById(assignedTo)) {
            throw new NotFoundException("Assignable account not found");
        }

        supportTicketPolicy.assign(existingTicket, assignedTo);
        SupportTicket savedTicket = supportTicketPortOut.save(existingTicket);
        notifyTicketAssigned(savedTicket);
        return savedTicket;
    }

    @Override
    @Transactional
    public SupportTicket startProgress(UUID supportTicketId) {
        SupportTicket existingTicket = findTicketOrThrow(supportTicketId);
        accessGuard.ensureCanProcess(existingTicket);

        supportTicketPolicy.startProgress(existingTicket);
        SupportTicket savedTicket = supportTicketPortOut.save(existingTicket);
        notifyTicketStatusChanged(savedTicket, NotificationType.SUPPORT_TICKET_IN_PROGRESS, "Ticket đang được xử lý", "Ticket hỗ trợ của bạn đang được xử lý.");
        return savedTicket;
    }

    @Override
    @Transactional
    public SupportTicket resolveTicket(UUID supportTicketId, String resolutionNote) {
        SupportTicket existingTicket = findTicketOrThrow(supportTicketId);
        accessGuard.ensureCanProcess(existingTicket);

        supportTicketPolicy.resolve(existingTicket, resolutionNote, Instant.now());
        SupportTicket savedTicket = supportTicketPortOut.save(existingTicket);
        notifyTicketStatusChanged(savedTicket, NotificationType.SUPPORT_TICKET_RESPONDED, "Ticket đã có phản hồi", "Ticket hỗ trợ của bạn đã được phản hồi và đánh dấu đã xử lý.");
        return savedTicket;
    }

    @Override
    @Transactional
    public SupportTicket reopenTicket(UUID supportTicketId) {
        SupportTicket existingTicket = findTicketOrThrow(supportTicketId);
        accessGuard.ensureCanReopen(existingTicket);

        supportTicketPolicy.reopen(existingTicket, Instant.now());
        SupportTicket savedTicket = supportTicketPortOut.save(existingTicket);
        notifyTicketStatusChanged(savedTicket, NotificationType.SUPPORT_TICKET_REOPENED, "Ticket được mở lại", "Ticket hỗ trợ của bạn đã được mở lại để tiếp tục xử lý.");
        return savedTicket;
    }

    @Override
    @Transactional
    public SupportTicket closeTicket(UUID supportTicketId) {
        SupportTicket existingTicket = findTicketOrThrow(supportTicketId);
        UUID closedBy = accessGuard.resolveClosedByForClose(existingTicket);

        supportTicketPolicy.close(existingTicket, closedBy, Instant.now());
        SupportTicket savedTicket = supportTicketPortOut.save(existingTicket);
        notifyTicketStatusChanged(savedTicket, NotificationType.SUPPORT_TICKET_CLOSED, "Ticket đã đóng", "Ticket hỗ trợ của bạn đã được đóng.");
        return savedTicket;
    }

    @Override
    @Transactional
    public ChatConversation openCustomerConversationForReply(UUID supportTicketId) {
        SupportTicket ticket = findTicketOrThrow(supportTicketId);
        UUID assignedAccountId = accessGuard.ensureCanReplyAsAssignee(ticket);
        return ticketConversationService.openPrivateConversationForReply(ticket, assignedAccountId);
    }

    private SupportTicket findTicketOrThrow(UUID supportTicketId) {
        return supportTicketPortOut.findById(supportTicketId)
                .orElseThrow(() -> new NotFoundException("Support ticket not found"));
    }

    private void validateActiveCategory(UUID categoryId) {
        if (!supportTicketPortOut.existsActiveCategoryById(categoryId)) {
            throw new NotFoundException("Active support ticket category not found");
        }
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    private void notifyTicketCreated(SupportTicket supportTicket) {
        if (notificationPortIn == null) {
            return;
        }
        sendCustomerNotification(
                supportTicket,
                NotificationType.SUPPORT_TICKET_CREATED,
                "Ticket hỗ trợ đã được tạo",
                "Ticket hỗ trợ của bạn đã được ghi nhận."
        );
        notificationPortIn.sendBroadcastWebNotification(new BroadcastNotificationCommand(
                false,
                NotificationAudience.OPERATIONS,
                null,
                null,
                NotificationType.SUPPORT_TICKET_CREATED,
                "Ticket hỗ trợ mới",
                "Có ticket hỗ trợ mới cần tiếp nhận: " + supportTicket.getTitle(),
                null,
                "operations",
                "support_tickets",
                supportTicket.getSupportTicketId()
        ));
    }

    private void notifyTicketAssigned(SupportTicket supportTicket) {
        if (notificationPortIn == null) {
            return;
        }
        notificationPortIn.sendWebNotification(new SendNotificationCommand(
                supportTicket.getAssignedTo(),
                NotificationType.SUPPORT_TICKET_ASSIGNED,
                "Bạn được giao ticket",
                "Bạn vừa được giao xử lý ticket: " + supportTicket.getTitle(),
                "operations",
                "support_tickets",
                supportTicket.getSupportTicketId()
        ));
    }

    private void notifyTicketStatusChanged(
            SupportTicket supportTicket,
            NotificationType notificationType,
            String title,
            String message
    ) {
        sendCustomerNotification(supportTicket, notificationType, title, message);
    }

    private void sendCustomerNotification(
            SupportTicket supportTicket,
            NotificationType notificationType,
            String title,
            String message
    ) {
        if (notificationPortIn == null) {
            return;
        }
        customerPortOut.findAccountIdByCustomerId(supportTicket.getCustomerId())
                .ifPresent(accountId -> notificationPortIn.sendWebNotification(new SendNotificationCommand(
                        accountId,
                        notificationType,
                        title,
                        message,
                        "operations",
                        "support_tickets",
                        supportTicket.getSupportTicketId()
                )));
    }
}
