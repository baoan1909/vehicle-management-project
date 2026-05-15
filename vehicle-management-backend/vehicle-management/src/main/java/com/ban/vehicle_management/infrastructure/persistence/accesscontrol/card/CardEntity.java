package com.ban.vehicle_management.infrastructure.persistence.accesscontrol.card;

import com.ban.vehicle_management.infrastructure.persistence.accesscontrol.lostcardreport.LostCardReportEntity;
import com.ban.vehicle_management.infrastructure.persistence.accesscontrol.subscription.SubscriptionEntity;
import com.ban.vehicle_management.infrastructure.persistence.catalog.cardtype.CardTypeEntity;
import com.ban.vehicle_management.infrastructure.persistence.catalog.vehicletype.VehicleTypeEntity;
import com.ban.vehicle_management.infrastructure.persistence.common.entity.AuditableEntity;
import com.ban.vehicle_management.infrastructure.persistence.parking.parkingsession.ParkingSessionEntity;
import com.ban.vehicle_management.shared.enumeration.CardStatus;
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

    @Column(name = "vehicle_type_id")
    private UUID vehicleTypeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_type_id", referencedColumnName = "vehicle_type_id", insertable = false, updatable = false)
    private VehicleTypeEntity vehicleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CardStatus status;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "blocked_at")
    private Instant blockedAt;

    @Column(name = "blocked_reason")
    private String blockedReason;

    @OneToMany(mappedBy = "card")
    private Set<SubscriptionEntity> subscriptions = new HashSet<>();

    @OneToMany(mappedBy = "card")
    private Set<LostCardReportEntity> lostCardReports = new HashSet<>();

    @OneToMany(mappedBy = "card")
    private Set<ParkingSessionEntity> parkingSessions = new HashSet<>();

}
