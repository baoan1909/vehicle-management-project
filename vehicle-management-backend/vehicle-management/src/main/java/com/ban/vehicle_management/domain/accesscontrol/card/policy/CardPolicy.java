package com.ban.vehicle_management.domain.accesscontrol.card.policy;

import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

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

    public void reclassify(Card card, UUID targetCardTypeId, String nextCardNumber) {
        requireStatus(card, CardStatus.AVAILABLE);
        requireField(targetCardTypeId, "targetCardTypeId");
        nextCardNumber = TextValidationUtils.normalizeRequiredText(nextCardNumber, "cardNumber", 50);

        card.setCardTypeId(targetCardTypeId);
        card.setCardNumber(nextCardNumber);
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

    public void markAssignedFromInUse(Card card) {
        requireStatus(card, CardStatus.IN_USE);

        card.setStatus(CardStatus.ASSIGNED);
        clearBlockMetadata(card);
    }


    public void block(Card card, UUID blockedBy, Instant blockedAt, String blockedReason) {
        requireCard(card);
        if (card.getStatus() != CardStatus.AVAILABLE
                && card.getStatus() != CardStatus.RESERVED
                && card.getStatus() != CardStatus.ASSIGNED
                && card.getStatus() != CardStatus.IN_USE) {
            throw new BadRequestException("Không thể khóa thẻ ở trạng thái hiện tại");
        }

        requireField(blockedBy, "blockedBy");
        requireField(blockedAt, "blockedAt");
        blockedReason = TextValidationUtils.normalizeRequiredText(blockedReason, "blockedReason", 500);

        card.setStatusBeforeBlocked(card.getStatus());
        card.setStatus(CardStatus.BLOCKED);
        card.setBlockedAt(blockedAt);
        card.setBlockedBy(blockedBy);
        card.setBlockedReason(blockedReason);
    }

    public void unblock(Card card) {
        requireStatus(card, CardStatus.BLOCKED);
        CardStatus statusBeforeBlocked = card.getStatusBeforeBlocked();
        if (statusBeforeBlocked == null) {
            throw new BadRequestException("Thẻ bị khóa phải có trạng thái trước khi khóa");
        }

        card.setStatus(statusBeforeBlocked);
        clearBlockMetadata(card);
    }

    public void markLost(Card card) {
        requireCard(card);
        if (card.getStatus() == CardStatus.LOST || card.getStatus() == CardStatus.RETIRED) {
            throw new BadRequestException("Không thể báo mất thẻ ở trạng thái hiện tại");
        }

        card.setStatus(CardStatus.LOST);
        clearBlockMetadata(card);
    }

    public void retire(Card card, UUID retiredBy, Instant retiredAt, String retiredReason) {
        requireCard(card);
        if (card.getStatus() == CardStatus.IN_USE) {
            throw new BadRequestException("Không thể ngưng sử dụng thẻ đang được sử dụng");
        }
        if (card.getStatus() == CardStatus.RETIRED) {
            return;
        }

        requireField(retiredBy, "retiredBy");
        requireField(retiredAt, "retiredAt");
        card.setStatus(CardStatus.RETIRED);
        clearBlockMetadata(card);
        card.setRetiredAt(retiredAt);
        card.setRetiredBy(retiredBy);
        card.setRetiredReason(TextValidationUtils.normalizeRequiredText(retiredReason, "retiredReason", 500));
    }

    public void recover(Card card, UUID recoveredBy, Instant recoveredAt, String recoveryNote) {
        requireStatus(card, CardStatus.LOST);
        requireField(recoveredBy, "recoveredBy");
        requireField(recoveredAt, "recoveredAt");

        card.setStatus(CardStatus.AVAILABLE);
        card.setRecoveredAt(recoveredAt);
        card.setRecoveredBy(recoveredBy);
        card.setRecoveryNote(TextValidationUtils.normalizeRequiredText(recoveryNote, "recoveryNote", 500));
    }

    public void validateState(Card card) {
        requireCard(card);
        normalizeCoreFields(card);
        requireText(card.getCardNumber(), "cardNumber");
        requireText(card.getUid(), "uid");
        requireField(card.getCardTypeId(), "cardTypeId");
        requireField(card.getStatus(), "status");

        boolean hasBlockedAt = card.getBlockedAt() != null;
        boolean hasBlockedBy = card.getBlockedBy() != null;
        boolean hasBlockedReason = !isBlank(card.getBlockedReason());

        if (card.getStatus() == CardStatus.BLOCKED) {
            if (card.getStatusBeforeBlocked() == null || !hasBlockedAt || !hasBlockedBy || !hasBlockedReason) {
                throw new BadRequestException("Thẻ bị khóa phải lưu trạng thái trước khi khóa và thông tin khóa");
            }
            return;
        }

        if (card.getStatusBeforeBlocked() != null || hasBlockedAt || hasBlockedBy || hasBlockedReason) {
            throw new BadRequestException("Chỉ thẻ ở trạng thái bị khóa mới được lưu thông tin khóa");
        }
    }

    public String normalizeKeyword(String keyword) {
        return TextValidationUtils.normalizeNullableText(keyword, "keyword", 0);
    }

    public boolean hasCoreIdentifierChanged(Card existingCard, Card newCard) {
        requireCard(existingCard);
        requireCard(newCard);

        String normalizedCurrentCardNumber = TextValidationUtils.normalizeNullableText(
                existingCard.getCardNumber(),
                "cardNumber",
                50
        );
        String normalizedNewCardNumber = TextValidationUtils.normalizeNullableText(
                newCard.getCardNumber(),
                "cardNumber",
                50
        );
        String normalizedCurrentUid = TextValidationUtils.normalizeNullableText(existingCard.getUid(), "uid", 100);
        String normalizedNewUid = TextValidationUtils.normalizeNullableText(newCard.getUid(), "uid", 100);

        return !Objects.equals(normalizedCurrentCardNumber, normalizedNewCardNumber)
                || !Objects.equals(normalizedCurrentUid, normalizedNewUid);
    }

    private void clearBlockMetadata(Card card) {
        card.setStatusBeforeBlocked(null);
        card.setBlockedAt(null);
        card.setBlockedBy(null);
        card.setBlockedReason(null);
    }

    private void normalizeCoreFields(Card card) {
        card.setCardNumber(TextValidationUtils.normalizeNullableText(card.getCardNumber(), "cardNumber", 50));
        card.setUid(TextValidationUtils.normalizeNullableText(card.getUid(), "uid", 100));
        card.setBlockedReason(TextValidationUtils.normalizeNullableText(card.getBlockedReason(), "blockedReason", 500));
        card.setRetiredReason(TextValidationUtils.normalizeNullableText(card.getRetiredReason(), "retiredReason", 500));
        card.setRecoveryNote(TextValidationUtils.normalizeNullableText(card.getRecoveryNote(), "recoveryNote", 500));
    }

    private void requireStatus(Card card, CardStatus expectedStatus) {
        requireCard(card);
        if (card.getStatus() != expectedStatus) {
            throw new BadRequestException("Thẻ phải ở trạng thái " + expectedStatus);
        }
    }

    private void requireCard(Card card) {
        requireField(card, "card");
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException("Trường " + fieldName + " không được để trống");
        }
    }

    private void requireText(String value, String fieldName) {
        if (isBlank(value)) {
            throw new BadRequestException("Trường " + fieldName + " không được để trống");
        }
    }

    public void reserve(Card card) {
        requireStatus(card, CardStatus.AVAILABLE);

        card.setStatus(CardStatus.RESERVED);
        clearBlockMetadata(card);
    }

    public void release(Card card) {
        requireCard(card);
        if (card.getStatus() != CardStatus.RESERVED
                && card.getStatus() != CardStatus.ASSIGNED
                && card.getStatus() != CardStatus.IN_USE
                && card.getStatus() != CardStatus.BLOCKED) {
            throw new BadRequestException("Chỉ có thể giải phóng thẻ từ trạng thái RESERVED, ASSIGNED, IN_USE hoặc BLOCKED");
        }

        card.setStatus(CardStatus.AVAILABLE);
        clearBlockMetadata(card);
    }

    public void assignReserved(Card card, Instant issuedAt) {
        requireStatus(card, CardStatus.RESERVED);
        requireField(issuedAt, "issuedAt");

        card.setStatus(CardStatus.ASSIGNED);
        card.setIssuedAt(issuedAt);
        clearBlockMetadata(card);
    }


    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

