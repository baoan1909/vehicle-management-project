package com.ban.vehicle_management.application.operations.approvalrequest.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.approvalrequest.authorization.SystemAdminApprovalAccessGuard;
import com.ban.vehicle_management.application.operations.approvalrequest.model.command.ReviewInternalEmployeeApprovalCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.command.SystemAdminApprovalFilterCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.SystemAdminApprovalResult;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.SystemAdminApprovalPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.infrastructure.mail.VehicleMailService;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class SystemAdminApprovalUseCaseImplTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private SystemAdminApprovalAccessGuard systemAdminApprovalAccessGuard;

    @Mock
    private SystemAdminApprovalPortOut systemAdminApprovalPortOut;

    @Mock
    private VehicleMailService vehicleMailService;

    @InjectMocks
    private SystemAdminApprovalUseCaseImpl systemAdminApprovalUseCase;

    @Test
    void shouldReturnSystemAdminApprovalsWhenCurrentUserIsSystemAdmin() {
        UUID accountId = UUID.randomUUID();
        SystemAdminApprovalResult expectedResult = approvalResult(UUID.randomUUID(), accountId, "PENDING");

        when(systemAdminApprovalAccessGuard.requireReadAccess()).thenReturn(currentSystemAdmin(accountId));
        when(systemAdminApprovalPortOut.findSystemAdminApprovalRequests(any()))
                .thenReturn(List.of(expectedResult));

        List<SystemAdminApprovalResult> results = systemAdminApprovalUseCase.getSystemAdminApprovals(
                new SystemAdminApprovalFilterCommand("admin", ApprovalRequestStatus.PENDING)
        );

        assertEquals(1, results.size());
        assertEquals("SYSTEM_ADMIN", results.get(0).account().roleCode());
    }

    @Test
    void shouldApproveSystemAdminApprovalAndActivateAccount() {
        UUID approverId = UUID.randomUUID();
        UUID targetAccountId = UUID.randomUUID();
        UUID approvalRequestId = UUID.randomUUID();
        ApprovalRequest approvalRequest = pendingApprovalRequest(approvalRequestId, targetAccountId, targetAccountId);
        SystemAdminApprovalResult expectedResult = approvalResult(approvalRequestId, targetAccountId, "APPROVED");

        when(systemAdminApprovalAccessGuard.requireWriteAccess()).thenReturn(currentSystemAdmin(approverId));
        when(systemAdminApprovalPortOut.findSystemAdminApprovalRequestById(approvalRequestId))
                .thenReturn(Optional.of(approvalRequest));
        when(systemAdminApprovalPortOut.findSystemAdminApprovalResultById(approvalRequestId))
                .thenReturn(Optional.of(expectedResult));

        SystemAdminApprovalResult result = systemAdminApprovalUseCase.approveSystemAdminApproval(
                approvalRequestId,
                new ReviewInternalEmployeeApprovalCommand("Approved")
        );

        ArgumentCaptor<ApprovalRequest> approvalCaptor = ArgumentCaptor.forClass(ApprovalRequest.class);
        ArgumentCaptor<AccountStatus> accountStatusCaptor = ArgumentCaptor.forClass(AccountStatus.class);
        verify(systemAdminApprovalPortOut).saveSystemAdminApprovalDecision(
                approvalCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(targetAccountId),
                accountStatusCaptor.capture()
        );
        assertEquals(ApprovalRequestStatus.APPROVED, approvalCaptor.getValue().getStatus());
        assertEquals(AccountStatus.ACTIVE, accountStatusCaptor.getValue());
        assertEquals("APPROVED", result.request().approvalRequestStatus());
        verify(vehicleMailService).sendOnboardingApprovedEmail(
                "system.admin.pending@example.com",
                "Pending System Admin",
                "quản trị hệ thống"
        );
    }

    @Test
    void shouldRejectSystemAdminApprovalAndKeepAccountPending() {
        UUID approverId = UUID.randomUUID();
        UUID targetAccountId = UUID.randomUUID();
        UUID approvalRequestId = UUID.randomUUID();
        ApprovalRequest approvalRequest = pendingApprovalRequest(approvalRequestId, targetAccountId, targetAccountId);
        SystemAdminApprovalResult expectedResult = approvalResult(approvalRequestId, targetAccountId, "REJECTED");

        when(systemAdminApprovalAccessGuard.requireWriteAccess()).thenReturn(currentSystemAdmin(approverId));
        when(systemAdminApprovalPortOut.findSystemAdminApprovalRequestById(approvalRequestId))
                .thenReturn(Optional.of(approvalRequest));
        when(systemAdminApprovalPortOut.findSystemAdminApprovalResultById(approvalRequestId))
                .thenReturn(Optional.of(expectedResult));

        SystemAdminApprovalResult result = systemAdminApprovalUseCase.rejectSystemAdminApproval(
                approvalRequestId,
                new ReviewInternalEmployeeApprovalCommand("Missing governance approval")
        );

        ArgumentCaptor<AccountStatus> accountStatusCaptor = ArgumentCaptor.forClass(AccountStatus.class);
        verify(systemAdminApprovalPortOut).saveSystemAdminApprovalDecision(
                any(ApprovalRequest.class),
                org.mockito.ArgumentMatchers.eq(targetAccountId),
                accountStatusCaptor.capture()
        );
        assertEquals(AccountStatus.PENDING, accountStatusCaptor.getValue());
        assertEquals("REJECTED", result.request().approvalRequestStatus());
        verify(vehicleMailService).sendOnboardingRejectedEmail(
                "system.admin.pending@example.com",
                "Pending System Admin",
                "quản trị hệ thống",
                null
        );
    }

    @Test
    void shouldRejectWhenSystemAdminTriesToApproveOwnRequest() {
        UUID accountId = UUID.randomUUID();
        UUID approvalRequestId = UUID.randomUUID();

        when(systemAdminApprovalAccessGuard.requireWriteAccess()).thenReturn(currentSystemAdmin(accountId));
        when(systemAdminApprovalPortOut.findSystemAdminApprovalRequestById(approvalRequestId))
                .thenReturn(Optional.of(pendingApprovalRequest(approvalRequestId, accountId, accountId)));
        doThrow(new ConflictException("System admin approval request cannot be self-reviewed"))
                .when(systemAdminApprovalAccessGuard)
                .ensureNotSelfReview(accountId, accountId);

        assertThrows(
                ConflictException.class,
                () -> systemAdminApprovalUseCase.approveSystemAdminApproval(
                        approvalRequestId,
                        new ReviewInternalEmployeeApprovalCommand("Self review")
                )
        );
    }

    @Test
    void shouldRejectListWhenCurrentUserIsNotSystemAdmin() {
        UUID accountId = UUID.randomUUID();

        doThrow(new AccessDeniedException("Access is denied"))
                .when(systemAdminApprovalAccessGuard)
                .requireReadAccess();

        assertThrows(
                AccessDeniedException.class,
                () -> systemAdminApprovalUseCase.getSystemAdminApprovals(new SystemAdminApprovalFilterCommand(null, null))
        );
    }

    @Test
    void shouldRejectResubmitWhenPendingRequestAlreadyExists() {
        UUID accountId = UUID.randomUUID();

        when(systemAdminApprovalAccessGuard.requireCurrentSystemAdmin()).thenReturn(currentSystemAdmin(accountId));
        when(systemAdminApprovalPortOut.existsPendingSystemAdminApprovalForAccount(accountId)).thenReturn(true);

        assertThrows(ConflictException.class, () -> systemAdminApprovalUseCase.resubmitMySystemAdminApproval());
    }

    private ApprovalRequest pendingApprovalRequest(UUID approvalRequestId, UUID targetAccountId, UUID requestedBy) {
        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setApprovalRequestId(approvalRequestId);
        approvalRequest.setRequestType(SystemAdminApprovalAccessGuard.REQUEST_TYPE);
        approvalRequest.setTargetSchema(SystemAdminApprovalAccessGuard.TARGET_SCHEMA);
        approvalRequest.setTargetTable(SystemAdminApprovalAccessGuard.TARGET_TABLE);
        approvalRequest.setTargetId(targetAccountId);
        approvalRequest.setRequestedBy(requestedBy);
        approvalRequest.setStatus(ApprovalRequestStatus.PENDING);
        return approvalRequest;
    }

    private SystemAdminApprovalResult approvalResult(UUID approvalRequestId, UUID accountId, String status) {
        return new SystemAdminApprovalResult(
                new SystemAdminApprovalResult.RequestInfoResult(
                        approvalRequestId,
                        SystemAdminApprovalAccessGuard.REQUEST_TYPE,
                        status,
                        null,
                        accountId,
                        "APPROVED".equals(status) ? UUID.randomUUID() : null,
                        "APPROVED".equals(status) ? Instant.now() : null,
                        Instant.now(),
                        Instant.now()
                ),
                new SystemAdminApprovalResult.AccountInfoResult(
                        accountId,
                        "system.admin.pending",
                        "system.admin.pending@example.com",
                        "SYSTEM_ADMIN",
                        "APPROVED".equals(status) ? "ACTIVE" : "PENDING"
                ),
                new SystemAdminApprovalResult.ProfileInfoResult(
                        UUID.randomUUID(),
                        "Pending System Admin",
                        "0901000001"
                )
        );
    }

    private CurrentAccountAccess currentSystemAdmin(UUID accountId) {
        return new CurrentAccountAccess(
                accountId,
                "sub-system-admin",
                "system.admin",
                "system.admin@example.com",
                UUID.randomUUID(),
                "SYSTEM_ADMIN",
                AccountStatus.ACTIVE,
                null,
                Set.of("ACCOUNT_READ_ALL", "ACCOUNT_UPDATE_ALL")
        );
    }

    private CurrentAccountAccess currentParkingManager(UUID accountId) {
        return new CurrentAccountAccess(
                accountId,
                "sub-manager",
                "parking.manager",
                "parking.manager@example.com",
                UUID.randomUUID(),
                "PARKING_MANAGER",
                AccountStatus.ACTIVE,
                null,
                Set.of("ACCOUNT_READ_ALL", "ACCOUNT_UPDATE_ALL")
        );
    }
}
