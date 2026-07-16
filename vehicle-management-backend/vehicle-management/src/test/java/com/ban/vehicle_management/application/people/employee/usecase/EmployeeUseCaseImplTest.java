package com.ban.vehicle_management.application.people.employee.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.InternalEmployeeApprovalCandidate;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.InternalEmployeeApprovalPortOut;
import com.ban.vehicle_management.application.people.employee.authorization.EmployeeAccessGuard;
import com.ban.vehicle_management.application.people.employee.port.out.EmployeePortOut;
import com.ban.vehicle_management.application.people.userprofile.port.in.UserProfileAvatarPortIn;
import com.ban.vehicle_management.application.people.userprofile.port.out.UserProfilePortOut;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class EmployeeUseCaseImplTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private EmployeeAccessGuard employeeAccessGuard;

    @Mock
    private EmployeePortOut employeePortOut;

    @Mock
    private InternalEmployeeApprovalPortOut internalEmployeeApprovalPortOut;

    @Mock
    private UserProfileAvatarPortIn userProfileAvatarPortIn;

    @Mock
    private UserProfilePortOut userProfilePortOut;

    @InjectMocks
    private EmployeeUseCaseImpl employeeUseCase;

    @Test
    void shouldUpdateEmployeeMetadata() {
        UUID employeeId = UUID.randomUUID();
        Employee existingEmployee = new Employee();
        existingEmployee.setEmployeeId(employeeId);
        existingEmployee.setUserProfileId(UUID.randomUUID());
        existingEmployee.setEmployeeCode("EMP-001");
        existingEmployee.setJobTitle("Cashier");
        existingEmployee.setStatus(EmployeeStatus.INACTIVE);

        Employee requestEmployee = new Employee();
        requestEmployee.setEmployeeCode("EMP-002");
        requestEmployee.setJobTitle("Supervisor");
        requestEmployee.setHiredAt(LocalDate.of(2025, 1, 1));

        org.mockito.Mockito.doNothing().when(currentAccountPortIn).requirePermission("EMPLOYEE_UPDATE_ALL");
        org.mockito.Mockito.doNothing().when(currentAccountPortIn).requirePermission("EMPLOYEE_READ_ALL");
        when(employeePortOut.findById(employeeId)).thenReturn(Optional.of(existingEmployee));
        when(employeePortOut.existsByEmployeeCodeAndEmployeeIdNot("EMP-002", employeeId)).thenReturn(false);
        when(employeePortOut.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Employee updatedEmployee = employeeUseCase.updateEmployee(employeeId, requestEmployee);

        assertEquals("EMP-002", updatedEmployee.getEmployeeCode());
        assertEquals("Supervisor", updatedEmployee.getJobTitle());
        assertEquals(LocalDate.of(2025, 1, 1), updatedEmployee.getHiredAt());
        assertEquals(EmployeeStatus.INACTIVE, updatedEmployee.getStatus());
    }

    @Test
    void shouldRejectDirectStatusChangeDuringEmployeeUpdate() {
        UUID employeeId = UUID.randomUUID();
        Employee existingEmployee = validEmployee(employeeId, EmployeeStatus.INACTIVE);

        Employee requestEmployee = new Employee();
        requestEmployee.setStatus(EmployeeStatus.ACTIVE);

        org.mockito.Mockito.doNothing().when(currentAccountPortIn).requirePermission("EMPLOYEE_UPDATE_ALL");
        org.mockito.Mockito.doNothing().when(currentAccountPortIn).requirePermission("EMPLOYEE_READ_ALL");
        when(employeePortOut.findById(employeeId)).thenReturn(Optional.of(existingEmployee));

        assertThrows(BadRequestException.class, () -> employeeUseCase.updateEmployee(employeeId, requestEmployee));
    }

    @Test
    void shouldReturnFilteredEmployees() {
        UUID firstProfileId = UUID.randomUUID();
        UUID secondProfileId = UUID.randomUUID();
        Employee firstEmployee = new Employee();
        firstEmployee.setUserProfile(profile(firstProfileId, "av/first.jpg"));
        Employee secondEmployee = new Employee();
        secondEmployee.setUserProfile(profile(secondProfileId, "av/second.jpg"));

        org.mockito.Mockito.doNothing().when(currentAccountPortIn).requirePermission("EMPLOYEE_READ_ALL");
        when(employeePortOut.findAll(EmployeeStatus.ACTIVE, "nguyen"))
                .thenReturn(List.of(firstEmployee, secondEmployee));
        when(employeeAccessGuard.filterReadableEmployees(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userProfileAvatarPortIn.withResolvedAvatarUrls(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Employee> employees = employeeUseCase.getEmployees(EmployeeStatus.ACTIVE, "nguyen");

        assertEquals(2, employees.size());
        verify(employeePortOut).findAll(EmployeeStatus.ACTIVE, "nguyen");
        verify(userProfileAvatarPortIn).withResolvedAvatarUrls(any());
    }

    @Test
    void shouldUploadEmployeeAvatarByResolvingEmployeeProfile() {
        UUID employeeId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        UUID uploaderAccountId = UUID.randomUUID();
        Employee employee = validEmployee(employeeId, EmployeeStatus.ACTIVE);
        employee.setUserProfileId(userProfileId);
        UserProfile updatedProfile = profile(userProfileId, "https://cdn.example.com/avatar.png");
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{1, 2, 3});

        org.mockito.Mockito.doNothing().when(currentAccountPortIn).requirePermission("EMPLOYEE_UPDATE_ALL");
        org.mockito.Mockito.doNothing().when(currentAccountPortIn).requirePermission("EMPLOYEE_READ_ALL");
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(uploaderAccountId);
        when(employeePortOut.findById(employeeId)).thenReturn(Optional.of(employee));
        when(userProfileAvatarPortIn.uploadAvatar(userProfileId, file, uploaderAccountId)).thenReturn(updatedProfile);
        when(userProfileAvatarPortIn.withResolvedAvatarUrl(updatedProfile)).thenReturn(updatedProfile);

        Employee result = employeeUseCase.uploadEmployeeAvatar(employeeId, file);

        verify(employeeAccessGuard).ensureCanManage(employee);
        verify(userProfileAvatarPortIn).uploadAvatar(userProfileId, file, uploaderAccountId);
        assertEquals("https://cdn.example.com/avatar.png", result.getUserProfile().getAvatarUrl());
    }

    @Test
    void shouldDeleteEmployeeAvatarByResolvingEmployeeProfile() {
        UUID employeeId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        Employee employee = validEmployee(employeeId, EmployeeStatus.ACTIVE);
        employee.setUserProfileId(userProfileId);
        UserProfile updatedProfile = profile(userProfileId, null);

        org.mockito.Mockito.doNothing().when(currentAccountPortIn).requirePermission("EMPLOYEE_UPDATE_ALL");
        org.mockito.Mockito.doNothing().when(currentAccountPortIn).requirePermission("EMPLOYEE_READ_ALL");
        when(employeePortOut.findById(employeeId)).thenReturn(Optional.of(employee));
        when(userProfileAvatarPortIn.deleteAvatar(userProfileId)).thenReturn(updatedProfile);
        when(userProfileAvatarPortIn.withResolvedAvatarUrl(updatedProfile)).thenReturn(updatedProfile);

        Employee result = employeeUseCase.deleteEmployeeAvatar(employeeId);

        verify(employeeAccessGuard).ensureCanManage(employee);
        verify(userProfileAvatarPortIn).deleteAvatar(userProfileId);
        assertEquals(null, result.getUserProfile().getAvatarUrl());
    }

    @Test
    void shouldSoftDeleteEmployeeBySettingInactiveStatus() {
        UUID employeeId = UUID.randomUUID();
        Employee existingEmployee = new Employee();
        existingEmployee.setEmployeeId(employeeId);
        existingEmployee.setUserProfileId(UUID.randomUUID());
        existingEmployee.setEmployeeCode("EMP-001");
        existingEmployee.setStatus(EmployeeStatus.ACTIVE);

        org.mockito.Mockito.doNothing().when(currentAccountPortIn).requirePermission("EMPLOYEE_DELETE_ALL");
        org.mockito.Mockito.doNothing().when(currentAccountPortIn).requirePermission("EMPLOYEE_READ_ALL");
        when(employeePortOut.findById(employeeId)).thenReturn(Optional.of(existingEmployee));
        when(employeePortOut.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        employeeUseCase.deleteEmployee(employeeId);

        assertEquals(EmployeeStatus.INACTIVE, existingEmployee.getStatus());
        verify(employeePortOut).save(existingEmployee);
    }

    @Test
    void shouldActivateEmployee() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = validEmployee(employeeId, EmployeeStatus.INACTIVE);
        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setApprovalRequestId(UUID.randomUUID());
        approvalRequest.setStatus(ApprovalRequestStatus.APPROVED);

        org.mockito.Mockito.doNothing().when(currentAccountPortIn).requirePermission("EMPLOYEE_UPDATE_ALL");
        org.mockito.Mockito.doNothing().when(currentAccountPortIn).requirePermission("EMPLOYEE_READ_ALL");
        when(employeePortOut.findById(employeeId)).thenReturn(Optional.of(employee));
        when(internalEmployeeApprovalPortOut.findCandidateByEmployeeId(employeeId)).thenReturn(Optional.of(
                new InternalEmployeeApprovalCandidate(
                        UUID.randomUUID(),
                        employee.getUserProfileId(),
                        employeeId,
                        "EMPLOYEE",
                        AccountStatus.ACTIVE,
                        EmployeeStatus.INACTIVE
                )
        ));
        when(internalEmployeeApprovalPortOut.findLatestInternalEmployeeApprovalRequest(employeeId))
                .thenReturn(Optional.of(approvalRequest));
        when(employeePortOut.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Employee updatedEmployee = employeeUseCase.activateEmployee(employeeId);

        assertEquals(EmployeeStatus.ACTIVE, updatedEmployee.getStatus());
    }

    @Test
    void shouldRejectActivateWhenInternalOnboardingApprovalIsStillPending() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = validEmployee(employeeId, EmployeeStatus.INACTIVE);
        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setApprovalRequestId(UUID.randomUUID());
        approvalRequest.setStatus(ApprovalRequestStatus.PENDING);

        org.mockito.Mockito.doNothing().when(currentAccountPortIn).requirePermission("EMPLOYEE_UPDATE_ALL");
        org.mockito.Mockito.doNothing().when(currentAccountPortIn).requirePermission("EMPLOYEE_READ_ALL");
        when(employeePortOut.findById(employeeId)).thenReturn(Optional.of(employee));
        when(internalEmployeeApprovalPortOut.findCandidateByEmployeeId(employeeId)).thenReturn(Optional.of(
                new InternalEmployeeApprovalCandidate(
                        UUID.randomUUID(),
                        employee.getUserProfileId(),
                        employeeId,
                        "EMPLOYEE",
                        AccountStatus.ACTIVE,
                        EmployeeStatus.INACTIVE
                )
        ));
        when(internalEmployeeApprovalPortOut.findLatestInternalEmployeeApprovalRequest(employeeId))
                .thenReturn(Optional.of(approvalRequest));

        assertThrows(ConflictException.class, () -> employeeUseCase.activateEmployee(employeeId));
    }

    @Test
    void shouldSuspendEmployee() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = validEmployee(employeeId, EmployeeStatus.ACTIVE);

        org.mockito.Mockito.doNothing().when(currentAccountPortIn).requirePermission("EMPLOYEE_UPDATE_ALL");
        org.mockito.Mockito.doNothing().when(currentAccountPortIn).requirePermission("EMPLOYEE_READ_ALL");
        when(employeePortOut.findById(employeeId)).thenReturn(Optional.of(employee));
        when(employeePortOut.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Employee updatedEmployee = employeeUseCase.suspendEmployee(employeeId);

        assertEquals(EmployeeStatus.SUSPENDED, updatedEmployee.getStatus());
    }

    @Test
    void shouldThrowWhenEmployeeDoesNotExist() {
        UUID employeeId = UUID.randomUUID();
        org.mockito.Mockito.doNothing().when(currentAccountPortIn).requirePermission("EMPLOYEE_READ_ALL");
        when(employeePortOut.findById(employeeId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> employeeUseCase.getEmployeeById(employeeId));
    }

    private Employee validEmployee(UUID employeeId, EmployeeStatus status) {
        Employee employee = new Employee();
        employee.setEmployeeId(employeeId);
        employee.setUserProfileId(UUID.randomUUID());
        employee.setEmployeeCode("EMP-001");
        employee.setJobTitle("Cashier");
        employee.setStatus(status);
        return employee;
    }

    private UserProfile profile(UUID userProfileId, String avatarUrl) {
        UserProfile userProfile = new UserProfile();
        userProfile.setUserProfileId(userProfileId);
        userProfile.setFullName("Nguyen Bao An");
        userProfile.setAvatarUrl(avatarUrl);
        return userProfile;
    }
}
