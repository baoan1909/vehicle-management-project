package com.ban.vehicle_management.application.operations.shift.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.ban.vehicle_management.application.operations.employeerosterrule.port.out.EmployeeRosterRulePortOut;
import com.ban.vehicle_management.application.operations.shift.port.out.ShiftPortOut;
import com.ban.vehicle_management.application.operations.shiftassignment.port.in.ShiftAssignmentPortIn;
import com.ban.vehicle_management.application.operations.shiftassignment.port.out.ShiftAssignmentPortOut;
import com.ban.vehicle_management.application.operations.shifttemplate.port.out.ShiftTemplatePortOut;
import com.ban.vehicle_management.application.parking.gate.port.out.GatePortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.application.people.employee.port.out.EmployeePortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.operations.employeerosterrule.model.EmployeeRosterRule;
import com.ban.vehicle_management.domain.operations.shift.model.Shift;
import com.ban.vehicle_management.domain.operations.shiftassignment.model.ShiftAssignment;
import com.ban.vehicle_management.domain.operations.shifttemplate.model.ShiftTemplate;
import com.ban.vehicle_management.domain.parking.gate.model.Gate;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.operations.AssignmentMode;
import com.ban.vehicle_management.shared.enumeration.operations.RosterRuleStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftAssignmentStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftTemplateStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import com.ban.vehicle_management.shared.enumeration.parking.GateStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShiftUseCaseImplTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;
    @Mock
    private ShiftPortOut shiftPortOut;
    @Mock
    private ShiftTemplatePortOut templatePortOut;
    @Mock
    private EmployeeRosterRulePortOut rosterRulePortOut;
    @Mock
    private ShiftAssignmentPortIn assignmentPortIn;
    @Mock
    private ShiftAssignmentPortOut assignmentPortOut;
    @Mock
    private ParkingLotPortOut parkingLotPortOut;
    @Mock
    private EmployeePortOut employeePortOut;
    @Mock
    private GatePortOut gatePortOut;
    @Mock
    private ZonePortOut zonePortOut;

    @InjectMocks
    private ShiftUseCaseImpl useCase;

    @Test
    void shouldGenerateTwentyOneShiftsAndFortyTwoAssignments() {
        LocalDate weekStart = nextMonday();
        UUID parkingLotId = UUID.randomUUID();
        UUID firstGateId = UUID.randomUUID();
        UUID secondGateId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();

        when(parkingLotPortOut.findById(parkingLotId))
                .thenReturn(Optional.of(parkingLot(
                        parkingLotId,
                        ParkingLotStatus.ACTIVE
                )));
        when(shiftPortOut.existsInDateRange(
                parkingLotId,
                weekStart,
                weekStart.plusDays(6)
        )).thenReturn(false);
        when(templatePortOut.findActiveByParkingLotId(parkingLotId))
                .thenReturn(templates(parkingLotId));
        when(rosterRulePortOut.findActiveByParkingLotId(parkingLotId))
                .thenReturn(rosterRules(
                        parkingLotId,
                        firstGateId,
                        secondGateId,
                        weekStart
                ));
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
                        zoneId,
                        GateStatus.ACTIVE
                )));
        when(zonePortOut.findById(zoneId))
                .thenReturn(Optional.of(zone(zoneId, parkingLotId)));
        when(shiftPortOut.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(assignmentPortIn.createAssignment(
                any(UUID.class),
                any(ShiftAssignment.class)
        )).thenAnswer(invocation -> invocation.getArgument(1));

        List<Shift> result = useCase.generateWeek(
                parkingLotId,
                weekStart
        );

        assertEquals(21, result.size());
        verify(currentAccountPortIn)
                .requirePermission("SHIFT_CREATE_ALL");
        verify(shiftPortOut).saveAll(
                org.mockito.ArgumentMatchers.argThat(
                        shifts -> shifts.size() == 21
                                && shifts.stream().allMatch(
                                shift -> shift.getStatus()
                                        == ShiftStatus.DRAFT
                        )
                )
        );
        verify(assignmentPortIn, times(42)).createAssignment(
                any(UUID.class),
                any(ShiftAssignment.class)
        );
    }

    @Test
    void shouldRejectGeneratingWeekWhenStartDateIsNotMonday() {
        LocalDate tuesday = nextMonday().plusDays(1);

        assertThrows(
                BadRequestException.class,
                () -> useCase.generateWeek(
                        UUID.randomUUID(),
                        tuesday
                )
        );

        verify(shiftPortOut, never()).saveAll(anyList());
    }

    @Test
    void shouldRejectApprovingIncompleteWeek() {
        UUID parkingLotId = UUID.randomUUID();
        LocalDate weekStart = nextMonday();

        when(parkingLotPortOut.findById(parkingLotId))
                .thenReturn(Optional.of(parkingLot(
                        parkingLotId,
                        ParkingLotStatus.ACTIVE
                )));
        when(shiftPortOut.findByParkingLotAndDateRangeForUpdate(
                parkingLotId,
                weekStart,
                weekStart.plusDays(6)
        )).thenReturn(List.of(draftShift(parkingLotId)));

        assertThrows(
                ConflictException.class,
                () -> useCase.approveWeek(
                        parkingLotId,
                        weekStart
                )
        );

        verify(shiftPortOut, never()).saveAll(anyList());
    }

    @Test
    void shouldOpenScheduledShiftWhenOperationalRequirementsAreValid() {
        UUID parkingLotId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        Shift shift = scheduledCurrentShift(parkingLotId);
        List<ShiftAssignment> assignments = List.of(
                assignment(shift.getShiftId(), UUID.randomUUID(), UUID.randomUUID()),
                assignment(shift.getShiftId(), UUID.randomUUID(), UUID.randomUUID())
        );

        when(currentAccountPortIn.hasPermission("SHIFT_UPDATE_ALL"))
                .thenReturn(true);
        when(currentAccountPortIn.getCurrentAccountOrThrow())
                .thenReturn(managerAccess(accountId));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow())
                .thenReturn(accountId);
        when(shiftPortOut.findByIdForUpdate(shift.getShiftId()))
                .thenReturn(Optional.of(shift));
        when(parkingLotPortOut.findById(parkingLotId))
                .thenReturn(Optional.of(parkingLot(
                        parkingLotId,
                        ParkingLotStatus.ACTIVE
                )));
        when(assignmentPortOut.findByShiftId(
                shift.getShiftId(),
                ShiftAssignmentStatus.ACTIVE
        )).thenReturn(assignments);
        stubValidAssignmentReferences(assignments, zoneId, parkingLotId);
        when(shiftPortOut.hasOpenShift(parkingLotId)).thenReturn(false);
        when(shiftPortOut.save(shift)).thenReturn(shift);

        Shift result = useCase.openShift(
                shift.getShiftId(),
                new BigDecimal("500000"),
                "Received cash"
        );

        assertEquals(ShiftStatus.OPEN, result.getStatus());
        assertEquals(new BigDecimal("500000"), result.getOpeningCash());
        assertEquals(accountId, result.getOpenedBy());
    }

    @Test
    void shouldRejectOpeningShiftWithMissingAssignment() {
        UUID parkingLotId = UUID.randomUUID();
        Shift shift = scheduledCurrentShift(parkingLotId);

        when(currentAccountPortIn.hasPermission("SHIFT_UPDATE_ALL"))
                .thenReturn(true);
        when(shiftPortOut.findByIdForUpdate(shift.getShiftId()))
                .thenReturn(Optional.of(shift));
        when(parkingLotPortOut.findById(parkingLotId))
                .thenReturn(Optional.of(parkingLot(
                        parkingLotId,
                        ParkingLotStatus.ACTIVE
                )));
        when(assignmentPortOut.findByShiftId(
                shift.getShiftId(),
                ShiftAssignmentStatus.ACTIVE
        )).thenReturn(List.of());

        assertThrows(
                ConflictException.class,
                () -> useCase.openShift(
                        shift.getShiftId(),
                        BigDecimal.ZERO,
                        null
                )
        );

        verify(shiftPortOut, never()).save(any(Shift.class));
    }

    @Test
    void shouldCloseOpenShift() {
        UUID accountId = UUID.randomUUID();
        Shift shift = openShift(UUID.randomUUID());

        when(shiftPortOut.findByIdForUpdate(shift.getShiftId()))
                .thenReturn(Optional.of(shift));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow())
                .thenReturn(accountId);
        when(shiftPortOut.save(shift)).thenReturn(shift);

        Shift result = useCase.closeShift(
                shift.getShiftId(),
                new BigDecimal("3200000"),
                "Counted"
        );

        verify(currentAccountPortIn)
                .requirePermission("SHIFT_UPDATE_ALL");
        assertEquals(ShiftStatus.CLOSED, result.getStatus());
        assertEquals(accountId, result.getClosedBy());
    }

    @Test
    void shouldCancelShiftAndRemoveActiveAssignments() {
        UUID accountId = UUID.randomUUID();
        Shift shift = draftShift(UUID.randomUUID());
        ShiftAssignment assignment = assignment(
                shift.getShiftId(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        when(shiftPortOut.findByIdForUpdate(shift.getShiftId()))
                .thenReturn(Optional.of(shift));
        when(assignmentPortOut.findByShiftId(
                shift.getShiftId(),
                ShiftAssignmentStatus.ACTIVE
        )).thenReturn(List.of(assignment));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow())
                .thenReturn(accountId);
        when(assignmentPortOut.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(shiftPortOut.save(shift)).thenReturn(shift);

        Shift result = useCase.cancelShift(
                shift.getShiftId(),
                "Parking lot maintenance"
        );

        assertEquals(ShiftStatus.CANCELLED, result.getStatus());
        assertEquals(ShiftAssignmentStatus.REMOVED, assignment.getStatus());
        assertEquals("Parking lot maintenance", result.getCancellationReason());
        verify(assignmentPortOut).saveAll(List.of(assignment));
    }

    @Test
    void shouldDelegateListFiltersAndNormalizeKeyword() {
        UUID parkingLotId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        LocalDate fromDate = LocalDate.of(2026, 7, 6);
        LocalDate toDate = fromDate.plusDays(6);

        when(shiftPortOut.findAll(
                parkingLotId,
                fromDate,
                toDate,
                ShiftType.MORNING,
                ShiftStatus.SCHEDULED,
                employeeId,
                "HCMUTE"
        )).thenReturn(List.of(new Shift()));

        List<Shift> result = useCase.getShifts(
                parkingLotId,
                fromDate,
                toDate,
                ShiftType.MORNING,
                ShiftStatus.SCHEDULED,
                employeeId,
                "  HCMUTE  "
        );

        assertEquals(1, result.size());
        verify(currentAccountPortIn)
                .requirePermission("SHIFT_READ_ALL");
    }

    @Test
    void shouldThrowWhenShiftDoesNotExist() {
        UUID shiftId = UUID.randomUUID();
        when(shiftPortOut.findById(shiftId))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> useCase.getShiftById(shiftId)
        );
    }

    private void stubValidAssignmentReferences(
            List<ShiftAssignment> assignments,
            UUID zoneId,
            UUID parkingLotId
    ) {
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
                        zoneId,
                        GateStatus.ACTIVE
                )));
        when(zonePortOut.findById(zoneId))
                .thenReturn(Optional.of(zone(zoneId, parkingLotId)));
    }

    private List<ShiftTemplate> templates(UUID parkingLotId) {
        return List.of(
                template(parkingLotId, ShiftType.MORNING, 6, 14),
                template(parkingLotId, ShiftType.AFTERNOON, 14, 22),
                template(parkingLotId, ShiftType.NIGHT, 22, 6)
        );
    }

    private List<EmployeeRosterRule> rosterRules(
            UUID parkingLotId,
            UUID firstGateId,
            UUID secondGateId,
            LocalDate effectiveFrom
    ) {
        List<EmployeeRosterRule> rules = new ArrayList<>();
        rules.add(fixedRule(parkingLotId, firstGateId, ShiftType.MORNING, DayOfWeek.MONDAY, effectiveFrom));
        rules.add(fixedRule(parkingLotId, secondGateId, ShiftType.MORNING, DayOfWeek.TUESDAY, effectiveFrom));
        rules.add(fixedRule(parkingLotId, firstGateId, ShiftType.AFTERNOON, DayOfWeek.WEDNESDAY, effectiveFrom));
        rules.add(fixedRule(parkingLotId, secondGateId, ShiftType.AFTERNOON, DayOfWeek.THURSDAY, effectiveFrom));
        rules.add(fixedRule(parkingLotId, firstGateId, ShiftType.NIGHT, DayOfWeek.FRIDAY, effectiveFrom));
        rules.add(fixedRule(parkingLotId, secondGateId, ShiftType.NIGHT, DayOfWeek.SATURDAY, effectiveFrom));

        EmployeeRosterRule relief = baseRule(
                parkingLotId,
                DayOfWeek.SUNDAY,
                effectiveFrom
        );
        relief.setAssignmentMode(AssignmentMode.RELIEF);
        rules.add(relief);
        return rules;
    }

    private EmployeeRosterRule fixedRule(
            UUID parkingLotId,
            UUID gateId,
            ShiftType shiftType,
            DayOfWeek dayOff,
            LocalDate effectiveFrom
    ) {
        EmployeeRosterRule rule = baseRule(
                parkingLotId,
                dayOff,
                effectiveFrom
        );
        rule.setAssignmentMode(AssignmentMode.FIXED);
        rule.setPreferredShiftType(shiftType);
        rule.setPreferredGateId(gateId);
        return rule;
    }

    private EmployeeRosterRule baseRule(
            UUID parkingLotId,
            DayOfWeek dayOff,
            LocalDate effectiveFrom
    ) {
        EmployeeRosterRule rule = new EmployeeRosterRule();
        rule.setRosterRuleId(UUID.randomUUID());
        rule.setParkingLotId(parkingLotId);
        rule.setEmployeeId(UUID.randomUUID());
        rule.setWeeklyDayOff(dayOff);
        rule.setEffectiveFrom(effectiveFrom);
        rule.setStatus(RosterRuleStatus.ACTIVE);
        return rule;
    }

    private ShiftTemplate template(
            UUID parkingLotId,
            ShiftType shiftType,
            int startHour,
            int endHour
    ) {
        ShiftTemplate template = new ShiftTemplate();
        template.setShiftTemplateId(UUID.randomUUID());
        template.setParkingLotId(parkingLotId);
        template.setShiftType(shiftType);
        template.setName(shiftType.name());
        template.setStartLocalTime(LocalTime.of(startHour, 0));
        template.setEndLocalTime(LocalTime.of(endHour, 0));
        template.setStatus(ShiftTemplateStatus.ACTIVE);
        return template;
    }

    private Shift draftShift(UUID parkingLotId) {
        Shift shift = baseShift(parkingLotId);
        shift.setStatus(ShiftStatus.DRAFT);
        return shift;
    }

    private Shift scheduledCurrentShift(UUID parkingLotId) {
        Shift shift = baseShift(parkingLotId);
        shift.setStatus(ShiftStatus.SCHEDULED);
        shift.setApprovedAt(Instant.now().minusSeconds(3600));
        shift.setApprovedBy(UUID.randomUUID());
        shift.setStartTime(Instant.now().minusSeconds(60));
        shift.setEndTime(Instant.now().plusSeconds(3600));
        return shift;
    }

    private Shift openShift(UUID parkingLotId) {
        Shift shift = scheduledCurrentShift(parkingLotId);
        shift.setStatus(ShiftStatus.OPEN);
        shift.setOpeningCash(BigDecimal.ZERO);
        shift.setOpenedAt(Instant.now().minusSeconds(30));
        shift.setOpenedBy(UUID.randomUUID());
        return shift;
    }

    private Shift baseShift(UUID parkingLotId) {
        Shift shift = new Shift();
        shift.setShiftId(UUID.randomUUID());
        shift.setShiftTemplateId(UUID.randomUUID());
        shift.setParkingLotId(parkingLotId);
        shift.setShiftCode("HCMUTE-20260706-MORNING");
        shift.setShiftDate(nextMonday());
        shift.setShiftType(ShiftType.MORNING);
        shift.setStartTime(Instant.now().plusSeconds(3600));
        shift.setEndTime(Instant.now().plusSeconds(7200));
        return shift;
    }

    private ShiftAssignment assignment(
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

    private ParkingLot parkingLot(
            UUID parkingLotId,
            ParkingLotStatus status
    ) {
        ParkingLot parkingLot = new ParkingLot();
        parkingLot.setParkingLotId(parkingLotId);
        parkingLot.setCode("HCMUTE");
        parkingLot.setName("HCMUTE Parking Lot");
        parkingLot.setTotalCapacity(1000);
        parkingLot.setStatus(status);
        return parkingLot;
    }

    private Employee employee(
            UUID employeeId,
            EmployeeStatus status
    ) {
        Employee employee = new Employee();
        employee.setEmployeeId(employeeId);
        employee.setEmployeeCode("EMP-" + employeeId);
        employee.setStatus(status);
        return employee;
    }

    private Gate gate(
            UUID gateId,
            UUID zoneId,
            GateStatus status
    ) {
        Gate gate = new Gate();
        gate.setGateId(gateId);
        gate.setZoneId(zoneId);
        gate.setCode("GATE");
        gate.setName("Gate");
        gate.setStatus(status);
        return gate;
    }

    private Zone zone(UUID zoneId, UUID parkingLotId) {
        Zone zone = new Zone();
        zone.setZoneId(zoneId);
        zone.setParkingLotId(parkingLotId);
        return zone;
    }

    private CurrentAccountAccess managerAccess(UUID accountId) {
        return new CurrentAccountAccess(
                accountId,
                "manager-subject",
                "manager",
                "manager@example.com",
                UUID.randomUUID(),
                "PARKING_MANAGER",
                AccountStatus.ACTIVE,
                EmployeeStatus.ACTIVE,
                Set.of("SHIFT_UPDATE_ALL")
        );
    }

    private LocalDate nextMonday() {
        return LocalDate.now(DateTimeUtils.VIETNAM_ZONE)
                .with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }
}
