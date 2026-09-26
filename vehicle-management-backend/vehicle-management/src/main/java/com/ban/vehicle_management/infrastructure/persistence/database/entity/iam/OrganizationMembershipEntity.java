package com.ban.vehicle_management.infrastructure.persistence.database.entity.iam;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationMembershipStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "organization_memberships", schema = "iam")
@Getter
@Setter
@NoArgsConstructor
public class OrganizationMembershipEntity extends AuditableEntity {

    @Id
    @Column(name = "organization_membership_id", nullable = false)
    private UUID organizationMembershipId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrganizationMembershipStatus status;
}
