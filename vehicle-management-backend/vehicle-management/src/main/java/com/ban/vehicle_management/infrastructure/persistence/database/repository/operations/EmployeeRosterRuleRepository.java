package com.ban.vehicle_management.infrastructure.persistence.database.repository.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.EmployeeRosterRuleEntity;
import com.ban.vehicle_management.shared.enumeration.operations.RosterRuleStatus;
import com.ban.vehicle_management.infrastructure.persistence.database.projection.iam.AccountDeletionParkingResourceProjection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeRosterRuleRepository
        extends JpaRepository<EmployeeRosterRuleEntity, UUID>,
        JpaSpecificationExecutor<EmployeeRosterRuleEntity> {

    @Query(value = """
        select roster_rule_id as "resourceId", parking_lot_id as "parkingLotId"
        from operations.employee_roster_rules
        where employee_id=:employeeId and status='ACTIVE'
          and (effective_to is null or effective_to>=current_date)
        """, nativeQuery = true)
    List<AccountDeletionParkingResourceProjection> findDeletionBlockingRosterRules(
            @Param("employeeId") UUID employeeId);

    List<EmployeeRosterRuleEntity> findAllByParkingLotIdAndStatus(
            UUID parkingLotId,
            RosterRuleStatus status
    );
}
