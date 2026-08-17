package com.ban.vehicle_management.application.accesscontrol.card.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.accesscontrol.card.port.out.CardPortOut;
import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.catalog.cardtype.port.out.CardTypePortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.domain.catalog.cardtype.model.CardType;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardNumberSeries;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
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
class CardUseCaseImplTest {

    @Mock
    private CardPortOut cardPort;

    @Mock
    private CardTypePortOut cardTypePort;

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private AuditLogPortOut auditLogPortOut;

    @InjectMocks
    private CardUseCaseImpl cardUseCase;

    @Test
    void shouldCreateCardWithDefaultAvailableStatus() {
        UUID cardTypeId = UUID.randomUUID();
        Card requestCard = new Card();
        requestCard.setCardTypeId(cardTypeId);

        when(cardTypePort.findById(cardTypeId)).thenReturn(Optional.of(cardType("VISITOR")));
        when(cardPort.nextCardNumberSequence(CardNumberSeries.VISITOR)).thenReturn(1L);
        when(cardPort.existsByCardNumber("V001")).thenReturn(false);
        when(cardPort.existsByUid(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);
        when(cardPort.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        Card createdCard = cardUseCase.createCard(requestCard);

        verify(currentAccountPortIn).requirePermission("CARD_CREATE_ALL");
        assertNotNull(createdCard.getCardId());
        assertEquals("V001", createdCard.getCardNumber());
        assertNotNull(UUID.fromString(createdCard.getUid()));
        assertEquals(CardStatus.AVAILABLE, createdCard.getStatus());
    }

    @Test
    void shouldRejectCreateWhenCardTypeDoesNotExist() {
        UUID cardTypeId = UUID.randomUUID();
        Card requestCard = new Card();
        requestCard.setCardTypeId(cardTypeId);

        when(cardTypePort.findById(cardTypeId)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> cardUseCase.createCard(requestCard));
        verify(cardPort, never()).save(any(Card.class));
    }

    @Test
    void shouldReturnFilteredCards() {
        UUID cardTypeId = UUID.randomUUID();
        when(cardPort.findAll(CardStatus.AVAILABLE, cardTypeId, "C001"))
                .thenReturn(List.of(new Card(), new Card()));

        List<Card> cards = cardUseCase.getCards(CardStatus.AVAILABLE, cardTypeId, " C001 ");

        assertEquals(2, cards.size());
        verify(currentAccountPortIn).requirePermission("CARD_READ_ALL");
        verify(cardPort).findAll(CardStatus.AVAILABLE, cardTypeId, "C001");
    }

    @Test
    void shouldRejectDirectCardUpdate() {
        UUID cardId = UUID.randomUUID();
        Card requestCard = updateRequest(UUID.randomUUID());

        BadRequestException exception = assertThrows(BadRequestException.class, () -> cardUseCase.updateCard(cardId, requestCard));

        assertEquals(
                "Không hỗ trợ cập nhật trực tiếp mã thẻ, UID/RFID hoặc loại thẻ. Hãy dùng chức năng phân loại lại hoặc tái cấp/thay thẻ",
                exception.getMessage()
        );
        verify(cardPort, never()).save(any(Card.class));
    }

    @Test
    void shouldReclassifyAvailableCardWithoutOperationalHistory() {
        UUID cardId = UUID.randomUUID();
        UUID targetCardTypeId = UUID.randomUUID();
        Card existingCard = existingCard(cardId, CardStatus.AVAILABLE);

        when(cardPort.findByIdForUpdate(cardId)).thenReturn(Optional.of(existingCard));
        when(cardPort.hasOperationalHistory(cardId)).thenReturn(false);
        when(cardTypePort.findById(targetCardTypeId)).thenReturn(Optional.of(cardType("VISITOR")));
        when(cardPort.nextCardNumberSequence(CardNumberSeries.VISITOR)).thenReturn(7L);
        when(cardPort.existsByCardNumber("V007")).thenReturn(false);
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(UUID.randomUUID());
        when(cardPort.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Card updatedCard = cardUseCase.reclassifyCard(cardId, targetCardTypeId, "Chuyển lô thẻ chưa phát hành");

        verify(currentAccountPortIn).requirePermission("CARD_UPDATE_ALL");
        assertEquals("V007", updatedCard.getCardNumber());
        assertEquals("UID-001", updatedCard.getUid());
        assertEquals(targetCardTypeId, updatedCard.getCardTypeId());
        verify(auditLogPortOut).save(any());
    }

    @Test
    void shouldRejectReclassificationWhenCardAlreadyHasOperationalHistory() {
        UUID cardId = UUID.randomUUID();
        Card existingCard = existingCard(cardId, CardStatus.AVAILABLE);

        when(cardPort.findByIdForUpdate(cardId)).thenReturn(Optional.of(existingCard));
        when(cardPort.hasOperationalHistory(cardId)).thenReturn(true);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> cardUseCase.reclassifyCard(cardId, UUID.randomUUID(), "Điều chỉnh lô thẻ")
        );

        assertEquals(
                "Thẻ đã phát sinh nghiệp vụ không thể phân loại lại. Hãy thực hiện tái cấp hoặc thay thẻ để bảo toàn lịch sử",
                exception.getMessage()
        );
        verify(cardPort, never()).save(any(Card.class));
    }

    @Test
    void shouldRetireCardOnDeleteWhenSafe() {
        UUID cardId = UUID.randomUUID();
        Card existingCard = existingCard(cardId, CardStatus.AVAILABLE);
        UUID currentAccountId = UUID.randomUUID();

        when(cardPort.findById(cardId)).thenReturn(Optional.of(existingCard));
        when(cardPort.hasActiveUsage(cardId)).thenReturn(false);
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(currentAccountId);
        when(cardPort.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        cardUseCase.deleteCard(cardId);

        verify(currentAccountPortIn).requirePermission("CARD_DELETE_ALL");
        assertEquals(CardStatus.RETIRED, existingCard.getStatus());
        assertEquals(currentAccountId, existingCard.getRetiredBy());
        verify(cardPort).save(existingCard);
    }

    @Test
    void shouldRejectDeleteWhenCardHasActiveUsage() {
        UUID cardId = UUID.randomUUID();
        Card existingCard = existingCard(cardId, CardStatus.ASSIGNED);

        when(cardPort.findById(cardId)).thenReturn(Optional.of(existingCard));
        when(cardPort.hasActiveUsage(cardId)).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> cardUseCase.deleteCard(cardId));

        assertEquals(
                "Thẻ đang được sử dụng trong phiên gửi xe đang hoạt động nên không thể ngưng sử dụng. Hãy hoàn tất checkout trước",
                exception.getMessage()
        );
        verify(cardPort, never()).save(any(Card.class));
    }

    @Test
    void shouldBlockCardAndRememberPreviousStatus() {
        UUID cardId = UUID.randomUUID();
        Card existingCard = existingCard(cardId, CardStatus.ASSIGNED);
        UUID currentAccountId = UUID.randomUUID();

        when(cardPort.findByIdForUpdate(cardId)).thenReturn(Optional.of(existingCard));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(currentAccountId);
        when(cardPort.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Card updatedCard = cardUseCase.blockCard(cardId, "Security review");

        assertEquals(CardStatus.BLOCKED, updatedCard.getStatus());
        assertEquals(CardStatus.ASSIGNED, updatedCard.getStatusBeforeBlocked());
        assertEquals(currentAccountId, updatedCard.getBlockedBy());
        assertEquals("Security review", updatedCard.getBlockedReason());
        assertNotNull(updatedCard.getBlockedAt());
    }

    @Test
    void shouldRestorePreviousStatusWhenUnblocking() {
        UUID cardId = UUID.randomUUID();
        Card existingCard = existingCard(cardId, CardStatus.BLOCKED);
        existingCard.setStatusBeforeBlocked(CardStatus.RESERVED);
        existingCard.setBlockedAt(java.time.Instant.now());
        existingCard.setBlockedBy(UUID.randomUUID());
        existingCard.setBlockedReason("Security review");
        when(cardPort.findByIdForUpdate(cardId)).thenReturn(Optional.of(existingCard));
        when(cardPort.canRestoreBlockedStatus(cardId, CardStatus.RESERVED)).thenReturn(true);
        when(cardPort.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Card updatedCard = cardUseCase.unblockCard(cardId);

        assertEquals(CardStatus.RESERVED, updatedCard.getStatus());
        assertNull(updatedCard.getStatusBeforeBlocked());
    }

    @Test
    void shouldRejectUnblockWhenPreviousBusinessStateIsNoLongerValid() {
        UUID cardId = UUID.randomUUID();
        Card existingCard = existingCard(cardId, CardStatus.BLOCKED);
        existingCard.setStatusBeforeBlocked(CardStatus.ASSIGNED);
        when(cardPort.findByIdForUpdate(cardId)).thenReturn(Optional.of(existingCard));
        when(cardPort.canRestoreBlockedStatus(cardId, CardStatus.ASSIGNED)).thenReturn(false);

        assertThrows(ConflictException.class, () -> cardUseCase.unblockCard(cardId));
        verify(cardPort, never()).save(any(Card.class));
    }

    @Test
    void shouldCreateBatchOfCardsWithConsecutiveNumbers() {
        UUID cardTypeId = UUID.randomUUID();

        when(cardTypePort.findById(cardTypeId)).thenReturn(Optional.of(cardType("VISITOR")));
        when(cardPort.nextCardNumberSequence(CardNumberSeries.VISITOR)).thenReturn(1L, 2L, 3L);
        when(cardPort.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Card> createdCards = cardUseCase.createCards(cardTypeId, 3);

        assertEquals(List.of("V001", "V002", "V003"), createdCards.stream().map(Card::getCardNumber).toList());
        createdCards.forEach(card -> assertNotNull(UUID.fromString(card.getUid())));
        verify(currentAccountPortIn).requirePermission("CARD_CREATE_ALL");
        verify(cardPort).saveAll(anyList());
    }

    @Test
    void shouldThrowWhenCardDoesNotExist() {
        UUID cardId = UUID.randomUUID();
        when(cardPort.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> cardUseCase.getCardById(cardId));
    }

    @Test
    void shouldRejectDuplicateGeneratedCardNumberOnCreate() {
        UUID cardTypeId = UUID.randomUUID();
        Card requestCard = new Card();
        requestCard.setCardTypeId(cardTypeId);

        when(cardTypePort.findById(cardTypeId)).thenReturn(Optional.of(cardType("REGISTERED")));
        when(cardPort.nextCardNumberSequence(CardNumberSeries.REGISTERED)).thenReturn(1L);
        when(cardPort.existsByCardNumber("R001")).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class, () -> cardUseCase.createCard(requestCard));

        assertEquals("Mã thẻ đã tồn tại", exception.getMessage());
        verify(cardPort, never()).save(any(Card.class));
    }

    private Card existingCard(UUID cardId, CardStatus status) {
        UUID cardTypeId = UUID.randomUUID();

        Card existingCard = new Card();
        existingCard.setCardId(cardId);
        existingCard.setCardNumber("C001");
        existingCard.setUid("UID-001");
        existingCard.setCardTypeId(cardTypeId);
        existingCard.setStatus(status);
        return existingCard;
    }

    private Card updateRequest(UUID cardTypeId) {
        Card requestCard = new Card();
        requestCard.setCardNumber("C001");
        requestCard.setUid("UID-001");
        requestCard.setCardTypeId(cardTypeId);
        return requestCard;
    }

    private CardType cardType(String code) {
        CardType cardType = new CardType();
        cardType.setCode(code);
        return cardType;
    }
}
