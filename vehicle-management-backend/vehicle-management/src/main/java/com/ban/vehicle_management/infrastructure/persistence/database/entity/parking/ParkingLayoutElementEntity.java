package com.ban.vehicle_management.infrastructure.persistence.database.entity.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "parking_layout_elements", schema = "parking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParkingLayoutElementEntity extends AuditableEntity {

    @Id
    @Column(name = "layout_element_id", nullable = false)
    private UUID layoutElementId;

    @Column(name = "layout_version_id", nullable = false)
    private UUID layoutVersionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "layout_version_id", referencedColumnName = "layout_version_id", insertable = false, updatable = false)
    private ParkingLayoutVersionEntity layoutVersion;

    @Column(name = "element_type", nullable = false)
    private String elementType;

    @Column(name = "geometry", columnDefinition = "jsonb", nullable = false)
    private JsonNode geometry;

    @Column(name = "style", columnDefinition = "jsonb")
    private JsonNode style;

    @Column(name = "label")
    private String label;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}