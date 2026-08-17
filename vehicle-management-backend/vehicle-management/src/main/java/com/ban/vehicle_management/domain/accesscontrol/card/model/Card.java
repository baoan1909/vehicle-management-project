package com.ban.vehicle_management.domain.accesscontrol.card.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Card extends AuditableDomainModel {

    private UUID cardId;
    private String cardNumber;
    private String uid;
    private UUID cardTypeId;
    private CardStatus status;
    private Instant issuedAt;
    private CardStatus statusBeforeBlocked;
    private Instant blockedAt;
    private UUID blockedBy;
    private String blockedReason;
    private Instant retiredAt;
    private UUID retiredBy;
    private String retiredReason;
    private Instant recoveredAt;
    private UUID recoveredBy;
    private String recoveryNote;
}

