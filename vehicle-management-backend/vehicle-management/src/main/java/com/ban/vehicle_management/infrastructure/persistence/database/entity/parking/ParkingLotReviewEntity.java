package com.ban.vehicle_management.infrastructure.persistence.database.entity.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "parking_lot_reviews", schema = "parking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParkingLotReviewEntity extends AuditableEntity {
    @Id
    @Column(name = "review_id", nullable = false)
    private UUID reviewId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "parking_lot_id", nullable = false)
    private UUID parkingLotId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parking_lot_id", insertable = false, updatable = false)
    private ParkingLotEntity parkingLot;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    private CustomerEntity customer;

    @Column(name = "rating", nullable = false)
    private short rating;

    @Column(name = "comment", length = 2000)
    private String comment;

    @Column(name = "source_parking_session_id")
    private UUID sourceParkingSessionId;

    @Column(name = "source_payment_id")
    private UUID sourcePaymentId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
