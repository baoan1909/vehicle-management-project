package com.ban.vehicle_management.domain.catalog.tickettype.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.catalog.tickettype.model.TicketType;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import org.junit.jupiter.api.Test;

class TicketTypePolicyTest {

    private final TicketTypePolicy ticketTypePolicy = new TicketTypePolicy();

    @Test
    void shouldInitializeTicketTypeWithDefaults() {
        TicketType ticketType = new TicketType();
        ticketType.setCode(" MONTHLY ");
        ticketType.setName(" Ve thang ");
        ticketType.setDurationDays(30);

        ticketTypePolicy.initialize(ticketType);

        assertEquals("MONTHLY", ticketType.getCode());
        assertEquals("Ve thang", ticketType.getName());
        assertEquals(30, ticketType.getDurationDays());
        assertEquals(Boolean.TRUE, ticketType.getIsActive());
    }

    @Test
    void shouldRejectNonPositiveDurationDays() {
        TicketType ticketType = new TicketType();
        ticketType.setCode("DAILY");
        ticketType.setName("Ve ngay");
        ticketType.setDurationDays(0);

        assertThrows(BadRequestException.class, () -> ticketTypePolicy.initialize(ticketType));
    }

    @Test
    void shouldRejectUnsupportedCharactersInTicketTypeName() {
        TicketType ticketType = new TicketType();
        ticketType.setCode("MONTHLY");
        ticketType.setName("Ve <thang>");

        assertThrows(BadRequestException.class, () -> ticketTypePolicy.initialize(ticketType));
    }
}

