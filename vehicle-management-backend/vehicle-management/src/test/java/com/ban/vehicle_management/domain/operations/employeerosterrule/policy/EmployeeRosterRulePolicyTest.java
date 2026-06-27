package com.ban.vehicle_management.domain.operations.employeerosterrule.policy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.domain.operations.employeerosterrule.model.EmployeeRosterRule;
import com.ban.vehicle_management.shared.enumeration.operations.AssignmentMode;
import com.ban.vehicle_management.shared.enumeration.operations.RosterRuleStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EmployeeRosterRulePolicyTest {

    private final EmployeeRosterRulePolicy policy = new EmployeeRosterRulePolicy();

    @Test
    void shouldInitializeFixedRuleAsActive() {
        EmployeeRosterRule rule = validFixedRule();
        rule.setStatus(null);

        policy.initialize(rule);

        assertEquals(RosterRuleStatus.ACTIVE, rule.getStatus());
    }

    @Test
    void shouldAcceptReliefRuleWithoutPreferredShiftAndGate() {
        EmployeeRosterRule rule = validReliefRule();

        assertDoesNotThrow(() -> policy.initialize(rule));
    }

    @Test
    void shouldRejectFixedRuleWithoutPreferredShiftType() {
        EmployeeRosterRule rule = validFixedRule();
        rule.setPreferredShiftType(null);

        assertThrows(BadRequestException.class, () -> policy.initialize(rule));
    }

    @Test
    void shouldRejectFixedRuleWithoutPreferredGateId() {
        EmployeeRosterRule rule = validFixedRule();
        rule.setPreferredGateId(null);

        assertThrows(BadRequestException.class, () -> policy.initialize(rule));
    }

    @Test
    void shouldRejectReliefRuleWithPreferredShiftType() {
        EmployeeRosterRule rule = validReliefRule();
        rule.setPreferredShiftType(ShiftType.MORNING);

        assertThrows(BadRequestException.class, () -> policy.initialize(rule));
    }

    @Test
    void shouldRejectReliefRuleWithPreferredGateId() {
        EmployeeRosterRule rule = validReliefRule();
        rule.setPreferredGateId(UUID.randomUUID());

        assertThrows(BadRequestException.class, () -> policy.initialize(rule));
    }

    @Test
    void shouldRejectEffectiveToBeforeEffectiveFrom() {
        EmployeeRosterRule rule = validFixedRule();
        rule.setEffectiveTo(rule.getEffectiveFrom().minusDays(1));

        assertThrows(BadRequestException.class, () -> policy.initialize(rule));
    }

    @Test
    void shouldRejectActivationWhenRuleHasExpired() {
        EmployeeRosterRule rule = validFixedRule();
        rule.setStatus(RosterRuleStatus.INACTIVE);
        rule.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        rule.setEffectiveTo(LocalDate.of(2026, 6, 1));

        assertThrows(
                ConflictException.class,
                () -> policy.activate(rule, LocalDate.of(2026, 6, 24))
        );
    }

    @Test
    void shouldDetectOverlappingOpenEndedPeriods() {
        EmployeeRosterRule first = validFixedRule();
        first.setEffectiveFrom(LocalDate.of(2026, 7, 1));
        first.setEffectiveTo(null);

        EmployeeRosterRule second = validFixedRule();
        second.setEffectiveFrom(LocalDate.of(2026, 8, 1));
        second.setEffectiveTo(LocalDate.of(2026, 8, 31));

        assertTrue(policy.periodsOverlap(first, second));
    }

    @Test
    void shouldTreatSharedBoundaryDateAsOverlap() {
        EmployeeRosterRule first = validFixedRule();
        first.setEffectiveFrom(LocalDate.of(2026, 7, 1));
        first.setEffectiveTo(LocalDate.of(2026, 7, 31));

        EmployeeRosterRule second = validFixedRule();
        second.setEffectiveFrom(LocalDate.of(2026, 7, 31));
        second.setEffectiveTo(LocalDate.of(2026, 8, 31));

        assertTrue(policy.periodsOverlap(first, second));
    }

    @Test
    void shouldNotOverlapSeparatedPeriods() {
        EmployeeRosterRule first = validFixedRule();
        first.setEffectiveFrom(LocalDate.of(2026, 7, 1));
        first.setEffectiveTo(LocalDate.of(2026, 7, 31));

        EmployeeRosterRule second = validFixedRule();
        second.setEffectiveFrom(LocalDate.of(2026, 8, 1));
        second.setEffectiveTo(null);

        assertFalse(policy.periodsOverlap(first, second));
    }

    @Test
    void shouldDetermineWhetherRuleIsEffectiveOnDate() {
        EmployeeRosterRule rule = validFixedRule();
        rule.setEffectiveFrom(LocalDate.of(2026, 7, 1));
        rule.setEffectiveTo(LocalDate.of(2026, 7, 31));

        assertTrue(policy.isEffectiveOn(rule, LocalDate.of(2026, 7, 1)));
        assertTrue(policy.isEffectiveOn(rule, LocalDate.of(2026, 7, 31)));
        assertFalse(policy.isEffectiveOn(rule, LocalDate.of(2026, 8, 1)));
    }

    @Test
    void shouldDeactivateRule() {
        EmployeeRosterRule rule = validFixedRule();

        policy.deactivate(rule);

        assertEquals(RosterRuleStatus.INACTIVE, rule.getStatus());
    }

    private EmployeeRosterRule validFixedRule() {
        EmployeeRosterRule rule = baseRule();
        rule.setPreferredShiftType(ShiftType.MORNING);
        rule.setPreferredGateId(UUID.randomUUID());
        rule.setAssignmentMode(AssignmentMode.FIXED);
        return rule;
    }

    private EmployeeRosterRule validReliefRule() {
        EmployeeRosterRule rule = baseRule();
        rule.setPreferredShiftType(null);
        rule.setPreferredGateId(null);
        rule.setAssignmentMode(AssignmentMode.RELIEF);
        return rule;
    }

    private EmployeeRosterRule baseRule() {
        EmployeeRosterRule rule = new EmployeeRosterRule();
        rule.setRosterRuleId(UUID.randomUUID());
        rule.setParkingLotId(UUID.randomUUID());
        rule.setEmployeeId(UUID.randomUUID());
        rule.setWeeklyDayOff(DayOfWeek.MONDAY);
        rule.setEffectiveFrom(LocalDate.of(2026, 7, 1));
        rule.setEffectiveTo(null);
        rule.setStatus(RosterRuleStatus.ACTIVE);
        return rule;
    }
}
