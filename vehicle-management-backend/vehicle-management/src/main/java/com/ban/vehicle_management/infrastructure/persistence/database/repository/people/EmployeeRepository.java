package com.ban.vehicle_management.infrastructure.persistence.database.repository.people;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.EmployeeEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends JpaRepository<EmployeeEntity, UUID>, JpaSpecificationExecutor<EmployeeEntity> {

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


