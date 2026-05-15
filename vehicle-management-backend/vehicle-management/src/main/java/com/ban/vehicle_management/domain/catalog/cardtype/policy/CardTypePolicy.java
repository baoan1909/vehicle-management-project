package com.ban.vehicle_management.domain.catalog.cardtype.policy;

import com.ban.vehicle_management.domain.catalog.cardtype.model.CardType;
import com.ban.vehicle_management.shared.exception.BadRequestException;

public class CardTypePolicy {

    public void initialize(CardType cardType) {
        requireCardType(cardType);
        cardType.setCode(normalizeRequired(cardType.getCode(), "code"));
        cardType.setName(normalizeRequired(cardType.getName(), "name"));
        cardType.setDescription(normalizeNullable(cardType.getDescription()));
        if (cardType.getIsReturnRequired() == null) {
            cardType.setIsReturnRequired(Boolean.TRUE);
        }
    }

    private void requireCardType(CardType cardType) {
        if (cardType == null) {
            throw new BadRequestException("cardType must not be null");
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalizedValue = normalizeNullable(value);
        if (normalizedValue == null) {
            throw new BadRequestException(fieldName + " must not be blank");
        }
        return normalizedValue;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }
}

