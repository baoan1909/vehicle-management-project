package com.ban.vehicle_management.application.operations.employeerosterrule.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.employeerosterrule.port.out.EmployeeRosterRulePortOut;
import com.ban.vehicle_management.application.operations.shifttemplate.port.out.ShiftTemplatePortOut;
import com.ban.vehicle_management.application.parking.gate.port.out.GatePortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.application.people.employee.port.out.EmployeePortOut;
import com.ban.vehicle_management.domain.operations.employeerosterrule.model.EmployeeRosterRule;
import com.ban.vehicle_management.domain.parking.gate.model.Gate;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.shared.enumeration.operations.AssignmentMode;
import com.ban.vehicle_management.shared.enumeration.operations.RosterRuleStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import com.ban.vehicle_management.shared.enumeration.parking.GateStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
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

@ExtendWith(MockitoExtension.class)
class EmployeeRosterRuleUseCaseImplTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private EmployeeRosterRulePortOut rosterRulePortOut;

    @Mock
    private ParkingLotPortOut parkingLotPortOut;

    @Mock
    private EmployeePortOut employeePortOut;

    @Mock
    private GatePortOut gatePortOut;

    @Mock
    private ZonePortOut zonePortOut;

    @Mock
    private ShiftTemplatePortOut shiftTemplatePortOut;

    @InjectMocks
    private EmployeeRosterRuleUseCaseImpl useCase;

    @Test
    void shouldCreateFixedRuleWhenReferencesAndScheduleAreValid() {
        EmployeeRosterRule rule = fixedRule();
        stubValidFixedReferences(rule);
        when(rosterRulePortOut.findActiveByParkingLotId(rule.getParkingLotId()))
                .thenReturn(List.of());
        when(rosterRulePortOut.save(any(EmployeeRosterRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EmployeeRosterRule result = useCase.createRule(rule);

        verify(currentAccountPortIn).requirePermission("SHIFT_ASSIGNMENT_CREATE_ALL");
        assertNotNull(result.getRosterRuleId());
        assertEquals(RosterRuleStatus.ACTIVE, result.getStatus());
        verify(rosterRulePortOut).save(rule);
    }

    @Test
    void shouldCreateReliefRuleWithoutCheckingGateOrShiftTemplate() {
        EmployeeRosterRule rule = reliefRule();
        stubValidCommonReferences(rule);
        when(rosterRulePortOut.findActiveByParkingLotId(rule.getParkingLotId()))
                .thenReturn(List.of());
        when(rosterRulePortOut.save(any(EmployeeRosterRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EmployeeRosterRule result = useCase.createRule(rule);

        assertEquals(AssignmentMode.RELIEF, result.getAssignmentMode());
        verifyNoInteractions(gatePortOut, zonePortOut, shiftTemplatePortOut);
    }

    @Test
    void shouldRejectCreateWhenParkingLotIsClosed() {
        EmployeeRosterRule rule = fixedRule();
        when(parkingLotPortOut.findById(rule.getParkingLotId()))
                .thenReturn(Optional.of(parkingLot(rule.getParkingLotId(), ParkingLotStatus.CLOSED)));

        assertThrows(ConflictException.class, () -> useCase.createRule(rule));

        verify(rosterRulePortOut, never()).save(any(EmployeeRosterRule.class));
    }

    @Test
    void shouldRejectCreateWhenEmployeeIsNotActive() {
        EmployeeRosterRule rule = fixedRule();
        when(parkingLotPortOut.findById(rule.getParkingLotId()))
                .thenReturn(Optional.of(parkingLot(rule.getParkingLotId(), ParkingLotStatus.ACTIVE)));
        when(employeePortOut.findById(rule.getEmployeeId()))
                .thenReturn(Optional.of(employee(rule.getEmployeeId(), EmployeeStatus.INACTIVE)));

        assertThrows(ConflictException.class, () -> useCase.createRule(rule));

        verify(rosterRulePortOut, never()).save(any(EmployeeRosterRule.class));
    }

    @Test
    void shouldRejectFixedRuleWhenGateBelongsToAnotherParkingLot() {
        EmployeeRosterRule rule = fixedRule();
        UUID zoneId = UUID.randomUUID();
        stubValidCommonReferences(rule);
        when(shiftTemplatePortOut.existsActiveByParkingLotIdAndShiftType(
                rule.getParkingLotId(),
                rule.getPreferredShiftType()
        )).thenReturn(true);
        when(gatePortOut.findById(rule.getPreferredGateId()))
                .thenReturn(Optional.of(gate(rule.getPreferredGateId(), zoneId)));
        when(zonePortOut.findById(zoneId))
                .thenReturn(Optional.of(zone(zoneId, UUID.randomUUID())));

        assertThrows(ConflictException.class, () -> useCase.createRule(rule));

        verify(rosterRulePortOut, never()).save(any(EmployeeRosterRule.class));
    }

    @Test
    void shouldRejectOverlappingRuleForSameEmployee() {
        EmployeeRosterRule candidate = fixedRule();
        EmployeeRosterRule existing = fixedRule();
        existing.setEmployeeId(candidate.getEmployeeId());
        existing.setWeeklyDayOff(DayOfWeek.TUESDAY);
        existing.setPreferredGateId(UUID.randomUUID());
        stubValidFixedReferences(candidate);
        when(rosterRulePortOut.findActiveByParkingLotId(candidate.getParkingLotId()))
                .thenReturn(List.of(existing));

        assertThrows(ConflictException.class, () -> useCase.createRule(candidate));

        verify(rosterRulePortOut, never()).save(any(EmployeeRosterRule.class));
    }

    @Test
    void shouldRejectOverlappingWeeklyDayOff() {
        EmployeeRosterRule candidate = fixedRule();
        EmployeeRosterRule existing = fixedRule();
        existing.setEmployeeId(UUID.randomUUID());
        existing.setPreferredGateId(UUID.randomUUID());
        existing.setWeeklyDayOff(candidate.getWeeklyDayOff());
        stubValidFixedReferences(candidate);
        when(rosterRulePortOut.findActiveByParkingLotId(candidate.getParkingLotId()))
                .thenReturn(List.of(existing));

        assertThrows(ConflictException.class, () -> useCase.createRule(candidate));
    }

    @Test
    void shouldRejectOverlappingFixedPosition() {
        EmployeeRosterRule candidate = fixedRule();
        EmployeeRosterRule existing = fixedRule();
        existing.setEmployeeId(UUID.randomUUID());
        existing.setWeeklyDayOff(DayOfWeek.TUESDAY);
        existing.setPreferredShiftType(candidate.getPreferredShiftType());
        existing.setPreferredGateId(candidate.getPreferredGateId());
        stubValidFixedReferences(candidate);
        when(rosterRulePortOut.findActiveByParkingLotId(candidate.getParkingLotId()))
                .thenReturn(List.of(existing));

        assertThrows(ConflictException.class, () -> useCase.createRule(candidate));
    }

    @Test
    void shouldRejectSecondOverlappingReliefRule() {
        EmployeeRosterRule candidate = reliefRule();
        EmployeeRosterRule existing = reliefRule();
        existing.setEmployeeId(UUID.randomUUID());
        existing.setWeeklyDayOff(DayOfWeek.TUESDAY);
        stubValidCommonReferences(candidate);
        when(rosterRulePortOut.findActiveByParkingLotId(candidate.getParkingLotId()))
                .thenReturn(List.of(existing));

        assertThrows(ConflictException.class, () -> useCase.createRule(candidate));
    }

    @Test
    void shouldUpdateActiveRuleAndExcludeItselfFromConflictCheck() {
        EmployeeRosterRule existing = fixedRule();
        existing.setRosterRuleId(UUID.randomUUID());
        EmployeeRosterRule request = fixedRule();
        request.setWeeklyDayOff(DayOfWeek.SATURDAY);
        request.setPreferredGateId(existing.getPreferredGateId());
        stubValidFixedReferences(existing);
        when(rosterRulePortOut.findById(existing.getRosterRuleId()))
                .thenReturn(Optional.of(existing));
        when(rosterRulePortOut.findActiveByParkingLotId(existing.getParkingLotId()))
                .thenReturn(List.of(existing));
        when(rosterRulePortOut.save(existing)).thenReturn(existing);

        EmployeeRosterRule result = useCase.updateRule(existing.getRosterRuleId(), request);

        verify(currentAccountPortIn).requirePermission("SHIFT_ASSIGNMENT_UPDATE_ALL");
        assertEquals(DayOfWeek.SATURDAY, result.getWeeklyDayOff());
        verify(rosterRulePortOut).save(existing);
    }

    @Test
    void shouldRejectActivationWhenRuleHasExpired() {
        EmployeeRosterRule existing = fixedRule();
        existing.setRosterRuleId(UUID.randomUUID());
        existing.setStatus(RosterRuleStatus.INACTIVE);
        existing.setEffectiveFrom(LocalDate.of(1999, 1, 1));
        existing.setEffectiveTo(LocalDate.of(2000, 1, 1));
        stubValidFixedReferences(existing);
        when(rosterRulePortOut.findById(existing.getRosterRuleId()))
                .thenReturn(Optional.of(existing));

        assertThrows(
                ConflictException.class,
                () -> useCase.activateRule(existing.getRosterRuleId())
        );

        verify(rosterRulePortOut, never()).save(any(EmployeeRosterRule.class));
    }

    @Test
    void shouldSoftDeleteActiveRule() {
        EmployeeRosterRule existing = fixedRule();
        existing.setRosterRuleId(UUID.randomUUID());
        when(rosterRulePortOut.findById(existing.getRosterRuleId()))
                .thenReturn(Optional.of(existing));
        when(rosterRulePortOut.save(existing)).thenReturn(existing);

        useCase.deleteRule(existing.getRosterRuleId());

        verify(currentAccountPortIn).requirePermission("SHIFT_ASSIGNMENT_DELETE_ALL");
        assertEquals(RosterRuleStatus.INACTIVE, existing.getStatus());
        verify(rosterRulePortOut).save(existing);
    }

    @Test
    void shouldDoNothingWhenDeletingInactiveRule() {
        EmployeeRosterRule existing = fixedRule();
        existing.setRosterRuleId(UUID.randomUUID());
        existing.setStatus(RosterRuleStatus.INACTIVE);
        when(rosterRulePortOut.findById(existing.getRosterRuleId()))
                .thenReturn(Optional.of(existing));

        useCase.deleteRule(existing.getRosterRuleId());

        verify(rosterRulePortOut, never()).save(any(EmployeeRosterRule.class));
    }

    @Test
    void shouldDelegateListFiltersAndRequireReadPermission() {
        UUID parkingLotId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID gateId = UUID.randomUUID();
        LocalDate effectiveDate = LocalDate.of(2026, 7, 1);
        when(rosterRulePortOut.findAll(
                parkingLotId,
                employeeId,
                ShiftType.NIGHT,
                gateId,
                DayOfWeek.FRIDAY,
                AssignmentMode.FIXED,
                RosterRuleStatus.ACTIVE,
                effectiveDate
        )).thenReturn(List.of(new EmployeeRosterRule()));

        List<EmployeeRosterRule> result = useCase.getRules(
                parkingLotId,
                employeeId,
                ShiftType.NIGHT,
                gateId,
                DayOfWeek.FRIDAY,
                AssignmentMode.FIXED,
                RosterRuleStatus.ACTIVE,
                effectiveDate
        );

        assertEquals(1, result.size());
        verify(currentAccountPortIn).requirePermission("SHIFT_ASSIGNMENT_READ_ALL");
    }

    @Test
    void shouldThrowWhenRuleDoesNotExist() {
        UUID ruleId = UUID.randomUUID();
        when(rosterRulePortOut.findById(ruleId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.getRuleById(ruleId));
    }

    private void stubValidCommonReferences(EmployeeRosterRule rule) {
        when(parkingLotPortOut.findById(rule.getParkingLotId()))
                .thenReturn(Optional.of(parkingLot(rule.getParkingLotId(), ParkingLotStatus.ACTIVE)));
        when(employeePortOut.findById(rule.getEmployeeId()))
                .thenReturn(Optional.of(employee(rule.getEmployeeId(), EmployeeStatus.ACTIVE)));
    }

    private void stubValidFixedReferences(EmployeeRosterRule rule) {
        UUID zoneId = UUID.randomUUID();
        stubValidCommonReferences(rule);
        when(shiftTemplatePortOut.existsActiveByParkingLotIdAndShiftType(
                rule.getParkingLotId(),
                rule.getPreferredShiftType()
        )).thenReturn(true);
        when(gatePortOut.findById(rule.getPreferredGateId()))
                .thenReturn(Optional.of(gate(rule.getPreferredGateId(), zoneId)));
        when(zonePortOut.findById(zoneId))
                .thenReturn(Optional.of(zone(zoneId, rule.getParkingLotId())));
    }

    private EmployeeRosterRule fixedRule() {
        EmployeeRosterRule rule = baseRule();
        rule.setPreferredShiftType(ShiftType.MORNING);
        rule.setPreferredGateId(UUID.randomUUID());
        rule.setAssignmentMode(AssignmentMode.FIXED);
        return rule;
    }

    private EmployeeRosterRule reliefRule() {
        EmployeeRosterRule rule = baseRule();
        rule.setPreferredShiftType(null);
        rule.setPreferredGateId(null);
        rule.setAssignmentMode(AssignmentMode.RELIEF);
        return rule;
    }

    private EmployeeRosterRule baseRule() {
        EmployeeRosterRule rule = new EmployeeRosterRule();
        rule.setParkingLotId(UUID.randomUUID());
        rule.setEmployeeId(UUID.randomUUID());
        rule.setWeeklyDayOff(DayOfWeek.MONDAY);
        rule.setEffectiveFrom(LocalDate.of(2026, 7, 1));
        rule.setEffectiveTo(null);
        rule.setStatus(RosterRuleStatus.ACTIVE);
        return rule;
    }

    private ParkingLot parkingLot(UUID parkingLotId, ParkingLotStatus status) {
        ParkingLot parkingLot = new ParkingLot();
        parkingLot.setParkingLotId(parkingLotId);
        parkingLot.setCode("HCMUTE");
        parkingLot.setName("Bai xe HCMUTE");
        parkingLot.setTotalCapacity(1000);
        parkingLot.setStatus(status);
        return parkingLot;
    }

    private Employee employee(UUID employeeId, EmployeeStatus status) {
        Employee employee = new Employee();
        employee.setEmployeeId(employeeId);
        employee.setEmployeeCode("EMP001");
        employee.setStatus(status);
        return employee;
    }

    private Gate gate(UUID gateId, UUID zoneId) {
        Gate gate = new Gate();
        gate.setGateId(gateId);
        gate.setZoneId(zoneId);
        gate.setCode("GATE-01");
        gate.setName("Gate 01");
        gate.setStatus(GateStatus.ACTIVE);
        return gate;
    }

    private Zone zone(UUID zoneId, UUID parkingLotId) {
        Zone zone = new Zone();
        zone.setZoneId(zoneId);
        zone.setParkingLotId(parkingLotId);
        return zone;
    }
}
