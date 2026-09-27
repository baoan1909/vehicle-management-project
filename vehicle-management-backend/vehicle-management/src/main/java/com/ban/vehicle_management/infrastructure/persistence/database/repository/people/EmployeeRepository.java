package com.ban.vehicle_management.infrastructure.persistence.database.repository.people;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.EmployeeEntity;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends JpaRepository<EmployeeEntity, UUID>, JpaSpecificationExecutor<EmployeeEntity> {

    @Query(value = """
        SELECT employee.employee_id
        FROM people.employees employee
        WHERE EXISTS (
            SELECT 1
            FROM operations.shift_assignments assignment
            JOIN operations.shifts shift ON shift.shift_id = assignment.shift_id
            WHERE assignment.employee_id = employee.employee_id
              AND shift.parking_lot_id = :parkingLotId
              AND assignment.status <> 'REMOVED'
              AND shift.status <> 'CANCELLED'
        ) OR EXISTS (
            SELECT 1
            FROM iam.accounts account
            JOIN iam.organization_memberships membership ON membership.account_id = account.account_id
            JOIN iam.member_parking_lot_scopes scope
              ON scope.organization_membership_id = membership.organization_membership_id
            WHERE account.user_profile_id = employee.user_profile_id
              AND membership.status = 'ACTIVE'
              AND scope.parking_lot_id = :parkingLotId
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


