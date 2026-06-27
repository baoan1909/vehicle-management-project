package com.ban.vehicle_management.infrastructure.persistence.database.repository.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.EmployeeRosterRuleEntity;
import com.ban.vehicle_management.shared.enumeration.operations.RosterRuleStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EmployeeRosterRuleRepository
        extends JpaRepository<EmployeeRosterRuleEntity, UUID>,
        JpaSpecificationExecutor<EmployeeRosterRuleEntity> {

    List<EmployeeRosterRuleEntity> findAllByParkingLotIdAndStatus(
            UUID parkingLotId,
            RosterRuleStatus status
    );
}