package com.ban.vehicle_management.application.operations.approvalrequest.port.out;

import com.ban.vehicle_management.application.operations.approvalrequest.model.command.InternalEmployeeApprovalFilterCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.InternalEmployeeApprovalCandidate;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.InternalEmployeeApprovalResult;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.domain.people.employee.model.Employee;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InternalEmployeeApprovalPortOut {
    void saveInternalEmployeeApprovalRequest(ApprovalRequest approvalRequest);

    void saveInternalEmployeeApprovalDecision(ApprovalRequest approvalRequest, Employee employee);

    boolean existsPendingInternalEmployeeApprovalForEmployee(UUID employeeId);

    Optional<ApprovalRequest> findInternalEmployeeApprovalRequestById(UUID approvalRequestId);

    Optional<ApprovalRequest> findLatestInternalEmployeeApprovalRequest(UUID employeeId);

    Optional<Employee> findEmployeeById(UUID employeeId);

    Optional<InternalEmployeeApprovalCandidate> findCandidateByEmployeeId(UUID employeeId);

    Optional<InternalEmployeeApprovalCandidate> findCandidateByAccountId(UUID accountId);

    List<InternalEmployeeApprovalResult> findInternalEmployeeApprovalRequests(InternalEmployeeApprovalFilterCommand command);

    Optional<InternalEmployeeApprovalResult> findInternalEmployeeApprovalResultById(UUID approvalRequestId);

    Optional<InternalEmployeeApprovalResult> findLatestInternalEmployeeApprovalResultByAccountId(UUID accountId);
}
