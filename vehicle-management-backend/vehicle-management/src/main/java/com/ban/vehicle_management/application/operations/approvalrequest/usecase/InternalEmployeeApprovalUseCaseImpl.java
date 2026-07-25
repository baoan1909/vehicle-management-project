package com.ban.vehicle_management.application.operations.approvalrequest.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.approvalrequest.authorization.InternalEmployeeApprovalAccessGuard;
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
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import com.ban.vehicle_management.infrastructure.mail.VehicleMailService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InternalEmployeeApprovalUseCaseImpl implements InternalEmployeeApprovalPortIn {

    private final CurrentAccountPortIn currentAccountPortIn;
    private final InternalEmployeeApprovalAccessGuard internalEmployeeApprovalAccessGuard;
    private final InternalEmployeeApprovalPortOut internalEmployeeApprovalPortOut;
    private final VehicleMailService vehicleMailService;
    private final ApprovalRequestPolicy approvalRequestPolicy = new ApprovalRequestPolicy();
    private final EmployeePolicy employeePolicy = new EmployeePolicy();
    private final Clock clock;

    @Autowired
    public InternalEmployeeApprovalUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            InternalEmployeeApprovalAccessGuard internalEmployeeApprovalAccessGuard,
            InternalEmployeeApprovalPortOut internalEmployeeApprovalPortOut,
            VehicleMailService vehicleMailService
    ) {
        this(
                currentAccountPortIn,
                internalEmployeeApprovalAccessGuard,
                internalEmployeeApprovalPortOut,
                vehicleMailService,
                Clock.systemUTC()
        );
    }

    InternalEmployeeApprovalUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            InternalEmployeeApprovalAccessGuard internalEmployeeApprovalAccessGuard,
            InternalEmployeeApprovalPortOut internalEmployeeApprovalPortOut,
            VehicleMailService vehicleMailService,
            Clock clock
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.internalEmployeeApprovalAccessGuard = internalEmployeeApprovalAccessGuard;
        this.internalEmployeeApprovalPortOut = internalEmployeeApprovalPortOut;
        this.vehicleMailService = vehicleMailService;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InternalEmployeeApprovalResult> getInternalEmployeeApprovals(InternalEmployeeApprovalFilterCommand command) {
        CurrentAccountAccess currentAccount = internalEmployeeApprovalAccessGuard.requireReadAccess();

        List<InternalEmployeeApprovalResult> results = internalEmployeeApprovalPortOut.findInternalEmployeeApprovalRequests(
                normalizeFilterCommand(command)
        );
        return results.stream()
                .filter(result -> internalEmployeeApprovalAccessGuard.canAccessTargetRole(
                        currentAccount,
                        result.account().roleCode()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InternalEmployeeApprovalResult getInternalEmployeeApprovalById(UUID approvalRequestId) {
        CurrentAccountAccess currentAccount = internalEmployeeApprovalAccessGuard.requireReadAccess();

        InternalEmployeeApprovalResult result = internalEmployeeApprovalPortOut.findInternalEmployeeApprovalResultById(approvalRequestId)
                .orElseThrow(() -> new NotFoundException("Internal employee approval request not found"));
        internalEmployeeApprovalAccessGuard.ensureCanReviewTarget(currentAccount, result.account().roleCode());
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
        CurrentAccountAccess currentAccount = internalEmployeeApprovalAccessGuard.requireWriteAccess();

        ApprovalRequest approvalRequest = internalEmployeeApprovalPortOut.findInternalEmployeeApprovalRequestById(approvalRequestId)
                .orElseThrow(() -> new NotFoundException("Internal employee approval request not found"));
        InternalEmployeeApprovalCandidate candidate = internalEmployeeApprovalPortOut.findCandidateByEmployeeId(approvalRequest.getTargetId())
                .orElseThrow(() -> new NotFoundException("Internal employee approval target not found"));
        internalEmployeeApprovalAccessGuard.ensureCanReviewTarget(currentAccount, candidate.roleCode());

        Employee employee = loadEmployee(candidate.employeeId());
        String note = normalizeNote(command);
        Instant approvedAt = Instant.now(clock);
        approvalRequestPolicy.approve(
                approvalRequest,
                currentAccount.accountId(),
                approvedAt,
                note
        );
        employeePolicy.activate(employee, DateTimeUtils.toVietnamLocalDate(approvedAt));
        internalEmployeeApprovalPortOut.saveInternalEmployeeApprovalDecision(approvalRequest, employee);

        InternalEmployeeApprovalResult result = internalEmployeeApprovalPortOut.findInternalEmployeeApprovalResultById(approvalRequestId)
                .orElseThrow(() -> new NotFoundException("Internal employee approval request not found"));
        sendOnboardingApprovedEmail(result);
        return result;
    }

    @Override
    @Transactional
    public InternalEmployeeApprovalResult rejectInternalEmployeeApproval(
            UUID approvalRequestId,
            ReviewInternalEmployeeApprovalCommand command
    ) {
        CurrentAccountAccess currentAccount = internalEmployeeApprovalAccessGuard.requireWriteAccess();

        ApprovalRequest approvalRequest = internalEmployeeApprovalPortOut.findInternalEmployeeApprovalRequestById(approvalRequestId)
                .orElseThrow(() -> new NotFoundException("Internal employee approval request not found"));
        InternalEmployeeApprovalCandidate candidate = internalEmployeeApprovalPortOut.findCandidateByEmployeeId(approvalRequest.getTargetId())
                .orElseThrow(() -> new NotFoundException("Internal employee approval target not found"));
        internalEmployeeApprovalAccessGuard.ensureCanReviewTarget(currentAccount, candidate.roleCode());

        Employee employee = loadEmployee(candidate.employeeId());
        approvalRequestPolicy.reject(approvalRequest, normalizeNote(command));
        employeePolicy.inactivate(employee);
        internalEmployeeApprovalPortOut.saveInternalEmployeeApprovalDecision(approvalRequest, employee);

        InternalEmployeeApprovalResult result = internalEmployeeApprovalPortOut.findInternalEmployeeApprovalResultById(approvalRequestId)
                .orElseThrow(() -> new NotFoundException("Internal employee approval request not found"));
        sendOnboardingRejectedEmail(result);
        return result;
    }

    @Override
    @Transactional
    public InternalEmployeeApprovalResult resubmitMyInternalEmployeeApproval() {
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        InternalEmployeeApprovalCandidate candidate = internalEmployeeApprovalPortOut.findCandidateByAccountId(currentAccount.accountId())
                .orElseThrow(() -> new NotFoundException("Internal employee approval target not found"));
        AdminProvisionableAccountRoleCode roleCode = internalEmployeeApprovalAccessGuard.requireProvisionableRole(candidate.roleCode());
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
        approvalRequest.setRequestType(InternalEmployeeApprovalAccessGuard.REQUEST_TYPE);
        approvalRequest.setTargetSchema(InternalEmployeeApprovalAccessGuard.TARGET_SCHEMA);
        approvalRequest.setTargetTable(InternalEmployeeApprovalAccessGuard.TARGET_TABLE);
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

    private Employee loadEmployee(UUID employeeId) {
        return internalEmployeeApprovalPortOut.findEmployeeById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));
    }

    private void sendOnboardingApprovedEmail(InternalEmployeeApprovalResult result) {
        vehicleMailService.sendOnboardingApprovedEmail(
                result.account().email(),
                result.profile().fullName(),
                resolveRoleLabel(result.account().roleCode())
        );
    }

    private void sendOnboardingRejectedEmail(InternalEmployeeApprovalResult result) {
        vehicleMailService.sendOnboardingRejectedEmail(
                result.account().email(),
                result.profile().fullName(),
                resolveRoleLabel(result.account().roleCode()),
                result.request().note()
        );
    }

    private String resolveRoleLabel(String roleCode) {
        if ("PARKING_MANAGER".equals(roleCode)) {
            return "quản lý bãi xe";
        }
        if ("PARKING_ATTENDANT".equals(roleCode)) {
            return "nhân viên vận hành";
        }
        return "nhân sự nội bộ";
    }
}
