package com.ban.vehicle_management.application.operations.supportticket.usecase;

import com.ban.vehicle_management.application.operations.supportticket.authorization.SupportTicketAccessGuard;
import com.ban.vehicle_management.application.operations.supportticket.model.SupportTicketChatIntake;
import com.ban.vehicle_management.application.operations.supportticket.model.SupportRequesterContext;
import com.ban.vehicle_management.application.operations.supportticket.port.in.SupportTicketPortIn;
import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketConversationLinkPortOut;
import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketPortOut;
import com.ban.vehicle_management.application.operations.supportticket.service.SupportTicketConversationService;
import com.ban.vehicle_management.application.notification.notification.model.BroadcastNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.model.NotificationRecipientCriteria;
import com.ban.vehicle_management.application.notification.notification.model.SendNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.application.operations.supportticketcategory.port.in.SupportTicketCategoryPortIn;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.domain.operations.supportticket.policy.SupportTicketPolicy;
import com.ban.vehicle_management.domain.operations.supportticketcategory.model.SupportTicketCategory;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationType;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketSource;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketEventType;
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
import com.ban.vehicle_management.shared.utils.TextValidationUtils;

@Service
public class SupportTicketUseCaseImpl implements SupportTicketPortIn {

    private final SupportTicketPortOut supportTicketPortOut;
    private final SupportTicketAccessGuard accessGuard;
    private final SupportTicketCategoryPortIn categoryPortIn;
    private final NotificationPortIn notificationPortIn;
    private final SupportTicketConversationService ticketConversationService;
    private final SupportTicketConversationLinkPortOut ticketConversationLinkPortOut;
    private final SupportTicketPolicy supportTicketPolicy = new SupportTicketPolicy();

    public SupportTicketUseCaseImpl(
            SupportTicketPortOut supportTicketPortOut,
            SupportTicketAccessGuard accessGuard,
            SupportTicketCategoryPortIn categoryPortIn,
            NotificationPortIn notificationPortIn,
            SupportTicketConversationService ticketConversationService,
            SupportTicketConversationLinkPortOut ticketConversationLinkPortOut
    ) {
        this.supportTicketPortOut = supportTicketPortOut;
        this.accessGuard = accessGuard;
        this.categoryPortIn = categoryPortIn;
        this.notificationPortIn = notificationPortIn;
        this.ticketConversationService = ticketConversationService;
        this.ticketConversationLinkPortOut = ticketConversationLinkPortOut;
    }

    @Override
    @Transactional
    public SupportTicket createTicket(SupportTicket supportTicket, String idempotencyKey) {
        SupportRequesterContext requester = accessGuard.requesterForCreate();

        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        SupportTicket existing = findIdempotentTicket(requester.accountId(), normalizedKey);
        if (existing != null) {
            return existing;
        }

        SupportTicketCategory category = categoryPortIn.ensureAvailableCategory(supportTicket.getCategoryId());
        initializeRequester(supportTicket, requester, category);
        supportTicketPolicy.initialize(supportTicket);
        supportTicket.setSupportTicketId(UUID.randomUUID());
        supportTicket.setSource(SupportTicketSource.SELF_SERVICE_PORTAL);
        supportTicket.setIdempotencyKey(normalizedKey);

        SupportTicket savedTicket = supportTicketPortOut.save(supportTicket);
        notifyTicketCreated(savedTicket);
        return savedTicket;
    }

