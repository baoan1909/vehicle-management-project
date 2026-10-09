package com.ban.vehicle_management.infrastructure.persistence.database.entity.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "parking_space_layout_items", schema = "parking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParkingSpaceLayoutItemEntity extends AuditableEntity {

    @Id
    @Column(name = "layout_item_id", nullable = false)
    private UUID layoutItemId;

    @Column(name = "layout_version_id", nullable = false)
    private UUID layoutVersionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "layout_version_id", referencedColumnName = "layout_version_id", insertable = false, updatable = false)
    private ParkingLayoutVersionEntity layoutVersion;

    @Column(name = "parking_space_id", nullable = false)
    private UUID parkingSpaceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parking_space_id", referencedColumnName = "parking_space_id", insertable = false, updatable = false)
    private ParkingSpaceEntity parkingSpace;

    @Column(name = "x", precision = 10, scale = 2, nullable = false)
    private BigDecimal x;

    @Column(name = "y", precision = 10, scale = 2, nullable = false)
    private BigDecimal y;

    @Column(name = "width", precision = 10, scale = 2, nullable = false)
    private BigDecimal width;

    @Column(name = "height", precision = 10, scale = 2, nullable = false)
    private BigDecimal height;

    @Column(name = "rotation", precision = 6, scale = 2, nullable = false)
    private BigDecimal rotation;
}