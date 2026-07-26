package com.ban.vehicle_management.application.operations.approvalrequest.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.approvalrequest.authorization.InternalEmployeeApprovalAccessGuard;
import com.ban.vehicle_management.application.operations.approvalrequest.model.command.InternalEmployeeApprovalFilterCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.command.ReviewInternalEmployeeApprovalCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.InternalEmployeeApprovalCandidate;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.InternalEmployeeApprovalResult;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.InternalEmployeeApprovalPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.infrastructure.mail.VehicleMailService;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class InternalEmployeeApprovalUseCaseImplTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private InternalEmployeeApprovalAccessGuard internalEmployeeApprovalAccessGuard;

    @Mock
    private InternalEmployeeApprovalPortOut internalEmployeeApprovalPortOut;

    @Mock
    private VehicleMailService vehicleMailService;

    private InternalEmployeeApprovalUseCaseImpl internalEmployeeApprovalUseCase;

    @BeforeEach
    void setUp() {
        internalEmployeeApprovalUseCase = new InternalEmployeeApprovalUseCaseImpl(
                currentAccountPortIn,
                internalEmployeeApprovalAccessGuard,
                internalEmployeeApprovalPortOut,
                vehicleMailService,
                Clock.fixed(Instant.parse("2026-06-20T16:29:30Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void shouldApproveEmployeeApprovalRequestAndActivateEmployee() {
        UUID approvalRequestId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();

        ApprovalRequest approvalRequest = pendingApprovalRequest(approvalRequestId, employeeId, accountId);
        Employee employee = pendingEmployee(employeeId, userProfileId);
        InternalEmployeeApprovalResult expectedResult = approvalResult(
                approvalRequestId,
                "EMPLOYEE",
                "APPROVED",
                "Approved by parking manager",
                employeeId,
                EmployeeStatus.ACTIVE
        );

        when(internalEmployeeApprovalAccessGuard.requireWriteAccess()).thenReturn(currentParkingManager(accountId));
        when(internalEmployeeApprovalPortOut.findInternalEmployeeApprovalRequestById(approvalRequestId))
                .thenReturn(Optional.of(approvalRequest));
        when(internalEmployeeApprovalPortOut.findCandidateByEmployeeId(employeeId))
                .thenReturn(Optional.of(candidate(accountId, userProfileId, employeeId, "EMPLOYEE")));
        when(internalEmployeeApprovalPortOut.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));
        when(internalEmployeeApprovalPortOut.findInternalEmployeeApprovalResultById(approvalRequestId))
                .thenReturn(Optional.of(expectedResult));

        InternalEmployeeApprovalResult result = internalEmployeeApprovalUseCase.approveInternalEmployeeApproval(
                approvalRequestId,
                new ReviewInternalEmployeeApprovalCommand("Approved by parking manager")
        );

        ArgumentCaptor<ApprovalRequest> approvalCaptor = ArgumentCaptor.forClass(ApprovalRequest.class);
        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(internalEmployeeApprovalPortOut).saveInternalEmployeeApprovalDecision(
                approvalCaptor.capture(),
                employeeCaptor.capture()
        );
        assertEquals(ApprovalRequestStatus.APPROVED, approvalCaptor.getValue().getStatus());
        assertEquals(EmployeeStatus.ACTIVE, employeeCaptor.getValue().getStatus());
        assertEquals("APPROVED", result.request().approvalRequestStatus());
        verify(vehicleMailService).sendOnboardingApprovedEmail("internal.user@example.com", "Internal User", "nhân sự nội bộ");
    }

    @Test
    void shouldSetHireDateWhenApprovingEmployeeWithoutHireDate() {
        UUID approvalRequestId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();

        ApprovalRequest approvalRequest = pendingApprovalRequest(approvalRequestId, employeeId, accountId);
        Employee employee = pendingEmployee(employeeId, userProfileId);
        employee.setHiredAt(null);
        InternalEmployeeApprovalResult expectedResult = approvalResult(
                approvalRequestId,
                "EMPLOYEE",
                "APPROVED",
                "Approved by parking manager",
                employeeId,
                EmployeeStatus.ACTIVE
        );

        when(internalEmployeeApprovalAccessGuard.requireWriteAccess()).thenReturn(currentParkingManager(accountId));
        when(internalEmployeeApprovalPortOut.findInternalEmployeeApprovalRequestById(approvalRequestId))
                .thenReturn(Optional.of(approvalRequest));
        when(internalEmployeeApprovalPortOut.findCandidateByEmployeeId(employeeId))
                .thenReturn(Optional.of(candidate(accountId, userProfileId, employeeId, "EMPLOYEE")));
        when(internalEmployeeApprovalPortOut.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));
        when(internalEmployeeApprovalPortOut.findInternalEmployeeApprovalResultById(approvalRequestId))
                .thenReturn(Optional.of(expectedResult));

        internalEmployeeApprovalUseCase.approveInternalEmployeeApproval(
                approvalRequestId,
                new ReviewInternalEmployeeApprovalCommand("Approved by parking manager")
        );

        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(internalEmployeeApprovalPortOut).saveInternalEmployeeApprovalDecision(any(), employeeCaptor.capture());
        assertEquals(EmployeeStatus.ACTIVE, employeeCaptor.getValue().getStatus());
        assertEquals(LocalDate.of(2026, 6, 20), employeeCaptor.getValue().getHiredAt());
    }

    @Test
    void shouldRejectApprovalWhenParkingManagerTriesToReviewParkingManagerRequest() {
        UUID approvalRequestId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();

        CurrentAccountAccess currentAccount = currentParkingManager(accountId);
        when(internalEmployeeApprovalAccessGuard.requireWriteAccess()).thenReturn(currentAccount);
        when(internalEmployeeApprovalPortOut.findInternalEmployeeApprovalRequestById(approvalRequestId))
                .thenReturn(Optional.of(pendingApprovalRequest(approvalRequestId, employeeId, accountId)));
        when(internalEmployeeApprovalPortOut.findCandidateByEmployeeId(employeeId))
                .thenReturn(Optional.of(candidate(accountId, userProfileId, employeeId, "PARKING_MANAGER")));
        doThrow(new AccessDeniedException("Access is denied"))
                .when(internalEmployeeApprovalAccessGuard)
                .ensureCanReviewTarget(currentAccount, "PARKING_MANAGER");

        assertThrows(
                AccessDeniedException.class,
                () -> internalEmployeeApprovalUseCase.approveInternalEmployeeApproval(
                        approvalRequestId,
                        new ReviewInternalEmployeeApprovalCommand("Not allowed")
                )
        );
    }

    @Test
    void shouldRejectEmployeeApprovalRequestAndKeepEmployeeInactive() {
        UUID approvalRequestId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();

        ApprovalRequest approvalRequest = pendingApprovalRequest(approvalRequestId, employeeId, accountId);
        Employee employee = pendingEmployee(employeeId, userProfileId);
        InternalEmployeeApprovalResult expectedResult = approvalResult(
                approvalRequestId,
                "EMPLOYEE",
                "REJECTED",
                "Missing documents",
                employeeId,
                EmployeeStatus.INACTIVE
        );

        when(internalEmployeeApprovalAccessGuard.requireWriteAccess()).thenReturn(currentParkingManager(accountId));
        when(internalEmployeeApprovalPortOut.findInternalEmployeeApprovalRequestById(approvalRequestId))
                .thenReturn(Optional.of(approvalRequest));
        when(internalEmployeeApprovalPortOut.findCandidateByEmployeeId(employeeId))
                .thenReturn(Optional.of(candidate(accountId, userProfileId, employeeId, "EMPLOYEE")));
        when(internalEmployeeApprovalPortOut.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));
        when(internalEmployeeApprovalPortOut.findInternalEmployeeApprovalResultById(approvalRequestId))
                .thenReturn(Optional.of(expectedResult));

        InternalEmployeeApprovalResult result = internalEmployeeApprovalUseCase.rejectInternalEmployeeApproval(
                approvalRequestId,
                new ReviewInternalEmployeeApprovalCommand("Missing documents")
        );

        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(internalEmployeeApprovalPortOut).saveInternalEmployeeApprovalDecision(any(), employeeCaptor.capture());
        assertEquals(EmployeeStatus.INACTIVE, employeeCaptor.getValue().getStatus());
        assertEquals("REJECTED", result.request().approvalRequestStatus());
        verify(vehicleMailService).sendOnboardingRejectedEmail(
                "internal.user@example.com",
                "Internal User",
                "nhân sự nội bộ",
                "Missing documents"
        );
    }

    @Test
    void shouldRejectResubmitWhenPendingRequestAlreadyExists() {
        UUID accountId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentEmployee(accountId));
        when(internalEmployeeApprovalAccessGuard.requireProvisionableRole("EMPLOYEE"))
                .thenReturn(com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode.EMPLOYEE);
        when(internalEmployeeApprovalPortOut.findCandidateByAccountId(accountId))
                .thenReturn(Optional.of(candidate(accountId, userProfileId, employeeId, "EMPLOYEE")));
        when(internalEmployeeApprovalPortOut.existsPendingInternalEmployeeApprovalForEmployee(employeeId)).thenReturn(true);

        assertThrows(ConflictException.class, () -> internalEmployeeApprovalUseCase.resubmitMyInternalEmployeeApproval());
    }

    @Test
    void shouldFilterParkingManagerListToEmployeeTargetsOnly() {
        UUID accountId = UUID.randomUUID();
        CurrentAccountAccess currentAccount = currentParkingManager(accountId);
        when(internalEmployeeApprovalAccessGuard.requireReadAccess()).thenReturn(currentAccount);
        when(internalEmployeeApprovalAccessGuard.canAccessTargetRole(currentAccount, "EMPLOYEE")).thenReturn(true);
        when(internalEmployeeApprovalAccessGuard.canAccessTargetRole(currentAccount, "PARKING_MANAGER")).thenReturn(false);
        when(internalEmployeeApprovalPortOut.findInternalEmployeeApprovalRequests(any())).thenReturn(List.of(
                approvalResult(UUID.randomUUID(), "EMPLOYEE", "PENDING", null, UUID.randomUUID(), EmployeeStatus.INACTIVE),
                approvalResult(UUID.randomUUID(), "PARKING_MANAGER", "PENDING", null, UUID.randomUUID(), EmployeeStatus.INACTIVE)
        ));

        List<InternalEmployeeApprovalResult> results = internalEmployeeApprovalUseCase.getInternalEmployeeApprovals(
                new InternalEmployeeApprovalFilterCommand(null, null, null)
        );

        assertEquals(1, results.size());
        assertEquals("EMPLOYEE", results.get(0).account().roleCode());
    }

    @Test
    void shouldAllowSystemAdminToApproveParkingManagerRequest() {
        UUID approvalRequestId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();

        ApprovalRequest approvalRequest = pendingApprovalRequest(approvalRequestId, employeeId, accountId);
        Employee employee = pendingEmployee(employeeId, userProfileId);
        InternalEmployeeApprovalResult expectedResult = approvalResult(
                approvalRequestId,
                "PARKING_MANAGER",
                "APPROVED",
                "Approved by system admin",
                employeeId,
                EmployeeStatus.ACTIVE
        );

        when(internalEmployeeApprovalAccessGuard.requireWriteAccess()).thenReturn(currentSystemAdmin(accountId));
        when(internalEmployeeApprovalPortOut.findInternalEmployeeApprovalRequestById(approvalRequestId))
                .thenReturn(Optional.of(approvalRequest));
        when(internalEmployeeApprovalPortOut.findCandidateByEmployeeId(employeeId))
                .thenReturn(Optional.of(candidate(accountId, userProfileId, employeeId, "PARKING_MANAGER")));
        when(internalEmployeeApprovalPortOut.findEmployeeById(employeeId)).thenReturn(Optional.of(employee));
        when(internalEmployeeApprovalPortOut.findInternalEmployeeApprovalResultById(approvalRequestId))
                .thenReturn(Optional.of(expectedResult));

        InternalEmployeeApprovalResult result = internalEmployeeApprovalUseCase.approveInternalEmployeeApproval(
                approvalRequestId,
                new ReviewInternalEmployeeApprovalCommand("Approved by system admin")
        );

        assertEquals("APPROVED", result.request().approvalRequestStatus());
        verify(internalEmployeeApprovalPortOut).saveInternalEmployeeApprovalDecision(any(), any());
    }

    @Test
    void shouldRejectWhenSystemAdminTriesToReviewEmployeeRequest() {
        UUID approvalRequestId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();

        CurrentAccountAccess currentAccount = currentSystemAdmin(accountId);
        when(internalEmployeeApprovalAccessGuard.requireWriteAccess()).thenReturn(currentAccount);
        when(internalEmployeeApprovalPortOut.findInternalEmployeeApprovalRequestById(approvalRequestId))
                .thenReturn(Optional.of(pendingApprovalRequest(approvalRequestId, employeeId, accountId)));
        when(internalEmployeeApprovalPortOut.findCandidateByEmployeeId(employeeId))
                .thenReturn(Optional.of(candidate(accountId, userProfileId, employeeId, "EMPLOYEE")));
        doThrow(new AccessDeniedException("Access is denied"))
                .when(internalEmployeeApprovalAccessGuard)
                .ensureCanReviewTarget(eq(currentAccount), eq("EMPLOYEE"));

        assertThrows(
                AccessDeniedException.class,
                () -> internalEmployeeApprovalUseCase.approveInternalEmployeeApproval(
                        approvalRequestId,
                        new ReviewInternalEmployeeApprovalCommand("Not allowed")
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
                Set.of("ACCOUNT_UPDATE_ALL", "ACCOUNT_READ_ALL")
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
                EmployeeStatus.ACTIVE,
                Set.of("EMPLOYEE_UPDATE_ALL", "EMPLOYEE_READ_ALL")
        );
    }

    private CurrentAccountAccess currentEmployee(UUID accountId) {
        return new CurrentAccountAccess(
                accountId,
                "sub-employee",
                "parking.employee",
                "parking.employee@example.com",
                UUID.randomUUID(),
                "EMPLOYEE",
                AccountStatus.ACTIVE,
                EmployeeStatus.INACTIVE,
                Set.of()
        );
    }

    private ApprovalRequest pendingApprovalRequest(UUID approvalRequestId, UUID employeeId, UUID requestedBy) {
        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setApprovalRequestId(approvalRequestId);
        approvalRequest.setRequestType(InternalEmployeeApprovalAccessGuard.REQUEST_TYPE);
        approvalRequest.setTargetSchema(InternalEmployeeApprovalAccessGuard.TARGET_SCHEMA);
        approvalRequest.setTargetTable(InternalEmployeeApprovalAccessGuard.TARGET_TABLE);
        approvalRequest.setTargetId(employeeId);
        approvalRequest.setStatus(ApprovalRequestStatus.PENDING);
        approvalRequest.setRequestedBy(requestedBy);
        return approvalRequest;
    }

    private Employee pendingEmployee(UUID employeeId, UUID userProfileId) {
        Employee employee = new Employee();
        employee.setEmployeeId(employeeId);
        employee.setUserProfileId(userProfileId);
        employee.setEmployeeCode("EMP-001");
        employee.setJobTitle("Parking Staff");
        employee.setHiredAt(LocalDate.of(2025, 1, 1));
        employee.setStatus(EmployeeStatus.INACTIVE);
        return employee;
    }

    private InternalEmployeeApprovalCandidate candidate(
            UUID accountId,
            UUID userProfileId,
            UUID employeeId,
            String roleCode
    ) {
        return new InternalEmployeeApprovalCandidate(
                accountId,
                userProfileId,
                employeeId,
                roleCode,
                AccountStatus.ACTIVE,
                EmployeeStatus.INACTIVE
        );
    }

    private InternalEmployeeApprovalResult approvalResult(
            UUID approvalRequestId,
            String roleCode,
            String requestStatus,
            String note,
            UUID employeeId,
            EmployeeStatus employeeStatus
    ) {
        return new InternalEmployeeApprovalResult(
                new InternalEmployeeApprovalResult.RequestInfoResult(
                        approvalRequestId,
                        InternalEmployeeApprovalAccessGuard.REQUEST_TYPE,
                        requestStatus,
                        note,
                        UUID.randomUUID(),
                        "APPROVED".equals(requestStatus) ? UUID.randomUUID() : null,
                        "APPROVED".equals(requestStatus) ? Instant.now() : null,
                        Instant.now(),
                        Instant.now()
                ),
                new InternalEmployeeApprovalResult.AccountInfoResult(
                        UUID.randomUUID(),
                        "internal.user",
                        "internal.user@example.com",
                        roleCode,
                        "ACTIVE"
                ),
                new InternalEmployeeApprovalResult.ProfileInfoResult(
                        UUID.randomUUID(),
                        "Internal User",
                        "0909000000"
                ),
                new InternalEmployeeApprovalResult.EmployeeInfoResult(
                        employeeId,
                        "EMP-001",
                        "Parking Staff",
                        LocalDate.of(2025, 1, 1),
                        employeeStatus.name()
                )
        );
    }
}
