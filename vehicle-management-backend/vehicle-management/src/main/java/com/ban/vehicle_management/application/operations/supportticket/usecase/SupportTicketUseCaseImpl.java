package com.ban.vehicle_management.application.operations.supportticket.usecase;

import com.ban.vehicle_management.application.operations.supportticket.authorization.SupportTicketAccessGuard;
import com.ban.vehicle_management.application.operations.supportticket.port.in.SupportTicketPortIn;
import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketPortOut;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.domain.operations.supportticket.policy.SupportTicketPolicy;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
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
    private final SupportTicketPolicy supportTicketPolicy = new SupportTicketPolicy();

    public SupportTicketUseCaseImpl(
            SupportTicketPortOut supportTicketPortOut,
            SupportTicketAccessGuard accessGuard
    ) {
        this.supportTicketPortOut = supportTicketPortOut;
        this.accessGuard = accessGuard;
    }

    @Override
    @Transactional
    public SupportTicket createTicket(SupportTicket supportTicket) {
        UUID customerId = accessGuard.resolveCustomerIdForCreate();
        supportTicket.setCustomerId(customerId);

        validateActiveCategory(supportTicket.getCategoryId());

        supportTicketPolicy.initialize(supportTicket);
        supportTicket.setSupportTicketId(UUID.randomUUID());

        return supportTicketPortOut.save(supportTicket);
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
        return supportTicketPortOut.save(existingTicket);
    }

    @Override
    @Transactional
    public SupportTicket startProgress(UUID supportTicketId) {
        SupportTicket existingTicket = findTicketOrThrow(supportTicketId);
        accessGuard.ensureCanProcess(existingTicket);

        supportTicketPolicy.startProgress(existingTicket);
        return supportTicketPortOut.save(existingTicket);
    }

    @Override
    @Transactional
    public SupportTicket resolveTicket(UUID supportTicketId, String resolutionNote) {
        SupportTicket existingTicket = findTicketOrThrow(supportTicketId);
        accessGuard.ensureCanProcess(existingTicket);

        supportTicketPolicy.resolve(existingTicket, resolutionNote, Instant.now());
        return supportTicketPortOut.save(existingTicket);
    }

    @Override
    @Transactional
    public SupportTicket reopenTicket(UUID supportTicketId) {
        SupportTicket existingTicket = findTicketOrThrow(supportTicketId);
        accessGuard.ensureCanReopen(existingTicket);

        supportTicketPolicy.reopen(existingTicket, Instant.now());
        return supportTicketPortOut.save(existingTicket);
    }

    @Override
    @Transactional
    public SupportTicket closeTicket(UUID supportTicketId) {
        SupportTicket existingTicket = findTicketOrThrow(supportTicketId);
        UUID closedBy = accessGuard.resolveClosedByForClose(existingTicket);

        supportTicketPolicy.close(existingTicket, closedBy, Instant.now());
        return supportTicketPortOut.save(existingTicket);
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
}