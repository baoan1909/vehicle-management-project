package com.ban.vehicle_management.domain.accesscontrol.card.policy;

import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.shared.enumeration.CardStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.time.Instant;

public class CardPolicy {

    public void initializeNewCard(Card card) {
        requireCard(card);
        normalizeCoreFields(card);
        requireText(card.getCardNumber(), "cardNumber");
        requireText(card.getUid(), "uid");
        requireField(card.getCardTypeId(), "cardTypeId");

        if (card.getStatus() == null) {
            card.setStatus(CardStatus.AVAILABLE);
        }

        validateState(card);
    }

    public void validateMaintenance(Card card) {
        requireCard(card);
        normalizeCoreFields(card);
        validateState(card);
    }

    public void assign(Card card, Instant issuedAt) {
        requireStatus(card, CardStatus.AVAILABLE);
        requireField(issuedAt, "issuedAt");

        card.setStatus(CardStatus.ASSIGNED);
        card.setIssuedAt(issuedAt);
        clearBlockMetadata(card);
    }

    public void markInUse(Card card) {
        requireStatus(card, CardStatus.ASSIGNED);

        card.setStatus(CardStatus.IN_USE);
        clearBlockMetadata(card);
    }

    public void release(Card card) {
        requireCard(card);
        if (card.getStatus() != CardStatus.ASSIGNED
                && card.getStatus() != CardStatus.IN_USE
                && card.getStatus() != CardStatus.BLOCKED) {
            throw new BadRequestException("Card can only be released from ASSIGNED, IN_USE, or BLOCKED status");
        }

        card.setStatus(CardStatus.AVAILABLE);
        clearBlockMetadata(card);
    }

    public void block(Card card, Instant blockedAt, String blockedReason) {
        requireCard(card);
        if (card.getStatus() == CardStatus.RETIRED || card.getStatus() == CardStatus.LOST || card.getStatus() == CardStatus.DAMAGED) {
            throw new BadRequestException("Card cannot be blocked from current status");
        }

        requireField(blockedAt, "blockedAt");
        requireText(blockedReason, "blockedReason");

        card.setStatus(CardStatus.BLOCKED);
        card.setBlockedAt(blockedAt);
        card.setBlockedReason(blockedReason.trim());
    }

    public void unblock(Card card) {
        requireStatus(card, CardStatus.BLOCKED);

        card.setStatus(CardStatus.AVAILABLE);
        clearBlockMetadata(card);
    }

    public void markLost(Card card) {
        requireCard(card);
        if (card.getStatus() == CardStatus.LOST || card.getStatus() == CardStatus.DAMAGED || card.getStatus() == CardStatus.RETIRED) {
            throw new BadRequestException("Card cannot be marked as lost from current status");
        }

        card.setStatus(CardStatus.LOST);
        clearBlockMetadata(card);
    }

    public void markDamaged(Card card) {
        requireCard(card);
        if (card.getStatus() == CardStatus.LOST || card.getStatus() == CardStatus.DAMAGED || card.getStatus() == CardStatus.RETIRED) {
            throw new BadRequestException("Card cannot be marked as damaged from current status");
        }

        card.setStatus(CardStatus.DAMAGED);
        clearBlockMetadata(card);
    }

    public void retire(Card card) {
        requireCard(card);
        if (card.getStatus() == CardStatus.IN_USE) {
            throw new BadRequestException("Card in use cannot be retired");
        }
        if (card.getStatus() == CardStatus.RETIRED) {
            return;
        }

        card.setStatus(CardStatus.RETIRED);
        clearBlockMetadata(card);
    }

    public void validateState(Card card) {
        requireCard(card);
        normalizeCoreFields(card);
        requireText(card.getCardNumber(), "cardNumber");
        requireText(card.getUid(), "uid");
        requireField(card.getCardTypeId(), "cardTypeId");
        requireField(card.getStatus(), "status");

        boolean hasBlockedAt = card.getBlockedAt() != null;
        boolean hasBlockedReason = !isBlank(card.getBlockedReason());

        if (card.getStatus() == CardStatus.BLOCKED) {
            if (!hasBlockedAt || !hasBlockedReason) {
                throw new BadRequestException("Blocked card must have blockedAt and blockedReason");
            }
            return;
        }

        if (hasBlockedAt || hasBlockedReason) {
            throw new BadRequestException("Only blocked card can keep blockedAt and blockedReason");
        }
    }

    private void clearBlockMetadata(Card card) {
        card.setBlockedAt(null);
        card.setBlockedReason(null);
    }

    private void normalizeCoreFields(Card card) {
        if (card.getCardNumber() != null) {
            card.setCardNumber(card.getCardNumber().trim());
        }
        if (card.getUid() != null) {
            card.setUid(card.getUid().trim());
        }
        if (card.getBlockedReason() != null) {
            card.setBlockedReason(card.getBlockedReason().trim());
        }
    }

    private void requireStatus(Card card, CardStatus expectedStatus) {
        requireCard(card);
        if (card.getStatus() != expectedStatus) {
            throw new BadRequestException("Card must be in " + expectedStatus + " status");
        }
    }

    private void requireCard(Card card) {
        requireField(card, "card");
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }

    private void requireText(String value, String fieldName) {
        if (isBlank(value)) {
            throw new BadRequestException(fieldName + " must not be blank");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

