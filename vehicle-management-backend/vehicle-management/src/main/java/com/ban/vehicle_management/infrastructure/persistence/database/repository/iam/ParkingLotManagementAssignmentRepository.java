package com.ban.vehicle_management.infrastructure.persistence.database.repository.iam;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.ParkingLotManagementAssignmentEntity;
import com.ban.vehicle_management.shared.enumeration.iam.ManagementAssignmentStatus;
import com.ban.vehicle_management.infrastructure.persistence.database.projection.iam.AccountDeletionManagementAssignmentProjection;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import java.util.Collection;
import org.springframework.data.repository.query.Param;

public interface ParkingLotManagementAssignmentRepository
        extends JpaRepository<ParkingLotManagementAssignmentEntity, UUID> {

    @Query(value = """
        select assignment_id as "assignmentId", organization_id as "organizationId",
               parking_lot_id as "parkingLotId", status as "status"
        from iam.parking_lot_management_assignments
        where manager_account_id=:accountId and status in ('PENDING_ACCEPTANCE','ACTIVE')
          and (effective_to is null or effective_to>now())
        """, nativeQuery = true)
    List<AccountDeletionManagementAssignmentProjection> findDeletionBlockingAssignments(
            @Param("accountId") UUID accountId);

    @Query(value = """
        select assignment.parking_lot_id
        from iam.parking_lot_management_assignments assignment
        join iam.accounts account on account.account_id = assignment.manager_account_id
        join iam.organization_memberships membership
          on membership.account_id = account.account_id
         and membership.organization_id = assignment.organization_id
        join iam.organizations organization on organization.organization_id = assignment.organization_id
        where assignment.manager_account_id = :accountId
          and assignment.status = cast(:#{#status.name()} as varchar)
          and assignment.effective_from <= :at
          and (assignment.effective_to is null or assignment.effective_to > :at)
          and account.status = 'ACTIVE'
          and membership.status = 'ACTIVE'
          and organization.status = 'ACTIVE'
        """, nativeQuery = true)
    Set<UUID> findEffectiveParkingLotIds(
            @Param("accountId") UUID accountId,
            @Param("status") ManagementAssignmentStatus status,
            @Param("at") Instant at
    );

    @Query("""
        select count(assignment) > 0
        from ParkingLotManagementAssignmentEntity assignment
        where assignment.parkingLotId = :parkingLotId
          and assignment.status = com.ban.vehicle_management.shared.enumeration.iam.ManagementAssignmentStatus.ACTIVE
          and assignment.effectiveFrom <= :at
          and (assignment.effectiveTo is null or assignment.effectiveTo > :at)
        """)
    boolean hasEffectiveActiveManager(
            @Param("parkingLotId") UUID parkingLotId,
            @Param("at") Instant at
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select assignment
        from ParkingLotManagementAssignmentEntity assignment
        where assignment.parkingLotId = :parkingLotId
          and assignment.status in :statuses
          and assignment.effectiveFrom < :periodEnd
          and (assignment.effectiveTo is null or assignment.effectiveTo > :periodStart)
        order by assignment.createdAt
        """)
    List<ParkingLotManagementAssignmentEntity> findOverlappingForUpdate(
            @Param("parkingLotId") UUID parkingLotId,
            @Param("statuses") Set<ManagementAssignmentStatus> statuses,
            @Param("periodStart") Instant periodStart,
            @Param("periodEnd") Instant periodEnd
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select assignment
        from ParkingLotManagementAssignmentEntity assignment
        where assignment.assignmentId = :assignmentId
        """)
    Optional<ParkingLotManagementAssignmentEntity> findByIdForUpdate(@Param("assignmentId") UUID assignmentId);

    List<ParkingLotManagementAssignmentEntity> findAllByParkingLotIdOrderByEffectiveFromDesc(UUID parkingLotId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select assignment
        from ParkingLotManagementAssignmentEntity assignment
        where assignment.managerAccountId = :managerAccountId
          and assignment.status in :statuses
          and (assignment.effectiveTo is null or assignment.effectiveTo > :at)
        """)
    List<ParkingLotManagementAssignmentEntity> findEffectiveByManagerForUpdate(
            @Param("managerAccountId") UUID managerAccountId,
            @Param("statuses") Set<ManagementAssignmentStatus> statuses,
            @Param("at") Instant at
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        update iam.parking_lot_management_assignments
           set status = case when status = 'ACTIVE' then 'ENDED' else 'CANCELLED' end,
               effective_to = coalesce(effective_to, :now), updated_at = :now
         where organization_id in (:organizationIds) and status in ('ACTIVE', 'PENDING_ACCEPTANCE')
        """, nativeQuery = true)
    int closeAllByOrganizationIds(
            @Param("organizationIds") Collection<UUID> organizationIds,
            @Param("now") Instant now);
}
