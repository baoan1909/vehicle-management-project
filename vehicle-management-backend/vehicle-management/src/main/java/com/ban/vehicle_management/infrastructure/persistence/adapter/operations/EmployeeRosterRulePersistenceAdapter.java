package com.ban.vehicle_management.infrastructure.persistence.adapter.operations;

import com.ban.vehicle_management.application.operations.employeerosterrule.port.out.EmployeeRosterRulePortOut;
import com.ban.vehicle_management.domain.operations.employeerosterrule.model.EmployeeRosterRule;
import com.ban.vehicle_management.infrastructure.mapper.operations.EmployeeRosterRulePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.EmployeeRosterRuleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.operations.EmployeeRosterRuleSpecifications;
import com.ban.vehicle_management.shared.enumeration.operations.AssignmentMode;
import com.ban.vehicle_management.shared.enumeration.operations.RosterRuleStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class EmployeeRosterRulePersistenceAdapter
        implements EmployeeRosterRulePortOut {

    private final EmployeeRosterRuleRepository repository;
    private final EmployeeRosterRulePersistenceMapper mapper;

    public EmployeeRosterRulePersistenceAdapter(
            EmployeeRosterRuleRepository repository,
            EmployeeRosterRulePersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public EmployeeRosterRule save(EmployeeRosterRule rule) {
        return mapper.toDomain(
                repository.saveAndFlush(mapper.toEntity(rule))
        );
    }

    @Override
    public Optional<EmployeeRosterRule> findById(UUID rosterRuleId) {
        return repository.findById(rosterRuleId)
                .map(mapper::toDomain);
    }

    @Override
    public List<EmployeeRosterRule> findAll(
            UUID parkingLotId,
            UUID employeeId,
            ShiftType preferredShiftType,
            UUID preferredGateId,
            DayOfWeek weeklyDayOff,
            AssignmentMode assignmentMode,
            RosterRuleStatus status,
            LocalDate effectiveDate
    ) {
        return repository.findAll(
                        EmployeeRosterRuleSpecifications.withFilters(
                                parkingLotId,
                                employeeId,
                                preferredShiftType,
                                preferredGateId,
                                weeklyDayOff,
                                assignmentMode,
                                status,
                                effectiveDate
                        )
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<EmployeeRosterRule> findActiveByParkingLotId(
            UUID parkingLotId
    ) {
        return repository.findAllByParkingLotIdAndStatus(
                        parkingLotId,
                        RosterRuleStatus.ACTIVE
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}