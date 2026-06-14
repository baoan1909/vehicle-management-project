package com.ban.vehicle_management.application.catalog.tickettype.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.catalog.tickettype.port.out.TicketTypePortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.catalog.tickettype.model.TicketType;
import com.ban.vehicle_management.shared.enumeration.catalog.TicketTypeStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TicketTypeUsecaseImplTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private TicketTypePortOut ticketTypePortOut;

    @InjectMocks
    private TicketTypeUsecaseImpl ticketTypeUseCase;

    @Test
    void shouldCreateTicketTypeWithDurationDerivedFromCode() {
        TicketType requestTicketType = new TicketType();
        requestTicketType.setCode(" monthly ");
        requestTicketType.setName(" Monthly ticket ");
        requestTicketType.setDescription(" Valid monthly ");

        when(ticketTypePortOut.existsActiveByCode("MONTHLY")).thenReturn(false);
        when(ticketTypePortOut.save(any(TicketType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketType createdTicketType = ticketTypeUseCase.createTicketType(requestTicketType);

        verify(currentAccountPortIn).requirePermission("TICKET_TYPE_CREATE_ALL");
        assertEquals("MONTHLY", createdTicketType.getCode());
        assertEquals("Monthly ticket", createdTicketType.getName());
        assertEquals(30, createdTicketType.getDurationDays());
        assertEquals(TicketTypeStatus.ACTIVE, createdTicketType.getStatus());
    }

    @Test
    void shouldRejectDuplicateActiveCodeOnCreate() {
        TicketType requestTicketType = new TicketType();
        requestTicketType.setCode("DAILY");
        requestTicketType.setName("Daily ticket");

        when(ticketTypePortOut.existsActiveByCode("DAILY")).thenReturn(true);

        assertThrows(ConflictException.class, () -> ticketTypeUseCase.createTicketType(requestTicketType));
        verify(ticketTypePortOut, never()).save(any(TicketType.class));
    }

    @Test
    void shouldReturnFilteredTicketTypes() {
        when(ticketTypePortOut.findAll(TicketTypeStatus.ACTIVE, "daily"))
                .thenReturn(List.of(new TicketType(), new TicketType()));

        List<TicketType> ticketTypes = ticketTypeUseCase.getTicketTypes(TicketTypeStatus.ACTIVE, " daily ");

        verify(currentAccountPortIn).requirePermission("TICKET_TYPE_READ_ALL");
        assertEquals(2, ticketTypes.size());
        verify(ticketTypePortOut).findAll(TicketTypeStatus.ACTIVE, "daily");
    }

    @Test
    void shouldUpdateActiveTicketType() {
        UUID ticketTypeId = UUID.randomUUID();
        TicketType existingTicketType = ticketType(ticketTypeId, "DAILY", TicketTypeStatus.ACTIVE);
        TicketType requestTicketType = new TicketType();
        requestTicketType.setCode("MONTHLY");
        requestTicketType.setName("Monthly ticket");
        requestTicketType.setDescription("Updated");

        when(ticketTypePortOut.findById(ticketTypeId)).thenReturn(Optional.of(existingTicketType));
        when(ticketTypePortOut.hasActivePriceRules(ticketTypeId)).thenReturn(false);
        when(ticketTypePortOut.existsActiveByCodeAndTicketTypeIdNot("MONTHLY", ticketTypeId)).thenReturn(false);
        when(ticketTypePortOut.save(any(TicketType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketType updatedTicketType = ticketTypeUseCase.updateTicketType(ticketTypeId, requestTicketType);

        verify(currentAccountPortIn).requirePermission("TICKET_TYPE_UPDATE_ALL");
        assertEquals("MONTHLY", updatedTicketType.getCode());
        assertEquals(30, updatedTicketType.getDurationDays());
        assertEquals("Updated", updatedTicketType.getDescription());
    }

    @Test
    void shouldRejectUpdateWhenTicketTypeIsInactive() {
        UUID ticketTypeId = UUID.randomUUID();
        TicketType existingTicketType = ticketType(ticketTypeId, "DAILY", TicketTypeStatus.INACTIVE);
        TicketType requestTicketType = ticketType(null, "MONTHLY", TicketTypeStatus.ACTIVE);

        when(ticketTypePortOut.findById(ticketTypeId)).thenReturn(Optional.of(existingTicketType));

        assertThrows(BadRequestException.class, () -> ticketTypeUseCase.updateTicketType(ticketTypeId, requestTicketType));
        verify(ticketTypePortOut, never()).save(any(TicketType.class));
    }

    @Test
    void shouldDeactivateTicketTypeWhenSafe() {
        UUID ticketTypeId = UUID.randomUUID();
        TicketType existingTicketType = ticketType(ticketTypeId, "DAILY", TicketTypeStatus.ACTIVE);

        when(ticketTypePortOut.findById(ticketTypeId)).thenReturn(Optional.of(existingTicketType));
        when(ticketTypePortOut.hasActivePriceRules(ticketTypeId)).thenReturn(false);
        when(ticketTypePortOut.hasBlockingSubcriptions(ticketTypeId)).thenReturn(false);
        when(ticketTypePortOut.save(any(TicketType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ticketTypeUseCase.deleteTicketType(ticketTypeId);

        verify(currentAccountPortIn).requirePermission("TICKET_TYPE_DELETE_ALL");
        assertEquals(TicketTypeStatus.INACTIVE, existingTicketType.getStatus());
        verify(ticketTypePortOut).save(existingTicketType);
    }

    @Test
    void shouldRejectDeactivateWhenBlockingSubscriptionsExist() {
        UUID ticketTypeId = UUID.randomUUID();
        TicketType existingTicketType = ticketType(ticketTypeId, "DAILY", TicketTypeStatus.ACTIVE);

        when(ticketTypePortOut.findById(ticketTypeId)).thenReturn(Optional.of(existingTicketType));
        when(ticketTypePortOut.hasActivePriceRules(ticketTypeId)).thenReturn(false);
        when(ticketTypePortOut.hasBlockingSubcriptions(ticketTypeId)).thenReturn(true);

        assertThrows(ConflictException.class, () -> ticketTypeUseCase.deleteTicketType(ticketTypeId));
        verify(ticketTypePortOut, never()).save(any(TicketType.class));
    }

    @Test
    void shouldActivateInactiveTicketType() {
        UUID ticketTypeId = UUID.randomUUID();
        TicketType existingTicketType = ticketType(ticketTypeId, "YEARLY", TicketTypeStatus.INACTIVE);

        when(ticketTypePortOut.findById(ticketTypeId)).thenReturn(Optional.of(existingTicketType));
        when(ticketTypePortOut.existsActiveByCodeAndTicketTypeIdNot("YEARLY", ticketTypeId)).thenReturn(false);
        when(ticketTypePortOut.save(any(TicketType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketType activatedTicketType = ticketTypeUseCase.activateTicketType(ticketTypeId);

        verify(currentAccountPortIn).requirePermission("TICKET_TYPE_UPDATE_ALL");
        assertEquals(TicketTypeStatus.ACTIVE, activatedTicketType.getStatus());
        assertEquals(365, activatedTicketType.getDurationDays());
    }

    @Test
    void shouldThrowWhenTicketTypeDoesNotExist() {
        UUID ticketTypeId = UUID.randomUUID();
        when(ticketTypePortOut.findById(ticketTypeId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> ticketTypeUseCase.getTicketTypeById(ticketTypeId));
    }

    private TicketType ticketType(UUID ticketTypeId, String code, TicketTypeStatus status) {
        TicketType ticketType = new TicketType();
        ticketType.setTicketTypeId(ticketTypeId);
        ticketType.setCode(code);
        ticketType.setName(code + " ticket");
        ticketType.setDescription(code + " description");
        ticketType.setDurationDays(1);
        ticketType.setStatus(status);
        return ticketType;
    }
}
