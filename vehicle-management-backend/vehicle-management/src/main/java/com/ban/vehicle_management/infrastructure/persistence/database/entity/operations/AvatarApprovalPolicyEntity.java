package com.ban.vehicle_management.infrastructure.persistence.database.entity.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "avatar_approval_policies", schema = "operations")
@Getter
@Setter
public class AvatarApprovalPolicyEntity extends AuditableEntity {
    @Id
    @Column(name = "policy_id", nullable = false)
    private UUID policyId;
    @Column(name = "auto_approve_enabled", nullable = false)
    private boolean autoApproveEnabled;
    @Column(name = "effective_from")
    private Instant effectiveFrom;
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
