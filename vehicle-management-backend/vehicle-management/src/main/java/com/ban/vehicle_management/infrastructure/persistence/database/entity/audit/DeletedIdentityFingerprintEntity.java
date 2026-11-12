package com.ban.vehicle_management.infrastructure.persistence.database.entity.audit;

import com.ban.vehicle_management.shared.enumeration.iam.DeletedIdentityType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "deleted_identity_fingerprints", schema = "audit")
@Getter
@Setter
@NoArgsConstructor
public class DeletedIdentityFingerprintEntity {
    @Id
    @Column(name = "identity_fingerprint_id", nullable = false)
    private UUID identityFingerprintId;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_type", nullable = false, length = 40)
    private DeletedIdentityType identityType;

    @Column(name = "fingerprint", nullable = false, length = 64)
    private String fingerprint;

    @Column(name = "key_version", nullable = false, length = 30)
    private String keyVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
