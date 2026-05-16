package com.ban.vehicle_management.application.accesscontrol.card.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.accesscontrol.card.port.out.CardPort;
import com.ban.vehicle_management.application.catalog.cardtype.port.out.CardTypePort;
import com.ban.vehicle_management.application.catalog.vehicletype.port.out.VehicleTypePort;
import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.domain.catalog.cardtype.model.CardType;
import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import com.ban.vehicle_management.shared.enumeration.CardStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
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
class CardUseCaseImplTest {

    @Mock
    private CardPort cardPort;

    @Mock
    private CardTypePort cardTypePort;

    @Mock
    private VehicleTypePort vehicleTypePort;

    @InjectMocks
    private CardUseCaseImpl cardUseCase;

    @Test
    void shouldCreateCardWithDefaultAvailableStatus() {
        UUID cardTypeId = UUID.randomUUID();
        UUID vehicleTypeId = UUID.randomUUID();
        Card requestCard = new Card();
        requestCard.setCardNumber(" C001 ");
        requestCard.setUid(" UID-001 ");
        requestCard.setCardTypeId(cardTypeId);
        requestCard.setVehicleTypeId(vehicleTypeId);

        when(cardTypePort.findById(cardTypeId)).thenReturn(Optional.of(new CardType()));
        when(vehicleTypePort.findById(vehicleTypeId)).thenReturn(Optional.of(new VehicleType()));
        when(cardPort.existsByCardNumber("C001")).thenReturn(false);
        when(cardPort.existsByUid("UID-001")).thenReturn(false);
        when(cardPort.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Card createdCard = cardUseCase.createCard(requestCard);

        assertNotNull(createdCard.getCardId());
        assertEquals("C001", createdCard.getCardNumber());
        assertEquals("UID-001", createdCard.getUid());
        assertEquals(CardStatus.AVAILABLE, createdCard.getStatus());
    }

    @Test
    void shouldRejectCreateWhenCardTypeDoesNotExist() {
        UUID cardTypeId = UUID.randomUUID();
        Card requestCard = new Card();
        requestCard.setCardNumber("C001");
        requestCard.setUid("UID-001");
        requestCard.setCardTypeId(cardTypeId);

        when(cardTypePort.findById(cardTypeId)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> cardUseCase.createCard(requestCard));
        verify(cardPort, never()).save(any(Card.class));
    }

    @Test
    void shouldReturnFilteredCards() {
        UUID cardTypeId = UUID.randomUUID();
        UUID vehicleTypeId = UUID.randomUUID();
        when(cardPort.findAll(CardStatus.AVAILABLE, cardTypeId, vehicleTypeId, "C001"))
                .thenReturn(List.of(new Card(), new Card()));

        List<Card> cards = cardUseCase.getCards(CardStatus.AVAILABLE, cardTypeId, vehicleTypeId, " C001 ");

        assertEquals(2, cards.size());
        verify(cardPort).findAll(CardStatus.AVAILABLE, cardTypeId, vehicleTypeId, "C001");
    }

    @Test
    void shouldRejectUpdateWhenCardIsInUse() {
        UUID cardId = UUID.randomUUID();
        Card existingCard = existingCard(cardId, CardStatus.IN_USE);
        Card requestCard = updateRequest(existingCard.getCardTypeId(), existingCard.getVehicleTypeId());

        when(cardPort.findById(cardId)).thenReturn(Optional.of(existingCard));

        assertThrows(BadRequestException.class, () -> cardUseCase.updateCard(cardId, requestCard));
        verify(cardPort, never()).save(any(Card.class));
    }

    @Test
    void shouldRejectSensitiveUpdateAfterOperationalHistoryExists() {
        UUID cardId = UUID.randomUUID();
        Card existingCard = existingCard(cardId, CardStatus.AVAILABLE);
        Card requestCard = updateRequest(existingCard.getCardTypeId(), existingCard.getVehicleTypeId());
        requestCard.setCardNumber("C999");

        when(cardPort.findById(cardId)).thenReturn(Optional.of(existingCard));
        when(cardPort.hasOperationalHistory(cardId)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> cardUseCase.updateCard(cardId, requestCard));
        verify(cardPort, never()).save(any(Card.class));
    }

    @Test
    void shouldUpdateCardWhenMaintenanceDataIsAllowed() {
        UUID cardId = UUID.randomUUID();
        UUID cardTypeId = UUID.randomUUID();
        UUID vehicleTypeId = UUID.randomUUID();
        Card existingCard = existingCard(cardId, CardStatus.AVAILABLE);
        Card requestCard = new Card();
        requestCard.setCardNumber(" C001 ");
        requestCard.setUid(" UID-001 ");
        requestCard.setCardTypeId(cardTypeId);
        requestCard.setVehicleTypeId(vehicleTypeId);

        when(cardPort.findById(cardId)).thenReturn(Optional.of(existingCard));
        when(cardTypePort.findById(cardTypeId)).thenReturn(Optional.of(new CardType()));
        when(vehicleTypePort.findById(vehicleTypeId)).thenReturn(Optional.of(new VehicleType()));
        when(cardPort.existsByCardNumberAndCardIdNot("C001", cardId)).thenReturn(false);
        when(cardPort.existsByUidAndCardIdNot("UID-001", cardId)).thenReturn(false);
        when(cardPort.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Card updatedCard = cardUseCase.updateCard(cardId, requestCard);

        assertEquals("C001", updatedCard.getCardNumber());
        assertEquals("UID-001", updatedCard.getUid());
        assertEquals(cardTypeId, updatedCard.getCardTypeId());
        assertEquals(vehicleTypeId, updatedCard.getVehicleTypeId());
    }

    @Test
    void shouldRetireCardOnDeleteWhenSafe() {
        UUID cardId = UUID.randomUUID();
        Card existingCard = existingCard(cardId, CardStatus.AVAILABLE);

        when(cardPort.findById(cardId)).thenReturn(Optional.of(existingCard));
        when(cardPort.hasActiveUsage(cardId)).thenReturn(false);
        when(cardPort.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        cardUseCase.deleteCard(cardId);

        assertEquals(CardStatus.RETIRED, existingCard.getStatus());
        verify(cardPort).save(existingCard);
    }

    @Test
    void shouldRejectDeleteWhenCardHasActiveUsage() {
        UUID cardId = UUID.randomUUID();
        Card existingCard = existingCard(cardId, CardStatus.ASSIGNED);

        when(cardPort.findById(cardId)).thenReturn(Optional.of(existingCard));
        when(cardPort.hasActiveUsage(cardId)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> cardUseCase.deleteCard(cardId));
        verify(cardPort, never()).save(any(Card.class));
    }

    @Test
    void shouldBlockCardThroughStatusUseCase() {
        UUID cardId = UUID.randomUUID();
        Card existingCard = existingCard(cardId, CardStatus.AVAILABLE);

        when(cardPort.findById(cardId)).thenReturn(Optional.of(existingCard));
        when(cardPort.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Card updatedCard = cardUseCase.changeCardStatus(cardId, CardStatus.BLOCKED, "Security review");

        assertEquals(CardStatus.BLOCKED, updatedCard.getStatus());
        assertEquals("Security review", updatedCard.getBlockedReason());
        assertNotNull(updatedCard.getBlockedAt());
    }

    @Test
    void shouldRejectUnsupportedAvailableTransitionFromNonBlockedCard() {
        UUID cardId = UUID.randomUUID();
        Card existingCard = existingCard(cardId, CardStatus.AVAILABLE);
        when(cardPort.findById(cardId)).thenReturn(Optional.of(existingCard));

        assertThrows(BadRequestException.class, () -> cardUseCase.changeCardStatus(cardId, CardStatus.AVAILABLE, null));
        verify(cardPort, never()).save(any(Card.class));
    }

    @Test
    void shouldThrowWhenCardDoesNotExist() {
        UUID cardId = UUID.randomUUID();
        when(cardPort.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> cardUseCase.getCardById(cardId));
    }

    @Test
    void shouldRejectDuplicateUidOnCreate() {
        UUID cardTypeId = UUID.randomUUID();
        Card requestCard = new Card();
        requestCard.setCardNumber("C001");
        requestCard.setUid("UID-001");
        requestCard.setCardTypeId(cardTypeId);

        when(cardTypePort.findById(cardTypeId)).thenReturn(Optional.of(new CardType()));
        when(cardPort.existsByCardNumber("C001")).thenReturn(false);
        when(cardPort.existsByUid("UID-001")).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> cardUseCase.createCard(requestCard));

        assertTrue(exception.getMessage().contains("uid"));
        verify(cardPort, never()).save(any(Card.class));
    }

    private Card existingCard(UUID cardId, CardStatus status) {
        UUID cardTypeId = UUID.randomUUID();
        UUID vehicleTypeId = UUID.randomUUID();

        Card existingCard = new Card();
        existingCard.setCardId(cardId);
        existingCard.setCardNumber("C001");
        existingCard.setUid("UID-001");
        existingCard.setCardTypeId(cardTypeId);
        existingCard.setVehicleTypeId(vehicleTypeId);
        existingCard.setStatus(status);
        return existingCard;
    }

    private Card updateRequest(UUID cardTypeId, UUID vehicleTypeId) {
        Card requestCard = new Card();
        requestCard.setCardNumber("C001");
        requestCard.setUid("UID-001");
        requestCard.setCardTypeId(cardTypeId);
        requestCard.setVehicleTypeId(vehicleTypeId);
        return requestCard;
    }
}
