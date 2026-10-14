package com.ban.vehicle_management.infrastructure.persistence.database.entity.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLevelStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "parking_levels", schema = "parking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParkingLevelEntity extends AuditableEntity {

    @Id
    @Column(name = "parking_level_id", nullable = false)
    private UUID parkingLevelId;

    @Column(name = "parking_lot_id", nullable = false)
    private UUID parkingLotId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parking_lot_id", referencedColumnName = "parking_lot_id", insertable = false, updatable = false)
    private ParkingLotEntity parkingLot;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "elevation", precision = 8, scale = 2)
    private BigDecimal elevation;

    @Column(name = "floor_height", precision = 8, scale = 2)
    private BigDecimal floorHeight;

    @Column(name = "canvas_width", precision = 10, scale = 2, nullable = false)
    private BigDecimal canvasWidth;

    @Column(name = "canvas_height", precision = 10, scale = 2, nullable = false)
    private BigDecimal canvasHeight;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ParkingLevelStatus status;
}