package com.ban.vehicle_management.infrastructure.persistence.database.repository.people;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.EmployeeEntity;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends JpaRepository<EmployeeEntity, UUID>, JpaSpecificationExecutor<EmployeeEntity> {

    @Query(value = """
        select employee.employee_id from iam.accounts account
        join people.employees employee on employee.user_profile_id=account.user_profile_id
        where account.account_id=:accountId and employee.status<>'DELETED'
        limit 1
        """, nativeQuery = true)
    Optional<UUID> findDeletionEmployeeIdByAccountId(@Param("accountId") UUID accountId);

    @Modifying
    @Query(value = """
        update people.employees set employee_code=:marker, job_title=null, hired_at=null,
               status='DELETED', updated_at=:now where user_profile_id=:profileId
        """, nativeQuery = true)
    int tombstoneByUserProfileId(
            @Param("profileId") UUID profileId,
            @Param("marker") String marker,
            @Param("now") java.time.Instant now);

    @Query(value = """
        SELECT DISTINCT employee.employee_id
        FROM people.employees employee
        JOIN people.user_profiles profile ON profile.user_profile_id = employee.user_profile_id
        JOIN iam.accounts account ON account.user_profile_id = profile.user_profile_id
        JOIN iam.roles role ON role.role_id = account.role_id
        JOIN parking.parking_lots lot ON lot.parking_lot_id = :parkingLotId
        JOIN iam.organization_memberships membership
          ON membership.account_id = account.account_id
         AND membership.organization_id = lot.organization_id
         AND membership.status = 'ACTIVE'
        WHERE employee.status <> 'DELETED'
          AND profile.status <> 'DELETED'
          AND account.status NOT IN ('PENDING_DELETION', 'DELETED')
          AND (
            (role.code = 'PARKING_MANAGER' AND EXISTS (
                SELECT 1 FROM iam.parking_lot_management_assignments assignment
                WHERE assignment.manager_account_id = account.account_id
                  AND assignment.organization_id = lot.organization_id
                  AND assignment.parking_lot_id = lot.parking_lot_id
                  AND assignment.status = 'ACTIVE'
                  AND assignment.effective_from <= CURRENT_TIMESTAMP
                  AND (assignment.effective_to IS NULL OR assignment.effective_to > CURRENT_TIMESTAMP)
            )) OR (role.code = 'EMPLOYEE' AND EXISTS (
                SELECT 1 FROM people.employee_parking_lot_assignments assignment
                WHERE assignment.employee_id = employee.employee_id
                  AND assignment.organization_id = lot.organization_id
                  AND assignment.parking_lot_id = lot.parking_lot_id
                  AND assignment.status IN ('ACTIVE', 'SCHEDULED')
                  AND assignment.effective_from <= CURRENT_TIMESTAMP
                  AND (assignment.effective_to IS NULL OR assignment.effective_to > CURRENT_TIMESTAMP)
            ))
          )
        """, nativeQuery = true)
    List<UUID> findEmployeeIdsLinkedToParkingLot(@Param("parkingLotId") UUID parkingLotId);

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByEmployeeCodeAndEmployeeIdNot(String employeeCode, UUID employeeId);

    boolean existsByUserProfileId(UUID userProfileId);

    Optional<EmployeeEntity> findByUserProfileId(UUID userProfileId);

    @Query("""
        SELECT employee
        FROM EmployeeEntity employee
        LEFT JOIN FETCH employee.userProfile userProfile
        LEFT JOIN FETCH userProfile.account account
        LEFT JOIN FETCH account.role
        WHERE employee.employeeId = :employeeId
          AND employee.status <> com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus.DELETED
          AND userProfile.status <> com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus.DELETED
          AND account.status NOT IN (
              com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.PENDING_DELETION,
              com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED)
        """)
    Optional<EmployeeEntity> findDetailedByEmployeeId(@Param("employeeId") UUID employeeId);

    @Query("""
        SELECT employee
        FROM EmployeeEntity employee
        LEFT JOIN FETCH employee.userProfile userProfile
        LEFT JOIN FETCH userProfile.account fetchedAccount
        LEFT JOIN FETCH fetchedAccount.role
        JOIN AccountEntity account
          ON account.userProfileId = employee.userProfileId
        WHERE account.accountId = :accountId
          AND employee.status <> com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus.DELETED
          AND userProfile.status <> com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus.DELETED
          AND fetchedAccount.status NOT IN (
              com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.PENDING_DELETION,
              com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED)
        """)
    Optional<EmployeeEntity> findByAccountId(
            @Param("accountId") UUID accountId
    );

    @Query("""
        SELECT account.accountId
        FROM EmployeeEntity employee
        JOIN AccountEntity account
          ON account.userProfileId = employee.userProfileId
        WHERE employee.employeeId = :employeeId
          AND employee.status <> com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus.DELETED
          AND account.status NOT IN (
              com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.PENDING_DELETION,
              com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED)
        """)
    Optional<UUID> findAccountIdByEmployeeId(
            @Param("employeeId") UUID employeeId
    );

    @Query("""
        SELECT CASE WHEN COUNT(employee) > 0
                    THEN TRUE ELSE FALSE END
        FROM EmployeeEntity employee
        JOIN AccountEntity account
          ON account.userProfileId = employee.userProfileId
        JOIN RoleEntity role
          ON role.roleId = account.roleId
        WHERE employee.employeeId = :employeeId
          AND role.code = :roleCode
        """)
    boolean existsByEmployeeIdAndAccountRoleCode(
            @Param("employeeId") UUID employeeId,
            @Param("roleCode") String roleCode
    );
}


