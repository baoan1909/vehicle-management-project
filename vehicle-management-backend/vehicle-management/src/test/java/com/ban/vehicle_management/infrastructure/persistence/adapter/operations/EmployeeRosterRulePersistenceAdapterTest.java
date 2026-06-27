package com.ban.vehicle_management.infrastructure.persistence.adapter.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.domain.operations.employeerosterrule.model.EmployeeRosterRule;
import com.ban.vehicle_management.infrastructure.mapper.operations.EmployeeRosterRulePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.EmployeeRosterRuleEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.EmployeeRosterRuleRepository;
import com.ban.vehicle_management.shared.enumeration.operations.AssignmentMode;
import com.ban.vehicle_management.shared.enumeration.operations.RosterRuleStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class EmployeeRosterRulePersistenceAdapterTest {

    @Mock
    private EmployeeRosterRuleRepository repository;

    @Mock
    private EmployeeRosterRulePersistenceMapper mapper;

    @InjectMocks
    private EmployeeRosterRulePersistenceAdapter adapter;

    @Test
    void shouldSaveAndFlushRule() {
        EmployeeRosterRule domain = new EmployeeRosterRule();
        EmployeeRosterRuleEntity entity = new EmployeeRosterRuleEntity();

        when(mapper.toEntity(domain)).thenReturn(entity);
        when(repository.saveAndFlush(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        EmployeeRosterRule result = adapter.save(domain);

        assertSame(domain, result);
        verify(repository).saveAndFlush(entity);
    }

    @Test
    void shouldMapRuleWhenFindingById() {
        UUID ruleId = UUID.randomUUID();
        EmployeeRosterRuleEntity entity = new EmployeeRosterRuleEntity();
        EmployeeRosterRule domain = new EmployeeRosterRule();
        domain.setRosterRuleId(ruleId);

        when(repository.findById(ruleId)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<EmployeeRosterRule> result = adapter.findById(ruleId);

        assertTrue(result.isPresent());
        assertEquals(ruleId, result.get().getRosterRuleId());
    }

    @Test
    void shouldReturnMappedFilteredRules() {
        UUID parkingLotId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID gateId = UUID.randomUUID();
        EmployeeRosterRuleEntity entity = new EmployeeRosterRuleEntity();
        EmployeeRosterRule domain = new EmployeeRosterRule();

        when(repository.findAll(any(Specification.class))).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        List<EmployeeRosterRule> result = adapter.findAll(
                parkingLotId,
                employeeId,
                ShiftType.MORNING,
                gateId,
                DayOfWeek.MONDAY,
                AssignmentMode.FIXED,
                RosterRuleStatus.ACTIVE,
                LocalDate.of(2026, 7, 1)
        );

        assertEquals(1, result.size());
        assertSame(domain, result.get(0));
    }

    @Test
    void shouldFindActiveRulesByParkingLotId() {
        UUID parkingLotId = UUID.randomUUID();
        EmployeeRosterRuleEntity entity = new EmployeeRosterRuleEntity();
        EmployeeRosterRule domain = new EmployeeRosterRule();

        when(repository.findAllByParkingLotIdAndStatus(
                parkingLotId,
                RosterRuleStatus.ACTIVE
        )).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        List<EmployeeRosterRule> result = adapter.findActiveByParkingLotId(parkingLotId);

        assertEquals(1, result.size());
        verify(repository).findAllByParkingLotIdAndStatus(
                parkingLotId,
                RosterRuleStatus.ACTIVE
        );
    }
}
