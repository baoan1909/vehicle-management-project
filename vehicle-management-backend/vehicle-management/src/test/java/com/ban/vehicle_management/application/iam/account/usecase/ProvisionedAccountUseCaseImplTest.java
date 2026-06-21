package com.ban.vehicle_management.application.iam.account.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.model.command.CreateProvisionedAccountCommand;
import com.ban.vehicle_management.application.iam.account.model.command.ProvisionedAccountFilterCommand;
import com.ban.vehicle_management.application.iam.account.model.command.UpdateProvisionedAccountRoleCommand;
import com.ban.vehicle_management.application.iam.account.model.command.UpdateProvisionedAccountStatusCommand;
import com.ban.vehicle_management.application.iam.account.model.result.ProvisionedAccountResult;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.IdentityProviderAdminPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.ProvisionedAccountPortOut;
import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.iam.account.policy.ProvisionedAccountPolicy;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProvisionedAccountUseCaseImplTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private ProvisionedAccountPortOut provisionedAccountPortOut;

    @Mock
    private IdentityProviderAdminPortOut identityProviderAdminPortOut;

    @Spy
    private ProvisionedAccountPolicy provisionedAccountPolicy = new ProvisionedAccountPolicy();

    @InjectMocks
    private ProvisionedAccountUseCaseImpl provisionedAccountUseCase;

    @Test
    void shouldCreateProfileTogetherWithProvisionedEmployeeAccount() {
        UUID roleId = UUID.randomUUID();
        String keycloakUserId = "kc-employee-id";
        Account account = new Account();
        account.setUsername(" employee.01 ");
        account.setEmail("employee01@example.com");
        CreateProvisionedAccountCommand command = new CreateProvisionedAccountCommand(
                account,
                "TemporaryPassword1!",
                AdminProvisionableAccountRoleCode.EMPLOYEE,
                " Nguyen Employee "
        );

        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentAccount("PARKING_MANAGER"));
        when(provisionedAccountPortOut.existsByUsername("employee.01")).thenReturn(false);
        when(provisionedAccountPortOut.existsByEmail("employee01@example.com")).thenReturn(false);
        when(provisionedAccountPortOut.findActiveRoleIdByCode(AdminProvisionableAccountRoleCode.EMPLOYEE))
                .thenReturn(roleId);
        when(identityProviderAdminPortOut.createProvisionedAccountUser(any(CreateProvisionedAccountCommand.class)))
                .thenReturn(keycloakUserId);
        when(provisionedAccountPortOut.findProvisionedAccountById(any(UUID.class)))
                .thenAnswer(invocation -> Optional.of(buildResult(invocation.getArgument(0), roleId)));

        ProvisionedAccountResult result = provisionedAccountUseCase.createProvisionedAccount(command);

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(provisionedAccountPortOut).provisionAccount(accountCaptor.capture(), profileCaptor.capture());
        Account persistedAccount = accountCaptor.getValue();
        UserProfile persistedProfile = profileCaptor.getValue();

        assertNotNull(persistedAccount.getAccountId());
        assertNotNull(persistedAccount.getUserProfileId());
        assertEquals(persistedAccount.getUserProfileId(), persistedProfile.getUserProfileId());
        assertEquals("employee.01", persistedAccount.getUsername());
        assertEquals("Nguyen Employee", persistedProfile.getFullName());
        assertEquals(AccountStatus.ACTIVE, persistedAccount.getStatus());
        assertEquals(UserProfileStatus.ACTIVE, persistedProfile.getStatus());
        assertEquals(0, persistedAccount.getFailedLoginCount());
        assertEquals(result.account().accountId(), persistedAccount.getAccountId());
        verify(identityProviderAdminPortOut).updateAccountIdAttribute(keycloakUserId, persistedAccount.getAccountId());
        verify(identityProviderAdminPortOut).sendUpdatePasswordEmail(keycloakUserId);
    }

    @Test
    void shouldRejectSystemAdminProvisioningEmployeeAccount() {
        Account account = new Account();
        account.setUsername("employee.01");
        account.setEmail("employee01@example.com");
        CreateProvisionedAccountCommand command = new CreateProvisionedAccountCommand(
                account,
                "TemporaryPassword1!",
                AdminProvisionableAccountRoleCode.EMPLOYEE,
                "Nguyen Employee"
        );

        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentAccount("SYSTEM_ADMIN"));

        assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> provisionedAccountUseCase.createProvisionedAccount(command)
        );
        verify(identityProviderAdminPortOut, never()).createProvisionedAccountUser(any());
    }

    @Test
    void shouldAllowSystemAdminProvisioningParkingManagerAccount() {
        UUID roleId = UUID.randomUUID();
        String keycloakUserId = "kc-manager-id";
        Account account = new Account();
        account.setUsername("manager.01");
        account.setEmail("manager01@example.com");
        CreateProvisionedAccountCommand command = new CreateProvisionedAccountCommand(
                account,
                "TemporaryPassword1!",
                AdminProvisionableAccountRoleCode.PARKING_MANAGER,
                "Tran Manager"
        );

        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentAccount("SYSTEM_ADMIN"));
        when(provisionedAccountPortOut.existsByUsername("manager.01")).thenReturn(false);
        when(provisionedAccountPortOut.existsByEmail("manager01@example.com")).thenReturn(false);
        when(provisionedAccountPortOut.findActiveRoleIdByCode(AdminProvisionableAccountRoleCode.PARKING_MANAGER))
                .thenReturn(roleId);
        when(identityProviderAdminPortOut.createProvisionedAccountUser(any(CreateProvisionedAccountCommand.class)))
                .thenReturn(keycloakUserId);
        when(provisionedAccountPortOut.findProvisionedAccountById(any(UUID.class)))
                .thenAnswer(invocation -> Optional.of(buildResult(
                        invocation.getArgument(0),
                        roleId,
                        AdminProvisionableAccountRoleCode.PARKING_MANAGER,
                        "manager.01",
                        "manager01@example.com"
                )));

        ProvisionedAccountResult result = provisionedAccountUseCase.createProvisionedAccount(command);

        assertEquals(AdminProvisionableAccountRoleCode.PARKING_MANAGER.name(), result.role().roleCode());
        verify(identityProviderAdminPortOut).sendUpdatePasswordEmail(keycloakUserId);
    }

    @Test
    void shouldAllowParkingManagerProvisioningCustomerAccount() {
        UUID roleId = UUID.randomUUID();
        String keycloakUserId = "kc-customer-id";
        Account account = new Account();
        account.setUsername("customer.01");
        account.setEmail("customer01@example.com");
        CreateProvisionedAccountCommand command = new CreateProvisionedAccountCommand(
                account,
                "TemporaryPassword1!",
                AdminProvisionableAccountRoleCode.CUSTOMER,
                "Nguyen Customer"
        );

        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentAccount("PARKING_MANAGER"));
        when(provisionedAccountPortOut.existsByUsername("customer.01")).thenReturn(false);
        when(provisionedAccountPortOut.existsByEmail("customer01@example.com")).thenReturn(false);
        when(provisionedAccountPortOut.findActiveRoleIdByCode(AdminProvisionableAccountRoleCode.CUSTOMER))
                .thenReturn(roleId);
        when(identityProviderAdminPortOut.createProvisionedAccountUser(any(CreateProvisionedAccountCommand.class)))
                .thenReturn(keycloakUserId);
        when(provisionedAccountPortOut.findProvisionedAccountById(any(UUID.class)))
                .thenAnswer(invocation -> Optional.of(buildResult(
                        invocation.getArgument(0),
                        roleId,
                        AdminProvisionableAccountRoleCode.CUSTOMER,
                        "customer.01",
                        "customer01@example.com"
                )));

        ProvisionedAccountResult result = provisionedAccountUseCase.createProvisionedAccount(command);

        assertEquals(AdminProvisionableAccountRoleCode.CUSTOMER.name(), result.role().roleCode());
        verify(identityProviderAdminPortOut).sendUpdatePasswordEmail(keycloakUserId);
    }

    @Test
    void shouldRejectSystemAdminProvisioningCustomerAccount() {
        Account account = new Account();
        account.setUsername("customer.01");
        account.setEmail("customer01@example.com");
        CreateProvisionedAccountCommand command = new CreateProvisionedAccountCommand(
                account,
                "TemporaryPassword1!",
                AdminProvisionableAccountRoleCode.CUSTOMER,
                "Nguyen Customer"
        );

        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentAccount("SYSTEM_ADMIN"));

        assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> provisionedAccountUseCase.createProvisionedAccount(command)
        );
        verify(identityProviderAdminPortOut, never()).createProvisionedAccountUser(any());
    }

    @Test
    void shouldRejectParkingManagerProvisioningSystemAdminAccount() {
        Account account = new Account();
        account.setUsername("admin.01");
        account.setEmail("admin01@example.com");
        CreateProvisionedAccountCommand command = new CreateProvisionedAccountCommand(
                account,
                "TemporaryPassword1!",
                AdminProvisionableAccountRoleCode.SYSTEM_ADMIN,
                "System Admin"
        );

        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentAccount("PARKING_MANAGER"));

        assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> provisionedAccountUseCase.createProvisionedAccount(command)
        );
        verify(identityProviderAdminPortOut, never()).createProvisionedAccountUser(any());
    }

    @Test
    void shouldRejectParkingManagerProvisioningParkingManagerAccount() {
        Account account = new Account();
        account.setUsername("manager.02");
        account.setEmail("manager02@example.com");
        CreateProvisionedAccountCommand command = new CreateProvisionedAccountCommand(
                account,
                "TemporaryPassword1!",
                AdminProvisionableAccountRoleCode.PARKING_MANAGER,
                "Second Manager"
        );

        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentAccount("PARKING_MANAGER"));

        assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> provisionedAccountUseCase.createProvisionedAccount(command)
        );
        verify(identityProviderAdminPortOut, never()).createProvisionedAccountUser(any());
    }

    @Test
    void shouldFilterProvisionedAccountListBySystemAdminManagedRoles() {
        ProvisionedAccountFilterCommand command = new ProvisionedAccountFilterCommand(
                " manager ",
                null,
                AccountStatus.ACTIVE,
                null
        );
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentAccount("SYSTEM_ADMIN"));
        when(provisionedAccountPortOut.findProvisionedAccounts(any(ProvisionedAccountFilterCommand.class)))
                .thenReturn(List.of());

        provisionedAccountUseCase.getProvisionedAccounts(command);

        ArgumentCaptor<ProvisionedAccountFilterCommand> commandCaptor =
                ArgumentCaptor.forClass(ProvisionedAccountFilterCommand.class);
        verify(provisionedAccountPortOut).findProvisionedAccounts(commandCaptor.capture());
        assertEquals("manager", commandCaptor.getValue().keyword());
        assertEquals(AccountStatus.ACTIVE, commandCaptor.getValue().accountStatus());
        assertEquals(
                Set.of(
                        AdminProvisionableAccountRoleCode.SYSTEM_ADMIN,
                        AdminProvisionableAccountRoleCode.PARKING_MANAGER
                ),
                commandCaptor.getValue().managedRoleCodes()
        );
    }

    @Test
    void shouldFilterProvisionedAccountListByParkingManagerManagedRoles() {
        ProvisionedAccountFilterCommand command = new ProvisionedAccountFilterCommand(
                null,
                AdminProvisionableAccountRoleCode.EMPLOYEE,
                null,
                null
        );
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentAccount("PARKING_MANAGER"));
        when(provisionedAccountPortOut.findProvisionedAccounts(any(ProvisionedAccountFilterCommand.class)))
                .thenReturn(List.of());

        provisionedAccountUseCase.getProvisionedAccounts(command);

        ArgumentCaptor<ProvisionedAccountFilterCommand> commandCaptor =
                ArgumentCaptor.forClass(ProvisionedAccountFilterCommand.class);
        verify(provisionedAccountPortOut).findProvisionedAccounts(commandCaptor.capture());
        assertEquals(AdminProvisionableAccountRoleCode.EMPLOYEE, commandCaptor.getValue().roleCode());
        assertEquals(
                Set.of(
                        AdminProvisionableAccountRoleCode.EMPLOYEE,
                        AdminProvisionableAccountRoleCode.CUSTOMER
                ),
                commandCaptor.getValue().managedRoleCodes()
        );
    }

    @Test
    void shouldRejectSystemAdminReadingEmployeeProvisionedAccount() {
        UUID accountId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        ProvisionedAccountResult existingAccount = buildResult(
                accountId,
                roleId,
                AdminProvisionableAccountRoleCode.EMPLOYEE,
                "employee.01",
                "employee01@example.com"
        );

        when(provisionedAccountPortOut.findProvisionedAccountById(accountId)).thenReturn(Optional.of(existingAccount));
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentAccount("SYSTEM_ADMIN"));

        assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> provisionedAccountUseCase.getProvisionedAccountById(accountId)
        );
    }

    @Test
    void shouldRejectSystemAdminUpdatingEmployeeProvisionedAccountStatus() {
        UUID accountId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        ProvisionedAccountResult existingAccount = buildResult(
                accountId,
                roleId,
                AdminProvisionableAccountRoleCode.EMPLOYEE,
                "employee.01",
                "employee01@example.com"
        );
        UpdateProvisionedAccountStatusCommand command =
                new UpdateProvisionedAccountStatusCommand(AccountStatus.LOCKED, "Policy violation");

        when(provisionedAccountPortOut.findProvisionedAccountById(accountId)).thenReturn(Optional.of(existingAccount));
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentAccount("SYSTEM_ADMIN"));

        assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> provisionedAccountUseCase.updateProvisionedAccountStatus(accountId, command)
        );
        verify(provisionedAccountPortOut, never()).updateProvisionedAccountStatus(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
        verify(identityProviderAdminPortOut, never()).updateUserEnabled(any(), eq(false));
    }

    @Test
    void shouldRejectSystemAdminUpdatingProvisionedAccountRoleToEmployee() {
        UUID accountId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        ProvisionedAccountResult existingAccount = buildResult(
                accountId,
                roleId,
                AdminProvisionableAccountRoleCode.PARKING_MANAGER,
                "manager.01",
                "manager01@example.com"
        );
        UpdateProvisionedAccountRoleCommand command =
                new UpdateProvisionedAccountRoleCommand(AdminProvisionableAccountRoleCode.EMPLOYEE);

        when(provisionedAccountPortOut.findProvisionedAccountById(accountId)).thenReturn(Optional.of(existingAccount));
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentAccount("SYSTEM_ADMIN"));

        assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> provisionedAccountUseCase.updateProvisionedAccountRole(accountId, command)
        );
        verify(provisionedAccountPortOut, never()).updateProvisionedAccountRole(any(), any());
    }

    @Test
    void shouldRejectSystemAdminUpdatingEmployeeRoleEvenWhenTargetRoleIsManaged() {
        UUID accountId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        ProvisionedAccountResult existingAccount = buildResult(
                accountId,
                roleId,
                AdminProvisionableAccountRoleCode.EMPLOYEE,
                "employee.01",
                "employee01@example.com"
        );
        UpdateProvisionedAccountRoleCommand command =
                new UpdateProvisionedAccountRoleCommand(AdminProvisionableAccountRoleCode.PARKING_MANAGER);

        when(provisionedAccountPortOut.findProvisionedAccountById(accountId)).thenReturn(Optional.of(existingAccount));
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentAccount("SYSTEM_ADMIN"));

        assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> provisionedAccountUseCase.updateProvisionedAccountRole(accountId, command)
        );
        verify(provisionedAccountPortOut, never()).updateProvisionedAccountRole(any(), any());
    }

    @Test
    void shouldRejectParkingManagerUpdatingProvisionedAccountRoleToSystemAdmin() {
        UUID accountId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        ProvisionedAccountResult existingAccount = buildResult(
                accountId,
                roleId,
                AdminProvisionableAccountRoleCode.EMPLOYEE,
                "employee.01",
                "employee01@example.com"
        );
        UpdateProvisionedAccountRoleCommand command =
                new UpdateProvisionedAccountRoleCommand(AdminProvisionableAccountRoleCode.SYSTEM_ADMIN);

        when(provisionedAccountPortOut.findProvisionedAccountById(accountId)).thenReturn(Optional.of(existingAccount));
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentAccount("PARKING_MANAGER"));

        assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> provisionedAccountUseCase.updateProvisionedAccountRole(accountId, command)
        );
        verify(provisionedAccountPortOut, never()).updateProvisionedAccountRole(any(), any());
    }

    @Test
    void shouldUpdateEmployeeStatusWhenUpdatingInternalProvisionedAccountStatus() {
        UUID accountId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID changedBy = UUID.randomUUID();
        ProvisionedAccountResult existingAccount = buildResult(
                accountId,
                roleId,
                AdminProvisionableAccountRoleCode.EMPLOYEE,
                "employee.01",
                "employee01@example.com"
        );
        UpdateProvisionedAccountStatusCommand command =
                new UpdateProvisionedAccountStatusCommand(AccountStatus.LOCKED, "Policy violation");

        when(provisionedAccountPortOut.findProvisionedAccountById(accountId)).thenReturn(Optional.of(existingAccount));
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentAccount("PARKING_MANAGER"));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(changedBy);

        provisionedAccountUseCase.updateProvisionedAccountStatus(accountId, command);

        verify(provisionedAccountPortOut).updateProvisionedAccountStatus(
                eq(accountId),
                eq(AccountStatus.LOCKED),
                eq(UserProfileStatus.SUSPENDED),
                eq(CustomerStatus.INACTIVE),
                eq(EmployeeStatus.SUSPENDED),
                eq(changedBy),
                eq("Policy violation")
        );
        verify(identityProviderAdminPortOut).updateUserEnabled("kc-employee-id", false);
    }

    private ProvisionedAccountResult buildResult(UUID accountId, UUID roleId) {
        return buildResult(
                accountId,
                roleId,
                AdminProvisionableAccountRoleCode.EMPLOYEE,
                "employee.01",
                "employee01@example.com"
        );
    }

    private ProvisionedAccountResult buildResult(
            UUID accountId,
            UUID roleId,
            AdminProvisionableAccountRoleCode roleCode,
            String username,
            String email
    ) {
        return new ProvisionedAccountResult(
                new ProvisionedAccountResult.AccountInfoResult(
                        accountId,
                        "kc-employee-id",
                        username,
                        email,
                        AccountStatus.ACTIVE,
                        null,
                        null
                ),
                new ProvisionedAccountResult.RoleInfoResult(
                        roleId,
                        roleCode.name(),
                        roleCode.name(),
                        List.of("PROFILE_READ_SELF")
                )
        );
    }

    private CurrentAccountAccess currentAccount(String roleCode) {
        return new CurrentAccountAccess(
                UUID.randomUUID(),
                "subject",
                "current.user",
                "current@example.com",
                UUID.randomUUID(),
                roleCode,
                AccountStatus.ACTIVE,
                "PARKING_MANAGER".equals(roleCode) ? EmployeeStatus.ACTIVE : null,
                Set.of("ACCOUNT_CREATE_ALL", "ACCOUNT_UPDATE_ALL")
        );
    }
}
