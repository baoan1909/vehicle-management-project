package com.ban.vehicle_management.application.operations.employeerosterrule.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.employeerosterrule.port.in.EmployeeRosterRulePortIn;
import com.ban.vehicle_management.application.operations.employeerosterrule.port.out.EmployeeRosterRulePortOut;
import com.ban.vehicle_management.application.operations.shifttemplate.port.out.ShiftTemplatePortOut;
import com.ban.vehicle_management.application.parking.gate.port.out.GatePortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.application.people.employee.port.out.EmployeePortOut;
import com.ban.vehicle_management.domain.operations.employeerosterrule.model.EmployeeRosterRule;
import com.ban.vehicle_management.domain.operations.employeerosterrule.policy.EmployeeRosterRulePolicy;
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
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeRosterRuleUseCaseImpl
        implements EmployeeRosterRulePortIn {

    private static final String CREATE_PERMISSION =
            "SHIFT_ASSIGNMENT_CREATE_ALL";
    private static final String READ_PERMISSION =
            "SHIFT_ASSIGNMENT_READ_ALL";
    private static final String UPDATE_PERMISSION =
            "SHIFT_ASSIGNMENT_UPDATE_ALL";
    private static final String DELETE_PERMISSION =
            "SHIFT_ASSIGNMENT_DELETE_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final EmployeeRosterRulePortOut rosterRulePortOut;
    private final ParkingLotPortOut parkingLotPortOut;
    private final EmployeePortOut employeePortOut;
    private final GatePortOut gatePortOut;
    private final ZonePortOut zonePortOut;
    private final ShiftTemplatePortOut shiftTemplatePortOut;
    private final EmployeeRosterRulePolicy policy =
            new EmployeeRosterRulePolicy();

    public EmployeeRosterRuleUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            EmployeeRosterRulePortOut rosterRulePortOut,
            ParkingLotPortOut parkingLotPortOut,
            EmployeePortOut employeePortOut,
            GatePortOut gatePortOut,
            ZonePortOut zonePortOut,
            ShiftTemplatePortOut shiftTemplatePortOut
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.rosterRulePortOut = rosterRulePortOut;
        this.parkingLotPortOut = parkingLotPortOut;
        this.employeePortOut = employeePortOut;
        this.gatePortOut = gatePortOut;
        this.zonePortOut = zonePortOut;
        this.shiftTemplatePortOut = shiftTemplatePortOut;
    }

    @Override
    @Transactional
    public EmployeeRosterRule createRule(EmployeeRosterRule rule) {
        currentAccountPortIn.requirePermission(CREATE_PERMISSION);
        policy.initialize(rule);

        validateOperationalReferences(rule);
        ensureNoActiveConflicts(rule, null);

        rule.setRosterRuleId(UUID.randomUUID());
        return rosterRulePortOut.save(rule);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeRosterRule getRuleById(UUID rosterRuleId) {
        currentAccountPortIn.requirePermission(READ_PERMISSION);
        return findExistingRule(rosterRuleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeRosterRule> getRules(
            UUID parkingLotId,
            UUID employeeId,
            ShiftType preferredShiftType,
            UUID preferredGateId,
            DayOfWeek weeklyDayOff,
            AssignmentMode assignmentMode,
            RosterRuleStatus status,
            LocalDate effectiveDate
    ) {
        currentAccountPortIn.requirePermission(READ_PERMISSION);

        return rosterRulePortOut.findAll(
                parkingLotId,
                employeeId,
                preferredShiftType,
                preferredGateId,
                weeklyDayOff,
                assignmentMode,
                status,
                effectiveDate
        );
    }

    @Override
    @Transactional
    public EmployeeRosterRule updateRule(
            UUID rosterRuleId,
            EmployeeRosterRule request
    ) {
        currentAccountPortIn.requirePermission(UPDATE_PERMISSION);

        EmployeeRosterRule existing =
                findExistingRule(rosterRuleId);

        existing.setPreferredShiftType(
                request.getPreferredShiftType()
        );
        existing.setPreferredGateId(request.getPreferredGateId());
        existing.setWeeklyDayOff(request.getWeeklyDayOff());
        existing.setAssignmentMode(request.getAssignmentMode());
        existing.setEffectiveFrom(request.getEffectiveFrom());
        existing.setEffectiveTo(request.getEffectiveTo());

        policy.validateState(existing);
        validateOperationalReferences(existing);

        if (existing.getStatus() == RosterRuleStatus.ACTIVE) {
            ensureNoActiveConflicts(existing, rosterRuleId);
        }

        return rosterRulePortOut.save(existing);
    }

    @Override
    @Transactional
    public EmployeeRosterRule activateRule(UUID rosterRuleId) {
        currentAccountPortIn.requirePermission(UPDATE_PERMISSION);

        EmployeeRosterRule existing =
                findExistingRule(rosterRuleId);

        if (existing.getStatus() == RosterRuleStatus.ACTIVE) {
            return existing;
        }

        validateOperationalReferences(existing);

        policy.activate(
                existing,
                LocalDate.now(DateTimeUtils.VIETNAM_ZONE)
        );

        ensureNoActiveConflicts(existing, rosterRuleId);

        return rosterRulePortOut.save(existing);
    }

    @Override
    @Transactional
    public void deleteRule(UUID rosterRuleId) {
        currentAccountPortIn.requirePermission(DELETE_PERMISSION);

        EmployeeRosterRule existing =
                findExistingRule(rosterRuleId);

        if (existing.getStatus() == RosterRuleStatus.INACTIVE) {
            return;
        }

        policy.deactivate(existing);
        rosterRulePortOut.save(existing);
    }

    private EmployeeRosterRule findExistingRule(UUID rosterRuleId) {
        return rosterRulePortOut.findById(rosterRuleId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Employee roster rule not found"
                        )
                );
    }

    private void validateOperationalReferences(
            EmployeeRosterRule rule
    ) {
        ensureParkingLotAvailable(rule.getParkingLotId());
        ensureEmployeeActive(rule.getEmployeeId());

        if (rule.getAssignmentMode() == AssignmentMode.FIXED) {
            ensureActiveShiftTemplate(
                    rule.getParkingLotId(),
                    rule.getPreferredShiftType()
            );

            ensureGateValidForParkingLot(
                    rule.getPreferredGateId(),
                    rule.getParkingLotId()
            );
        }
    }

    private void ensureParkingLotAvailable(UUID parkingLotId) {
        ParkingLot parkingLot = parkingLotPortOut.findById(parkingLotId)
                .orElseThrow(() ->
                        new NotFoundException("Parking lot not found")
                );

        if (parkingLot.getStatus() == ParkingLotStatus.CLOSED) {
            throw new ConflictException(
                    "Cannot use roster rule for a closed parking lot"
            );
        }
    }

    private void ensureEmployeeActive(UUID employeeId) {
        Employee employee = employeePortOut.findById(employeeId)
                .orElseThrow(() ->
                        new NotFoundException("Employee not found")
                );

        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new ConflictException(
                    "Employee must be active"
            );
        }
    }

    private void ensureActiveShiftTemplate(
            UUID parkingLotId,
            ShiftType shiftType
    ) {
        boolean exists = shiftTemplatePortOut
                .existsActiveByParkingLotIdAndShiftType(
                        parkingLotId,
                        shiftType
                );

        if (!exists) {
            throw new ConflictException(
                    "Active shift template not found for shift type"
            );
        }
    }

    private void ensureGateValidForParkingLot(
            UUID gateId,
            UUID parkingLotId
    ) {
        Gate gate = gatePortOut.findById(gateId)
                .orElseThrow(() ->
                        new NotFoundException("Gate not found")
                );

        if (gate.getStatus() == GateStatus.CLOSED) {
            throw new ConflictException(
                    "Gate must not be closed"
            );
        }

        Zone zone = zonePortOut.findById(gate.getZoneId())
                .orElseThrow(() ->
                        new NotFoundException("Gate zone not found")
                );

        if (!Objects.equals(
                zone.getParkingLotId(),
                parkingLotId
        )) {
            throw new ConflictException(
                    "Gate does not belong to the selected parking lot"
            );
        }
    }

    private void ensureNoActiveConflicts(
            EmployeeRosterRule candidate,
            UUID excludedRosterRuleId
    ) {
        List<EmployeeRosterRule> activeRules =
                rosterRulePortOut.findActiveByParkingLotId(
                        candidate.getParkingLotId()
                );

        for (EmployeeRosterRule existing : activeRules) {
            if (excludedRosterRuleId != null
                    && Objects.equals(
                    existing.getRosterRuleId(),
                    excludedRosterRuleId
            )) {
                continue;
            }

            if (!policy.periodsOverlap(candidate, existing)) {
                continue;
            }

            ensureEmployeeDoesNotOverlap(candidate, existing);
            ensureWeeklyDayOffDoesNotOverlap(candidate, existing);
            ensureFixedPositionDoesNotOverlap(candidate, existing);
            ensureReliefRuleDoesNotOverlap(candidate, existing);
        }
    }

    private void ensureEmployeeDoesNotOverlap(
            EmployeeRosterRule candidate,
            EmployeeRosterRule existing
    ) {
        if (Objects.equals(
                candidate.getEmployeeId(),
                existing.getEmployeeId()
        )) {
            throw new ConflictException(
                    "Employee already has an active overlapping roster rule"
            );
        }
    }

    private void ensureWeeklyDayOffDoesNotOverlap(
            EmployeeRosterRule candidate,
            EmployeeRosterRule existing
    ) {
        if (candidate.getWeeklyDayOff()
                == existing.getWeeklyDayOff()) {
            throw new ConflictException(
                    "Weekly day off is already used by another active rule"
            );
        }
    }

    private void ensureFixedPositionDoesNotOverlap(
            EmployeeRosterRule candidate,
            EmployeeRosterRule existing
    ) {
        if (candidate.getAssignmentMode() != AssignmentMode.FIXED
                || existing.getAssignmentMode()
                != AssignmentMode.FIXED) {
            return;
        }

        boolean samePosition =
                candidate.getPreferredShiftType()
                        == existing.getPreferredShiftType()
                        && Objects.equals(
                        candidate.getPreferredGateId(),
                        existing.getPreferredGateId()
                );

        if (samePosition) {
            throw new ConflictException(
                    "Fixed shift and gate position is already assigned"
            );
        }
    }

    private void ensureReliefRuleDoesNotOverlap(
            EmployeeRosterRule candidate,
            EmployeeRosterRule existing
    ) {
        if (candidate.getAssignmentMode() == AssignmentMode.RELIEF
                && existing.getAssignmentMode()
                == AssignmentMode.RELIEF) {
            throw new ConflictException(
                    "An active relief rule already exists"
            );
        }
    }
}