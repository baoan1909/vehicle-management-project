package com.ban.vehicle_management.application.operations.shift.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.employeerosterrule.port.out.EmployeeRosterRulePortOut;
import com.ban.vehicle_management.application.operations.shift.port.in.ShiftPortIn;
import com.ban.vehicle_management.application.operations.shift.port.out.ShiftPortOut;
import com.ban.vehicle_management.application.operations.shiftassignment.port.in.ShiftAssignmentPortIn;
import com.ban.vehicle_management.application.operations.shiftassignment.port.out.ShiftAssignmentPortOut;
import com.ban.vehicle_management.application.operations.shifttemplate.port.out.ShiftTemplatePortOut;
import com.ban.vehicle_management.application.notification.notification.model.SendNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.application.parking.gate.port.out.GatePortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.application.people.employee.port.out.EmployeePortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.operations.employeerosterrule.model.EmployeeRosterRule;
import com.ban.vehicle_management.domain.operations.shift.model.Shift;
import com.ban.vehicle_management.domain.operations.shift.policy.ShiftPolicy;
import com.ban.vehicle_management.domain.operations.shiftassignment.model.ShiftAssignment;
import com.ban.vehicle_management.domain.operations.shiftassignment.policy.ShiftAssignmentPolicy;
import com.ban.vehicle_management.domain.operations.shifttemplate.model.ShiftTemplate;
import com.ban.vehicle_management.domain.operations.shifttemplate.policy.ShiftTemplatePolicy;
import com.ban.vehicle_management.domain.parking.gate.model.Gate;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.shared.enumeration.operations.AssignmentMode;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftAssignmentStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import com.ban.vehicle_management.shared.enumeration.parking.GateStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShiftUseCaseImpl implements ShiftPortIn {

    private static final String CREATE_PERMISSION = "SHIFT_CREATE_ALL";
    private static final String READ_PERMISSION = "SHIFT_READ_ALL";
    private static final String UPDATE_PERMISSION = "SHIFT_UPDATE_ALL";
    private static final String OPEN_OWN_PERMISSION = "SHIFT_OPEN_OWN";

    private static final int EXPECTED_SHIFT_COUNT = 21;
    private static final int ASSIGNMENTS_PER_SHIFT = 2;
    private static final int MAX_SHIFTS_PER_WEEK = 6;
    private static final Duration MINIMUM_REST = Duration.ofHours(8);

    private final CurrentAccountPortIn currentAccountPortIn;
    private final ShiftPortOut shiftPortOut;
    private final ShiftTemplatePortOut templatePortOut;
    private final EmployeeRosterRulePortOut rosterRulePortOut;
    private final ShiftAssignmentPortIn assignmentPortIn;
    private final ShiftAssignmentPortOut assignmentPortOut;
    private final ParkingLotPortOut parkingLotPortOut;
    private final EmployeePortOut employeePortOut;
    private final GatePortOut gatePortOut;
    private final ZonePortOut zonePortOut;
    private final NotificationPortIn notificationPortIn;

    private final ShiftPolicy shiftPolicy = new ShiftPolicy();
    private final ShiftTemplatePolicy templatePolicy = new ShiftTemplatePolicy();
    private final ShiftAssignmentPolicy assignmentPolicy = new ShiftAssignmentPolicy();

    public ShiftUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            ShiftPortOut shiftPortOut,
            ShiftTemplatePortOut templatePortOut,
            EmployeeRosterRulePortOut rosterRulePortOut,
            ShiftAssignmentPortIn assignmentPortIn,
            ShiftAssignmentPortOut assignmentPortOut,
            ParkingLotPortOut parkingLotPortOut,
            EmployeePortOut employeePortOut,
            GatePortOut gatePortOut,
            ZonePortOut zonePortOut,
            NotificationPortIn notificationPortIn
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.shiftPortOut = shiftPortOut;
        this.templatePortOut = templatePortOut;
        this.rosterRulePortOut = rosterRulePortOut;
        this.assignmentPortIn = assignmentPortIn;
        this.assignmentPortOut = assignmentPortOut;
        this.parkingLotPortOut = parkingLotPortOut;
        this.employeePortOut = employeePortOut;
        this.gatePortOut = gatePortOut;
        this.zonePortOut = zonePortOut;
        this.notificationPortIn = notificationPortIn;
    }

    @Override
    @Transactional
    public List<Shift> generateWeek(
            UUID parkingLotId,
            LocalDate weekStartDate
    ) {
        currentAccountPortIn.requirePermission(CREATE_PERMISSION);
        validateWeekRequest(parkingLotId, weekStartDate);

        LocalDate today =
                LocalDate.now(DateTimeUtils.VIETNAM_ZONE);

        if (weekStartDate.isBefore(today)) {
            throw new ConflictException(
                    "Cannot generate schedule for a past week"
            );
        }

        ParkingLot parkingLot = findParkingLot(parkingLotId);

        if (parkingLot.getStatus() == ParkingLotStatus.CLOSED) {
            throw new ConflictException(
                    "Cannot generate schedule for a closed parking lot"
            );
        }

        LocalDate weekEndDate = weekStartDate.plusDays(6);

        if (shiftPortOut.existsInDateRange(
                parkingLotId,
                weekStartDate,
                weekEndDate
        )) {
            throw new ConflictException(
                    "Work schedule already exists for this week"
            );
        }

        List<ShiftTemplate> templates =
                getValidTemplates(parkingLotId);

        List<EmployeeRosterRule> rosterRules =
                rosterRulePortOut.findActiveByParkingLotId(
                        parkingLotId
                );

        GeneratedPlan plan = buildPlan(
                parkingLot,
                weekStartDate,
                templates,
                rosterRules
        );

        List<Shift> savedShifts =
                shiftPortOut.saveAll(plan.shifts());

        for (PlannedAssignment planned : plan.assignments()) {
            ShiftAssignment assignment = new ShiftAssignment();
            assignment.setEmployeeId(planned.employeeId());
            assignment.setGateId(planned.gateId());

            assignmentPortIn.createAssignment(
                    planned.shiftId(),
                    assignment
            );
        }

        return savedShifts;
    }

    @Override
    @Transactional
    public List<Shift> approveWeek(
            UUID parkingLotId,
            LocalDate weekStartDate
    ) {
        currentAccountPortIn.requirePermission(UPDATE_PERMISSION);
        validateWeekRequest(parkingLotId, weekStartDate);

        ParkingLot parkingLot = findParkingLot(parkingLotId);

        if (parkingLot.getStatus() == ParkingLotStatus.CLOSED) {
            throw new ConflictException(
                    "Cannot approve schedule for a closed parking lot"
            );
        }

        LocalDate weekEndDate = weekStartDate.plusDays(6);

        List<Shift> shifts =
                shiftPortOut
                        .findByParkingLotAndDateRangeForUpdate(
                                parkingLotId,
                                weekStartDate,
                                weekEndDate
                        );

        if (shifts.size() != EXPECTED_SHIFT_COUNT) {
            throw new ConflictException(
                    "Work schedule must contain exactly 21 shifts"
            );
        }

        List<UUID> shiftIds = shifts.stream()
                .map(Shift::getShiftId)
                .toList();

        List<ShiftAssignment> assignments =
                assignmentPortOut.findByShiftIds(
                        shiftIds,
                        ShiftAssignmentStatus.DRAFT
                );

        Instant now = Instant.now();
        validateWeekForApproval(
                shifts,
                assignments,
                weekStartDate,
                now
        );

        UUID accountId =
                currentAccountPortIn.getCurrentAccountIdOrThrow();

        for (Shift shift : shifts) {
            shiftPolicy.approve(shift, accountId, now);
        }

        for (ShiftAssignment assignment : assignments) {
            assignmentPolicy.schedule(assignment);
        }

        assignmentPortOut.saveAll(assignments);

        List<Shift> savedShifts = shiftPortOut.saveAll(shifts);
        assignments.forEach(assignment -> notifyEmployeeShiftScheduled(assignment, findFrom(savedShifts, assignment.getShiftId())));
        return savedShifts;
    }

    @Override
    @Transactional(readOnly = true)
    public Shift getShiftById(UUID shiftId) {
        currentAccountPortIn.requirePermission(READ_PERMISSION);
        return findShift(shiftId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Shift> getShifts(
            UUID parkingLotId,
            LocalDate fromDate,
            LocalDate toDate,
            ShiftType shiftType,
            ShiftStatus status,
            UUID employeeId,
            String keyword
    ) {
        currentAccountPortIn.requirePermission(READ_PERMISSION);
        validateDateRange(fromDate, toDate);

        return shiftPortOut.findAll(
                parkingLotId,
                fromDate,
                toDate,
                shiftType,
                status,
                employeeId,
                TextValidationUtils.normalizeNullableText(
                        keyword,
                        "keyword",
                        100
                )
        );
    }

    @Override
    @Transactional
    public Shift openShift(
            UUID shiftId,
            BigDecimal openingCash,
            String note
    ) {
        requireOpenPermission();

        Shift shift = findShiftForUpdate(shiftId);

        ParkingLot parkingLot =
                findParkingLot(shift.getParkingLotId());

        if (parkingLot.getStatus() != ParkingLotStatus.ACTIVE) {
            throw new ConflictException(
                    "Parking lot must be active"
            );
        }

        List<ShiftAssignment> assignments =
                assignmentPortOut.findByShiftId(
                        shiftId,
                        ShiftAssignmentStatus.SCHEDULED
                );

        validateCompleteShift(
                shift,
                assignments,
                true
        );

        ensureEmployeeCallerAssigned(assignments);

        if (shiftPortOut.hasOpenShift(
                shift.getParkingLotId()
        )) {
            throw new ConflictException(
                    "Parking lot already has an open shift"
            );
        }

        shiftPolicy.open(
                shift,
                openingCash,
                currentAccountPortIn.getCurrentAccountIdOrThrow(),
                Instant.now(),
                note
        );

        for (ShiftAssignment assignment : assignments) {
            assignmentPolicy.activate(assignment);
        }

        assignmentPortOut.saveAll(assignments);

        Shift cancelledShift = shiftPortOut.save(shift);
        assignments.forEach(assignment -> notifyEmployeeShiftCancelled(assignment, cancelledShift));
        return cancelledShift;
    }

    @Override
    @Transactional
    public Shift closeShift(
            UUID shiftId,
            BigDecimal closingCash,
            String note
    ) {
        currentAccountPortIn.requirePermission(UPDATE_PERMISSION);

        Shift shift = findShiftForUpdate(shiftId);

        shiftPolicy.close(
                shift,
                closingCash,
                currentAccountPortIn.getCurrentAccountIdOrThrow(),
                Instant.now(),
                note
        );

        return shiftPortOut.save(shift);
    }

    @Override
    @Transactional
    public Shift cancelShift(
            UUID shiftId,
            String reason
    ) {
        currentAccountPortIn.requirePermission(UPDATE_PERMISSION);

        Shift shift = findShiftForUpdate(shiftId);

        if (shift.getStatus() == ShiftStatus.CANCELLED) {
            return shift;
        }

        List<ShiftAssignment> assignments =
                assignmentPortOut.findNotRemovedByShiftId(shiftId);

        for (ShiftAssignment assignment : assignments) {
            assignmentPolicy.remove(assignment);
        }

        if (!assignments.isEmpty()) {
            assignmentPortOut.saveAll(assignments);
        }

        shiftPolicy.cancel(
                shift,
                reason,
                currentAccountPortIn.getCurrentAccountIdOrThrow(),
                Instant.now()
        );

        return shiftPortOut.save(shift);
    }

    private GeneratedPlan buildPlan(
            ParkingLot parkingLot,
            LocalDate weekStartDate,
            List<ShiftTemplate> templates,
            List<EmployeeRosterRule> rosterRules
    ) {
        List<Shift> shifts = new ArrayList<>();
        List<PlannedAssignment> assignments =
                new ArrayList<>();

        Map<LocalDate, Set<UUID>> usedEmployees =
                new HashMap<>();

        for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
            LocalDate shiftDate =
                    weekStartDate.plusDays(dayOffset);

            Set<UUID> employeesForDate =
                    usedEmployees.computeIfAbsent(
                            shiftDate,
                            ignored -> new HashSet<>()
                    );

            for (ShiftTemplate template : templates) {
                Shift shift = createDraftShift(
                        parkingLot,
                        shiftDate,
                        template
                );

                shifts.add(shift);

                assignments.addAll(
                        planAssignments(
                                shift,
                                rosterRules,
                                employeesForDate
                        )
                );
            }
        }

        if (shifts.size() != EXPECTED_SHIFT_COUNT) {
            throw new ConflictException(
                    "Generated schedule must contain 21 shifts"
            );
        }

        return new GeneratedPlan(shifts, assignments);
    }

    private List<PlannedAssignment> planAssignments(
            Shift shift,
            List<EmployeeRosterRule> rosterRules,
            Set<UUID> usedEmployees
    ) {
        List<EmployeeRosterRule> fixedRules =
                rosterRules.stream()
                        .filter(rule ->
                                rule.getAssignmentMode()
                                        == AssignmentMode.FIXED
                        )
                        .filter(rule ->
                                rule.getPreferredShiftType()
                                        == shift.getShiftType()
                        )
                        .filter(rule ->
                                isEffectiveOn(
                                        rule,
                                        shift.getShiftDate()
                                )
                        )
                        .toList();

        if (fixedRules.size() != ASSIGNMENTS_PER_SHIFT) {
            throw new ConflictException(
                    "Each shift type requires exactly two fixed roster rules"
            );
        }

        if (fixedRules.stream()
                .map(EmployeeRosterRule::getPreferredGateId)
                .distinct()
                .count() != ASSIGNMENTS_PER_SHIFT) {
            throw new ConflictException(
                    "Fixed roster rules must use distinct gates"
            );
        }

        List<PlannedAssignment> result =
                new ArrayList<>();

        List<EmployeeRosterRule> missing =
                new ArrayList<>();

        for (EmployeeRosterRule fixed : fixedRules) {
            if (fixed.getWeeklyDayOff()
                    == shift.getShiftDate().getDayOfWeek()) {
                missing.add(fixed);
                continue;
            }

            addPlannedAssignment(
                    result,
                    shift,
                    fixed.getEmployeeId(),
                    fixed.getPreferredGateId(),
                    usedEmployees
            );
        }

        if (missing.size() > 1) {
            throw new ConflictException(
                    "More than one fixed employee is absent in the same shift"
            );
        }

        if (missing.size() == 1) {
            EmployeeRosterRule relief = findReliefRule(
                    rosterRules,
                    shift.getShiftDate(),
                    usedEmployees
            );

            addPlannedAssignment(
                    result,
                    shift,
                    relief.getEmployeeId(),
                    missing.getFirst().getPreferredGateId(),
                    usedEmployees
            );
        }

        if (result.size() != ASSIGNMENTS_PER_SHIFT) {
            throw new ConflictException(
                    "Cannot create enough assignments for "
                            + shift.getShiftCode()
            );
        }

        return result;
    }

    private void addPlannedAssignment(
            List<PlannedAssignment> result,
            Shift shift,
            UUID employeeId,
            UUID gateId,
            Set<UUID> usedEmployees
    ) {
        if (!usedEmployees.add(employeeId)) {
            throw new ConflictException(
                    "Employee cannot work more than one shift per day"
            );
        }

        ensureOperationalEmployee(employeeId);
        ensureGateValid(
                gateId,
                shift.getParkingLotId(),
                false
        );

        result.add(new PlannedAssignment(
                shift.getShiftId(),
                employeeId,
                gateId
        ));
    }

    private EmployeeRosterRule findReliefRule(
            List<EmployeeRosterRule> rosterRules,
            LocalDate date,
            Set<UUID> usedEmployees
    ) {
        List<EmployeeRosterRule> candidates =
                rosterRules.stream()
                        .filter(rule ->
                                rule.getAssignmentMode()
                                        == AssignmentMode.RELIEF
                        )
                        .filter(rule -> isEffectiveOn(rule, date))
                        .filter(rule ->
                                rule.getWeeklyDayOff()
                                        != date.getDayOfWeek()
                        )
                        .filter(rule ->
                                !usedEmployees.contains(
                                        rule.getEmployeeId()
                                )
                        )
                        .toList();

        if (candidates.size() != 1) {
            throw new ConflictException(
                    "Exactly one available relief employee is required"
            );
        }

        return candidates.getFirst();
    }

    private Shift createDraftShift(
            ParkingLot parkingLot,
            LocalDate shiftDate,
            ShiftTemplate template
    ) {
        Shift shift = new Shift();

        shift.setShiftId(UUID.randomUUID());
        shift.setShiftTemplateId(
                template.getShiftTemplateId()
        );
        shift.setParkingLotId(
                parkingLot.getParkingLotId()
        );
        shift.setShiftDate(shiftDate);
        shift.setShiftType(template.getShiftType());

        shift.setShiftCode(
                parkingLot.getCode()
                        + "-"
                        + shiftDate.format(
                        DateTimeFormatter.BASIC_ISO_DATE
                )
                        + "-"
                        + template.getShiftType().name()
        );

        ZonedDateTime start =
                shiftDate.atTime(
                        template.getStartLocalTime()
                ).atZone(DateTimeUtils.VIETNAM_ZONE);

        LocalDate endDate =
                endsNextDay(template)
                        ? shiftDate.plusDays(1)
                        : shiftDate;

        ZonedDateTime end =
                endDate.atTime(
                        template.getEndLocalTime()
                ).atZone(DateTimeUtils.VIETNAM_ZONE);

        shift.setStartTime(start.toInstant());
        shift.setEndTime(end.toInstant());

        shiftPolicy.initializeDraft(shift);
        return shift;
    }

    private List<ShiftTemplate> getValidTemplates(
            UUID parkingLotId
    ) {
        List<ShiftTemplate> templates =
                templatePortOut.findActiveByParkingLotId(
                        parkingLotId
                );

        if (templates.size() != 3) {
            throw new ConflictException(
                    "Parking lot must have exactly three active templates"
            );
        }

        if (templates.stream()
                .map(ShiftTemplate::getShiftType)
                .distinct()
                .count() != 3) {
            throw new ConflictException(
                    "Templates must have distinct shift types"
            );
        }

        for (int first = 0; first < templates.size(); first++) {
            for (int second = first + 1;
                 second < templates.size();
                 second++) {
                if (templatePolicy.overlaps(
                        templates.get(first),
                        templates.get(second)
                )) {
                    throw new ConflictException(
                            "Shift templates must not overlap"
                    );
                }
            }
        }

        return templates.stream()
                .sorted(Comparator.comparing(
                        ShiftTemplate::getStartLocalTime
                ))
                .toList();
    }

    private void validateWeekForApproval(
            List<Shift> shifts,
            List<ShiftAssignment> assignments,
            LocalDate weekStartDate,
            Instant currentTime
    ) {
        Map<UUID, List<ShiftAssignment>> byShift =
                assignments.stream()
                        .collect(Collectors.groupingBy(
                                ShiftAssignment::getShiftId
                        ));

        Set<UUID> employeeIds = new HashSet<>();

        for (Shift shift : shifts) {
            if (shift.getStatus() != ShiftStatus.DRAFT) {
                throw new ConflictException(
                        "All shifts must be DRAFT"
                );
            }

            if (!currentTime.isBefore(shift.getStartTime())) {
                throw new ConflictException(
                        "A shift has already started"
                );
            }

            List<ShiftAssignment> shiftAssignments =
                    byShift.getOrDefault(
                            shift.getShiftId(),
                            List.of()
                    );

            validateCompleteShift(
                    shift,
                    shiftAssignments,
                    false
            );

            shiftAssignments.forEach(item ->
                    employeeIds.add(item.getEmployeeId())
            );
        }

        for (UUID employeeId : employeeIds) {
            validateEmployeeWeek(
                    employeeId,
                    weekStartDate
            );
        }
    }

    private void validateCompleteShift(
            Shift shift,
            List<ShiftAssignment> assignments,
            boolean requireActiveGate
    ) {
        if (assignments.size() != ASSIGNMENTS_PER_SHIFT) {
            throw new ConflictException(
                    "Shift must have exactly two active assignments"
            );
        }

        if (assignments.stream()
                .map(ShiftAssignment::getEmployeeId)
                .distinct()
                .count() != ASSIGNMENTS_PER_SHIFT) {
            throw new ConflictException(
                    "Assignments must use distinct employees"
            );
        }

        if (assignments.stream()
                .map(ShiftAssignment::getGateId)
                .distinct()
                .count() != ASSIGNMENTS_PER_SHIFT) {
            throw new ConflictException(
                    "Assignments must use distinct gates"
            );
        }

        for (ShiftAssignment assignment : assignments) {
            ensureOperationalEmployee(
                    assignment.getEmployeeId()
            );

            ensureGateValid(
                    assignment.getGateId(),
                    shift.getParkingLotId(),
                    requireActiveGate
            );
        }
    }

    private void validateEmployeeWeek(
            UUID employeeId,
            LocalDate weekStartDate
    ) {
        List<Shift> shifts = assignmentPortOut
                .findNotRemovedEmployeeSchedule(
                        employeeId,
                        weekStartDate.minusDays(1),
                        weekStartDate.plusDays(7)
                )
                .stream()
                .map(ShiftAssignment::getShiftId)
                .distinct()
                .map(this::findShift)
                .sorted(Comparator.comparing(Shift::getStartTime))
                .toList();

        LocalDate weekEndDate = weekStartDate.plusDays(6);

        long weeklyCount = shifts.stream()
                .filter(shift ->
                        !shift.getShiftDate()
                                .isBefore(weekStartDate)
                                && !shift.getShiftDate()
                                .isAfter(weekEndDate)
                )
                .count();

        if (weeklyCount > MAX_SHIFTS_PER_WEEK) {
            throw new ConflictException(
                    "Employee cannot work more than six shifts per week"
            );
        }

        Set<LocalDate> workedDates = new HashSet<>();

        for (Shift shift : shifts) {
            if (!workedDates.add(shift.getShiftDate())) {
                throw new ConflictException(
                        "Employee cannot work more than one shift per day"
                );
            }
        }

        for (int index = 1; index < shifts.size(); index++) {
            Shift previous = shifts.get(index - 1);
            Shift current = shifts.get(index);

            if (previous.getEndTime()
                    .isAfter(current.getStartTime())) {
                throw new ConflictException(
                        "Employee has overlapping shifts"
                );
            }

            Duration rest = Duration.between(
                    previous.getEndTime(),
                    current.getStartTime()
            );

            if (rest.compareTo(MINIMUM_REST) < 0) {
                throw new ConflictException(
                        "Employee must have at least eight hours of rest"
                );
            }
        }
    }

    private void ensureEmployeeCallerAssigned(
            List<ShiftAssignment> assignments
    ) {
        CurrentAccountAccess current =
                currentAccountPortIn.getCurrentAccountOrThrow();

        if (!"EMPLOYEE".equals(current.roleCode())) {
            return;
        }

        Employee employee = employeePortOut
                .findByAccountId(current.accountId())
                .orElseThrow(() ->
                        new NotFoundException(
                                "Employee record not found"
                        )
                );

        boolean assigned = assignments.stream().anyMatch(
                assignment -> Objects.equals(
                        assignment.getEmployeeId(),
                        employee.getEmployeeId()
                )
        );

        if (!assigned) {
            throw new ConflictException(
                    "Employee is not assigned to this shift"
            );
        }
    }

    private void requireOpenPermission() {
        if (currentAccountPortIn.hasPermission(
                UPDATE_PERMISSION
        )) {
            return;
        }

        currentAccountPortIn.requirePermission(
                OPEN_OWN_PERMISSION
        );
    }

    private void ensureOperationalEmployee(UUID employeeId) {
        Employee employee = employeePortOut.findById(employeeId)
                .orElseThrow(() ->
                        new NotFoundException("Employee not found")
                );

        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new ConflictException(
                    "Employee must be active"
            );
        }

        if (!employeePortOut.hasAccountRole(
                employeeId,
                "EMPLOYEE"
        )) {
            throw new ConflictException(
                    "Only operational employees can be assigned"
            );
        }
    }

    private void ensureGateValid(
            UUID gateId,
            UUID parkingLotId,
            boolean requireActive
    ) {
        Gate gate = gatePortOut.findById(gateId)
                .orElseThrow(() ->
                        new NotFoundException("Gate not found")
                );

        if (requireActive
                ? gate.getStatus() != GateStatus.ACTIVE
                : gate.getStatus() == GateStatus.CLOSED) {
            throw new ConflictException(
                    requireActive
                            ? "Gate must be active"
                            : "Gate must not be closed"
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
                    "Gate does not belong to parking lot"
            );
        }
    }

    private boolean isEffectiveOn(
            EmployeeRosterRule rule,
            LocalDate date
    ) {
        return !date.isBefore(rule.getEffectiveFrom())
                && (
                rule.getEffectiveTo() == null
                        || !date.isAfter(rule.getEffectiveTo())
        );
    }

    private boolean endsNextDay(ShiftTemplate template) {
        LocalTime start = template.getStartLocalTime();
        LocalTime end = template.getEndLocalTime();
        return !end.isAfter(start);
    }

    private void validateWeekRequest(
            UUID parkingLotId,
            LocalDate weekStartDate
    ) {
        if (parkingLotId == null) {
            throw new BadRequestException(
                    "parkingLotId must not be null"
            );
        }

        if (weekStartDate == null) {
            throw new BadRequestException(
                    "weekStartDate must not be null"
            );
        }

        if (weekStartDate.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new BadRequestException(
                    "weekStartDate must be Monday"
            );
        }
    }

    private void validateDateRange(
            LocalDate fromDate,
            LocalDate toDate
    ) {
        if (fromDate != null
                && toDate != null
                && fromDate.isAfter(toDate)) {
            throw new BadRequestException(
                    "fromDate must not be after toDate"
            );
        }
    }

    private ParkingLot findParkingLot(UUID parkingLotId) {
        return parkingLotPortOut.findById(parkingLotId)
                .orElseThrow(() ->
                        new NotFoundException("Parking lot not found")
                );
    }

    private Shift findShift(UUID shiftId) {
        return shiftPortOut.findById(shiftId)
                .orElseThrow(() ->
                        new NotFoundException("Shift not found")
                );
    }

    private Shift findShiftForUpdate(UUID shiftId) {
        return shiftPortOut.findByIdForUpdate(shiftId)
                .orElseThrow(() ->
                        new NotFoundException("Shift not found")
                );
    }

    private Shift findFrom(List<Shift> shifts, UUID shiftId) {
        return shifts.stream()
                .filter(shift -> Objects.equals(shift.getShiftId(), shiftId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Shift not found"));
    }

    private void notifyEmployeeShiftScheduled(ShiftAssignment assignment, Shift shift) {
        if (notificationPortIn == null) {
            return;
        }
        employeePortOut.findAccountIdByEmployeeId(assignment.getEmployeeId())
                .ifPresent(accountId -> notificationPortIn.sendWebNotification(new SendNotificationCommand(
                        accountId,
                        "Bạn có ca trực mới",
                        "Bạn được phân vào ca " + shift.getShiftCode() + " ngày " + shift.getShiftDate() + ".",
                        "operations",
                        "shifts",
                        shift.getShiftId()
                )));
    }

    private void notifyEmployeeShiftCancelled(ShiftAssignment assignment, Shift shift) {
        if (notificationPortIn == null) {
            return;
        }
        employeePortOut.findAccountIdByEmployeeId(assignment.getEmployeeId())
                .ifPresent(accountId -> notificationPortIn.sendWebNotification(new SendNotificationCommand(
                        accountId,
                        "Ca trực đã hủy",
                        "Ca " + shift.getShiftCode() + " ngày " + shift.getShiftDate() + " đã bị hủy.",
                        "operations",
                        "shifts",
                        shift.getShiftId()
                )));
    }

    private record PlannedAssignment(
            UUID shiftId,
            UUID employeeId,
            UUID gateId
    ) {
    }

    private record GeneratedPlan(
            List<Shift> shifts,
            List<PlannedAssignment> assignments
    ) {
    }
}
