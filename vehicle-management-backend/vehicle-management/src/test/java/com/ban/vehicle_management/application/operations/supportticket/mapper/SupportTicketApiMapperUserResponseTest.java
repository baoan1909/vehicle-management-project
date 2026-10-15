package com.ban.vehicle_management.application.operations.supportticket.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.entrypoint.dto.operations.supportticket.response.SupportTicketUserResponse;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class SupportTicketApiMapperUserResponseTest {

    private final SupportTicketApiMapper mapper = Mappers.getMapper(SupportTicketApiMapper.class);

    @Test
    void shouldMapUserResponseWithoutAuditFields() {
        SupportTicket ticket = new SupportTicket();
        ticket.setSupportTicketId(UUID.randomUUID());
        ticket.setCustomerId(UUID.randomUUID());
        ticket.setCategoryId(UUID.randomUUID());
        ticket.setCategoryName("Payment");
        ticket.setPriority(SupportTicketCategoryPriority.HIGH);
        ticket.setTitle("Help");
        ticket.setContent("Details");
        ticket.setStatus(SupportTicketStatus.OPEN);
        ticket.setAssignedTo(UUID.randomUUID());
        ticket.setCreatedBy(UUID.randomUUID());
        ticket.setUpdatedBy(UUID.randomUUID());

        SupportTicketUserResponse response = mapper.toUserResponse(ticket);

        assertEquals(ticket.getSupportTicketId(), response.getSupportTicketId());
        assertEquals("Payment", response.getCategoryName());
        assertEquals(SupportTicketCategoryPriority.HIGH, response.getPriority());
        assertEquals("Help", response.getTitle());
        assertEquals(SupportTicketStatus.OPEN, response.getStatus());
        assertEquals(ticket.getAssignedTo(), response.getAssignedTo());
    }

    @Test
    void shouldMapUserResponseList() {
        SupportTicket ticket = new SupportTicket();
        ticket.setSupportTicketId(UUID.randomUUID());
        ticket.setStatus(SupportTicketStatus.OPEN);

        List<SupportTicketUserResponse> responses = mapper.toUserResponses(List.of(ticket));

        assertEquals(1, responses.size());
        assertEquals(ticket.getSupportTicketId(), responses.get(0).getSupportTicketId());
    }
}
