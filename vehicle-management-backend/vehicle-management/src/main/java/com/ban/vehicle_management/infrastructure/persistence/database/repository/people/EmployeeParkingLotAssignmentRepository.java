package com.ban.vehicle_management.infrastructure.persistence.database.repository.people;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.EmployeeParkingLotAssignmentEntity;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeParkingLotAssignmentStatus;
import com.ban.vehicle_management.infrastructure.persistence.database.projection.iam.AccountDeletionResourceScopeProjection;
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

public interface EmployeeParkingLotAssignmentRepository
        extends JpaRepository<EmployeeParkingLotAssignmentEntity, UUID> {

    @Query(value = """
        select assignment_id as "resourceId", organization_id as "organizationId",
               parking_lot_id as "parkingLotId"
        from people.employee_parking_lot_assignments
        where employee_id=:employeeId and status in ('SCHEDULED','ACTIVE','PENDING_REVIEW')
          and (effective_to is null or effective_to>now())
        """, nativeQuery = true)
    List<AccountDeletionResourceScopeProjection> findDeletionBlockingAssignments(
            @Param("employeeId") UUID employeeId);

    List<EmployeeParkingLotAssignmentEntity> findAllByOrganizationIdOrderByEffectiveFromDesc(UUID organizationId);

    @Query(value = """
        select distinct assignment.parking_lot_id
        from people.employee_parking_lot_assignments assignment
        join people.employees employee on employee.employee_id = assignment.employee_id
        join iam.accounts account on account.user_profile_id = employee.user_profile_id
        join iam.organization_memberships membership
          on membership.account_id = account.account_id
         and membership.organization_id = assignment.organization_id
        join iam.organizations organization on organization.organization_id = membership.organization_id
        where account.account_id = :accountId
          and account.status = 'ACTIVE'
          and employee.status = 'ACTIVE'
          and membership.status = 'ACTIVE'
          and organization.status = 'ACTIVE'
          and assignment.status in ('ACTIVE', 'SCHEDULED')
          and assignment.effective_from <= :at
          and (assignment.effective_to is null or assignment.effective_to > :at)
        """, nativeQuery = true)
    Set<UUID> findReadableParkingLotIds(@Param("accountId") UUID accountId, @Param("at") Instant at);

    @Query(value = """
        select distinct assignment.parking_lot_id
        from people.employee_parking_lot_assignments assignment
        join people.employees employee on employee.employee_id = assignment.employee_id
        join iam.accounts account on account.user_profile_id = employee.user_profile_id
        join iam.organization_memberships membership
          on membership.account_id = account.account_id
         and membership.organization_id = assignment.organization_id
        join iam.organizations organization on organization.organization_id = assignment.organization_id
        join operations.shift_assignments shift_assignment
          on shift_assignment.employee_id = employee.employee_id
         and shift_assignment.status = 'ACTIVE'
        join operations.shifts shift
          on shift.shift_id = shift_assignment.shift_id
         and shift.parking_lot_id = assignment.parking_lot_id
         and shift.status = 'OPEN'
         and shift.start_time <= :at
         and shift.end_time > :at
        join parking.parking_lots lot on lot.parking_lot_id = assignment.parking_lot_id
        where account.account_id = :accountId
          and account.status = 'ACTIVE'
          and employee.status = 'ACTIVE'
          and membership.status = 'ACTIVE'
          and organization.status = 'ACTIVE'
          and assignment.status = 'ACTIVE'
          and assignment.effective_from <= :at
          and (assignment.effective_to is null or assignment.effective_to > :at)
          and lot.management_state <> 'OPERATION_SUSPENDED'
        """, nativeQuery = true)
    Set<UUID> findOperableParkingLotIds(@Param("accountId") UUID accountId, @Param("at") Instant at);

    @Query("""
        select assignment
        from EmployeeParkingLotAssignmentEntity assignment
        where assignment.employeeId = :employeeId
          and assignment.parkingLotId = :parkingLotId
          and assignment.status in :statuses
          and assignment.effectiveFrom <= :fromTime
          and (assignment.effectiveTo is null or assignment.effectiveTo >= :toTime)
        """)
    Optional<EmployeeParkingLotAssignmentEntity> findCovering(
            @Param("employeeId") UUID employeeId,
            @Param("parkingLotId") UUID parkingLotId,
            @Param("statuses") Set<EmployeeParkingLotAssignmentStatus> statuses,
            @Param("fromTime") Instant fromTime,
            @Param("toTime") Instant toTime
    );

    @Query("""
        select distinct assignment.employeeId
        from EmployeeParkingLotAssignmentEntity assignment
        where assignment.parkingLotId = :parkingLotId
          and assignment.status in :statuses
          and assignment.effectiveFrom <= :at
          and (assignment.effectiveTo is null or assignment.effectiveTo > :at)
        """)
    Set<UUID> findAssignedEmployeeIds(
            @Param("parkingLotId") UUID parkingLotId,
            @Param("statuses") Set<EmployeeParkingLotAssignmentStatus> statuses,
            @Param("at") Instant at
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select assignment
        from EmployeeParkingLotAssignmentEntity assignment
        where assignment.employeeId = :employeeId
          and assignment.parkingLotId = :parkingLotId
          and assignment.status in :statuses
          and (assignment.effectiveTo is null or assignment.effectiveTo > :at)
        """)
    List<EmployeeParkingLotAssignmentEntity> findEffectiveForUpdate(
            @Param("employeeId") UUID employeeId,
            @Param("parkingLotId") UUID parkingLotId,
            @Param("statuses") Set<EmployeeParkingLotAssignmentStatus> statuses,
            @Param("at") Instant at
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        update people.employee_parking_lot_assignments
           set status = case when status in ('ACTIVE', 'SUSPENDED') then 'ENDED' else 'CANCELLED' end,
               effective_to = coalesce(effective_to, :now), updated_at = :now
         where organization_id in (:organizationIds)
           and status in ('ACTIVE', 'SUSPENDED', 'SCHEDULED', 'PENDING_REVIEW')
        """, nativeQuery = true)
    int closeAllByOrganizationIds(
            @Param("organizationIds") Collection<UUID> organizationIds,
            @Param("now") Instant now);
}
