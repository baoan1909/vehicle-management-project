package com.ban.vehicle_management.infrastructure.persistence.database.entity.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingLotEntity;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftStatus;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "shifts", schema = "operations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShiftEntity extends AuditableEntity {

    @Id
    @Column(name = "shift_id", nullable = false)
    private UUID shiftId;

    @Column(name = "shift_code", nullable = false, unique = true)
    private String shiftCode;

    @Column(name = "parking_lot_id", nullable = false)
    private UUID parkingLotId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parking_lot_id", referencedColumnName = "parking_lot_id", insertable = false, updatable = false)
    private ParkingLotEntity parkingLot;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ShiftStatus status;

    @Column(name = "opening_cash", nullable = false, precision = 12, scale = 2)
    private BigDecimal openingCash;

    @Column(name = "closing_cash", precision = 12, scale = 2)
    private BigDecimal closingCash;

    @OneToMany(mappedBy = "shift")
    private Set<ShiftAssignmentEntity> shiftAssignments = new HashSet<>();

}


