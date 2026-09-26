package com.ban.vehicle_management.infrastructure.persistence.database.entity.iam;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "member_parking_lot_scopes", schema = "iam")
@Getter
@Setter
@NoArgsConstructor
public class MemberParkingLotScopeEntity extends AuditableEntity {

    @Id
    @Column(name = "member_parking_lot_scope_id", nullable = false)
    private UUID memberParkingLotScopeId;

    @Column(name = "organization_membership_id", nullable = false)
    private UUID organizationMembershipId;

    @Column(name = "parking_lot_id", nullable = false)
    private UUID parkingLotId;
}
