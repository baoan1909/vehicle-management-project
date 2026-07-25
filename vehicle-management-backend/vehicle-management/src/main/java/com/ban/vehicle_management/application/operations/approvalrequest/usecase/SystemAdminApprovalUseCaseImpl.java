package com.ban.vehicle_management.application.operations.approvalrequest.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.approvalrequest.authorization.SystemAdminApprovalAccessGuard;
import com.ban.vehicle_management.application.operations.approvalrequest.model.command.ReviewInternalEmployeeApprovalCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.command.SystemAdminApprovalFilterCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.SystemAdminApprovalResult;
import com.ban.vehicle_management.application.operations.approvalrequest.port.in.SystemAdminApprovalPortIn;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.SystemAdminApprovalPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.domain.operations.approvalrequest.policy.ApprovalRequestPolicy;
import com.ban.vehicle_management.infrastructure.mail.VehicleMailService;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemAdminApprovalUseCaseImpl implements SystemAdminApprovalPortIn {

    private final CurrentAccountPortIn currentAccountPortIn;
    private final SystemAdminApprovalAccessGuard systemAdminApprovalAccessGuard;
    private final SystemAdminApprovalPortOut systemAdminApprovalPortOut;
    private final VehicleMailService vehicleMailService;
    private final ApprovalRequestPolicy approvalRequestPolicy = new ApprovalRequestPolicy();
    private final Clock clock;

    public SystemAdminApprovalUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            SystemAdminApprovalAccessGuard systemAdminApprovalAccessGuard,
            SystemAdminApprovalPortOut systemAdminApprovalPortOut,
            VehicleMailService vehicleMailService
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.systemAdminApprovalAccessGuard = systemAdminApprovalAccessGuard;
        this.systemAdminApprovalPortOut = systemAdminApprovalPortOut;
        this.vehicleMailService = vehicleMailService;
        this.clock = Clock.systemUTC();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemAdminApprovalResult> getSystemAdminApprovals(SystemAdminApprovalFilterCommand command) {
        systemAdminApprovalAccessGuard.requireReadAccess();
        return systemAdminApprovalPortOut.findSystemAdminApprovalRequests(normalizeFilterCommand(command));
    }

    @Override
    @Transactional(readOnly = true)
    public SystemAdminApprovalResult getSystemAdminApprovalById(UUID approvalRequestId) {
        systemAdminApprovalAccessGuard.requireReadAccess();
        return systemAdminApprovalPortOut.findSystemAdminApprovalResultById(approvalRequestId)
                .orElseThrow(() -> new NotFoundException("System admin approval request not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public SystemAdminApprovalResult getMyLatestSystemAdminApproval() {
        UUID accountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        return systemAdminApprovalPortOut.findLatestSystemAdminApprovalResultByAccountId(accountId)
                .orElseThrow(() -> new NotFoundException("System admin approval request not found"));
    }

    @Override
    @Transactional
    public SystemAdminApprovalResult approveSystemAdminApproval(
            UUID approvalRequestId,
            ReviewInternalEmployeeApprovalCommand command
    ) {
        CurrentAccountAccess currentAccount = systemAdminApprovalAccessGuard.requireWriteAccess();
        ApprovalRequest approvalRequest = systemAdminApprovalPortOut.findSystemAdminApprovalRequestById(approvalRequestId)
                .orElseThrow(() -> new NotFoundException("System admin approval request not found"));
        systemAdminApprovalAccessGuard.ensureNotSelfReview(currentAccount.accountId(), approvalRequest.getTargetId());

        approvalRequestPolicy.approve(
                approvalRequest,
                currentAccount.accountId(),
                Instant.now(clock),
                normalizeNote(command)
        );
        systemAdminApprovalPortOut.saveSystemAdminApprovalDecision(
                approvalRequest,
                approvalRequest.getTargetId(),
                AccountStatus.ACTIVE
        );
        SystemAdminApprovalResult result = systemAdminApprovalPortOut.findSystemAdminApprovalResultById(approvalRequestId)
                .orElseThrow(() -> new NotFoundException("System admin approval request not found"));
        sendOnboardingApprovedEmail(result);
        return result;
    }

    @Override
    @Transactional
    public SystemAdminApprovalResult rejectSystemAdminApproval(
            UUID approvalRequestId,
            ReviewInternalEmployeeApprovalCommand command
    ) {
        CurrentAccountAccess currentAccount = systemAdminApprovalAccessGuard.requireWriteAccess();
        ApprovalRequest approvalRequest = systemAdminApprovalPortOut.findSystemAdminApprovalRequestById(approvalRequestId)
                .orElseThrow(() -> new NotFoundException("System admin approval request not found"));
        systemAdminApprovalAccessGuard.ensureNotSelfReview(currentAccount.accountId(), approvalRequest.getTargetId());

        approvalRequestPolicy.reject(approvalRequest, normalizeNote(command));
        systemAdminApprovalPortOut.saveSystemAdminApprovalDecision(
                approvalRequest,
                approvalRequest.getTargetId(),
                AccountStatus.PENDING
        );
        SystemAdminApprovalResult result = systemAdminApprovalPortOut.findSystemAdminApprovalResultById(approvalRequestId)
                .orElseThrow(() -> new NotFoundException("System admin approval request not found"));
        sendOnboardingRejectedEmail(result);
        return result;
    }

    @Override
    @Transactional
    public SystemAdminApprovalResult resubmitMySystemAdminApproval() {
        CurrentAccountAccess currentAccount = systemAdminApprovalAccessGuard.requireCurrentSystemAdmin();

        UUID accountId = currentAccount.accountId();
        if (systemAdminApprovalPortOut.existsPendingSystemAdminApprovalForAccount(accountId)) {
            throw new ConflictException("A system admin approval request is already pending");
        }

        ApprovalRequest latestApprovalRequest = systemAdminApprovalPortOut.findLatestSystemAdminApprovalRequest(accountId)
                .orElse(null);
        if (latestApprovalRequest != null && ApprovalRequestStatus.APPROVED.equals(latestApprovalRequest.getStatus())) {
            throw new ConflictException("System admin approval has already been approved");
        }

        ApprovalRequest approvalRequest = buildPendingApprovalRequest(accountId, accountId);
        systemAdminApprovalPortOut.saveSystemAdminApprovalRequest(approvalRequest);
        return systemAdminApprovalPortOut.findSystemAdminApprovalResultById(approvalRequest.getApprovalRequestId())
                .orElseThrow(() -> new NotFoundException("System admin approval request not found"));
    }

    public ApprovalRequest buildPendingApprovalRequest(UUID accountId, UUID requestedBy) {
        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setApprovalRequestId(UUID.randomUUID());
        approvalRequest.setRequestType(SystemAdminApprovalAccessGuard.REQUEST_TYPE);
        approvalRequest.setTargetSchema(SystemAdminApprovalAccessGuard.TARGET_SCHEMA);
        approvalRequest.setTargetTable(SystemAdminApprovalAccessGuard.TARGET_TABLE);
        approvalRequest.setTargetId(accountId);
        approvalRequest.setStatus(ApprovalRequestStatus.PENDING);
        approvalRequest.setRequestedBy(requestedBy);
        approvalRequestPolicy.initialize(approvalRequest);
        return approvalRequest;
    }

    private SystemAdminApprovalFilterCommand normalizeFilterCommand(SystemAdminApprovalFilterCommand command) {
        if (command == null) {
            return new SystemAdminApprovalFilterCommand(null, null);
        }
        return new SystemAdminApprovalFilterCommand(
                TextValidationUtils.normalizeNullableText(command.keyword(), "keyword", 100),
                command.status()
        );
    }

    private String normalizeNote(ReviewInternalEmployeeApprovalCommand command) {
        return TextValidationUtils.normalizeNullableText(command == null ? null : command.note(), "note", 0);
    }

    private void sendOnboardingApprovedEmail(SystemAdminApprovalResult result) {
        vehicleMailService.sendOnboardingApprovedEmail(
                result.account().email(),
                result.profile().fullName(),
                "quản trị hệ thống"
        );
    }

    private void sendOnboardingRejectedEmail(SystemAdminApprovalResult result) {
        vehicleMailService.sendOnboardingRejectedEmail(
                result.account().email(),
                result.profile().fullName(),
                "quản trị hệ thống",
                result.request().note()
        );
    }
}
