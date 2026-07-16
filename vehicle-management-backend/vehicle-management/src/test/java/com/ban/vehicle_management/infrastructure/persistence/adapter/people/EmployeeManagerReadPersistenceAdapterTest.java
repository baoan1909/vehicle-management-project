package com.ban.vehicle_management.infrastructure.persistence.adapter.people;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.people.employee.model.result.EmployeeActivityTimelineResult;
import com.ban.vehicle_management.application.people.employee.model.result.EmployeeRecentShiftResult;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.audit.AuditLogRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountStatusHistoryRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ApprovalRequestRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ShiftAssignmentRepository;
import com.ban.vehicle_management.infrastructure.mapper.people.EmployeeManagerReadPersistenceMapper;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftAssignmentStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class EmployeeManagerReadPersistenceAdapterTest {

    @Mock
    private ShiftAssignmentRepository shiftAssignmentRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AccountStatusHistoryRepository accountStatusHistoryRepository;

    @Mock
    private ApprovalRequestRepository approvalRequestRepository;

    @Spy
    private EmployeeManagerReadPersistenceMapper employeeManagerReadPersistenceMapper =
            Mappers.getMapper(EmployeeManagerReadPersistenceMapper.class);

    @InjectMocks
    private EmployeeManagerReadPersistenceAdapter adapter;

    @Test
    void shouldMapRecentShiftProjection() {
        UUID employeeId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();

        when(shiftAssignmentRepository.findRecentEmployeeShifts(
                org.mockito.Mockito.eq(employeeId),
                org.mockito.Mockito.eq(ShiftAssignmentStatus.REMOVED),
                any(Pageable.class)
        )).thenReturn(List.of(new RecentShiftProjection(
                assignmentId,
                shiftId,
                LocalDate.of(2026, 7, 13),
                ShiftType.MORNING,
                Instant.parse("2026-07-13T00:00:00Z"),
                Instant.parse("2026-07-13T04:30:00Z"),
                "Cong A",
                ShiftAssignmentStatus.SCHEDULED
        )));

        List<EmployeeRecentShiftResult> results = adapter.findRecentShifts(employeeId, 3);

        assertEquals(1, results.size());
        assertEquals(shiftId, results.get(0).shiftId());
        assertEquals(assignmentId, results.get(0).assignmentId());
        assertEquals("07:00 - 11:30", results.get(0).timeRange());
        assertEquals("Cong A", results.get(0).locationName());
        assertEquals("OPERATOR", results.get(0).roleInShift());
    }

    @Test
    void shouldMergeSortAndLimitTimelineEvents() {
        UUID employeeId = UUID.randomUUID();
        UUID auditId = UUID.randomUUID();
        UUID statusHistoryId = UUID.randomUUID();
        UUID approvalId = UUID.randomUUID();

        when(auditLogRepository.findEmployeeAuditTimeline(
                org.mockito.Mockito.eq(employeeId),
                any(Pageable.class)
        )).thenReturn(List.of(new AuditTimelineProjection(
                auditId,
                Instant.parse("2026-07-13T01:00:00Z"),
                "UPDATE",
                UUID.randomUUID(),
                "manager",
                "Parking Manager"
        )));
        when(accountStatusHistoryRepository.findEmployeeAccountStatusTimeline(
                org.mockito.Mockito.eq(employeeId),
                any(Pageable.class)
        )).thenReturn(List.of(new AccountStatusTimelineProjection(
                statusHistoryId,
                Instant.parse("2026-07-13T03:00:00Z"),
                AccountStatus.ACTIVE,
                AccountStatus.LOCKED,
                "temporary lock",
                UUID.randomUUID(),
                "manager",
                "Parking Manager"
        )));
        when(approvalRequestRepository.findEmployeeApprovalTimeline(
                org.mockito.Mockito.eq(employeeId),
                org.mockito.Mockito.anyString(),
                org.mockito.Mockito.anyString(),
                org.mockito.Mockito.anyString(),
                any(Pageable.class)
        )).thenReturn(List.of(new ApprovalTimelineProjection(
                approvalId,
                Instant.parse("2026-07-13T02:00:00Z"),
                ApprovalRequestStatus.APPROVED,
                "ok",
                UUID.randomUUID(),
                "admin",
                "System Admin"
        )));

        List<EmployeeActivityTimelineResult> results = adapter.findActivityTimeline(employeeId, 2);

        assertEquals(2, results.size());
        assertEquals(statusHistoryId, results.get(0).eventId());
        assertEquals(approvalId, results.get(1).eventId());
    }

    private record RecentShiftProjection(
            UUID assignmentId,
            UUID shiftId,
            LocalDate shiftDate,
            ShiftType shiftType,
            Instant startTime,
            Instant endTime,
            String locationName,
            ShiftAssignmentStatus status
    ) implements ShiftAssignmentRepository.RecentEmployeeShiftProjection {
        @Override
        public UUID getAssignmentId() {
            return assignmentId;
        }

        @Override
        public UUID getShiftId() {
            return shiftId;
        }

        @Override
        public LocalDate getShiftDate() {
            return shiftDate;
        }

        @Override
        public ShiftType getShiftType() {
            return shiftType;
        }

        @Override
        public Instant getStartTime() {
            return startTime;
        }

        @Override
        public Instant getEndTime() {
            return endTime;
        }

        @Override
        public String getLocationName() {
            return locationName;
        }

        @Override
        public ShiftAssignmentStatus getStatus() {
            return status;
        }
    }

    private record AuditTimelineProjection(
            UUID eventId,
            Instant eventTime,
            String action,
            UUID actorAccountId,
            String actorUsername,
            String actorFullName
    ) implements AuditLogRepository.EmployeeAuditTimelineProjection {
        @Override
        public UUID getEventId() {
            return eventId;
        }

        @Override
        public Instant getEventTime() {
            return eventTime;
        }

        @Override
        public String getAction() {
            return action;
        }

        @Override
        public UUID getActorAccountId() {
            return actorAccountId;
        }

        @Override
        public String getActorUsername() {
            return actorUsername;
        }

        @Override
        public String getActorFullName() {
            return actorFullName;
        }
    }

    private record AccountStatusTimelineProjection(
            UUID eventId,
            Instant eventTime,
            AccountStatus oldStatus,
            AccountStatus newStatus,
            String reason,
            UUID actorAccountId,
            String actorUsername,
            String actorFullName
    ) implements AccountStatusHistoryRepository.EmployeeAccountStatusTimelineProjection {
        @Override
        public UUID getEventId() {
            return eventId;
        }

        @Override
        public Instant getEventTime() {
            return eventTime;
        }

        @Override
        public AccountStatus getOldStatus() {
            return oldStatus;
        }

        @Override
        public AccountStatus getNewStatus() {
            return newStatus;
        }

        @Override
        public String getReason() {
            return reason;
        }

        @Override
        public UUID getActorAccountId() {
            return actorAccountId;
        }

        @Override
        public String getActorUsername() {
            return actorUsername;
        }

        @Override
        public String getActorFullName() {
            return actorFullName;
        }
    }

    private record ApprovalTimelineProjection(
            UUID eventId,
            Instant eventTime,
            ApprovalRequestStatus status,
            String note,
            UUID actorAccountId,
            String actorUsername,
            String actorFullName
    ) implements ApprovalRequestRepository.EmployeeApprovalTimelineProjection {
        @Override
        public UUID getEventId() {
            return eventId;
        }

        @Override
        public Instant getEventTime() {
            return eventTime;
        }

        @Override
        public ApprovalRequestStatus getStatus() {
            return status;
        }

        @Override
        public String getNote() {
            return note;
        }

        @Override
        public UUID getActorAccountId() {
            return actorAccountId;
        }

        @Override
        public String getActorUsername() {
            return actorUsername;
        }

        @Override
        public String getActorFullName() {
            return actorFullName;
        }
    }
}
