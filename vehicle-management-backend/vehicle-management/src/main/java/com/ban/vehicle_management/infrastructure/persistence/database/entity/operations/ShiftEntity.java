package com.ban.vehicle_management.infrastructure.persistence.database.entity.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingLotEntity;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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

    @Column(name = "shift_template_id")
    private UUID shiftTemplateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "shift_template_id",
            referencedColumnName = "shift_template_id",
            insertable = false,
            updatable = false
    )
    private ShiftTemplateEntity shiftTemplate;

    @Column(name = "parking_lot_id", nullable = false)
    private UUID parkingLotId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "parking_lot_id",
            referencedColumnName = "parking_lot_id",
            insertable = false,
            updatable = false
    )
    private ParkingLotEntity parkingLot;

    @Column(name = "shift_code", nullable = false, unique = true)
    private String shiftCode;

    @Column(name = "shift_date", nullable = false)
    private LocalDate shiftDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type", nullable = false)
    private ShiftType shiftType;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ShiftStatus status;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "opening_cash", precision = 12, scale = 2)
    private BigDecimal openingCash;

    @Column(name = "closing_cash", precision = 12, scale = 2)
    private BigDecimal closingCash;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "opened_by")
    private UUID openedBy;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by")
    private UUID closedBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "note")
    private String note;

    @OneToMany(mappedBy = "shift")
    private Set<ShiftAssignmentEntity> shiftAssignments =
            new HashSet<>();
}