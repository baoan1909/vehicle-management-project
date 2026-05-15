package com.ban.vehicle_management.domain.catalog.cardtype.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.catalog.cardtype.model.CardType;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import org.junit.jupiter.api.Test;

class CardTypePolicyTest {

    private final CardTypePolicy cardTypePolicy = new CardTypePolicy();

    @Test
    void shouldInitializeCardTypeWithDefaults() {
        CardType cardType = new CardType();
        cardType.setCode(" VISITOR ");
        cardType.setName(" The vang lai ");

        cardTypePolicy.initialize(cardType);

        assertEquals("VISITOR", cardType.getCode());
        assertEquals("The vang lai", cardType.getName());
        assertEquals(Boolean.TRUE, cardType.getIsReturnRequired());
    }

    @Test
    void shouldRejectBlankName() {
        CardType cardType = new CardType();
        cardType.setCode("REGISTERED");
        cardType.setName(" ");

        assertThrows(BadRequestException.class, () -> cardTypePolicy.initialize(cardType));
    }
}

