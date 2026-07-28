package com.ban.vehicle_management.application.operations.shiftassignment.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.shift.port.out.ShiftPortOut;
import com.ban.vehicle_management.application.operations.shiftassignment.port.out.ShiftAssignmentPortOut;
import com.ban.vehicle_management.application.parking.gate.port.out.GatePortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.application.people.employee.port.out.EmployeePortOut;
import com.ban.vehicle_management.domain.operations.shift.model.Shift;
import com.ban.vehicle_management.domain.operations.shiftassignment.model.ShiftAssignment;
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
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShiftAssignmentUseCaseImplTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;
    @Mock
    private ShiftAssignmentPortOut assignmentPortOut;
    @Mock
    private ShiftPortOut shiftPortOut;
    @Mock
    private EmployeePortOut employeePortOut;
    @Mock
    private GatePortOut gatePortOut;
    @Mock
    private ZonePortOut zonePortOut;

    @InjectMocks
    private ShiftAssignmentUseCaseImpl useCase;

    @Test
    void shouldCreateAssignmentWhenCandidateIsValid() {
        UUID parkingLotId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        Shift shift = futureShift(
                parkingLotId,
                nextMonday(),
                ShiftStatus.DRAFT
        );
        ShiftAssignment request = requestAssignment();

        when(shiftPortOut.findById(shift.getShiftId()))
                .thenReturn(Optional.of(shift));
        stubValidCandidate(request, shift, zoneId);
        when(assignmentPortOut.save(any(ShiftAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShiftAssignment result = useCase.createAssignment(
                shift.getShiftId(),
                request
        );

        verify(currentAccountPortIn)
                .requirePermission("SHIFT_ASSIGNMENT_CREATE_ALL");
        assertNotNull(result.getShiftAssignmentId());
        assertEquals(shift.getShiftId(), result.getShiftId());
        assertEquals(ShiftAssignmentStatus.DRAFT, result.getStatus());
    }

    @Test
    void shouldRejectCreateWhenShiftIsNotDraft() {
        Shift shift = futureShift(
                UUID.randomUUID(),
                nextMonday(),
                ShiftStatus.SCHEDULED
        );

        when(shiftPortOut.findById(shift.getShiftId()))
                .thenReturn(Optional.of(shift));

        assertThrows(
                ConflictException.class,
                () -> useCase.createAssignment(
                        shift.getShiftId(),
                        requestAssignment()
                )
        );

        verify(assignmentPortOut, never())
                .save(any(ShiftAssignment.class));
    }

    @Test
    void shouldRejectCreateWhenGateAlreadyAssigned() {
        UUID parkingLotId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        Shift shift = futureShift(
                parkingLotId,
                nextMonday(),
                ShiftStatus.DRAFT
        );
        ShiftAssignment request = requestAssignment();
        ShiftAssignment existing = activeAssignment(
                shift.getShiftId(),
                UUID.randomUUID(),
                request.getGateId()
        );

        when(shiftPortOut.findById(shift.getShiftId()))
                .thenReturn(Optional.of(shift));
        stubEmployeeAndGate(request, shift, zoneId);
        when(assignmentPortOut.findNotRemovedByShiftId(shift.getShiftId()))
                .thenReturn(List.of(existing));

        assertThrows(
                ConflictException.class,
                () -> useCase.createAssignment(
                        shift.getShiftId(),
                        request
                )
        );

        verify(assignmentPortOut, never())
                .save(any(ShiftAssignment.class));
    }

    @Test
    void shouldRejectEmployeeWithSixExistingShiftsInWeek() {
        UUID parkingLotId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        LocalDate monday = nextMonday();
        Shift candidate = futureShift(
                parkingLotId,
                monday.plusDays(6),
                ShiftStatus.DRAFT
        );
        ShiftAssignment request = requestAssignment();
        List<ShiftAssignment> existingAssignments = new ArrayList<>();

        when(shiftPortOut.findById(candidate.getShiftId()))
                .thenReturn(Optional.of(candidate));
        stubEmployeeAndGate(request, candidate, zoneId);
        when(assignmentPortOut.findNotRemovedByShiftId(candidate.getShiftId()))
                .thenReturn(List.of());

        for (int index = 0; index < 6; index++) {
            Shift existingShift = futureShift(
                    parkingLotId,
                    monday.plusDays(index),
                    ShiftStatus.SCHEDULED
            );
            ShiftAssignment existingAssignment = activeAssignment(
                    existingShift.getShiftId(),
                    request.getEmployeeId(),
                    UUID.randomUUID()
            );
            existingAssignments.add(existingAssignment);
            when(shiftPortOut.findById(existingShift.getShiftId()))
                    .thenReturn(Optional.of(existingShift));
        }

        when(assignmentPortOut.findNotRemovedEmployeeSchedule(
                eq(request.getEmployeeId()),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(existingAssignments);

        assertThrows(
                ConflictException.class,
                () -> useCase.createAssignment(
                        candidate.getShiftId(),
                        request
                )
        );
    }

    @Test
    void shouldDefaultShiftAssignmentListToActiveStatus() {
        UUID shiftId = UUID.randomUUID();
        Shift shift = futureShift(
                UUID.randomUUID(),
                nextMonday(),
                ShiftStatus.DRAFT
        );
        shift.setShiftId(shiftId);
        ShiftAssignment assignment = activeAssignment(
                shiftId,
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        when(shiftPortOut.findById(shiftId))
                .thenReturn(Optional.of(shift));
        when(assignmentPortOut.findByShiftId(
                shiftId,
                ShiftAssignmentStatus.ACTIVE
        )).thenReturn(List.of(assignment));

        List<ShiftAssignment> result =
                useCase.getAssignmentsByShift(shiftId, null);

        assertEquals(1, result.size());
        verify(currentAccountPortIn)
                .requirePermission("SHIFT_ASSIGNMENT_READ_ALL");
    }

    @Test
    void shouldResolveCurrentEmployeeWhenReadingOwnAssignments() {
        UUID accountId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        LocalDate fromDate = nextMonday();
        LocalDate toDate = fromDate.plusDays(6);
        Employee employee = employee(employeeId, EmployeeStatus.ACTIVE);

        when(currentAccountPortIn.getCurrentAccountIdOrThrow())
                .thenReturn(accountId);
        when(employeePortOut.findByAccountId(accountId))
                .thenReturn(Optional.of(employee));
        when(assignmentPortOut.findAll(
                null,
                null,
                employeeId,
                null,
                ShiftAssignmentStatus.ACTIVE,
                fromDate,
                toDate,
                null
        )).thenReturn(List.of(new ShiftAssignment()));

        List<ShiftAssignment> result = useCase.getMyAssignments(
                fromDate,
                toDate,
                null
        );

        assertEquals(1, result.size());
        verify(currentAccountPortIn)
                .requirePermission("SHIFT_ASSIGNMENT_READ_OWN");
    }

    @Test
    void shouldThrowWhenCurrentAccountHasNoEmployeeRecord() {
        UUID accountId = UUID.randomUUID();
        when(currentAccountPortIn.getCurrentAccountIdOrThrow())
                .thenReturn(accountId);
        when(employeePortOut.findByAccountId(accountId))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> useCase.getMyAssignments(null, null, null)
        );
    }

    @Test
    void shouldUpdateDraftAssignmentAndPreserveIdentity() {
        UUID parkingLotId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        Shift shift = futureShift(
                parkingLotId,
                nextMonday(),
                ShiftStatus.DRAFT
        );
        ShiftAssignment existing = activeAssignment(
                shift.getShiftId(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );
        existing.setStatus(ShiftAssignmentStatus.DRAFT);
        UUID assignmentId = existing.getShiftAssignmentId();
        ShiftAssignment request = requestAssignment();

        when(assignmentPortOut.findByIdForUpdate(assignmentId))
                .thenReturn(Optional.of(existing));
        when(shiftPortOut.findByIdForUpdate(shift.getShiftId()))
                .thenReturn(Optional.of(shift));
        stubValidCandidate(request, shift, zoneId);
        when(assignmentPortOut.save(existing)).thenReturn(existing);

        ShiftAssignment result = useCase.updateAssignment(
                assignmentId,
                request
        );

        assertEquals(assignmentId, result.getShiftAssignmentId());
        assertEquals(request.getEmployeeId(), result.getEmployeeId());
        assertEquals(request.getGateId(), result.getGateId());
    }

    @Test
    void shouldReplaceAssignmentTransactionally() {
        UUID parkingLotId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        Shift shift = futureShift(
                parkingLotId,
                nextMonday(),
                ShiftStatus.SCHEDULED
        );
        ShiftAssignment existing = activeAssignment(
                shift.getShiftId(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );
        existing.setStatus(ShiftAssignmentStatus.SCHEDULED);
        UUID replacementEmployeeId = UUID.randomUUID();

        when(assignmentPortOut.findByIdForUpdate(
                existing.getShiftAssignmentId()
        )).thenReturn(Optional.of(existing));
        when(shiftPortOut.findByIdForUpdate(shift.getShiftId()))
                .thenReturn(Optional.of(shift));
        stubEmployeeAndGate(
                replacementEmployeeId,
                existing.getGateId(),
                shift,
                zoneId
        );
        when(assignmentPortOut.findNotRemovedByShiftId(shift.getShiftId()))
                .thenReturn(List.of(existing));
        when(assignmentPortOut.findNotRemovedEmployeeSchedule(
                eq(replacementEmployeeId),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(List.of());
        when(assignmentPortOut.save(any(ShiftAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShiftAssignment result = useCase.replaceAssignment(
                existing.getShiftAssignmentId(),
                replacementEmployeeId,
                "Employee requested leave"
        );

        assertEquals(ShiftAssignmentStatus.REMOVED, existing.getStatus());
        assertEquals(ShiftAssignmentStatus.SCHEDULED, result.getStatus());
        assertEquals(replacementEmployeeId, result.getEmployeeId());
        assertNotEquals(existing.getShiftAssignmentId(), result.getShiftAssignmentId());
        verify(assignmentPortOut, times(2))
                .save(any(ShiftAssignment.class));
    }

    @Test
    void shouldRejectReplacementWithSameEmployee() {
        Shift shift = futureShift(
                UUID.randomUUID(),
                nextMonday(),
                ShiftStatus.SCHEDULED
        );
        ShiftAssignment existing = activeAssignment(
                shift.getShiftId(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );
        existing.setStatus(ShiftAssignmentStatus.SCHEDULED);

        when(assignmentPortOut.findByIdForUpdate(
                existing.getShiftAssignmentId()
        )).thenReturn(Optional.of(existing));
        when(shiftPortOut.findByIdForUpdate(shift.getShiftId()))
                .thenReturn(Optional.of(shift));

        assertThrows(
                BadRequestException.class,
                () -> useCase.replaceAssignment(
                        existing.getShiftAssignmentId(),
                        existing.getEmployeeId(),
                        "Same employee"
                )
        );
    }

    @Test
    void shouldSwapTwoAssignments() {
        UUID parkingLotId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        LocalDate monday = nextMonday();
        Shift firstShift = futureShift(
                parkingLotId,
                monday,
                ShiftStatus.SCHEDULED
        );
        Shift secondShift = futureShift(
                parkingLotId,
                monday.plusDays(1),
                ShiftStatus.SCHEDULED
        );
        ShiftAssignment first = activeAssignment(
                firstShift.getShiftId(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );
        ShiftAssignment second = activeAssignment(
                secondShift.getShiftId(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );
        first.setStatus(ShiftAssignmentStatus.SCHEDULED);
        second.setStatus(ShiftAssignmentStatus.SCHEDULED);

        when(assignmentPortOut.findAllByIdsForUpdate(anyList()))
                .thenReturn(List.of(first, second));
        when(shiftPortOut.findByIdForUpdate(firstShift.getShiftId()))
                .thenReturn(Optional.of(firstShift));
        when(shiftPortOut.findByIdForUpdate(secondShift.getShiftId()))
                .thenReturn(Optional.of(secondShift));
        when(employeePortOut.findById(any(UUID.class)))
                .thenAnswer(invocation -> Optional.of(employee(
                        invocation.getArgument(0),
                        EmployeeStatus.ACTIVE
                )));
        when(employeePortOut.hasAccountRole(
                any(UUID.class),
                eq("EMPLOYEE")
        )).thenReturn(true);
        when(gatePortOut.findById(any(UUID.class)))
                .thenAnswer(invocation -> Optional.of(gate(
                        invocation.getArgument(0),
                        zoneId
                )));
        when(zonePortOut.findById(zoneId))
                .thenReturn(Optional.of(zone(zoneId, parkingLotId)));
        when(assignmentPortOut.findNotRemovedByShiftId(
                any(UUID.class)
        )).thenAnswer(invocation -> {
            UUID shiftId = invocation.getArgument(0);
            return shiftId.equals(firstShift.getShiftId())
                    ? List.of(first)
                    : List.of(second);
        });
        when(assignmentPortOut.findNotRemovedEmployeeSchedule(
                any(UUID.class),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(List.of());
        when(assignmentPortOut.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<ShiftAssignment> result = useCase.swapAssignments(
                first.getShiftAssignmentId(),
                second.getShiftAssignmentId(),
                "Employees agreed to swap"
        );

        assertEquals(2, result.size());
        assertEquals(ShiftAssignmentStatus.REMOVED, first.getStatus());
        assertEquals(ShiftAssignmentStatus.REMOVED, second.getStatus());
        assertEquals(first.getEmployeeId(), result.getFirst().getEmployeeId());
        assertEquals(secondShift.getShiftId(), result.getFirst().getShiftId());
        verify(assignmentPortOut, times(2)).saveAll(anyList());
    }

    @Test
    void shouldRejectSwapUsingSameAssignmentId() {
        UUID assignmentId = UUID.randomUUID();

        assertThrows(
                BadRequestException.class,
                () -> useCase.swapAssignments(
                        assignmentId,
                        assignmentId,
                        "Invalid swap"
                )
        );

        verify(assignmentPortOut, never())
                .findAllByIdsForUpdate(any());
    }

    @Test
    void shouldSoftDeleteDraftAssignment() {
        Shift shift = futureShift(
                UUID.randomUUID(),
                nextMonday(),
                ShiftStatus.DRAFT
        );
        ShiftAssignment assignment = activeAssignment(
                shift.getShiftId(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );
        assignment.setStatus(ShiftAssignmentStatus.DRAFT);

        when(assignmentPortOut.findByIdForUpdate(
                assignment.getShiftAssignmentId()
        )).thenReturn(Optional.of(assignment));
        when(shiftPortOut.findByIdForUpdate(shift.getShiftId()))
                .thenReturn(Optional.of(shift));
        when(assignmentPortOut.save(assignment))
                .thenReturn(assignment);

        useCase.deleteAssignment(assignment.getShiftAssignmentId());

        verify(currentAccountPortIn)
                .requirePermission("SHIFT_ASSIGNMENT_DELETE_ALL");
        assertEquals(ShiftAssignmentStatus.REMOVED, assignment.getStatus());
        verify(assignmentPortOut).save(assignment);
    }

    @Test
    void shouldRejectInvalidFilterDateRange() {
        LocalDate fromDate = nextMonday().plusDays(1);
        LocalDate toDate = fromDate.minusDays(1);

        assertThrows(
                BadRequestException.class,
                () -> useCase.getAssignments(
                        null,
                        null,
                        null,
                        null,
                        null,
                        fromDate,
                        toDate,
                        null
                )
        );

        verify(assignmentPortOut, never()).findAll(
                any(), any(), any(), any(),
                any(), any(), any(), any()
        );
    }

    private void stubValidCandidate(
            ShiftAssignment candidate,
            Shift shift,
            UUID zoneId
    ) {
        stubEmployeeAndGate(candidate, shift, zoneId);
        when(assignmentPortOut.findNotRemovedByShiftId(shift.getShiftId()))
                .thenReturn(List.of());
        when(assignmentPortOut.findNotRemovedEmployeeSchedule(
                eq(candidate.getEmployeeId()),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(List.of());
    }

    private void stubEmployeeAndGate(
            ShiftAssignment candidate,
            Shift shift,
            UUID zoneId
    ) {
        stubEmployeeAndGate(
                candidate.getEmployeeId(),
                candidate.getGateId(),
                shift,
                zoneId
        );
    }

    private void stubEmployeeAndGate(
            UUID employeeId,
            UUID gateId,
            Shift shift,
            UUID zoneId
    ) {
        when(employeePortOut.findById(employeeId))
                .thenReturn(Optional.of(employee(
                        employeeId,
                        EmployeeStatus.ACTIVE
                )));
        when(employeePortOut.hasAccountRole(employeeId, "EMPLOYEE"))
                .thenReturn(true);
        when(gatePortOut.findById(gateId))
                .thenReturn(Optional.of(gate(gateId, zoneId)));
        when(zonePortOut.findById(zoneId))
                .thenReturn(Optional.of(zone(
                        zoneId,
                        shift.getParkingLotId()
                )));
    }

    private ShiftAssignment requestAssignment() {
        ShiftAssignment assignment = new ShiftAssignment();
        assignment.setEmployeeId(UUID.randomUUID());
        assignment.setGateId(UUID.randomUUID());
        return assignment;
    }

    private ShiftAssignment activeAssignment(
            UUID shiftId,
            UUID employeeId,
            UUID gateId
    ) {
        ShiftAssignment assignment = new ShiftAssignment();
        assignment.setShiftAssignmentId(UUID.randomUUID());
        assignment.setShiftId(shiftId);
        assignment.setEmployeeId(employeeId);
        assignment.setGateId(gateId);
        assignment.setStatus(ShiftAssignmentStatus.ACTIVE);
        return assignment;
    }

    private Shift futureShift(
            UUID parkingLotId,
            LocalDate shiftDate,
            ShiftStatus status
    ) {
        Shift shift = new Shift();
        shift.setShiftId(UUID.randomUUID());
        shift.setShiftTemplateId(UUID.randomUUID());
        shift.setParkingLotId(parkingLotId);
        shift.setShiftCode("SHIFT-" + UUID.randomUUID());
        shift.setShiftDate(shiftDate);
        shift.setShiftType(ShiftType.MORNING);
        shift.setStartTime(shiftDate.atTime(6, 0)
                .atZone(DateTimeUtils.VIETNAM_ZONE)
                .toInstant());
        shift.setEndTime(shiftDate.atTime(14, 0)
                .atZone(DateTimeUtils.VIETNAM_ZONE)
                .toInstant());
        shift.setStatus(status);
        if (status == ShiftStatus.SCHEDULED) {
            shift.setApprovedAt(Instant.now());
            shift.setApprovedBy(UUID.randomUUID());
        }
        return shift;
    }

    private Employee employee(UUID employeeId, EmployeeStatus status) {
        Employee employee = new Employee();
        employee.setEmployeeId(employeeId);
        employee.setEmployeeCode("EMP-" + employeeId);
        employee.setStatus(status);
        return employee;
    }

    private Gate gate(UUID gateId, UUID zoneId) {
        Gate gate = new Gate();
        gate.setGateId(gateId);
        gate.setZoneId(zoneId);
        gate.setCode("GATE");
        gate.setName("Gate");
        gate.setStatus(GateStatus.ACTIVE);
        return gate;
    }

    private Zone zone(UUID zoneId, UUID parkingLotId) {
        Zone zone = new Zone();
        zone.setZoneId(zoneId);
        zone.setParkingLotId(parkingLotId);
        return zone;
    }

    private LocalDate nextMonday() {
        return LocalDate.now(DateTimeUtils.VIETNAM_ZONE)
                .with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }
}
