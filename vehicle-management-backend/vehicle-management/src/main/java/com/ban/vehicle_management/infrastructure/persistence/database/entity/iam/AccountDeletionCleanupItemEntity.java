package com.ban.vehicle_management.infrastructure.persistence.database.entity.iam;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.iam.AccountDeletionCleanupResourceType;
import com.ban.vehicle_management.shared.enumeration.iam.AccountDeletionCleanupStatus;
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
@Table(name = "account_deletion_cleanup_items", schema = "iam")
@Getter
@Setter
@NoArgsConstructor
public class AccountDeletionCleanupItemEntity extends AuditableEntity {

    @Id
    @Column(name = "cleanup_item_id", nullable = false)
    private UUID cleanupItemId;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    private AccountDeletionCleanupResourceType resourceType;

    @Column(name = "external_identifier", nullable = false, length = 1000)
    private String externalIdentifier;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountDeletionCleanupStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "processing_lease_until")
    private Instant processingLeaseUntil;

    @Column(name = "claim_token", length = 100)
    private String claimToken;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
