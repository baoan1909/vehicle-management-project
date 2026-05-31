package com.ban.vehicle_management.domain.accesscontrol.card.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CardPolicyTest {

    private final CardPolicy cardPolicy = new CardPolicy();

    @Test
    void shouldInitializeNewCardWithAvailableStatus() {
        Card card = new Card();
        card.setCardNumber("C-001");
        card.setUid("UID-001");
        card.setCardTypeId(UUID.randomUUID());

        cardPolicy.initializeNewCard(card);

        assertEquals(CardStatus.AVAILABLE, card.getStatus());
    }

    @Test
    void shouldAssignAndMarkInUse() {
        Card card = validCard(CardStatus.AVAILABLE);
        Instant issuedAt = Instant.parse("2026-05-15T10:00:00Z");

        cardPolicy.assign(card, issuedAt);
        cardPolicy.markInUse(card);

        assertEquals(CardStatus.IN_USE, card.getStatus());
        assertEquals(issuedAt, card.getIssuedAt());
    }

    @Test
    void shouldRequireReasonWhenBlockingCard() {
        Card card = validCard(CardStatus.ASSIGNED);

        assertThrows(BadRequestException.class, () -> cardPolicy.block(card, Instant.now(), " "));
    }

    @Test
    void shouldClearBlockMetadataWhenUnblocking() {
        Card card = validCard(CardStatus.ASSIGNED);
        cardPolicy.block(card, Instant.parse("2026-05-15T10:00:00Z"), "Security issue");

        cardPolicy.unblock(card);

        assertEquals(CardStatus.AVAILABLE, card.getStatus());
        assertNull(card.getBlockedAt());
        assertNull(card.getBlockedReason());
    }

    @Test
    void shouldRejectNonBlockedMetadataForAvailableCard() {
        Card card = validCard(CardStatus.AVAILABLE);
        card.setBlockedReason("not allowed");

        assertThrows(BadRequestException.class, () -> cardPolicy.validateState(card));
    }

    @Test
    void shouldRejectRetireWhenCardIsInUse() {
        Card card = validCard(CardStatus.IN_USE);

        assertThrows(BadRequestException.class, () -> cardPolicy.retire(card));
    }

    @Test
    void shouldNormalizeKeywordForSearch() {
        assertEquals("abc", cardPolicy.normalizeKeyword("  abc  "));
        assertNull(cardPolicy.normalizeKeyword("   "));
    }

    @Test
    void shouldDetectCoreIdentifierChangedAfterNormalization() {
        Card existingCard = validCard(CardStatus.AVAILABLE);
        existingCard.setCardNumber(" C-001 ");
        existingCard.setUid(" UID-001 ");

        Card sameCard = validCard(CardStatus.AVAILABLE);
        sameCard.setCardNumber("C-001");
        sameCard.setUid("UID-001");
        assertFalse(cardPolicy.hasCoreIdentifierChanged(existingCard, sameCard));

        Card changedCard = validCard(CardStatus.AVAILABLE);
        changedCard.setCardNumber("C-999");
        changedCard.setUid("UID-001");
        assertTrue(cardPolicy.hasCoreIdentifierChanged(existingCard, changedCard));
    }

    private Card validCard(CardStatus status) {
        Card card = new Card();
        card.setCardId(UUID.randomUUID());
        card.setCardNumber("C-001");
        card.setUid("UID-001");
        card.setCardTypeId(UUID.randomUUID());
        card.setStatus(status);
        return card;
    }
}

