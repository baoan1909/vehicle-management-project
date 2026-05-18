package com.ban.vehicle_management.application.catalog.cardtype.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.catalog.cardtype.port.out.CardTypePortOut;
import com.ban.vehicle_management.domain.catalog.cardtype.model.CardType;
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
class CardTypeUseCaseImplTest {

    @Mock
    private CardTypePortOut cardTypePort;

    @InjectMocks
    private CardTypeUseCaseImpl cardTypeUseCase;

    @Test
    void shouldCreateCardTypeWithDefaultReturnRequiredFlag() {
        CardType requestCardType = new CardType();
        requestCardType.setCode(" RFID ");
        requestCardType.setName(" RFID Card ");
        requestCardType.setDescription(" Reusable access card ");

        when(cardTypePort.existsByCode("RFID")).thenReturn(false);
        when(cardTypePort.save(any(CardType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CardType createdCardType = cardTypeUseCase.createCardType(requestCardType);

        assertEquals("RFID", createdCardType.getCode());
        assertEquals("RFID Card", createdCardType.getName());
        assertEquals("Reusable access card", createdCardType.getDescription());
        assertTrue(createdCardType.getIsReturnRequired());
        assertTrue(createdCardType.getIsActive());
    }

    @Test
    void shouldRejectDuplicateCardTypeCodeOnCreate() {
        CardType requestCardType = new CardType();
        requestCardType.setCode("RFID");
        requestCardType.setName("RFID Card");

        when(cardTypePort.existsByCode("RFID")).thenReturn(true);

        assertThrows(ConflictException.class, () -> cardTypeUseCase.createCardType(requestCardType));
        verify(cardTypePort, never()).save(any(CardType.class));
    }

    @Test
    void shouldUpdateCardType() {
        UUID cardTypeId = UUID.randomUUID();
        CardType existingCardType = new CardType();
        existingCardType.setCardTypeId(cardTypeId);
        existingCardType.setCode("RFID");
        existingCardType.setName("RFID Card");
        existingCardType.setDescription("Old");
        existingCardType.setIsReturnRequired(true);

        CardType requestCardType = new CardType();
        requestCardType.setCode("TEMP");
        requestCardType.setName("Temporary Card");
        requestCardType.setDescription("Updated");
        requestCardType.setIsReturnRequired(false);
        requestCardType.setIsActive(false);

        when(cardTypePort.findById(cardTypeId)).thenReturn(Optional.of(existingCardType));
        when(cardTypePort.existsByCodeAndCardTypeIdNot("TEMP", cardTypeId)).thenReturn(false);
        when(cardTypePort.save(any(CardType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CardType updatedCardType = cardTypeUseCase.updateCardType(cardTypeId, requestCardType);

        assertEquals("TEMP", updatedCardType.getCode());
        assertEquals("Temporary Card", updatedCardType.getName());
        assertEquals("Updated", updatedCardType.getDescription());
        assertEquals(Boolean.FALSE, updatedCardType.getIsReturnRequired());
        assertEquals(Boolean.FALSE, updatedCardType.getIsActive());
    }

    @Test
    void shouldReturnOrderedCardTypes() {
        when(cardTypePort.findAll(Boolean.TRUE)).thenReturn(List.of(new CardType(), new CardType()));

        List<CardType> cardTypes = cardTypeUseCase.getCardTypes(Boolean.TRUE);

        assertEquals(2, cardTypes.size());
        verify(cardTypePort).findAll(Boolean.TRUE);
    }

    @Test
    void shouldDeactivateCardTypeWhenDeleting() {
        UUID cardTypeId = UUID.randomUUID();
        CardType existingCardType = new CardType();
        existingCardType.setCardTypeId(cardTypeId);
        existingCardType.setCode("RFID");
        existingCardType.setName("RFID Card");
        existingCardType.setIsReturnRequired(true);
        existingCardType.setIsActive(true);

        when(cardTypePort.findById(cardTypeId)).thenReturn(Optional.of(existingCardType));
        when(cardTypePort.save(any(CardType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        cardTypeUseCase.deleteCardType(cardTypeId);

        assertEquals(Boolean.FALSE, existingCardType.getIsActive());
        verify(cardTypePort).save(existingCardType);
    }

    @Test
    void shouldReturnWhenDeletingInactiveCardType() {
        UUID cardTypeId = UUID.randomUUID();
        CardType existingCardType = new CardType();
        existingCardType.setCardTypeId(cardTypeId);
        existingCardType.setCode("RFID");
        existingCardType.setName("RFID Card");
        existingCardType.setIsReturnRequired(true);
        existingCardType.setIsActive(false);

        when(cardTypePort.findById(cardTypeId)).thenReturn(Optional.of(existingCardType));

        cardTypeUseCase.deleteCardType(cardTypeId);

        verify(cardTypePort, never()).save(any(CardType.class));
    }

    @Test
    void shouldThrowWhenCardTypeDoesNotExist() {
        UUID cardTypeId = UUID.randomUUID();
        when(cardTypePort.findById(cardTypeId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> cardTypeUseCase.getCardTypeById(cardTypeId));
    }
}

