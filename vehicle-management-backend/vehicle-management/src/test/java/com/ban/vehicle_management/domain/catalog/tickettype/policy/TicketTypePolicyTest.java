package com.ban.vehicle_management.domain.catalog.tickettype.policy;

import com.ban.vehicle_management.domain.catalog.tickettype.model.TicketType;
import com.ban.vehicle_management.shared.enumeration.catalog.TicketTypeStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TicketTypePolicyTest {

    private final TicketTypePolicy ticketTypePolicy = new TicketTypePolicy();

    @Test
    void shouldNormalizeAndInitializeTicketType() {
        TicketType ticketType = new TicketType();
        ticketType.setCode(" monthly ");
        ticketType.setName(" Ve thang ");

        ticketTypePolicy.initialize(ticketType);

        assertEquals("MONTHLY", ticketType.getCode());
        assertEquals("Ve thang", ticketType.getName());
        assertEquals(30, ticketType.getDurationDays());
        assertEquals(TicketTypeStatus.ACTIVE, ticketType.getStatus());
    }

    @Test
    void shouldSetDurationByCode() {
        assertDuration("DAILY", 1);
        assertDuration("MONTHLY", 30);
        assertDuration("QUARTERLY", 90);
        assertDuration("YEARLY", 365);
        assertDuration("FREE", 180);
    }

    @Test
    void shouldRejectUnsupportedCode() {
        TicketType ticketType = new TicketType();
        ticketType.setCode("WEEKLY");
        ticketType.setName("Ve tuan");

        assertThrows(BadRequestException.class, () -> ticketTypePolicy.initialize(ticketType));
    }

    @Test
    void shouldRejectBlankName() {
        TicketType ticketType = new TicketType();
        ticketType.setCode("DAILY");
        ticketType.setName(" ");

        assertThrows(BadRequestException.class, () -> ticketTypePolicy.initialize(ticketType));
    }

    @Test
    void shouldActivateTicketType() {
        TicketType ticketType = new TicketType();
        ticketType.setCode("FREE");
        ticketType.setName("Ve mien phi");
        ticketType.setStatus(TicketTypeStatus.INACTIVE);

        ticketTypePolicy.activate(ticketType);

        assertEquals(TicketTypeStatus.ACTIVE, ticketType.getStatus());
        assertEquals(180, ticketType.getDurationDays());
    }

    @Test
    void shouldDeactivateTicketType() {
        TicketType ticketType = new TicketType();
        ticketType.setCode("YEARLY");
        ticketType.setName("Ve nam");
        ticketType.setStatus(TicketTypeStatus.ACTIVE);

        ticketTypePolicy.deactivate(ticketType);

        assertEquals(TicketTypeStatus.INACTIVE, ticketType.getStatus());
        assertEquals(365, ticketType.getDurationDays());
    }

    private void assertDuration(String code, int expectedDuration) {
        TicketType ticketType = new TicketType();
        ticketType.setCode(code);
        ticketType.setName(code);

        ticketTypePolicy.initialize(ticketType);

        assertEquals(expectedDuration, ticketType.getDurationDays());
    }
}