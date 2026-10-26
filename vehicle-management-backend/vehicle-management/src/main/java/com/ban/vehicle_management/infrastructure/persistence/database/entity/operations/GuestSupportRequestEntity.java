package com.ban.vehicle_management.infrastructure.persistence.database.entity.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "guest_support_requests", schema = "operations", uniqueConstraints = {
        @UniqueConstraint(name = "uq_guest_support_reference", columnNames = "public_reference_code"),
        @UniqueConstraint(name = "uq_guest_support_idempotency", columnNames = {"contact_hash", "idempotency_hash"})
})
@Getter
@Setter
public class GuestSupportRequestEntity extends AuditableEntity implements Persistable<UUID> {
    @Id
    @Column(name = "guest_support_request_id", nullable = false)
    private UUID requestId;

    @Column(name = "public_reference_code", nullable = false, length = 32)
    private String publicReferenceCode;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "content", nullable = false, length = 4000)
    private String content;

    @Column(name = "email", length = 254)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "contact_hash", nullable = false, length = 64)
    private String contactHash;

    @Column(name = "ip_hash", nullable = false, length = 64)
    private String ipHash;

    @Column(name = "session_hash", length = 64)
    private String sessionHash;

    @Column(name = "idempotency_hash", nullable = false, length = 64)
    private String idempotencyHash;

    @Column(name = "capability_token_hash", nullable = false, length = 64)
    private String capabilityTokenHash;

    @Column(name = "capability_expires_at", nullable = false)
    private Instant capabilityExpiresAt;

    @Column(name = "retention_expires_at", nullable = false)
    private Instant retentionExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SupportTicketStatus status;

    @Override
    public UUID getId() {
        return requestId;
    }

    @Override
    public boolean isNew() {
        return getCreatedAt() == null;
    }
}