    @Override
    @Transactional
    public SupportTicketChatIntake createChatIntake(SupportTicket supportTicket, String idempotencyKey) {
        SupportRequesterContext requester = accessGuard.requesterForCreate();

        ChatConversation assistantConversation = ticketConversationService.openOrCreateAssistantConversation(
                requester.accountId()
        );

        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        SupportTicket existing = findIdempotentTicket(requester.accountId(), normalizedKey);
        if (existing != null) {
            ticketConversationService.postTicketCardFromRequester(existing, assistantConversation, requester.accountId());
            return new SupportTicketChatIntake(existing, assistantConversation, true);
        }

        SupportTicketCategory category = categoryPortIn.ensureAvailableCategory(supportTicket.getCategoryId());
        initializeRequester(supportTicket, requester, category);
        supportTicketPolicy.initialize(supportTicket);
        supportTicket.setSupportTicketId(UUID.randomUUID());
        supportTicket.setSource(SupportTicketSource.ASSISTANT_CHAT);
        supportTicket.setSourceConversationId(assistantConversation.getConversationId());
        supportTicket.setIdempotencyKey(normalizedKey);
        SupportTicket savedTicket = supportTicketPortOut.save(supportTicket);
        ticketConversationService.postTicketCardFromRequester(savedTicket, assistantConversation, requester.accountId());
        notifyTicketCreated(savedTicket);
        return new SupportTicketChatIntake(savedTicket, assistantConversation, false);
    }

    @Override
    @Transactional
    public ChatConversation openAssistantConversation() {
        SupportRequesterContext requester = accessGuard.requesterForAssistant();
        return ticketConversationService.openOrCreateAssistantConversation(requester.accountId());
    }

