package com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result;

import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import java.util.UUID;

public record LostCardReplacementCardResult(
        UUID cardId,
        String cardNumber,
        String uid,
        UUID cardTypeId,
        CardStatus status
) {
}
