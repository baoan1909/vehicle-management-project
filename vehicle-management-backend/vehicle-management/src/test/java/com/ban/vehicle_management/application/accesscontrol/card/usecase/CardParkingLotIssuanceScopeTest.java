package com.ban.vehicle_management.application.accesscontrol.card.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.accesscontrol.card.port.out.CardPortOut;
import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.catalog.cardtype.port.out.CardTypePortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.domain.catalog.cardtype.model.CardType;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardNumberSeries;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class CardParkingLotIssuanceScopeTest {

    @Mock private CurrentAccountPortIn currentAccountPortIn;
    @Mock private CardPortOut cardPort;
    @Mock private CardTypePortOut cardTypePort;
    @Mock private AuditLogPortOut auditLogPortOut;
    @Mock private OrganizationAccessGuard organizationAccessGuard;
    @Mock private ParkingLotPortOut parkingLotPortOut;
    @InjectMocks private CardUseCaseImpl useCase;

    @Test
    void partnerMustSelectLotEvenIfItOwnsOnlyOne() {
        UUID lotId = UUID.randomUUID();
        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut)).thenReturn(Set.of(lotId));
        when(currentAccountPortIn.hasPermission(anyString())).thenAnswer(invocation ->
                OrganizationAccessGuard.PARKING_SCOPE_PARTNER.equals(invocation.getArgument(0)));

        assertThrows(BadRequestException.class, () -> useCase.createCards(UUID.randomUUID(), 1, null));
    }

    @Test
    void managerSingleAssignedLotIsUsedByDefault() {
        UUID lotId = UUID.randomUUID();
        UUID cardTypeId = UUID.randomUUID();
        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut)).thenReturn(Set.of(lotId));
        when(currentAccountPortIn.hasPermission(OrganizationAccessGuard.PARKING_SCOPE_ASSIGNED)).thenReturn(true);
        stubCardCreation(cardTypeId);

        List<Card> created = useCase.createCards(cardTypeId, 1, null);

        assertEquals(lotId, created.getFirst().getParkingLotId());
        verify(cardPort).saveAll(anyList());
    }

    @Test
    void partnerCanIssueOnlyToSelectedOwnedLot() {
        UUID lotId = UUID.randomUUID();
        UUID cardTypeId = UUID.randomUUID();
        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut)).thenReturn(Set.of(lotId));
        stubCardCreation(cardTypeId);

        List<Card> created = useCase.createCards(cardTypeId, 1, lotId);

        assertEquals(lotId, created.getFirst().getParkingLotId());
    }

    @Test
    void managerCannotIssueCardToAnotherLot() {
        UUID assignedLotId = UUID.randomUUID();
        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut)).thenReturn(Set.of(assignedLotId));

        assertThrows(AccessDeniedException.class,
                () -> useCase.createCards(UUID.randomUUID(), 1, UUID.randomUUID()));
    }

    private void stubCardCreation(UUID cardTypeId) {
        CardType cardType = new CardType();
        cardType.setCode("VISITOR");
        when(cardTypePort.findById(cardTypeId)).thenReturn(Optional.of(cardType));
        when(cardPort.nextCardNumberSequence(CardNumberSeries.VISITOR)).thenReturn(1L);
        when(cardPort.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }
}
