package com.ban.vehicle_management.infrastructure.persistence.database.entity.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.parking.LayoutVersionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "parking_layout_versions", schema = "parking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParkingLayoutVersionEntity extends AuditableEntity implements Persistable<UUID> {

    @Id
    @Column(name = "layout_version_id", nullable = false)
    private UUID layoutVersionId;

    @Column(name = "zone_id", nullable = false)
    private UUID zoneId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id", referencedColumnName = "zone_id", insertable = false, updatable = false)
    private ZoneEntity zone;

    @Column(name = "version", nullable = false)
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LayoutVersionStatus status;

    @Column(name = "published_at")
    private java.time.Instant publishedAt;

    @Column(name = "published_by")
    private UUID publishedBy;

    @Version
    @Column(name = "revision", nullable = false)
    private Long revision;

    @Column(name = "last_saved_at")
    private Instant lastSavedAt;

    @Override
    public UUID getId() {
        return layoutVersionId;
    }

    @Override
    public boolean isNew() {
        // New drafts keep revision null until the first persist seeds it, so
        // Spring Data uses persist() instead of merge() and avoids a phantom
        // optimistic-locking conflict on insert.
        return revision == null;
    }
}