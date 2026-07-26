package com.ban.vehicle_management.entrypoint.dto.accesscontrol.lostcardreport.response;

import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import java.util.UUID;

public record LostCardReplacementCardResponse(
        UUID cardId,
        String cardNumber,
        String uid,
        UUID cardTypeId,
        CardStatus status
) {
}
