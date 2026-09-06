package com.ban.vehicle_management.domain.operations.approvalrequest.policy;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketEscalationReason;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SupportTicketEscalationPolicyTest {

    private final SupportTicketEscalationPolicy policy = new SupportTicketEscalationPolicy();

    @Test
    void rejectsClosedOrUnassignedTickets() {
        SupportTicket closed = ticket(SupportTicketStatus.CLOSED, UUID.randomUUID());
        SupportTicket unassigned = ticket(SupportTicketStatus.OPEN, null);

        assertThrows(ConflictException.class, () -> policy.validateCreate(
                closed, SupportTicketEscalationReason.OTHER, "Không hài lòng với kết quả"
        ));
        assertThrows(ConflictException.class, () -> policy.validateCreate(
                unassigned, SupportTicketEscalationReason.RESPONSE_DELAY, "Chưa có người hỗ trợ"
        ));
    }

    @Test
    void rejectsRawMarkupAndControlCharactersInCustomerDescription() {
        SupportTicket ticket = ticket(SupportTicketStatus.OPEN, UUID.randomUUID());

        assertThrows(BadRequestException.class, () -> policy.validateCreate(
                ticket, SupportTicketEscalationReason.OTHER, "<script>alert(1)</script>"
        ));
        assertThrows(BadRequestException.class, () -> policy.validateCreate(
                ticket, SupportTicketEscalationReason.OTHER, "Nội dung\u0000không hợp lệ"
        ));
    }

    private SupportTicket ticket(SupportTicketStatus status, UUID assignee) {
        SupportTicket ticket = new SupportTicket();
        ticket.setStatus(status);
        ticket.setAssignedTo(assignee);
        return ticket;
    }
}
