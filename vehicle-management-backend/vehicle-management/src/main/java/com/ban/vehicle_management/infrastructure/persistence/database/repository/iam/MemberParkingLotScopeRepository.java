package com.ban.vehicle_management.infrastructure.persistence.database.repository.iam;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.MemberParkingLotScopeEntity;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationMembershipStatus;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberParkingLotScopeRepository extends JpaRepository<MemberParkingLotScopeEntity, UUID> {

    @Modifying
    void deleteByOrganizationMembershipId(UUID organizationMembershipId);

    @Query("""
        select scope.parkingLotId
        from MemberParkingLotScopeEntity scope, OrganizationMembershipEntity membership
        where scope.organizationMembershipId = membership.organizationMembershipId
          and membership.accountId = :accountId
          and membership.status = :status
        """)
    Set<UUID> findActiveParkingLotIdsByAccountId(
            @Param("accountId") UUID accountId,
            @Param("status") OrganizationMembershipStatus status
    );

}
