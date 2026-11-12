package com.ban.vehicle_management.infrastructure.persistence.database.entity.iam;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationClosureStatus;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "account_deletion_organization_scopes", schema = "iam")
@Getter
@Setter
@NoArgsConstructor
public class AccountDeletionOrganizationScopeEntity extends AuditableEntity {

    @Id
    @Column(name = "scope_id", nullable = false)
    private UUID scopeId;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_organization_status", nullable = false)
    private OrganizationStatus previousOrganizationStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "closure_status", nullable = false)
    private OrganizationClosureStatus closureStatus;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
