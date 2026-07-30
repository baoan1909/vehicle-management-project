package com.ban.vehicle_management.application.operations.shiftassignment.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.shift.port.out.ShiftPortOut;
import com.ban.vehicle_management.application.operations.shiftassignment.port.in.ShiftAssignmentPortIn;
import com.ban.vehicle_management.application.operations.shiftassignment.port.out.ShiftAssignmentPortOut;
import com.ban.vehicle_management.application.notification.notification.model.SendNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.application.parking.gate.port.out.GatePortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.application.people.employee.port.out.EmployeePortOut;
import com.ban.vehicle_management.domain.operations.shift.model.Shift;
import com.ban.vehicle_management.domain.operations.shiftassignment.model.ShiftAssignment;
import com.ban.vehicle_management.domain.operations.shiftassignment.policy.ShiftAssignmentPolicy;
import com.ban.vehicle_management.domain.parking.gate.model.Gate;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftAssignmentStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import com.ban.vehicle_management.shared.enumeration.parking.GateStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShiftAssignmentUseCaseImpl
        implements ShiftAssignmentPortIn {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ShiftAssignmentUseCaseImpl.class);

    private static final String CREATE_PERMISSION =
            "SHIFT_ASSIGNMENT_CREATE_ALL";
    private static final String READ_PERMISSION =
            "SHIFT_ASSIGNMENT_READ_ALL";
    private static final String READ_OWN_PERMISSION =
            "SHIFT_ASSIGNMENT_READ_OWN";
    private static final String UPDATE_PERMISSION =
            "SHIFT_ASSIGNMENT_UPDATE_ALL";
    private static final String DELETE_PERMISSION =
            "SHIFT_ASSIGNMENT_DELETE_ALL";

    private static final String EMPLOYEE_ROLE = "EMPLOYEE";
    private static final int MAX_SHIFTS_PER_WEEK = 6;
    private static final Duration MINIMUM_REST =
            Duration.ofHours(8);

    private final CurrentAccountPortIn currentAccountPortIn;
    private final ShiftAssignmentPortOut assignmentPortOut;
    private final ShiftPortOut shiftPortOut;
    private final EmployeePortOut employeePortOut;
    private final GatePortOut gatePortOut;
    private final ZonePortOut zonePortOut;
    private final NotificationPortIn notificationPortIn;
    private final ShiftAssignmentPolicy policy =
            new ShiftAssignmentPolicy();

    public ShiftAssignmentUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            ShiftAssignmentPortOut assignmentPortOut,
            ShiftPortOut shiftPortOut,
            EmployeePortOut employeePortOut,
            GatePortOut gatePortOut,
            ZonePortOut zonePortOut,
            NotificationPortIn notificationPortIn
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.assignmentPortOut = assignmentPortOut;
        this.shiftPortOut = shiftPortOut;
        this.employeePortOut = employeePortOut;
        this.gatePortOut = gatePortOut;
        this.zonePortOut = zonePortOut;
        this.notificationPortIn = notificationPortIn;
    }

    @Override
    @Transactional
    public ShiftAssignment createAssignment(
            UUID shiftId,
            ShiftAssignment assignment
    ) {
        currentAccountPortIn.requirePermission(CREATE_PERMISSION);

        Shift shift = findShift(shiftId);
        ensureDraftAndNotStarted(shift);

        assignment.setShiftAssignmentId(UUID.randomUUID());
        assignment.setShiftId(shiftId);
        policy.initializeNew(assignment);

        validateCandidate(assignment, shift, Set.of());

        return assignmentPortOut.save(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public ShiftAssignment getAssignmentById(UUID assignmentId) {
        currentAccountPortIn.requirePermission(READ_PERMISSION);
        return findAssignment(assignmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftAssignment> getAssignmentsByShift(
            UUID shiftId,
            ShiftAssignmentStatus status
    ) {
        currentAccountPortIn.requirePermission(READ_PERMISSION);
        findShift(shiftId);

        ShiftAssignmentStatus resolvedStatus =
                status == null
                        ? ShiftAssignmentStatus.ACTIVE
                        : status;

        return assignmentPortOut.findByShiftId(
                shiftId,
                resolvedStatus
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftAssignment> getAssignments(
            UUID parkingLotId,
            UUID shiftId,
            UUID employeeId,
            UUID gateId,
            ShiftAssignmentStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            ShiftType shiftType
    ) {
        currentAccountPortIn.requirePermission(READ_PERMISSION);
        validateDateRange(fromDate, toDate);

        return assignmentPortOut.findAll(
                parkingLotId,
                shiftId,
                employeeId,
                gateId,
                status,
                fromDate,
                toDate,
                shiftType
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftAssignment> getMyAssignments(
            LocalDate fromDate,
            LocalDate toDate,
            ShiftAssignmentStatus status
    ) {
        currentAccountPortIn.requirePermission(READ_OWN_PERMISSION);
        validateDateRange(fromDate, toDate);

        UUID accountId =
                currentAccountPortIn.getCurrentAccountIdOrThrow();

        Employee employee = employeePortOut
                .findByAccountId(accountId)
                .orElseThrow(() -> new NotFoundException(
                        "Employee record not found for current account"
                ));

        ShiftAssignmentStatus resolvedStatus =
                status == null
                        ? ShiftAssignmentStatus.ACTIVE
                        : status;

        return assignmentPortOut.findAll(
                null,
                null,
                employee.getEmployeeId(),
                null,
                resolvedStatus,
                fromDate,
                toDate,
                null
        );
    }

    @Override
    @Transactional
    public ShiftAssignment updateAssignment(
            UUID assignmentId,
            ShiftAssignment request
    ) {
        currentAccountPortIn.requirePermission(UPDATE_PERMISSION);

        ShiftAssignment existing =
                findAssignmentForUpdate(assignmentId);
        ensureEditableAssignment(existing);
        UUID previousEmployeeId = existing.getEmployeeId();

        Shift shift = findShiftForUpdate(existing.getShiftId());
        ensureDraftAndNotStarted(shift);

        existing.setEmployeeId(request.getEmployeeId());
        existing.setGateId(request.getGateId());
        policy.validateState(existing);

        validateCandidate(existing, shift, Set.of(assignmentId));

        ShiftAssignment savedAssignment = assignmentPortOut.save(existing);
        notifyAssignmentChanged(previousEmployeeId, savedAssignment, shift);
        return savedAssignment;
    }

    @Override
    @Transactional
    public ShiftAssignment replaceAssignment(
            UUID assignmentId,
            UUID replacementEmployeeId,
            String reason
    ) {
        currentAccountPortIn.requirePermission(UPDATE_PERMISSION);

        String normalizedReason =
                TextValidationUtils.normalizeRequiredText(
                        reason,
                        "reason",
                        255
                );

        ShiftAssignment existing =
                findAssignmentForUpdate(assignmentId);
        ensureEditableAssignment(existing);

        Shift shift = findShiftForUpdate(existing.getShiftId());
        ensureReplaceableShift(shift);

        if (Objects.equals(
                existing.getEmployeeId(),
                replacementEmployeeId
        )) {
            throw new BadRequestException(
                    "Replacement employee must be different"
            );
        }

        ShiftAssignment replacement = new ShiftAssignment();
        replacement.setShiftAssignmentId(UUID.randomUUID());
        replacement.setShiftId(existing.getShiftId());
        replacement.setEmployeeId(replacementEmployeeId);
        replacement.setGateId(existing.getGateId());
        initializeAssignmentForShift(replacement, shift);

        validateCandidate(
                replacement,
                shift,
                Set.of(existing.getShiftAssignmentId())
        );

        policy.remove(existing);
        assignmentPortOut.save(existing);

        ShiftAssignment saved =
                assignmentPortOut.save(replacement);
        notifyAssignmentRemoved(existing.getEmployeeId(), shift);
        notifyAssignmentAdded(saved, shift);

        LOGGER.info(
                "Shift assignment {} replaced by {}. Reason: {}",
                assignmentId,
                saved.getShiftAssignmentId(),
                normalizedReason
        );

        return saved;
    }

    @Override
    @Transactional
    public List<ShiftAssignment> swapAssignments(
            UUID firstAssignmentId,
            UUID secondAssignmentId,
            String reason
    ) {
        currentAccountPortIn.requirePermission(UPDATE_PERMISSION);

        if (Objects.equals(firstAssignmentId, secondAssignmentId)) {
            throw new BadRequestException(
                    "Two different assignments are required"
            );
        }

        String normalizedReason =
                TextValidationUtils.normalizeRequiredText(
                        reason,
                        "reason",
                        255
                );

        List<ShiftAssignment> locked =
                assignmentPortOut.findAllByIdsForUpdate(
                        List.of(
                                firstAssignmentId,
                                secondAssignmentId
                        )
                );

        if (locked.size() != 2) {
            throw new NotFoundException(
                    "One or more shift assignments were not found"
            );
        }

        ShiftAssignment first = findFrom(
                locked,
                firstAssignmentId
        );
        ShiftAssignment second = findFrom(
                locked,
                secondAssignmentId
        );

        ensureEditableAssignment(first);
        ensureEditableAssignment(second);

        if (Objects.equals(
                first.getEmployeeId(),
                second.getEmployeeId()
        )) {
            throw new ConflictException(
                    "Assignments must belong to different employees"
            );
        }

        Shift firstShift =
                findShiftForUpdate(first.getShiftId());
        Shift secondShift =
                findShiftForUpdate(second.getShiftId());

        ensureReplaceableShift(firstShift);
        ensureReplaceableShift(secondShift);

        Set<UUID> excludedIds = Set.of(
                firstAssignmentId,
                secondAssignmentId
        );

        ShiftAssignment employeeFromFirst =
                createNewAssignment(
                        secondShift,
                        first.getEmployeeId(),
                        second.getGateId()
                );

        ShiftAssignment employeeFromSecond =
                createNewAssignment(
                        firstShift,
                        second.getEmployeeId(),
                        first.getGateId()
                );

        validateCandidate(
                employeeFromFirst,
                secondShift,
                excludedIds
        );

        validateCandidate(
                employeeFromSecond,
                firstShift,
                excludedIds
        );

        policy.remove(first);
        policy.remove(second);
        assignmentPortOut.saveAll(List.of(first, second));

        List<ShiftAssignment> saved =
                assignmentPortOut.saveAll(
                        List.of(
                                employeeFromFirst,
                                employeeFromSecond
                        )
                );
        notifyAssignmentRemoved(first.getEmployeeId(), firstShift);
        notifyAssignmentRemoved(second.getEmployeeId(), secondShift);
        notifyAssignmentAdded(employeeFromFirst, secondShift);
        notifyAssignmentAdded(employeeFromSecond, firstShift);

        LOGGER.info(
                "Assignments {} and {} swapped. Reason: {}",
                firstAssignmentId,
                secondAssignmentId,
                normalizedReason
        );

        return saved;
    }

    @Override
    @Transactional
    public void deleteAssignment(UUID assignmentId) {
        currentAccountPortIn.requirePermission(DELETE_PERMISSION);

        ShiftAssignment assignment =
                findAssignmentForUpdate(assignmentId);

        if (assignment.getStatus()
                == ShiftAssignmentStatus.REMOVED) {
            return;
        }

        Shift shift = findShiftForUpdate(assignment.getShiftId());
        ensureDraftAndNotStarted(shift);

        policy.remove(assignment);
        assignmentPortOut.save(assignment);
        notifyAssignmentRemoved(assignment.getEmployeeId(), shift);
    }

    private void validateCandidate(
            ShiftAssignment candidate,
            Shift shift,
            Set<UUID> excludedAssignmentIds
    ) {
        ensureOperationalEmployee(candidate.getEmployeeId());
        ensureGateValid(candidate.getGateId(), shift);
        ensureUniqueWithinShift(
                candidate,
                excludedAssignmentIds
        );
        ensureValidEmployeeSchedule(
                candidate.getEmployeeId(),
                shift,
                excludedAssignmentIds
        );
    }

    private void ensureUniqueWithinShift(
            ShiftAssignment candidate,
            Set<UUID> excludedIds
    ) {
        List<ShiftAssignment> assignments =
                assignmentPortOut.findNotRemovedByShiftId(
                        candidate.getShiftId()
                );

        for (ShiftAssignment existing : assignments) {
            if (excludedIds.contains(
                    existing.getShiftAssignmentId()
            )) {
                continue;
            }

            if (Objects.equals(
                    existing.getEmployeeId(),
                    candidate.getEmployeeId()
            )) {
                throw new ConflictException(
                        "Employee is already assigned to this shift"
                );
            }

            if (Objects.equals(
                    existing.getGateId(),
                    candidate.getGateId()
            )) {
                throw new ConflictException(
                        "Gate already has an active assignment in this shift"
                );
            }
        }
    }

    private void ensureValidEmployeeSchedule(
            UUID employeeId,
            Shift candidateShift,
            Set<UUID> excludedIds
    ) {
        LocalDate weekStart = candidateShift.getShiftDate()
                .with(TemporalAdjusters.previousOrSame(
                        java.time.DayOfWeek.MONDAY
                ));
        LocalDate weekEnd = weekStart.plusDays(6);

        List<ShiftAssignment> existingAssignments =
                assignmentPortOut.findNotRemovedEmployeeSchedule(
                                employeeId,
                                weekStart.minusDays(1),
                                weekEnd.plusDays(1)
                        )
                        .stream()
                        .filter(item -> !excludedIds.contains(
                                item.getShiftAssignmentId()
                        ))
                        .toList();

        List<Shift> existingShifts = existingAssignments.stream()
                .map(item -> findShift(item.getShiftId()))
                .sorted(Comparator.comparing(Shift::getStartTime))
                .toList();

        long shiftsInWeek = existingShifts.stream()
                .filter(item ->
                        !item.getShiftDate().isBefore(weekStart)
                                && !item.getShiftDate()
                                .isAfter(weekEnd)
                )
                .count();

        if (shiftsInWeek >= MAX_SHIFTS_PER_WEEK) {
            throw new ConflictException(
                    "Employee cannot work more than six shifts per week"
            );
        }

        for (Shift existing : existingShifts) {
            if (existing.getShiftDate().equals(
                    candidateShift.getShiftDate()
            )) {
                throw new ConflictException(
                        "Employee cannot work more than one shift per day"
                );
            }

            ensureNoOverlap(candidateShift, existing);
            ensureMinimumRest(candidateShift, existing);
        }
    }

    private void ensureNoOverlap(
            Shift candidate,
            Shift existing
    ) {
        boolean overlaps =
                candidate.getStartTime().isBefore(
                        existing.getEndTime()
                )
                        && existing.getStartTime().isBefore(
                        candidate.getEndTime()
                );

        if (overlaps) {
            throw new ConflictException(
                    "Employee has an overlapping shift"
            );
        }
    }

    private void ensureMinimumRest(
            Shift candidate,
            Shift existing
    ) {
        Duration rest;

        if (!candidate.getStartTime()
                .isBefore(existing.getEndTime())) {
            rest = Duration.between(
                    existing.getEndTime(),
                    candidate.getStartTime()
            );
        } else {
            rest = Duration.between(
                    candidate.getEndTime(),
                    existing.getStartTime()
            );
        }

        if (rest.compareTo(MINIMUM_REST) < 0) {
            throw new ConflictException(
                    "Employee must have at least eight hours of rest between shifts"
            );
        }
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
                EMPLOYEE_ROLE
        )) {
            throw new ConflictException(
                    "Only operational employees can be assigned"
            );
        }
    }

    private void ensureGateValid(UUID gateId, Shift shift) {
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
                shift.getParkingLotId()
        )) {
            throw new ConflictException(
                    "Gate does not belong to the shift parking lot"
            );
        }
    }

    private ShiftAssignment createNewAssignment(
            Shift shift,
            UUID employeeId,
            UUID gateId
    ) {
        ShiftAssignment assignment = new ShiftAssignment();
        assignment.setShiftAssignmentId(UUID.randomUUID());
        assignment.setShiftId(shift.getShiftId());
        assignment.setEmployeeId(employeeId);
        assignment.setGateId(gateId);
        initializeAssignmentForShift(assignment, shift);
        return assignment;
    }

    private ShiftAssignment findAssignment(UUID assignmentId) {
        return assignmentPortOut.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException(
                        "Shift assignment not found"
                ));
    }

    private ShiftAssignment findAssignmentForUpdate(
            UUID assignmentId
    ) {
        return assignmentPortOut.findByIdForUpdate(assignmentId)
                .orElseThrow(() -> new NotFoundException(
                        "Shift assignment not found"
                ));
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

    private ShiftAssignment findFrom(
            Collection<ShiftAssignment> assignments,
            UUID assignmentId
    ) {
        return assignments.stream()
                .filter(item -> Objects.equals(
                        item.getShiftAssignmentId(),
                        assignmentId
                ))
                .findFirst()
                .orElseThrow(() ->
                        new NotFoundException(
                                "Shift assignment not found"
                        )
                );
    }

    private void ensureDraftAndNotStarted(Shift shift) {
        if (shift.getStatus() != ShiftStatus.DRAFT) {
            throw new ConflictException(
                    "Shift must be in DRAFT status"
            );
        }

        ensureNotStarted(shift);
    }

    private void ensureReplaceableShift(Shift shift) {
        if (shift.getStatus() != ShiftStatus.DRAFT
                && shift.getStatus() != ShiftStatus.SCHEDULED) {
            throw new ConflictException(
                    "Shift must be DRAFT or SCHEDULED"
            );
        }

        ensureNotStarted(shift);
    }

    private void ensureNotStarted(Shift shift) {
        if (shift.getStartTime() == null) {
            throw new ConflictException(
                    "Shift start time is not configured"
            );
        }

        if (!Instant.now().isBefore(shift.getStartTime())) {
            throw new ConflictException(
                    "Shift has already started"
            );
        }
    }

    private void ensureEditableAssignment(ShiftAssignment assignment) {
        if (assignment.getStatus() != ShiftAssignmentStatus.DRAFT
                && assignment.getStatus() != ShiftAssignmentStatus.SCHEDULED) {
            throw new ConflictException(
                    "Shift assignment must be DRAFT or SCHEDULED"
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

    private void initializeAssignmentForShift(
            ShiftAssignment assignment,
            Shift shift
    ) {
        policy.initializeNew(assignment);

        if (shift.getStatus() == ShiftStatus.SCHEDULED) {
            policy.schedule(assignment);
        }
    }

    private void notifyAssignmentChanged(UUID previousEmployeeId, ShiftAssignment assignment, Shift shift) {
        if (Objects.equals(previousEmployeeId, assignment.getEmployeeId())) {
            notifyAssignmentAdded(assignment, shift);
            return;
        }
        notifyAssignmentRemoved(previousEmployeeId, shift);
        notifyAssignmentAdded(assignment, shift);
    }

    private void notifyAssignmentAdded(ShiftAssignment assignment, Shift shift) {
        if (notificationPortIn == null) {
            return;
        }
        employeePortOut.findAccountIdByEmployeeId(assignment.getEmployeeId())
                .ifPresent(accountId -> notificationPortIn.sendWebNotification(new SendNotificationCommand(
                        accountId,
                        "Bạn được phân ca",
                        "Bạn được phân vào ca " + shift.getShiftCode() + " ngày " + shift.getShiftDate() + ".",
                        "operations",
                        "shift_assignments",
                        assignment.getShiftAssignmentId()
                )));
    }

    private void notifyAssignmentRemoved(UUID employeeId, Shift shift) {
        if (notificationPortIn == null) {
            return;
        }
        employeePortOut.findAccountIdByEmployeeId(employeeId)
                .ifPresent(accountId -> notificationPortIn.sendWebNotification(new SendNotificationCommand(
                        accountId,
                        "Phân ca đã thay đổi",
                        "Bạn không còn được phân vào ca " + shift.getShiftCode() + " ngày " + shift.getShiftDate() + ".",
                        "operations",
                        "shifts",
                        shift.getShiftId()
                )));
    }
}
