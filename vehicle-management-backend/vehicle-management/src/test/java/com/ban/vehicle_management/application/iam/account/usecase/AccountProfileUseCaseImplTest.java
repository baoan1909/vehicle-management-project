package com.ban.vehicle_management.application.iam.account.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.mapper.AccountProfileResultMapper;
import com.ban.vehicle_management.application.iam.account.model.command.CompleteAccountProfileCommand;
import com.ban.vehicle_management.application.iam.account.model.result.AccountProfileStatusResult;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountProfilePortOut;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.CustomerOnboardingApprovalPortOut;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.InternalEmployeeApprovalPortOut;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.SystemAdminApprovalPortOut;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.domain.iam.account.model.AccountProfileState;
import com.ban.vehicle_management.domain.iam.account.policy.AccountProfilePolicy;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerType;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountProfileUseCaseImplTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private AccountProfilePortOut accountProfilePortOut;

    @Mock
    private CustomerOnboardingApprovalPortOut customerOnboardingApprovalPortOut;

    @Mock
    private InternalEmployeeApprovalPortOut internalEmployeeApprovalPortOut;

    @Mock
    private SystemAdminApprovalPortOut systemAdminApprovalPortOut;

    @Mock
    private AccountProfileResultMapper accountProfileResultMapper;

    @Mock
    private AccountProfilePolicy accountProfilePolicy;

    @InjectMocks
    private AccountProfileUseCaseImpl accountProfileUseCase;

    @Test
    void shouldReturnDetailedOnboardingStatusWhenCustomerProfileExists() {
        UUID accountId = UUID.fromString("c6e12a53-e72b-441a-8a1e-bb84b49e0ca4");
        UUID userProfileId = UUID.fromString("ec761405-c091-4a65-b1dd-c8fb23f0d6bd");
        UUID customerId = UUID.fromString("1f53b3c1-1ca4-4898-b35f-80ddf8745ae3");
        AccountProfileState state = new AccountProfileState(
                accountId,
                "baoan3236",
                "baoan3236@gmail.com",
                "23d493f8-e9f8-4843-917c-9e6c431bfeea",
                AdminProvisionableAccountRoleCode.CUSTOMER.name(),
                userProfileId,
                "Nguyen Bao An",
                LocalDate.of(2003, 9, 19),
                "MALE",
                "+84901234567",
                "Ho Chi Minh City",
                "079203001234",
                "https://cdn.example.com/avatars/bao-an.jpg",
                UserProfileStatus.ACTIVE,
                null,
                null,
                null,
                null,
                null,
                customerId,
                "CUS-ABC123",
                CustomerType.REGISTERED,
                CustomerStatus.INACTIVE,
                CustomerApprovalStatus.PENDING,
                AccountStatus.ACTIVE
        );

        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(accountProfilePortOut.findProfileStateByAccountId(accountId)).thenReturn(Optional.of(state));
        when(accountProfileResultMapper.toStatusResult(state, false)).thenReturn(new AccountProfileStatusResult(
                false,
                new AccountProfileStatusResult.AccountInfoResult(
                        accountId,
                        "ACTIVE",
                        "baoan3236",
                        "baoan3236@gmail.com",
                        "23d493f8-e9f8-4843-917c-9e6c431bfeea"
                ),
                new AccountProfileStatusResult.ProfileInfoResult(
                        userProfileId,
                        "Nguyen Bao An",
                        LocalDate.of(2003, 9, 19),
                        "MALE",
                        "+84901234567",
                        "Ho Chi Minh City",
                        "079203001234",
                        "https://cdn.example.com/avatars/bao-an.jpg",
                        "ACTIVE"
                ),
                null,
                new AccountProfileStatusResult.CustomerInfoResult(
                        customerId,
                        "CUS-ABC123",
                        "REGISTERED",
                        "INACTIVE",
                        "PENDING"
                )
        ));

        AccountProfileStatusResult result = accountProfileUseCase.getMyProfile();

        assertFalse(result.onboardingRequired());
        assertEquals(accountId, result.account().accountId());
        assertEquals(customerId, result.customer().customerId());
        assertNull(result.employee());
    }

    @Test
    void shouldMarkOnboardingRequiredForEmployeeRoleWithoutProfileOrEmployee() {
        UUID accountId = UUID.randomUUID();
        AccountProfileState state = onboardingPendingState(accountId, AdminProvisionableAccountRoleCode.EMPLOYEE);

        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(accountProfilePortOut.findProfileStateByAccountId(accountId)).thenReturn(Optional.of(state));
        when(accountProfileResultMapper.toStatusResult(state, true)).thenReturn(new AccountProfileStatusResult(
                true,
                new AccountProfileStatusResult.AccountInfoResult(
                        accountId,
                        "ACTIVE",
                        "pending-user",
                        "pending@example.com",
                        "sub-123"
                ),
                new AccountProfileStatusResult.ProfileInfoResult(null, null, null, null, null, null, null, null, null),
                new AccountProfileStatusResult.EmployeeInfoResult(null, null, null, null, null),
                null
        ));

        AccountProfileStatusResult result = accountProfileUseCase.getMyProfile();

        assertTrue(result.onboardingRequired());
        assertNull(result.profile().userProfileId());
        assertNull(result.employee().employeeId());
        assertNull(result.customer());
    }

    @Test
    void shouldMarkOnboardingRequiredForSystemAdminWithoutProfile() {
        UUID accountId = UUID.randomUUID();
        AccountProfileState state = onboardingPendingState(accountId, AdminProvisionableAccountRoleCode.SYSTEM_ADMIN);

        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(accountProfilePortOut.findProfileStateByAccountId(accountId)).thenReturn(Optional.of(state));
        when(accountProfileResultMapper.toStatusResult(state, true)).thenReturn(new AccountProfileStatusResult(
                true,
                new AccountProfileStatusResult.AccountInfoResult(
                        accountId,
                        "ACTIVE",
                        "pending-user",
                        "pending@example.com",
                        "sub-123"
                ),
                new AccountProfileStatusResult.ProfileInfoResult(null, null, null, null, null, null, null, null, null),
                null,
                null
        ));

        AccountProfileStatusResult result = accountProfileUseCase.getMyProfile();

        assertTrue(result.onboardingRequired());
        assertNull(result.profile().userProfileId());
        assertNull(result.employee());
        assertNull(result.customer());
    }

    @Test
    void shouldReturnDetailedResultWhenCompleteEmployeeOnboardingSuccessfully() {
        UUID accountId = UUID.fromString("c6e12a53-e72b-441a-8a1e-bb84b49e0ca4");
        UUID userProfileId = UUID.fromString("ec761405-c091-4a65-b1dd-c8fb23f0d6bd");
        UUID employeeId = UUID.fromString("6e761405-c091-4a65-b1dd-c8fb23f0d6aa");

        AccountProfileState initialState = onboardingPendingState(accountId, AdminProvisionableAccountRoleCode.EMPLOYEE);
        AccountProfileState completedState = new AccountProfileState(
                accountId,
                "baoan3236",
                "baoan3236@gmail.com",
                "23d493f8-e9f8-4843-917c-9e6c431bfeea",
                AdminProvisionableAccountRoleCode.EMPLOYEE.name(),
                userProfileId,
                "Nguyen Bao An",
                LocalDate.of(2003, 9, 19),
                "MALE",
                "+84901234567",
                "Ho Chi Minh City",
                "079203001234",
                "https://cdn.example.com/avatars/bao-an.jpg",
                UserProfileStatus.ACTIVE,
                employeeId,
                null,
                "Parking Staff",
                null,
                EmployeeStatus.INACTIVE,
                null,
                null,
                null,
                null,
                null,
                AccountStatus.ACTIVE
        );
        Account updatedAccount = buildUpdatedAccount(accountId, "baoan3236", "baoan3236@gmail.com");

        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(accountProfilePortOut.findProfileStateByAccountId(accountId))
                .thenReturn(Optional.of(initialState), Optional.of(completedState));
        when(accountProfilePolicy.normalizeForComplete(any(CompleteAccountProfileCommand.class)))
                .thenReturn(new CompleteAccountProfileCommand(
                        "Nguyen Bao An",
                        "+84901234567",
                        LocalDate.of(2003, 9, 19),
                        "MALE",
                        "Ho Chi Minh City",
                        "079203001234",
                        "https://cdn.example.com/avatars/bao-an.jpg"
                ));
        when(accountProfilePortOut.existsByPhoneNumber("+84901234567")).thenReturn(false);
        when(accountProfilePortOut.existsByIdentifyCard("079203001234")).thenReturn(false);
        when(accountProfilePortOut.completeInternalProfile(eq(accountId), any(), any())).thenReturn(updatedAccount);
        when(accountProfileResultMapper.toStatusResult(completedState, false)).thenReturn(new AccountProfileStatusResult(
                false,
                new AccountProfileStatusResult.AccountInfoResult(
                        accountId,
                        "ACTIVE",
                        "baoan3236",
                        "baoan3236@gmail.com",
                        "23d493f8-e9f8-4843-917c-9e6c431bfeea"
                ),
                new AccountProfileStatusResult.ProfileInfoResult(
                        userProfileId,
                        "Nguyen Bao An",
                        LocalDate.of(2003, 9, 19),
                        "MALE",
                        "+84901234567",
                        "Ho Chi Minh City",
                        "079203001234",
                        "https://cdn.example.com/avatars/bao-an.jpg",
                        "ACTIVE"
                ),
                new AccountProfileStatusResult.EmployeeInfoResult(
                        employeeId,
                        null,
                        "Parking Staff",
                        null,
                        "INACTIVE"
                ),
                null
        ));

        AccountProfileStatusResult result = accountProfileUseCase.completeMyProfile(
                new CompleteAccountProfileCommand(
                        "Nguyen Bao An",
                        "+84901234567",
                        LocalDate.of(2003, 9, 19),
                        "MALE",
                        "Ho Chi Minh City",
                        "079203001234",
                        "https://cdn.example.com/avatars/bao-an.jpg"
                )
        );

        assertFalse(result.onboardingRequired());
        assertEquals(accountId, result.account().accountId());
        assertEquals(userProfileId, result.profile().userProfileId());
        assertEquals(employeeId, result.employee().employeeId());
        assertEquals("Parking Staff", result.employee().jobTitle());
        assertEquals("INACTIVE", result.employee().employeeStatus());
        assertNull(result.customer());

        ArgumentCaptor<com.ban.vehicle_management.domain.people.employee.model.Employee> employeeCaptor =
                ArgumentCaptor.forClass(com.ban.vehicle_management.domain.people.employee.model.Employee.class);
        ArgumentCaptor<ApprovalRequest> approvalRequestCaptor = ArgumentCaptor.forClass(ApprovalRequest.class);
        verify(accountProfilePortOut).completeInternalProfile(eq(accountId), any(), employeeCaptor.capture());
        verify(internalEmployeeApprovalPortOut).saveInternalEmployeeApprovalRequest(approvalRequestCaptor.capture());
        assertTrue(employeeCaptor.getValue().getEmployeeCode().startsWith("EMP-"));
        assertEquals(EmployeeStatus.INACTIVE, employeeCaptor.getValue().getStatus());
        assertEquals("Parking Staff", employeeCaptor.getValue().getJobTitle());
        assertEquals(employeeCaptor.getValue().getEmployeeId(), approvalRequestCaptor.getValue().getTargetId());
    }

    @Test
    void shouldCreateCustomerOnboardingApprovalWhenCustomerCompletesOnboarding() {
        UUID accountId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        AccountProfileState initialState = onboardingPendingState(accountId, AdminProvisionableAccountRoleCode.CUSTOMER);
        AccountProfileState completedState = new AccountProfileState(
                accountId,
                "customer.registered",
                "customer@example.com",
                "sub-customer",
                AdminProvisionableAccountRoleCode.CUSTOMER.name(),
                userProfileId,
                "Nguyen Customer",
                LocalDate.of(1998, 3, 15),
                "MALE",
                "0901002003",
                "Ho Chi Minh City",
                "079100200003",
                null,
                UserProfileStatus.ACTIVE,
                null,
                null,
                null,
                null,
                null,
                customerId,
                "CUS-001",
                CustomerType.REGISTERED,
                CustomerStatus.INACTIVE,
                CustomerApprovalStatus.PENDING,
                AccountStatus.ACTIVE
        );
        Account updatedAccount = buildUpdatedAccount(accountId, "customer.registered", "customer@example.com");

        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(accountProfilePortOut.findProfileStateByAccountId(accountId))
                .thenReturn(Optional.of(initialState), Optional.of(completedState));
        when(accountProfilePolicy.normalizeForComplete(any(CompleteAccountProfileCommand.class)))
                .thenReturn(new CompleteAccountProfileCommand(
                        "Nguyen Customer",
                        "0901002003",
                        LocalDate.of(1998, 3, 15),
                        "MALE",
                        "Ho Chi Minh City",
                        "079100200003",
                        null
                ));
        when(accountProfilePortOut.existsByPhoneNumber("0901002003")).thenReturn(false);
        when(accountProfilePortOut.existsByIdentifyCard("079100200003")).thenReturn(false);
        when(accountProfilePortOut.completeProfile(eq(accountId), any(), any())).thenReturn(updatedAccount);
        when(accountProfileResultMapper.toStatusResult(completedState, false)).thenReturn(new AccountProfileStatusResult(
                false,
                new AccountProfileStatusResult.AccountInfoResult(
                        accountId,
                        "ACTIVE",
                        "customer.registered",
                        "customer@example.com",
                        "sub-customer"
                ),
                new AccountProfileStatusResult.ProfileInfoResult(
                        userProfileId,
                        "Nguyen Customer",
                        LocalDate.of(1998, 3, 15),
                        "MALE",
                        "0901002003",
                        "Ho Chi Minh City",
                        "079100200003",
                        null,
                        "ACTIVE"
                ),
                null,
                new AccountProfileStatusResult.CustomerInfoResult(
                        customerId,
                        "CUS-001",
                        "REGISTERED",
                        "INACTIVE",
                        "PENDING"
                )
        ));

        AccountProfileStatusResult result = accountProfileUseCase.completeMyProfile(
                new CompleteAccountProfileCommand(
                        "Nguyen Customer",
                        "0901002003",
                        LocalDate.of(1998, 3, 15),
                        "MALE",
                        "Ho Chi Minh City",
                        "079100200003",
                        null
                )
        );

        ArgumentCaptor<com.ban.vehicle_management.domain.people.customer.model.Customer> customerCaptor =
                ArgumentCaptor.forClass(com.ban.vehicle_management.domain.people.customer.model.Customer.class);
        ArgumentCaptor<ApprovalRequest> approvalRequestCaptor = ArgumentCaptor.forClass(ApprovalRequest.class);
        verify(accountProfilePortOut).completeProfile(eq(accountId), any(), customerCaptor.capture());
        verify(customerOnboardingApprovalPortOut).saveCustomerOnboardingApprovalRequest(
                approvalRequestCaptor.capture()
        );
        assertEquals(CustomerStatus.INACTIVE, customerCaptor.getValue().getStatus());
        assertEquals(CustomerApprovalStatus.PENDING, customerCaptor.getValue().getApprovalStatus());
        assertEquals(customerCaptor.getValue().getCustomerId(), approvalRequestCaptor.getValue().getTargetId());
        assertEquals("CUSTOMER_ONBOARDING", approvalRequestCaptor.getValue().getRequestType());
        assertEquals("people", approvalRequestCaptor.getValue().getTargetSchema());
        assertEquals("customers", approvalRequestCaptor.getValue().getTargetTable());
        assertEquals(customerId, result.customer().customerId());
    }

    @Test
    void shouldTreatParkingManagerAsEmployeeBackedRoleDuringOnboarding() {
        UUID accountId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        AccountProfileState initialState = onboardingPendingState(accountId, AdminProvisionableAccountRoleCode.PARKING_MANAGER);
        AccountProfileState completedState = new AccountProfileState(
                accountId,
                "manager.provisioned.01",
                "manager@example.com",
                "sub-456",
                AdminProvisionableAccountRoleCode.PARKING_MANAGER.name(),
                userProfileId,
                "Tran Thi Manager",
                LocalDate.of(1992, 6, 10),
                "Female",
                "0987001003",
                "78 Le Loi, District 1, Ho Chi Minh City",
                "079123450203",
                "https://example.com/avatars/manager-01.jpg",
                UserProfileStatus.ACTIVE,
                employeeId,
                null,
                "Parking Manager",
                null,
                EmployeeStatus.INACTIVE,
                null,
                null,
                null,
                null,
                null,
                AccountStatus.ACTIVE
        );
        Account updatedAccount = buildUpdatedAccount(accountId, "manager.provisioned.01", "manager@example.com");

        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(accountProfilePortOut.findProfileStateByAccountId(accountId))
                .thenReturn(Optional.of(initialState), Optional.of(completedState));
        when(accountProfilePolicy.normalizeForComplete(any(CompleteAccountProfileCommand.class)))
                .thenReturn(new CompleteAccountProfileCommand(
                        "Tran Thi Manager",
                        "0987001003",
                        LocalDate.of(1992, 6, 10),
                        "Female",
                        "78 Le Loi, District 1, Ho Chi Minh City",
                        "079123450203",
                        "https://example.com/avatars/manager-01.jpg"
                ));
        when(accountProfilePortOut.existsByPhoneNumber("0987001003")).thenReturn(false);
        when(accountProfilePortOut.existsByIdentifyCard("079123450203")).thenReturn(false);
        when(accountProfilePortOut.completeInternalProfile(eq(accountId), any(), any())).thenReturn(updatedAccount);
        when(accountProfileResultMapper.toStatusResult(completedState, false)).thenReturn(new AccountProfileStatusResult(
                false,
                new AccountProfileStatusResult.AccountInfoResult(
                        accountId,
                        "ACTIVE",
                        "manager.provisioned.01",
                        "manager@example.com",
                        "sub-456"
                ),
                new AccountProfileStatusResult.ProfileInfoResult(
                        userProfileId,
                        "Tran Thi Manager",
                        LocalDate.of(1992, 6, 10),
                        "Female",
                        "0987001003",
                        "78 Le Loi, District 1, Ho Chi Minh City",
                        "079123450203",
                        "https://example.com/avatars/manager-01.jpg",
                        "ACTIVE"
                ),
                new AccountProfileStatusResult.EmployeeInfoResult(
                        employeeId,
                        null,
                        "Parking Manager",
                        null,
                        "INACTIVE"
                ),
                null
        ));

        AccountProfileStatusResult result = accountProfileUseCase.completeMyProfile(
                new CompleteAccountProfileCommand(
                        "Tran Thi Manager",
                        "0987001003",
                        LocalDate.of(1992, 6, 10),
                        "Female",
                        "78 Le Loi, District 1, Ho Chi Minh City",
                        "079123450203",
                        "https://example.com/avatars/manager-01.jpg"
                )
        );

        assertEquals(employeeId, result.employee().employeeId());
        assertEquals("Parking Manager", result.employee().jobTitle());
        assertNull(result.customer());
        verify(accountProfilePortOut).completeInternalProfile(eq(accountId), any(), any());
        verify(internalEmployeeApprovalPortOut).saveInternalEmployeeApprovalRequest(any(ApprovalRequest.class));
    }

    @Test
    void shouldCompleteSystemAdminOnboardingWithProfileOnly() {
        UUID accountId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        AccountProfileState initialState = onboardingPendingState(accountId, AdminProvisionableAccountRoleCode.SYSTEM_ADMIN);
        AccountProfileState completedState = new AccountProfileState(
                accountId,
                "sysadmin",
                "sysadmin@example.com",
                "sub-admin",
                AdminProvisionableAccountRoleCode.SYSTEM_ADMIN.name(),
                userProfileId,
                "System Admin",
                LocalDate.of(1990, 1, 1),
                "MALE",
                "0901000000",
                "Ho Chi Minh City",
                "079100000001",
                "https://example.com/avatars/sysadmin.jpg",
                UserProfileStatus.ACTIVE,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                AccountStatus.ACTIVE
        );
        Account updatedAccount = buildUpdatedAccount(accountId, "sysadmin", "sysadmin@example.com");

        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(accountProfilePortOut.findProfileStateByAccountId(accountId))
                .thenReturn(Optional.of(initialState), Optional.of(completedState));
        when(accountProfilePolicy.normalizeForComplete(any(CompleteAccountProfileCommand.class)))
                .thenReturn(new CompleteAccountProfileCommand(
                        "System Admin",
                        "0901000000",
                        LocalDate.of(1990, 1, 1),
                        "MALE",
                        "Ho Chi Minh City",
                        "079100000001",
                        "https://example.com/avatars/sysadmin.jpg"
                ));
        when(accountProfilePortOut.existsByPhoneNumber("0901000000")).thenReturn(false);
        when(accountProfilePortOut.existsByIdentifyCard("079100000001")).thenReturn(false);
        when(accountProfilePortOut.completeProfileOnly(eq(accountId), any())).thenReturn(updatedAccount);
        when(accountProfileResultMapper.toStatusResult(completedState, false)).thenReturn(new AccountProfileStatusResult(
                false,
                new AccountProfileStatusResult.AccountInfoResult(
                        accountId,
                        "ACTIVE",
                        "sysadmin",
                        "sysadmin@example.com",
                        "sub-admin"
                ),
                new AccountProfileStatusResult.ProfileInfoResult(
                        userProfileId,
                        "System Admin",
                        LocalDate.of(1990, 1, 1),
                        "MALE",
                        "0901000000",
                        "Ho Chi Minh City",
                        "079100000001",
                        "https://example.com/avatars/sysadmin.jpg",
                        "ACTIVE"
                ),
                null,
                null
        ));

        AccountProfileStatusResult result = accountProfileUseCase.completeMyProfile(
                new CompleteAccountProfileCommand(
                        "System Admin",
                        "0901000000",
                        LocalDate.of(1990, 1, 1),
                        "MALE",
                        "Ho Chi Minh City",
                        "079100000001",
                        "https://example.com/avatars/sysadmin.jpg"
                )
        );

        assertFalse(result.onboardingRequired());
        assertEquals(userProfileId, result.profile().userProfileId());
        assertNull(result.employee());
        assertNull(result.customer());
        verify(accountProfilePortOut).completeProfileOnly(eq(accountId), any());
        verify(internalEmployeeApprovalPortOut, never()).saveInternalEmployeeApprovalRequest(any(ApprovalRequest.class));
        verify(systemAdminApprovalPortOut, never()).saveSystemAdminApprovalRequest(any(ApprovalRequest.class));
    }

    @Test
    void shouldCreateSystemAdminApprovalWhenPendingSystemAdminCompletesOnboarding() {
        UUID accountId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        AccountProfileState initialState = onboardingPendingState(
                accountId,
                AdminProvisionableAccountRoleCode.SYSTEM_ADMIN,
                AccountStatus.PENDING
        );
        AccountProfileState completedState = new AccountProfileState(
                accountId,
                "sysadmin.pending",
                "sysadmin.pending@example.com",
                "sub-admin-pending",
                AdminProvisionableAccountRoleCode.SYSTEM_ADMIN.name(),
                userProfileId,
                "Pending System Admin",
                LocalDate.of(1990, 1, 1),
                "MALE",
                "0901000001",
                "Ho Chi Minh City",
                "079100000002",
                null,
                UserProfileStatus.ACTIVE,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                AccountStatus.PENDING
        );
        Account updatedAccount = buildUpdatedAccount(accountId, "sysadmin.pending", "sysadmin.pending@example.com");
        updatedAccount.setStatus(AccountStatus.PENDING);

        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(accountProfilePortOut.findProfileStateByAccountId(accountId))
                .thenReturn(Optional.of(initialState), Optional.of(completedState));
        when(accountProfilePolicy.normalizeForComplete(any(CompleteAccountProfileCommand.class)))
                .thenReturn(new CompleteAccountProfileCommand(
                        "Pending System Admin",
                        "0901000001",
                        LocalDate.of(1990, 1, 1),
                        "MALE",
                        "Ho Chi Minh City",
                        "079100000002",
                        null
                ));
        when(accountProfilePortOut.existsByPhoneNumber("0901000001")).thenReturn(false);
        when(accountProfilePortOut.existsByIdentifyCard("079100000002")).thenReturn(false);
        when(accountProfilePortOut.completeProfileOnly(eq(accountId), any())).thenReturn(updatedAccount);
        when(accountProfileResultMapper.toStatusResult(completedState, false)).thenReturn(new AccountProfileStatusResult(
                false,
                new AccountProfileStatusResult.AccountInfoResult(
                        accountId,
                        "PENDING",
                        "sysadmin.pending",
                        "sysadmin.pending@example.com",
                        "sub-admin-pending"
                ),
                new AccountProfileStatusResult.ProfileInfoResult(
                        userProfileId,
                        "Pending System Admin",
                        LocalDate.of(1990, 1, 1),
                        "MALE",
                        "0901000001",
                        "Ho Chi Minh City",
                        "079100000002",
                        null,
                        "ACTIVE"
                ),
                null,
                null
        ));

        AccountProfileStatusResult result = accountProfileUseCase.completeMyProfile(
                new CompleteAccountProfileCommand(
                        "Pending System Admin",
                        "0901000001",
                        LocalDate.of(1990, 1, 1),
                        "MALE",
                        "Ho Chi Minh City",
                        "079100000002",
                        null
                )
        );

        ArgumentCaptor<ApprovalRequest> approvalRequestCaptor = ArgumentCaptor.forClass(ApprovalRequest.class);
        verify(systemAdminApprovalPortOut).saveSystemAdminApprovalRequest(approvalRequestCaptor.capture());
        assertEquals(accountId, approvalRequestCaptor.getValue().getTargetId());
        assertEquals("SYSTEM_ADMIN_ONBOARDING", approvalRequestCaptor.getValue().getRequestType());
        assertEquals("iam", approvalRequestCaptor.getValue().getTargetSchema());
        assertEquals("accounts", approvalRequestCaptor.getValue().getTargetTable());
        assertEquals("PENDING", result.account().accountStatus());
    }

    @Test
    void shouldRejectOnboardingWhenCurrentAccountRoleIsUnsupported() {
        UUID accountId = UUID.randomUUID();
        AccountProfileState state = new AccountProfileState(
                accountId,
                "unknown.role.user",
                "unknown@example.com",
                "sub-789",
                "GUEST",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                AccountStatus.ACTIVE
        );

        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(accountProfilePortOut.findProfileStateByAccountId(accountId)).thenReturn(Optional.of(state));
        when(accountProfilePolicy.normalizeForComplete(any(CompleteAccountProfileCommand.class)))
                .thenReturn(new CompleteAccountProfileCommand(
                        "Unknown User",
                        "0987001999",
                        LocalDate.of(1990, 1, 1),
                        "Male",
                        "Unknown",
                        "079123450999",
                        null
                ));
        when(accountProfilePortOut.existsByPhoneNumber("0987001999")).thenReturn(false);
        when(accountProfilePortOut.existsByIdentifyCard("079123450999")).thenReturn(false);

        assertThrows(
                BadRequestException.class,
                () -> accountProfileUseCase.completeMyProfile(new CompleteAccountProfileCommand(
                        "Unknown User",
                        "0987001999",
                        LocalDate.of(1990, 1, 1),
                        "Male",
                        "Unknown",
                        "079123450999",
                        null
                ))
        );
    }

    private AccountProfileState onboardingPendingState(UUID accountId, AdminProvisionableAccountRoleCode roleCode) {
        return onboardingPendingState(accountId, roleCode, AccountStatus.ACTIVE);
    }

    private AccountProfileState onboardingPendingState(
            UUID accountId,
            AdminProvisionableAccountRoleCode roleCode,
            AccountStatus accountStatus
    ) {
        return new AccountProfileState(
                accountId,
                "pending-user",
                "pending@example.com",
                "sub-123",
                roleCode.name(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                accountStatus
        );
    }

    private Account buildUpdatedAccount(UUID accountId, String username, String email) {
        Account updatedAccount = new Account();
        updatedAccount.setAccountId(accountId);
        updatedAccount.setUsername(username);
        updatedAccount.setEmail(email);
        updatedAccount.setKeycloakUserId("23d493f8-e9f8-4843-917c-9e6c431bfeea");
        updatedAccount.setStatus(AccountStatus.ACTIVE);
        return updatedAccount;
    }
}
