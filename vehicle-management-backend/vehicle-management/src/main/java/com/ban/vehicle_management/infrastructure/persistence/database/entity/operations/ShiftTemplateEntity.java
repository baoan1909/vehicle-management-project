package com.ban.vehicle_management.infrastructure.persistence.database.entity.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingLotEntity;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftTemplateStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "shift_templates", schema = "operations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShiftTemplateEntity extends AuditableEntity {

    @Id
    @Column(name = "shift_template_id", nullable = false)
    private UUID shiftTemplateId;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type", nullable = false)
    private ShiftType shiftType;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "start_local_time", nullable = false)
    private LocalTime startLocalTime;

    @Column(name = "end_local_time", nullable = false)
    private LocalTime endLocalTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ShiftTemplateStatus status;
}