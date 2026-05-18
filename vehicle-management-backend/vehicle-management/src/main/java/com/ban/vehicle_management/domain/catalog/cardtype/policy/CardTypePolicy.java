package com.ban.vehicle_management.domain.catalog.cardtype.policy;

import com.ban.vehicle_management.domain.catalog.cardtype.model.CardType;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;

public class CardTypePolicy {

    public void initialize(CardType cardType) {
        requireCardType(cardType);
        cardType.setCode(TextValidationUtils.normalizeCode(cardType.getCode(), "code", 50));
        cardType.setName(TextValidationUtils.normalizeRequiredText(cardType.getName(), "name", 100));
        cardType.setDescription(TextValidationUtils.normalizeNullableText(cardType.getDescription(), "description", 0));
        if (cardType.getIsReturnRequired() == null) {
            cardType.setIsReturnRequired(Boolean.TRUE);
        }
        if (cardType.getIsActive() == null) {
            cardType.setIsActive(Boolean.TRUE);
        }
    }

    public void deactivate(CardType cardType) {
        requireCardType(cardType);
        cardType.setIsActive(Boolean.FALSE);
    }

    private void requireCardType(CardType cardType) {
        if (cardType == null) {
            throw new BadRequestException("cardType must not be null");
        }
    }
}

