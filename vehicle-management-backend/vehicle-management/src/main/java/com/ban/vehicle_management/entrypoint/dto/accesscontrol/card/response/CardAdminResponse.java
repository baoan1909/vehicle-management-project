package com.ban.vehicle_management.entrypoint.dto.accesscontrol.card.response;

import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CardAdminResponse {

    private UUID cardId;
    private String cardNumber;
    private String uid;
    private UUID cardTypeId;
    private UUID vehicleTypeId;
    private CardStatus status;
    private String issuedAt;
    private String blockedAt;
    private String blockedReason;
    private String createdAt;
    private UUID createdBy;
    private String updatedAt;
    private UUID updatedBy;
}

