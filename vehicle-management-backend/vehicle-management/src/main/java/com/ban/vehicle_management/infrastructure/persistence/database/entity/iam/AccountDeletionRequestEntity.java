package com.ban.vehicle_management.infrastructure.persistence.database.entity.iam;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.iam.AccountDeletionRequestStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
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
@Table(name = "account_deletion_requests", schema = "iam")
@Getter
@Setter
@NoArgsConstructor
public class AccountDeletionRequestEntity extends AuditableEntity {

    @Id
    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountDeletionRequestStatus status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "scheduled_deletion_at", nullable = false)
    private Instant scheduledDeletionAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "processing_started_at")
    private Instant processingStartedAt;

    @Column(name = "processing_lease_until")
    private Instant processingLeaseUntil;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "claim_token", length = 100)
    private String claimToken;

    @Column(name = "last_error_code", length = 100)
    private String lastErrorCode;

    @Column(name = "last_error_message", length = 1000)
    private String lastErrorMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_account_status", nullable = false)
    private AccountStatus previousAccountStatus;

    @Column(name = "identity_provider_subject_snapshot", length = 255)
    private String identityProviderSubjectSnapshot;

    @Column(name = "active_subscription_no_refund_confirmed", nullable = false)
    private boolean activeSubscriptionNoRefundConfirmed;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