    @Override
    @Transactional
    public SupportTicket createTicketFromConversation(
            SupportTicket supportTicket,
            UUID conversationId,
            String idempotencyKey
    ) {
        UUID requesterAccountId = accessGuard.currentAccountId();
        ChatConversation conversation = ticketConversationService.getRequesterTicketOriginConversation(
                conversationId, requesterAccountId
        );
        if (!requesterAccountId.equals(conversation.getOwnerAccountId())) {
            throw new AccessDeniedException("Only the requester can create a support ticket in this conversation");
        }
        SupportRequesterContext requester = conversation.getConversationType() == ChatConversationType.ASSISTANT_SUPPORT
                ? accessGuard.requesterForCreate()
                : accessGuard.requesterForCreateFromChat();
        UUID assignedAccountId = conversation.getConversationType() == ChatConversationType.SUPPORT_REQUESTER_DIRECT
                ? ticketConversationService.resolveCounterpartAccountId(conversation, requesterAccountId)
                : null;

        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        SupportTicket existing = findIdempotentTicket(requester.accountId(), normalizedKey);
        if (existing != null) {
            ticketConversationService.postTicketCardFromRequester(existing, conversation, requesterAccountId);
            return existing;
        }

        SupportTicketCategory category = categoryPortIn.ensureAvailableCategory(supportTicket.getCategoryId());
        initializeRequester(supportTicket, requester, category);
        supportTicketPolicy.initialize(supportTicket);
        supportTicket.setAssignedTo(assignedAccountId);
        supportTicket.setSupportTicketId(UUID.randomUUID());
        supportTicket.setSource(conversation.getConversationType() == ChatConversationType.ASSISTANT_SUPPORT
                ? SupportTicketSource.ASSISTANT_CHAT
                : SupportTicketSource.SUPPORT_CONVERSATION);
        supportTicket.setSourceConversationId(conversationId);
        supportTicket.setIdempotencyKey(normalizedKey);

        SupportTicket savedTicket = supportTicketPortOut.save(supportTicket);
        ticketConversationService.postTicketCardFromRequester(savedTicket, conversation, requesterAccountId);
        notifyTicketCreated(savedTicket);
        if (assignedAccountId != null) {
            notifyTicketAssigned(savedTicket);
        }
        return savedTicket;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicket> getMyTickets(SupportTicketStatus status, String keyword) {
        return getMyTickets(status, keyword, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicket> getMyTickets(SupportTicketStatus status, String keyword, Integer limit) {
        UUID requesterAccountId = accessGuard.requesterForOwnTickets().accountId();
        int resolvedLimit = limit == null ? 100 : Math.min(Math.max(limit, 1), 200);
        return supportTicketPortOut.findOwnTickets(requesterAccountId, status, normalizeKeyword(keyword), resolvedLimit);
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
            UUID currentAccountId = accessGuard.requesterForOwnTickets().accountId();
            if (!currentAccountId.equals(conversation.getOwnerAccountId())) {
                throw new AccessDeniedException("Access is denied");
            }
        } else if (conversation.getConversationType() != ChatConversationType.SUPPORT_REQUESTER_DIRECT) {
            throw new BadRequestException("Ticket history is only available for requester support conversations");
        }

        return supportTicketPortOut.findAll(
                        conversation.getOwnerAccountId(), null, null, null, null, null, null,
                        status, null, normalizeKeyword(keyword)
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
    public SupportTicket shareTicketWithAssistant(UUID supportTicketId, String idempotencyKey) {
        UUID requesterAccountId = accessGuard.requesterForAssistant().accountId();
        SupportTicket ticket = findTicketOrThrow(supportTicketId);
        if (!requesterAccountId.equals(ticket.getRequesterAccountId())) {
            throw new AccessDeniedException("Access is denied");
        }
        ChatConversation assistantConversation = ticketConversationService.openOrCreateAssistantConversation(
                requesterAccountId
        );
        ticketConversationService.repostTicketCardFromRequester(
                ticket,
                assistantConversation,
                requesterAccountId,
                snapshotIdempotencyKey(idempotencyKey, "assistant-share")
        );
        return ticket;
    }

    @Override
    @Transactional
    public SupportTicket shareTicketWithConversation(
            UUID supportTicketId,
            UUID conversationId,
            String idempotencyKey
    ) {
        SupportTicket ticket = findTicketOrThrow(supportTicketId);
        SupportTicketStatus previousStatus = ticket.getStatus();
        UUID actorAccountId = accessGuard.currentAccountId();
        ChatConversation conversation = ticketConversationService.getRequesterTicketOriginConversation(
                conversationId,
                actorAccountId
        );
        if (!ticket.getRequesterAccountId().equals(conversation.getOwnerAccountId())) {
            throw new AccessDeniedException("Access is denied");
        }

        if (actorAccountId.equals(ticket.getRequesterAccountId())) {
            accessGuard.ensureCanReadOwnRequesterConversation(ticket);
        } else {
            accessGuard.ensureCanShareAsAssignee(ticket);
            if (ticket.getStatus() == SupportTicketStatus.OPEN) {
                supportTicketPolicy.startProgress(ticket);
                if (ticket.getFirstRespondedAt() == null) {
                    ticket.setFirstRespondedAt(Instant.now());
                }
                ticket = supportTicketPortOut.save(ticket);
                ticketConversationService.postAssistantTicketUpdate(
                        ticket,
                        "Nhân viên phụ trách đã phản hồi yêu cầu của bạn."
                );
            }
        }

        SupportTicketEventType eventType = previousStatus == SupportTicketStatus.OPEN
                && ticket.getStatus() == SupportTicketStatus.IN_PROGRESS
                ? SupportTicketEventType.STARTED_PROCESSING
                : SupportTicketEventType.SHARED;
        ticketConversationService.repostTicketCardFromConversation(
                ticket,
                conversation,
                actorAccountId,
                eventType,
                previousStatus,
                snapshotIdempotencyKey(idempotencyKey, "conversation-share")
        );
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
            UUID requesterAccountId,
            String requesterRoleCode,
            UUID categoryId,
            UUID assignedTo,
            UUID organizationId,
            UUID parkingLotId,
            String featureCode,
            SupportTicketStatus status,
            SupportTicketCategoryPriority priority,
            String keyword
    ) {
        return supportTicketPortOut.findAll(
                        requesterAccountId, requesterRoleCode, categoryId, assignedTo,
                        organizationId, parkingLotId, featureCode, status, priority, normalizeKeyword(keyword)
                )
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

        SupportTicketCategory category = categoryPortIn.ensureAvailableCategory(supportTicket.getCategoryId());

        existingTicket.setCategoryId(supportTicket.getCategoryId());
        existingTicket.setFeatureCode(category.getFeatureCode());
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
        if (!supportTicketPortOut.claimIfAvailable(supportTicketId, accountId)) {
            SupportTicket current = findTicketOrThrow(supportTicketId);
            if (!accountId.equals(current.getAssignedTo())) {
                throw new ConflictException("Support ticket has already been claimed by another system administrator");
            }
            return current;
        }
        SupportTicket savedTicket = findTicketOrThrow(supportTicketId);
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
        UUID resolvedByAccountId = accessGuard.currentAccountId();

        if (existingTicket.getStatus() == SupportTicketStatus.RESOLVED) {
            return existingTicket;
        }

        SupportTicketStatus previousStatus = existingTicket.getStatus();

        supportTicketPolicy.resolve(existingTicket, resolutionNote, Instant.now());
        SupportTicket savedTicket = supportTicketPortOut.save(existingTicket);
        ticketConversationService.postTicketStatusCard(
                savedTicket,
                resolvedByAccountId,
                SupportTicketEventType.RESOLVED,
                previousStatus,
                lifecycleEventKey("resolved", savedTicket)
        );
        ticketConversationService.postAssistantTicketUpdate(savedTicket, "Yêu cầu của bạn đã được giải quyết.");
        notifyTicketStatusChanged(savedTicket, NotificationType.SUPPORT_TICKET_RESPONDED, "Ticket đã có phản hồi", "Ticket hỗ trợ của bạn đã được phản hồi và đánh dấu đã xử lý.");
        return savedTicket;
    }

    @Override
    @Transactional
    public SupportTicket reopenTicket(UUID supportTicketId) {
        SupportTicket existingTicket = findTicketOrThrow(supportTicketId);
        accessGuard.ensureCanReopen(existingTicket);
        UUID reopenedByAccountId = accessGuard.currentAccountId();

        if (existingTicket.getStatus() == SupportTicketStatus.OPEN
                || existingTicket.getStatus() == SupportTicketStatus.IN_PROGRESS) {
            return existingTicket;
        }

        SupportTicketStatus previousStatus = existingTicket.getStatus();

        supportTicketPolicy.reopen(existingTicket, Instant.now());
        SupportTicket savedTicket = supportTicketPortOut.save(existingTicket);
        ticketConversationService.repostReopenedTicketCard(
                savedTicket,
                reopenedByAccountId,
                previousStatus,
                lifecycleEventKey("reopened", savedTicket)
        );
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
        ticketConversationService.postTicketStatusCard(
                savedTicket,
                closedBy,
                SupportTicketEventType.CLOSED,
                SupportTicketStatus.RESOLVED,
                lifecycleEventKey("closed", savedTicket)
        );
        ticketConversationService.postAssistantTicketUpdate(savedTicket, "Yêu cầu của bạn đã được đóng.");
        notifyTicketStatusChanged(savedTicket, NotificationType.SUPPORT_TICKET_CLOSED, "Ticket đã đóng", "Ticket hỗ trợ của bạn đã được đóng.");
        return savedTicket;
    }

    @Override
    @Transactional
    public ChatConversation openRequesterConversationForReply(UUID supportTicketId, String idempotencyKey) {
        SupportTicket ticket = findTicketOrThrow(supportTicketId);
        UUID assignedAccountId = accessGuard.ensureCanReplyAsAssignee(ticket);
        SupportTicketStatus previousStatus = ticket.getStatus();
        if (ticket.getStatus() == SupportTicketStatus.OPEN) {
            supportTicketPolicy.startProgress(ticket);
            if (ticket.getFirstRespondedAt() == null) {
                ticket.setFirstRespondedAt(Instant.now());
            }
            ticket = supportTicketPortOut.save(ticket);
            ticketConversationService.postAssistantTicketUpdate(
                    ticket,
                    "Nhân viên phụ trách đã phản hồi yêu cầu của bạn."
            );
        }
        String snapshotKey = previousStatus == SupportTicketStatus.OPEN
                ? lifecycleEventKey("started", ticket)
                : snapshotIdempotencyKey(idempotencyKey, "open-conversation");
        return ticketConversationService.openRequesterConversationForReply(
                ticket,
                assignedAccountId,
                previousStatus,
                snapshotKey
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ChatConversation getActiveRequesterConversation(UUID supportTicketId) {
        SupportTicket ticket = findTicketOrThrow(supportTicketId);
        UUID requesterAccountId = accessGuard.ensureCanReadOwnRequesterConversation(ticket);
        return ticketConversationService.getActiveRequesterConversation(ticket, requesterAccountId);
    }

    private SupportTicket findTicketOrThrow(UUID supportTicketId) {
        return supportTicketPortOut.findById(supportTicketId)
                .orElseThrow(() -> new NotFoundException("Support ticket not found"));
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    private String snapshotIdempotencyKey(String idempotencyKey, String prefix) {
        String normalized = TextValidationUtils.normalizeNullableText(idempotencyKey, "idempotencyKey", 100);
        return normalized == null ? prefix + ":" + UUID.randomUUID() : normalized;
    }

    private String lifecycleEventKey(String prefix, SupportTicket ticket) {
        int reopenCount = ticket.getReopenCount() == null ? 0 : ticket.getReopenCount();
        return prefix + ":" + ticket.getSupportTicketId() + ":" + reopenCount;
    }

    private void initializeRequester(
            SupportTicket supportTicket,
            SupportRequesterContext requester,
            SupportTicketCategory category
    ) {
        accessGuard.ensureRequesterScope(
                requester, supportTicket.getOrganizationId(), supportTicket.getParkingLotId()
        );
        supportTicket.setRequesterAccountId(requester.accountId());
        supportTicket.setRequesterRoleCode(requester.roleCode());
        supportTicket.setRequesterNameSnapshot(requester.displayName());
        supportTicket.setRequesterEmailSnapshot(requester.email());
        supportTicket.setFeatureCode(category.getFeatureCode());
        supportTicket.setPriority(category.getPriority());
        supportTicket.setContextKey(TextValidationUtils.normalizeNullableText(
                supportTicket.getContextKey(), "contextKey", 255
        ));
    }

    private SupportTicket findIdempotentTicket(UUID requesterAccountId, String idempotencyKey) {
        if (idempotencyKey == null) {
            return null;
        }
        supportTicketPortOut.lockRequesterSupport(requesterAccountId);
        return supportTicketPortOut.findByRequesterAccountIdAndIdempotencyKey(
                requesterAccountId, idempotencyKey
        ).orElse(null);
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
        sendRequesterNotification(
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
                "Yêu cầu hỗ trợ mới",
                "Có yêu cầu hỗ trợ mới cần tiếp nhận: " + supportTicket.getTitle(),
                "/admin/support-tickets?ticketId=" + supportTicket.getSupportTicketId(),
                "operations",
                "support_tickets",
                supportTicket.getSupportTicketId(),
                new NotificationRecipientCriteria(
                        false,
                        Set.of(
                                "SUPPORT_TICKET_READ_ALL",
                                "SUPPORT_TICKET_ASSIGN",
                                "SUPPORT_TICKET_PROCESS_ALL"
                        ),
                        Set.of(supportTicket.getRequesterAccountId()),
                        false
                )
        ));
    }

    private void notifyTicketAssigned(SupportTicket supportTicket) {
        if (notificationPortIn == null || supportTicket.getAssignedTo() == null) {
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
        sendRequesterNotification(supportTicket, notificationType, title, message);
    }

    private void sendRequesterNotification(
            SupportTicket supportTicket,
            NotificationType notificationType,
            String title,
            String message
    ) {
        if (notificationPortIn == null) {
            return;
        }
        notificationPortIn.sendWebNotification(new SendNotificationCommand(
                supportTicket.getRequesterAccountId(),
                notificationType,
                title,
                message,
                "operations",
                "support_tickets",
                supportTicket.getSupportTicketId()
        ));
    }
}
