package com.ban.vehicle_management.application.accesscontrol.card.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.accesscontrol.card.port.out.CardPortOut;
import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.catalog.cardtype.port.out.CardTypePortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
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
class CardPartnerScopeTest {

    @Mock private CurrentAccountPortIn currentAccountPortIn;
    @Mock private CardPortOut cardPort;
    @Mock private CardTypePortOut cardTypePort;
    @Mock private AuditLogPortOut auditLogPortOut;
    @Mock private OrganizationAccessGuard organizationAccessGuard;
    @Mock private ParkingLotPortOut parkingLotPortOut;

    @InjectMocks private CardUseCaseImpl cardUseCase;

    @Test
    void partnerCanReadCardFromAnyLotInItsOrganization() {
        UUID firstLotId = UUID.randomUUID();
        UUID secondLotId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        Card card = new Card();
        card.setParkingLotId(secondLotId);

        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut))
                .thenReturn(Set.of(firstLotId, secondLotId));
        when(cardPort.findById(cardId)).thenReturn(Optional.of(card));

        assertEquals(card, cardUseCase.getCardById(cardId));
    }

    @Test
    void partnerCannotReadCardFromAnotherOrganization() {
        UUID ownLotId = UUID.randomUUID();
        UUID otherLotId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        Card card = new Card();
        card.setParkingLotId(otherLotId);

        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut))
                .thenReturn(Set.of(ownLotId));
        when(cardPort.findById(cardId)).thenReturn(Optional.of(card));

        assertThrows(AccessDeniedException.class, () -> cardUseCase.getCardById(cardId));
    }

    @Test
    void partnerCardListQueriesOnlyItsParkingLots() {
        Set<UUID> ownLots = Set.of(UUID.randomUUID(), UUID.randomUUID());
        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut)).thenReturn(ownLots);

        cardUseCase.getCards(CardStatus.AVAILABLE, null, null);

        verify(cardPort).findAll(CardStatus.AVAILABLE, null, null, ownLots);
    }

}
