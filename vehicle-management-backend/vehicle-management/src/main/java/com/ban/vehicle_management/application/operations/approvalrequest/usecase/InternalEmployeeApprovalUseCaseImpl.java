package com.ban.vehicle_management.application.operations.approvalrequest.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.approvalrequest.model.command.InternalEmployeeApprovalFilterCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.command.ReviewInternalEmployeeApprovalCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.InternalEmployeeApprovalCandidate;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.InternalEmployeeApprovalResult;
import com.ban.vehicle_management.application.operations.approvalrequest.port.in.InternalEmployeeApprovalPortIn;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.InternalEmployeeApprovalPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.domain.operations.approvalrequest.policy.ApprovalRequestPolicy;
import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.domain.people.employee.policy.EmployeePolicy;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InternalEmployeeApprovalUseCaseImpl implements InternalEmployeeApprovalPortIn {

    public static final String REQUEST_TYPE = "INTERNAL_EMPLOYEE_ONBOARDING";
    public static final String TARGET_SCHEMA = "people";
    public static final String TARGET_TABLE = "employees";

    private static final String ACCOUNT_READ_ALL = "ACCOUNT_READ_ALL";
    private static final String ACCOUNT_UPDATE_ALL = "ACCOUNT_UPDATE_ALL";
    private static final String EMPLOYEE_READ_ALL = "EMPLOYEE_READ_ALL";
    private static final String EMPLOYEE_UPDATE_ALL = "EMPLOYEE_UPDATE_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final InternalEmployeeApprovalPortOut internalEmployeeApprovalPortOut;
    private final ApprovalRequestPolicy approvalRequestPolicy = new ApprovalRequestPolicy();
    private final EmployeePolicy employeePolicy = new EmployeePolicy();
    private final Clock clock;

    public InternalEmployeeApprovalUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            InternalEmployeeApprovalPortOut internalEmployeeApprovalPortOut
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.internalEmployeeApprovalPortOut = internalEmployeeApprovalPortOut;
        this.clock = Clock.systemUTC();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InternalEmployeeApprovalResult> getInternalEmployeeApprovals(InternalEmployeeApprovalFilterCommand command) {
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        ensureCanReadApprovals(currentAccount);

        List<InternalEmployeeApprovalResult> results = internalEmployeeApprovalPortOut.findInternalEmployeeApprovalRequests(
                normalizeFilterCommand(command)
        );
        return results.stream()
                .filter(result -> canAccessTargetRole(currentAccount, result.account().roleCode()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InternalEmployeeApprovalResult getInternalEmployeeApprovalById(UUID approvalRequestId) {
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        ensureCanReadApprovals(currentAccount);

        InternalEmployeeApprovalResult result = internalEmployeeApprovalPortOut.findInternalEmployeeApprovalResultById(approvalRequestId)
                .orElseThrow(() -> new NotFoundException("Internal employee approval request not found"));
        ensureCanReviewTarget(currentAccount, result.account().roleCode());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public InternalEmployeeApprovalResult getMyLatestInternalEmployeeApproval() {
        UUID accountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        return internalEmployeeApprovalPortOut.findLatestInternalEmployeeApprovalResultByAccountId(accountId)
                .orElseThrow(() -> new NotFoundException("Internal employee approval request not found"));
    }

    @Override
    @Transactional
    public InternalEmployeeApprovalResult approveInternalEmployeeApproval(
            UUID approvalRequestId,
            ReviewInternalEmployeeApprovalCommand command
    ) {
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        ensureCanWriteApprovals(currentAccount);

        ApprovalRequest approvalRequest = internalEmployeeApprovalPortOut.findInternalEmployeeApprovalRequestById(approvalRequestId)
                .orElseThrow(() -> new NotFoundException("Internal employee approval request not found"));
        InternalEmployeeApprovalCandidate candidate = internalEmployeeApprovalPortOut.findCandidateByEmployeeId(approvalRequest.getTargetId())
                .orElseThrow(() -> new NotFoundException("Internal employee approval target not found"));
        ensureCanReviewTarget(currentAccount, candidate.roleCode());

        Employee employee = loadEmployee(candidate.employeeId());
        String note = normalizeNote(command);
        approvalRequestPolicy.approve(
                approvalRequest,
                currentAccount.accountId(),
                Instant.now(clock),
                note
        );
        employeePolicy.activate(employee);
        internalEmployeeApprovalPortOut.saveInternalEmployeeApprovalDecision(approvalRequest, employee);

        return internalEmployeeApprovalPortOut.findInternalEmployeeApprovalResultById(approvalRequestId)
                .orElseThrow(() -> new NotFoundException("Internal employee approval request not found"));
    }

    @Override
    @Transactional
    public InternalEmployeeApprovalResult rejectInternalEmployeeApproval(
            UUID approvalRequestId,
            ReviewInternalEmployeeApprovalCommand command
    ) {
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        ensureCanWriteApprovals(currentAccount);

        ApprovalRequest approvalRequest = internalEmployeeApprovalPortOut.findInternalEmployeeApprovalRequestById(approvalRequestId)
                .orElseThrow(() -> new NotFoundException("Internal employee approval request not found"));
        InternalEmployeeApprovalCandidate candidate = internalEmployeeApprovalPortOut.findCandidateByEmployeeId(approvalRequest.getTargetId())
                .orElseThrow(() -> new NotFoundException("Internal employee approval target not found"));
        ensureCanReviewTarget(currentAccount, candidate.roleCode());

        Employee employee = loadEmployee(candidate.employeeId());
        approvalRequestPolicy.reject(approvalRequest, normalizeNote(command));
        employeePolicy.inactivate(employee);
        internalEmployeeApprovalPortOut.saveInternalEmployeeApprovalDecision(approvalRequest, employee);

        return internalEmployeeApprovalPortOut.findInternalEmployeeApprovalResultById(approvalRequestId)
                .orElseThrow(() -> new NotFoundException("Internal employee approval request not found"));
    }

    @Override
    @Transactional
    public InternalEmployeeApprovalResult resubmitMyInternalEmployeeApproval() {
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        InternalEmployeeApprovalCandidate candidate = internalEmployeeApprovalPortOut.findCandidateByAccountId(currentAccount.accountId())
                .orElseThrow(() -> new NotFoundException("Internal employee approval target not found"));
        AdminProvisionableAccountRoleCode roleCode = requireProvisionableRole(candidate.roleCode());
        if (!roleCode.requiresEmployeeRecord()) {
            throw new ConflictException("Current account does not require internal employee approval");
        }
        if (internalEmployeeApprovalPortOut.existsPendingInternalEmployeeApprovalForEmployee(candidate.employeeId())) {
            throw new ConflictException("An internal employee approval request is already pending");
        }

        ApprovalRequest latestApprovalRequest = internalEmployeeApprovalPortOut.findLatestInternalEmployeeApprovalRequest(candidate.employeeId())
                .orElse(null);
        if (latestApprovalRequest != null && ApprovalRequestStatus.APPROVED.equals(latestApprovalRequest.getStatus())) {
            throw new ConflictException("Internal employee approval has already been approved");
        }

        ApprovalRequest approvalRequest = buildPendingApprovalRequest(candidate.employeeId(), currentAccount.accountId());
        internalEmployeeApprovalPortOut.saveInternalEmployeeApprovalRequest(approvalRequest);

        return internalEmployeeApprovalPortOut.findInternalEmployeeApprovalResultById(approvalRequest.getApprovalRequestId())
                .orElseThrow(() -> new NotFoundException("Internal employee approval request not found"));
    }

    public ApprovalRequest buildPendingApprovalRequest(UUID employeeId, UUID requestedBy) {
        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setApprovalRequestId(UUID.randomUUID());
        approvalRequest.setRequestType(REQUEST_TYPE);
        approvalRequest.setTargetSchema(TARGET_SCHEMA);
        approvalRequest.setTargetTable(TARGET_TABLE);
        approvalRequest.setTargetId(employeeId);
        approvalRequest.setStatus(ApprovalRequestStatus.PENDING);
        approvalRequest.setRequestedBy(requestedBy);
        approvalRequestPolicy.initialize(approvalRequest);
        return approvalRequest;
    }

    private InternalEmployeeApprovalFilterCommand normalizeFilterCommand(InternalEmployeeApprovalFilterCommand command) {
        if (command == null) {
            return new InternalEmployeeApprovalFilterCommand(null, null, null);
        }
        return new InternalEmployeeApprovalFilterCommand(
                TextValidationUtils.normalizeNullableText(command.keyword(), "keyword", 100),
                command.roleCode(),
                command.status()
        );
    }

    private String normalizeNote(ReviewInternalEmployeeApprovalCommand command) {
        return TextValidationUtils.normalizeNullableText(command == null ? null : command.note(), "note", 0);
    }

    private void ensureCanReadApprovals(CurrentAccountAccess currentAccount) {
        if (currentAccountPortIn.hasPermission(ACCOUNT_READ_ALL) || currentAccountPortIn.hasPermission(EMPLOYEE_READ_ALL)) {
            return;
        }
        throw new AccessDeniedException("Access is denied");
    }

    private void ensureCanWriteApprovals(CurrentAccountAccess currentAccount) {
        if (currentAccountPortIn.hasPermission(ACCOUNT_UPDATE_ALL) || currentAccountPortIn.hasPermission(EMPLOYEE_UPDATE_ALL)) {
            return;
        }
        throw new AccessDeniedException("Access is denied");
    }

    private void ensureCanReviewTarget(CurrentAccountAccess currentAccount, String targetRoleCode) {
        if (!canAccessTargetRole(currentAccount, targetRoleCode)) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    private boolean canAccessTargetRole(CurrentAccountAccess currentAccount, String targetRoleCode) {
        AdminProvisionableAccountRoleCode approverRole = requireProvisionableRole(currentAccount.roleCode());
        AdminProvisionableAccountRoleCode targetRole = requireProvisionableRole(targetRoleCode);
        if (!targetRole.requiresEmployeeRecord()) {
            return false;
        }
        return switch (approverRole) {
            case SYSTEM_ADMIN -> true;
            case PARKING_MANAGER -> AdminProvisionableAccountRoleCode.EMPLOYEE.equals(targetRole);
            case CUSTOMER, EMPLOYEE -> false;
        };
    }

    private AdminProvisionableAccountRoleCode requireProvisionableRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            throw new AccessDeniedException("Access is denied");
        }
        try {
            return AdminProvisionableAccountRoleCode.valueOf(roleCode);
        } catch (IllegalArgumentException exception) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    private Employee loadEmployee(UUID employeeId) {
        return internalEmployeeApprovalPortOut.findEmployeeById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));
    }
}
