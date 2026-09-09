package com.ban.vehicle_management.application.operations.approvalrequest.usecase;

import com.ban.vehicle_management.application.notification.notification.model.BroadcastNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.model.NotificationRecipientCriteria;
import com.ban.vehicle_management.application.notification.notification.model.SendNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.application.operations.approvalrequest.model.command.CreateSupportTicketEscalationCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.command.ReviewSupportTicketEscalationCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.SupportTicketEscalationResult;
import com.ban.vehicle_management.application.operations.approvalrequest.port.in.SupportTicketEscalationPortIn;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.SupportTicketEscalationPortOut;
import com.ban.vehicle_management.application.operations.supportticket.authorization.SupportTicketAccessGuard;
import com.ban.vehicle_management.application.operations.supportticket.port.in.SupportTicketPortIn;
import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketPortOut;
import com.ban.vehicle_management.application.operations.supportticket.service.SupportTicketConversationService;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.domain.operations.approvalrequest.policy.ApprovalRequestPolicy;
import com.ban.vehicle_management.domain.operations.approvalrequest.policy.SupportTicketEscalationPolicy;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.infrastructure.persistence.adapter.operations.SupportTicketEscalationPersistenceAdapter;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketEscalationDecision;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketEscalationReason;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class SupportTicketEscalationUseCaseImpl implements SupportTicketEscalationPortIn {

    private static final int MAX_REQUESTS_PER_DAY = 3;
    private static final String REVIEW_PERMISSION = "SUPPORT_TICKET_ESCALATION_REVIEW_ALL";

    private final SupportTicketEscalationPortOut escalationPortOut;
    private final SupportTicketPortOut supportTicketPortOut;
    private final SupportTicketPortIn supportTicketPortIn;
    private final SupportTicketAccessGuard accessGuard;
    private final SupportTicketConversationService conversationService;
    private final NotificationPortIn notificationPortIn;
    private final ApprovalRequestPolicy approvalRequestPolicy = new ApprovalRequestPolicy();
    private final SupportTicketEscalationPolicy escalationPolicy = new SupportTicketEscalationPolicy();
    private final Clock clock;

    @Autowired
    public SupportTicketEscalationUseCaseImpl(
            SupportTicketEscalationPortOut escalationPortOut,
            SupportTicketPortOut supportTicketPortOut,
            SupportTicketPortIn supportTicketPortIn,
            SupportTicketAccessGuard accessGuard,
            SupportTicketConversationService conversationService,
            NotificationPortIn notificationPortIn
    ) {
        this(escalationPortOut, supportTicketPortOut, supportTicketPortIn, accessGuard,
                conversationService, notificationPortIn, Clock.systemUTC());
    }

    SupportTicketEscalationUseCaseImpl(
            SupportTicketEscalationPortOut escalationPortOut,
            SupportTicketPortOut supportTicketPortOut,
            SupportTicketPortIn supportTicketPortIn,
            SupportTicketAccessGuard accessGuard,
            SupportTicketConversationService conversationService,
            NotificationPortIn notificationPortIn,
            Clock clock
    ) {
        this.escalationPortOut = escalationPortOut;
        this.supportTicketPortOut = supportTicketPortOut;
        this.supportTicketPortIn = supportTicketPortIn;
        this.accessGuard = accessGuard;
        this.conversationService = conversationService;
        this.notificationPortIn = notificationPortIn;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SupportTicketEscalationResult create(
            UUID supportTicketId,
            CreateSupportTicketEscalationCommand command
    ) {
        SupportTicket ticket = findTicket(supportTicketId);
        UUID requestedBy = accessGuard.ensureCanCreateOrReadOwnEscalation(ticket);
        supportTicketPortOut.lockCustomerSupport(ticket.getCustomerId());

        String idempotencyKey = TextValidationUtils.normalizeNullableText(
                command == null ? null : command.idempotencyKey(), "idempotencyKey", 100
        );
        Optional<ApprovalRequest> replay = escalationPortOut.findByRequesterAndIdempotencyKey(
                requestedBy, idempotencyKey
        );
        if (replay.isPresent()) {
            if (!supportTicketId.equals(replay.get().getTargetId())) {
                throw new ConflictException("Idempotency key was already used for another support ticket");
            }
            return toResult(replay.get());
        }

        if (escalationPortOut.findPendingByTicketId(supportTicketId).isPresent()) {
            throw new ConflictException("A support ticket escalation is already pending");
        }
        if (escalationPortOut.countRecentByRequester(
                requestedBy, Instant.now(clock).minus(24, ChronoUnit.HOURS)
        ) >= MAX_REQUESTS_PER_DAY) {
            throw new ConflictException("Too many support ticket escalation requests. Please try again later");
        }

        SupportTicketEscalationReason reason = command == null ? null : command.reasonCode();
        String description = escalationPolicy.validateCreate(
                ticket, reason, command == null ? null : command.description()
        );

        ApprovalRequest escalation = new ApprovalRequest();
        escalation.setApprovalRequestId(UUID.randomUUID());
        escalation.setRequestType(SupportTicketEscalationPersistenceAdapter.REQUEST_TYPE);
        escalation.setTargetSchema(SupportTicketEscalationPersistenceAdapter.TARGET_SCHEMA);
        escalation.setTargetTable(SupportTicketEscalationPersistenceAdapter.TARGET_TABLE);
        escalation.setTargetId(ticket.getSupportTicketId());
        escalation.setStatus(ApprovalRequestStatus.PENDING);
        escalation.setRequestedBy(requestedBy);
        escalation.setIdempotencyKey(idempotencyKey);
        escalation.setRequestData(Map.of(
                "reasonCode", reason.name(),
                "description", description,
                "currentAssigneeId", ticket.getAssignedTo().toString()
        ));
        approvalRequestPolicy.initialize(escalation);
        ApprovalRequest saved = escalationPortOut.save(escalation);

        conversationService.postAssistantTicketUpdate(
                ticket, "Yêu cầu quản lý xem xét của bạn đã được tiếp nhận. Người phụ trách hiện tại vẫn tiếp tục hỗ trợ trong thời gian chờ xử lý."
        );
        notifyReviewers(saved, ticket.getAssignedTo());
        return toResult(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SupportTicketEscalationResult> getMyCurrent(UUID supportTicketId) {
        SupportTicket ticket = findTicket(supportTicketId);
        accessGuard.ensureCanCreateOrReadOwnEscalation(ticket);
        return escalationPortOut.findPendingByTicketId(supportTicketId).map(this::toResult);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicketEscalationResult> getAll(ApprovalRequestStatus status) {
        UUID reviewerAccountId = accessGuard.ensureCanReviewEscalation();
        return escalationPortOut.findAll(status).stream()
                .filter(escalation -> !reviewerAccountId.equals(requestedAssignee(escalation)))
                .map(this::toResult)
                .toList();
    }

    @Override
    @Transactional
    public SupportTicketEscalationResult approve(
            UUID escalationId,
            ReviewSupportTicketEscalationCommand command
    ) {
        UUID reviewerAccountId = accessGuard.ensureCanReviewEscalation();
        ApprovalRequest escalation = findEscalationForUpdate(escalationId);
        ensureNotOriginallyAssignedReviewer(escalation, reviewerAccountId);
        SupportTicket ticket = findTicket(escalation.getTargetId());

        SupportTicketEscalationDecision decision = command == null ? null : command.decision();
        UUID assignedTo = command == null ? null : command.assignedTo();
        String note = escalationPolicy.validateReview(
                ticket, reviewerAccountId, decision, assignedTo, command == null ? null : command.note()
        );

        UUID reassignedTo = null;
        if (decision == SupportTicketEscalationDecision.REASSIGN) {
            ticket = supportTicketPortIn.assignTicket(ticket.getSupportTicketId(), assignedTo);
            reassignedTo = ticket.getAssignedTo();
        }

        Instant reviewedAt = Instant.now(clock);
        escalation.setDecisionData(decisionData(
                decision.name(), reviewerAccountId, reviewedAt, reassignedTo, note
        ));
        approvalRequestPolicy.approve(escalation, reviewerAccountId, reviewedAt, note);
        ApprovalRequest saved = escalationPortOut.save(escalation);

        String customerMessage = decision == SupportTicketEscalationDecision.REASSIGN
                ? "Quản lý đã chấp nhận yêu cầu và chuyển phiếu cho người phụ trách mới."
                : "Quản lý đã xem xét yêu cầu và giữ nguyên người phụ trách hiện tại. Lý do: " + note;
        conversationService.postAssistantTicketUpdate(ticket, customerMessage);
        notifyRequester(saved, customerMessage);
        return toResult(saved);
    }

    @Override
    @Transactional
    public SupportTicketEscalationResult reject(UUID escalationId, String note) {
        UUID reviewerAccountId = accessGuard.ensureCanReviewEscalation();
        ApprovalRequest escalation = findEscalationForUpdate(escalationId);
        ensureNotOriginallyAssignedReviewer(escalation, reviewerAccountId);
        SupportTicket ticket = findTicket(escalation.getTargetId());
        String normalizedNote = escalationPolicy.validateRejection(ticket, reviewerAccountId, note);
        Instant reviewedAt = Instant.now(clock);
        escalation.setDecisionData(decisionData(
                "REJECT", reviewerAccountId, reviewedAt, null, normalizedNote
        ));
        approvalRequestPolicy.reject(escalation, normalizedNote);
        ApprovalRequest saved = escalationPortOut.save(escalation);

        String customerMessage = "Quản lý đã xem xét nhưng chưa chấp nhận yêu cầu đổi người hỗ trợ. Lý do: "
                + normalizedNote;
        conversationService.postAssistantTicketUpdate(ticket, customerMessage);
        notifyRequester(saved, customerMessage);
        return toResult(saved);
    }

    private SupportTicket findTicket(UUID supportTicketId) {
        return supportTicketPortOut.findById(supportTicketId)
                .orElseThrow(() -> new NotFoundException("Support ticket not found"));
    }

    private ApprovalRequest findEscalationForUpdate(UUID escalationId) {
        ApprovalRequest escalation = escalationPortOut.findByIdForUpdate(escalationId)
                .orElseThrow(() -> new NotFoundException("Support ticket escalation not found"));
        if (escalation.getStatus() != ApprovalRequestStatus.PENDING) {
            throw new ConflictException("Support ticket escalation has already been reviewed");
        }
        return escalation;
    }

    private void ensureNotOriginallyAssignedReviewer(ApprovalRequest escalation, UUID reviewerAccountId) {
        if (reviewerAccountId.equals(requestedAssignee(escalation))) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "The assigned employee cannot review their own escalation"
            );
        }
    }

    private UUID requestedAssignee(ApprovalRequest escalation) {
        Map<String, String> request = escalation.getRequestData() == null ? Map.of() : escalation.getRequestData();
        return uuidValue(request.get("currentAssigneeId"));
    }

    private Map<String, String> decisionData(
            String decision,
            UUID reviewedBy,
            Instant reviewedAt,
            UUID reassignedTo,
            String note
    ) {
        Map<String, String> data = new HashMap<>();
        data.put("decision", decision);
        data.put("reviewedBy", reviewedBy.toString());
        data.put("reviewedAt", reviewedAt.toString());
        data.put("decisionNote", note);
        if (reassignedTo != null) {
            data.put("reassignedTo", reassignedTo.toString());
        }
        return Map.copyOf(data);
    }

    private SupportTicketEscalationResult toResult(ApprovalRequest escalation) {
        Map<String, String> request = escalation.getRequestData() == null ? Map.of() : escalation.getRequestData();
        Map<String, String> decision = escalation.getDecisionData() == null ? Map.of() : escalation.getDecisionData();
        return new SupportTicketEscalationResult(
                escalation.getApprovalRequestId(),
                escalation.getTargetId(),
                escalation.getStatus(),
                enumValue(SupportTicketEscalationReason.class, request.get("reasonCode")),
                request.get("description"),
                escalation.getRequestedBy(),
                uuidValue(request.get("currentAssigneeId")),
                escalation.getCreatedAt(),
                uuidValue(decision.get("reviewedBy")),
                instantValue(decision.get("reviewedAt")),
                enumValue(SupportTicketEscalationDecision.class, decision.get("decision")),
                uuidValue(decision.get("reassignedTo")),
                decision.get("decisionNote")
        );
    }

    private <E extends Enum<E>> E enumValue(Class<E> type, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private UUID uuidValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private Instant instantValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void notifyReviewers(ApprovalRequest escalation, UUID currentAssigneeId) {
        notificationPortIn.sendBroadcastWebNotification(new BroadcastNotificationCommand(
                true,
                null,
                null,
                null,
                NotificationType.SYSTEM_NOTICE,
                "Khách hàng yêu cầu xem xét hỗ trợ",
                "Có yêu cầu đổi người hỗ trợ cần quản lý xử lý.",
                "/admin/support-tickets?escalation=pending",
                "operations",
                "support_tickets",
                escalation.getTargetId(),
                new NotificationRecipientCriteria(
                        true,
                        Set.of(REVIEW_PERMISSION),
                        currentAssigneeId == null ? Set.of() : Set.of(currentAssigneeId),
                        true
                )
        ));
    }

    private void notifyRequester(ApprovalRequest escalation, String message) {
        notificationPortIn.sendWebNotification(new SendNotificationCommand(
                escalation.getRequestedBy(),
                NotificationType.SYSTEM_NOTICE,
                "Kết quả xem xét yêu cầu hỗ trợ",
                message,
                "/customer/support",
                "operations",
                "support_tickets",
                escalation.getTargetId()
        ));
    }
}
