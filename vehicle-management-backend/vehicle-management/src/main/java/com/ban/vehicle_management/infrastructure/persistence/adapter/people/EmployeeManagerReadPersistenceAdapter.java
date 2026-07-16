package com.ban.vehicle_management.infrastructure.persistence.adapter.people;

import com.ban.vehicle_management.application.operations.approvalrequest.authorization.InternalEmployeeApprovalAccessGuard;
import com.ban.vehicle_management.application.people.employee.model.result.EmployeeActivityTimelineResult;
import com.ban.vehicle_management.application.people.employee.model.result.EmployeeRecentShiftResult;
import com.ban.vehicle_management.application.people.employee.port.out.EmployeeManagerReadPortOut;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.audit.AuditLogRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountStatusHistoryRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ApprovalRequestRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ShiftAssignmentRepository;
import com.ban.vehicle_management.infrastructure.mapper.people.EmployeeManagerReadPersistenceMapper;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftAssignmentStatus;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class EmployeeManagerReadPersistenceAdapter implements EmployeeManagerReadPortOut {

    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final AuditLogRepository auditLogRepository;
    private final AccountStatusHistoryRepository accountStatusHistoryRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final EmployeeManagerReadPersistenceMapper employeeManagerReadPersistenceMapper;

    public EmployeeManagerReadPersistenceAdapter(
            ShiftAssignmentRepository shiftAssignmentRepository,
            AuditLogRepository auditLogRepository,
            AccountStatusHistoryRepository accountStatusHistoryRepository,
            ApprovalRequestRepository approvalRequestRepository,
            EmployeeManagerReadPersistenceMapper employeeManagerReadPersistenceMapper
    ) {
        this.shiftAssignmentRepository = shiftAssignmentRepository;
        this.auditLogRepository = auditLogRepository;
        this.accountStatusHistoryRepository = accountStatusHistoryRepository;
        this.approvalRequestRepository = approvalRequestRepository;
        this.employeeManagerReadPersistenceMapper = employeeManagerReadPersistenceMapper;
    }

    @Override
    public List<EmployeeRecentShiftResult> findRecentShifts(
            UUID employeeId,
            int limit
    ) {
        return shiftAssignmentRepository.findRecentEmployeeShifts(
                        employeeId,
                        ShiftAssignmentStatus.REMOVED,
                        PageRequest.of(0, limit)
                )
                .stream()
                .map(employeeManagerReadPersistenceMapper::toRecentShiftResult)
                .toList();
    }

    @Override
    public List<EmployeeActivityTimelineResult> findActivityTimeline(
            UUID employeeId,
            int limit
    ) {
        PageRequest pageRequest = PageRequest.of(0, limit);

        List<EmployeeActivityTimelineResult> auditEvents = auditLogRepository
                .findEmployeeAuditTimeline(employeeId, pageRequest)
                .stream()
                .map(employeeManagerReadPersistenceMapper::toAuditTimelineResult)
                .toList();

        List<EmployeeActivityTimelineResult> accountEvents = accountStatusHistoryRepository
                .findEmployeeAccountStatusTimeline(employeeId, pageRequest)
                .stream()
                .map(employeeManagerReadPersistenceMapper::toAccountStatusTimelineResult)
                .toList();

        List<EmployeeActivityTimelineResult> approvalEvents = approvalRequestRepository
                .findEmployeeApprovalTimeline(
                        employeeId,
                        InternalEmployeeApprovalAccessGuard.REQUEST_TYPE,
                        InternalEmployeeApprovalAccessGuard.TARGET_SCHEMA,
                        InternalEmployeeApprovalAccessGuard.TARGET_TABLE,
                        pageRequest
                )
                .stream()
                .map(employeeManagerReadPersistenceMapper::toApprovalTimelineResult)
                .toList();

        return java.util.stream.Stream.of(auditEvents, accountEvents, approvalEvents)
                .flatMap(List::stream)
                .sorted(Comparator.comparing(
                        EmployeeActivityTimelineResult::eventTime,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed())
                .limit(limit)
                .toList();
    }
}
