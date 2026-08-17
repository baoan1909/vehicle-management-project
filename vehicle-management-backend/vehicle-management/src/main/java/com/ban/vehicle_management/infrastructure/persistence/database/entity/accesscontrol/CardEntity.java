package com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.LostCardReportEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.SubscriptionEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.CardTypeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingSessionEntity;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_type_id", referencedColumnName = "card_type_id", insertable = false, updatable = false)
    private CardTypeEntity cardType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CardStatus status;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_before_blocked")
    private CardStatus statusBeforeBlocked;

    @Column(name = "blocked_at")
    private Instant blockedAt;

    @Column(name = "blocked_by")
    private UUID blockedBy;

    @Column(name = "blocked_reason")
    private String blockedReason;

    @Column(name = "retired_at")
    private Instant retiredAt;

    @Column(name = "retired_by")
    private UUID retiredBy;

    @Column(name = "retired_reason")
    private String retiredReason;

    @Column(name = "recovered_at")
    private Instant recoveredAt;

    @Column(name = "recovered_by")
    private UUID recoveredBy;

    @Column(name = "recovery_note")
    private String recoveryNote;

    @OneToMany(mappedBy = "card")
    private Set<SubscriptionEntity> subscriptions = new HashSet<>();

    @OneToMany(mappedBy = "card")
    private Set<LostCardReportEntity> lostCardReports = new HashSet<>();

    @OneToMany(mappedBy = "card")
    private Set<ParkingSessionEntity> parkingSessions = new HashSet<>();

}


