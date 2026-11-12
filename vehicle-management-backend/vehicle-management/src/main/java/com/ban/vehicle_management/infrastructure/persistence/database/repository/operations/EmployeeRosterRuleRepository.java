package com.ban.vehicle_management.infrastructure.persistence.database.repository.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.EmployeeRosterRuleEntity;
import com.ban.vehicle_management.shared.enumeration.operations.RosterRuleStatus;
import com.ban.vehicle_management.infrastructure.persistence.database.projection.iam.AccountDeletionParkingResourceProjection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import java.util.Collection;
import org.springframework.data.repository.query.Param;

public interface EmployeeRosterRuleRepository
        extends JpaRepository<EmployeeRosterRuleEntity, UUID>,
        JpaSpecificationExecutor<EmployeeRosterRuleEntity> {

    @Query(value = """
        select rule.roster_rule_id as "resourceId", lot.organization_id as "organizationId",
               rule.parking_lot_id as "parkingLotId"
        from operations.employee_roster_rules rule
        join parking.parking_lots lot on lot.parking_lot_id=rule.parking_lot_id
        where rule.employee_id=:employeeId and rule.status='ACTIVE'
          and (rule.effective_to is null or rule.effective_to>=current_date)
        """, nativeQuery = true)
    List<AccountDeletionParkingResourceProjection> findDeletionBlockingRosterRules(
            @Param("employeeId") UUID employeeId);

    List<EmployeeRosterRuleEntity> findAllByParkingLotIdAndStatus(
            UUID parkingLotId,
            RosterRuleStatus status
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        update operations.employee_roster_rules rule
           set status = 'INACTIVE', updated_at = :now
          from parking.parking_lots lot
         where lot.parking_lot_id = rule.parking_lot_id
           and lot.organization_id in (:organizationIds) and rule.status = 'ACTIVE'
        """, nativeQuery = true)
    int deactivateAllByOrganizationIds(
            @Param("organizationIds") Collection<UUID> organizationIds,
            @Param("now") java.time.Instant now);
}
