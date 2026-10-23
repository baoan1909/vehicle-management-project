package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.RoleEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.EmployeeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.RolePermissionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.RoleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.CustomerRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.EmployeeRepository;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountAuthorizationPersistenceAdapterTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private AccountAuthorizationPersistenceAdapter adapter;

    @ParameterizedTest
    @MethodSource("authorizationStates")
    void shouldResolveCurrentAccessPermissionsWithSharedLifecyclePolicy(
            String roleCode,
            boolean roleActive,
            EmployeeStatus employeeStatus,
            CustomerStatus customerStatus,
            CustomerApprovalStatus customerApprovalStatus,
            boolean expectedAllowed
    ) {
        UUID accountId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        Set<String> assignedPermissions = Set.of("ACCOUNT_READ_ALL");

        AccountEntity account = new AccountEntity();
        account.setAccountId(accountId);
        account.setRoleId(roleId);
        account.setUserProfileId(userProfileId);
        account.setStatus(AccountStatus.ACTIVE);

        RoleEntity role = new RoleEntity();
        role.setRoleId(roleId);
        role.setCode(roleCode);
        role.setIsActive(roleActive);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(rolePermissionRepository.findActivePermissionCodesByRoleId(roleId))
                .thenReturn(assignedPermissions);

        if (employeeStatus != null) {
            EmployeeEntity employee = new EmployeeEntity();
            employee.setStatus(employeeStatus);
            when(employeeRepository.findByUserProfileId(userProfileId)).thenReturn(Optional.of(employee));
        }
        if (customerStatus != null || customerApprovalStatus != null) {
            CustomerEntity customer = new CustomerEntity();
            customer.setStatus(customerStatus);
            customer.setApprovalStatus(customerApprovalStatus);
            when(customerRepository.findByUserProfileId(userProfileId)).thenReturn(Optional.of(customer));
        }

        CurrentAccountAccess access = adapter.findByAccountId(accountId).orElseThrow();

        assertEquals(expectedAllowed, access.canUseBusinessPermissions());
        assertEquals(expectedAllowed ? assignedPermissions : Set.of(), access.getEffectivePermissionCodes());
    }

    private static Stream<Arguments> authorizationStates() {
        return Stream.of(
                Arguments.of("SYSTEM_ADMIN", true, null, null, null, true),
                Arguments.of("SYSTEM_ADMIN", false, null, null, null, false),
                Arguments.of("EMPLOYEE", true, EmployeeStatus.ACTIVE, null, null, true),
                Arguments.of("EMPLOYEE", true, EmployeeStatus.INACTIVE, null, null, false),
                Arguments.of("EMPLOYEE", true, null, null, null, false),
                Arguments.of("CUSTOMER", true, null,
                        CustomerStatus.ACTIVE, CustomerApprovalStatus.APPROVED, true),
                Arguments.of("CUSTOMER", true, null,
                        CustomerStatus.INACTIVE, CustomerApprovalStatus.APPROVED, false),
                Arguments.of("CUSTOMER", true, null,
                        CustomerStatus.ACTIVE, CustomerApprovalStatus.PENDING, false),
                Arguments.of("CUSTOMER", true, null, null, null, false),
                Arguments.of("CUSTOM_OPERATOR", true, null, null, null, true),
                Arguments.of("CUSTOM_OPERATOR", true, EmployeeStatus.INACTIVE, null, null, false),
                Arguments.of("custom_operator", true, null, null, null, false)
        );
    }
}
