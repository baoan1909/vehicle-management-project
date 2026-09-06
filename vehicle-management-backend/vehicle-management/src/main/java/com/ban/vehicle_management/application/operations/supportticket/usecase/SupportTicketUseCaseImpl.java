package com.ban.vehicle_management.application.operations.supportticket.usecase;

import com.ban.vehicle_management.application.operations.supportticket.authorization.SupportTicketAccessGuard;
import com.ban.vehicle_management.application.operations.supportticket.model.SupportTicketChatIntake;
import com.ban.vehicle_management.application.operations.supportticket.port.in.SupportTicketPortIn;
import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketConversationLinkPortOut;
import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketPortOut;
import com.ban.vehicle_management.application.operations.supportticket.service.SupportTicketConversationService;
import com.ban.vehicle_management.application.notification.notification.model.BroadcastNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.model.NotificationRecipientCriteria;
import com.ban.vehicle_management.application.notification.notification.model.SendNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.application.people.customer.port.out.CustomerPortOut;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.domain.operations.supportticket.policy.SupportTicketPolicy;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationType;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketSource;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import org.springframework.security.access.AccessDeniedException;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
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
    private final SupportTicketConversationLinkPortOut ticketConversationLinkPortOut;
    private final SupportTicketPolicy supportTicketPolicy = new SupportTicketPolicy();

    public SupportTicketUseCaseImpl(
            SupportTicketPortOut supportTicketPortOut,
            SupportTicketAccessGuard accessGuard,
            CustomerPortOut customerPortOut,
            NotificationPortIn notificationPortIn,
            SupportTicketConversationService ticketConversationService,
            SupportTicketConversationLinkPortOut ticketConversationLinkPortOut
    ) {
        this.supportTicketPortOut = supportTicketPortOut;
        this.accessGuard = accessGuard;
        this.customerPortOut = customerPortOut;
        this.notificationPortIn = notificationPortIn;
        this.ticketConversationService = ticketConversationService;
        this.ticketConversationLinkPortOut = ticketConversationLinkPortOut;
    }

    @Override
    @Transactional
    public SupportTicket createTicket(SupportTicket supportTicket, String idempotencyKey) {
        UUID customerId = accessGuard.resolveCustomerIdForCreate();
        supportTicket.setCustomerId(customerId);

        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        SupportTicket existing = findIdempotentTicket(customerId, normalizedKey);
        if (existing != null) {
            return existing;
        }

        validateActiveCategory(supportTicket.getCategoryId());

        supportTicketPolicy.initialize(supportTicket);
        supportTicket.setSupportTicketId(UUID.randomUUID());
        supportTicket.setSource(SupportTicketSource.CUSTOMER_PORTAL);
        supportTicket.setIdempotencyKey(normalizedKey);

        SupportTicket savedTicket = supportTicketPortOut.save(supportTicket);
        notifyTicketCreated(savedTicket);
        return savedTicket;
    }

    @Override
    @Transactional
    public SupportTicketChatIntake createChatIntake(SupportTicket supportTicket, String idempotencyKey) {
        UUID customerId = accessGuard.resolveCustomerIdForCreate();
        UUID customerAccountId = accessGuard.currentAccountId();
        supportTicket.setCustomerId(customerId);

        ChatConversation assistantConversation = ticketConversationService.openOrCreateAssistantConversation(
                customerId,
                customerAccountId
        );

        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        SupportTicket existing = findIdempotentTicket(customerId, normalizedKey);
        if (existing != null) {
            ticketConversationService.postTicketCardFromCustomer(existing, assistantConversation, customerAccountId);
            return new SupportTicketChatIntake(existing, assistantConversation, true);
        }

        validateActiveCategory(supportTicket.getCategoryId());

        supportTicketPolicy.initialize(supportTicket);
        supportTicket.setSupportTicketId(UUID.randomUUID());
        supportTicket.setSource(SupportTicketSource.ASSISTANT_CHAT);
        supportTicket.setSourceConversationId(assistantConversation.getConversationId());
        supportTicket.setIdempotencyKey(normalizedKey);
        SupportTicket savedTicket = supportTicketPortOut.save(supportTicket);
        ticketConversationService.postTicketCardFromCustomer(savedTicket, assistantConversation, customerAccountId);
        notifyTicketCreated(savedTicket);
        return new SupportTicketChatIntake(savedTicket, assistantConversation, false);
    }

    @Override
    @Transactional
    public ChatConversation openAssistantConversation() {
        UUID customerId = accessGuard.resolveCustomerIdForAssistant();
        return ticketConversationService.openOrCreateAssistantConversation(customerId, accessGuard.currentAccountId());
    }

    @Override
    @Transactional
    public SupportTicket createTicketFromConversation(
            SupportTicket supportTicket,
            UUID conversationId,
            String idempotencyKey
    ) {
        UUID customerAccountId = accessGuard.currentAccountId();
        ChatConversation conversation = ticketConversationService.getCustomerTicketOriginConversation(conversationId, customerAccountId);
        UUID customerId = conversation.getConversationType() == ChatConversationType.ASSISTANT_SUPPORT
                ? accessGuard.resolveCustomerIdForCreate()
                : accessGuard.resolveCustomerIdForCreateFromChat();
        UUID assignedAccountId = conversation.getConversationType() == ChatConversationType.CUSTOMER_DIRECT
                ? ticketConversationService.resolveCounterpartAccountId(conversation, customerAccountId)
                : null;
        supportTicket.setCustomerId(customerId);

        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        SupportTicket existing = findIdempotentTicket(customerId, normalizedKey);
        if (existing != null) {
            ticketConversationService.postTicketCardFromCustomer(existing, conversation, customerAccountId);
            return existing;
        }

        validateActiveCategory(supportTicket.getCategoryId());
        supportTicketPolicy.initialize(supportTicket);
        // Only a customer-to-staff direct conversation can infer an immediate assignee.
        supportTicket.setAssignedTo(assignedAccountId);
        supportTicket.setSupportTicketId(UUID.randomUUID());
        supportTicket.setSource(conversation.getConversationType() == ChatConversationType.ASSISTANT_SUPPORT
                ? SupportTicketSource.ASSISTANT_CHAT
                : SupportTicketSource.EMPLOYEE_CHAT);
        supportTicket.setSourceConversationId(conversationId);
        supportTicket.setIdempotencyKey(normalizedKey);

        SupportTicket savedTicket = supportTicketPortOut.save(supportTicket);
        ticketConversationService.postTicketCardFromCustomer(savedTicket, conversation, customerAccountId);
        notifyTicketCreated(savedTicket);
        if (assignedAccountId != null) {
            notifyTicketAssigned(savedTicket);
        }
        return savedTicket;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicket> getMyTickets(SupportTicketStatus status, String keyword) {
        UUID customerId = accessGuard.resolveCustomerIdForOwnTickets();
        return supportTicketPortOut.findAll(customerId, null, null, status, null, normalizeKeyword(keyword));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicket> getConversationTicketHistory(
            UUID conversationId,
            SupportTicketStatus status,
            String keyword
    ) {
        ChatConversation conversation = ticketConversationService.getConversationForHistory(conversationId);
        if (conversation.getConversationType() == ChatConversationType.ASSISTANT_SUPPORT) {
            UUID currentCustomerId = accessGuard.resolveCustomerIdForOwnTickets();
            if (!currentCustomerId.equals(conversation.getCustomerId())) {
                throw new AccessDeniedException("Access is denied");
            }
        } else if (conversation.getConversationType() != ChatConversationType.CUSTOMER_DIRECT) {
            throw new BadRequestException("Ticket history is only available for customer support conversations");
        }

        return supportTicketPortOut.findAll(
                        conversation.getCustomerId(), null, null, status, null, normalizeKeyword(keyword)
                ).stream()
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
    public SupportTicket shareTicketWithAssistant(UUID supportTicketId) {
        UUID customerId = accessGuard.resolveCustomerIdForAssistant();
        SupportTicket ticket = findTicketOrThrow(supportTicketId);
        if (!customerId.equals(ticket.getCustomerId())) {
            throw new AccessDeniedException("Access is denied");
        }
        UUID accountId = accessGuard.currentAccountId();
        ChatConversation assistantConversation = ticketConversationService.openOrCreateAssistantConversation(
                customerId, accountId
        );
        ticketConversationService.postTicketCardFromCustomer(ticket, assistantConversation, accountId);
        return ticket;
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

        UUID previousAssignee = existingTicket.getAssignedTo();
        supportTicketPolicy.assign(existingTicket, assignedTo);
        SupportTicket savedTicket = supportTicketPortOut.save(existingTicket);
        if (previousAssignee != null && !previousAssignee.equals(assignedTo)) {
            ticketConversationLinkPortOut.deactivate(savedTicket.getSupportTicketId());
            ticketConversationService.postAssistantTicketUpdate(
                    savedTicket,
                    "Yêu cầu của bạn đã được chuyển cho nhân viên phụ trách mới."
            );
        }
        notifyTicketAssigned(savedTicket);
        return savedTicket;
    }

    @Override
    @Transactional
    public SupportTicket claimTicket(UUID supportTicketId) {
        UUID accountId = accessGuard.resolveAccountIdForClaim();
        if (!supportTicketPortOut.existsAssignableAccountById(accountId)) {
            throw new NotFoundException("Assignable account not found");
        }
        SupportTicket existingTicket = findTicketOrThrow(supportTicketId);
        supportTicketPolicy.claim(existingTicket, accountId);
        SupportTicket savedTicket = supportTicketPortOut.save(existingTicket);
        ticketConversationService.postAssistantTicketUpdate(savedTicket, "Yêu cầu của bạn đang được xử lý.");
        notifyTicketAssigned(savedTicket);
        return savedTicket;
    }

    @Override
    @Transactional
    public SupportTicket startProgress(UUID supportTicketId) {
        throw new ConflictException(
                "Support ticket moves to IN_PROGRESS automatically when the assignee sends the first reply"
        );
    }

    @Override
    @Transactional
    public SupportTicket resolveTicket(UUID supportTicketId, String resolutionNote) {
        SupportTicket existingTicket = findTicketOrThrow(supportTicketId);
        accessGuard.ensureCanProcess(existingTicket);

        supportTicketPolicy.resolve(existingTicket, resolutionNote, Instant.now());
        SupportTicket savedTicket = supportTicketPortOut.save(existingTicket);
        ticketConversationService.postAssistantTicketUpdate(savedTicket, "Yêu cầu của bạn đã được giải quyết.");
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
        ticketConversationLinkPortOut.deactivate(savedTicket.getSupportTicketId());
        ticketConversationService.postAssistantTicketUpdate(savedTicket, "Yêu cầu của bạn đã được mở lại để tiếp tục xử lý.");
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
        ticketConversationService.postAssistantTicketUpdate(savedTicket, "Yêu cầu của bạn đã được đóng.");
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

    @Override
    @Transactional(readOnly = true)
    public ChatConversation getActiveCustomerConversation(UUID supportTicketId) {
        SupportTicket ticket = findTicketOrThrow(supportTicketId);
        UUID customerAccountId = accessGuard.ensureCanReadOwnCustomerConversation(ticket);
        return ticketConversationService.getActivePrivateConversation(ticket, customerAccountId);
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

    private SupportTicket findIdempotentTicket(UUID customerId, String idempotencyKey) {
        if (idempotencyKey == null) {
            return null;
        }
        supportTicketPortOut.lockCustomerSupport(customerId);
        return supportTicketPortOut.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey).orElse(null);
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        String normalized = idempotencyKey.trim();
        if (normalized.length() > 100) {
            throw new BadRequestException("Idempotency-Key must not exceed 100 characters");
        }
        return normalized;
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
                true,
                null,
                null,
                null,
                NotificationType.SUPPORT_TICKET_CREATED,
                "Ticket hỗ trợ mới",
                "Có ticket hỗ trợ mới cần tiếp nhận: " + supportTicket.getTitle(),
                null,
                "operations",
                "support_tickets",
                supportTicket.getSupportTicketId(),
                new NotificationRecipientCriteria(
                        true,
                        Set.of("SUPPORT_TICKET_READ_ALL", "SUPPORT_TICKET_ASSIGN"),
                        Set.of(),
                        true
                )
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
