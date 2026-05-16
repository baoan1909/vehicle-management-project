package com.ban.vehicle_management.domain.accesscontrol.card.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.CardStatus;
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
    private UUID vehicleTypeId;
    private CardStatus status;
    private Instant issuedAt;
    private Instant blockedAt;
    private String blockedReason;
}

