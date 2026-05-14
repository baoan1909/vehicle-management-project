package com.ban.vehicle_management.infrastructure.persistence.accesscontrol.card;

import com.ban.vehicle_management.infrastructure.persistence.common.entity.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.CardStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cards", schema = "access_control")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CardEntity extends AuditableEntity {

    @Id
    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Column(name = "card_number", nullable = false, unique = true)
    private String cardNumber;

    @Column(name = "uid", nullable = false, unique = true)
    private String uid;

    @Column(name = "card_type_id", nullable = false)
    private UUID cardTypeId;

    @Column(name = "vehicle_type_id")
    private UUID vehicleTypeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CardStatus status;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "blocked_at")
    private Instant blockedAt;

    @Column(name = "blocked_reason")
    private String blockedReason;

}
