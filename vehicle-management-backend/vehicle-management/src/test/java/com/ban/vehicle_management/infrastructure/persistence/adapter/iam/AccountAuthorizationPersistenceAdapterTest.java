package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.RoleEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.EmployeeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.*;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.CustomerRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.EmployeeRepository;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationMembershipStatus;
import com.ban.vehicle_management.shared.enumeration.people.*;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

@ExtendWith(MockitoExtension.class)
class AccountAuthorizationPersistenceAdapterTest {
    @Mock AccountRepository accountRepository;
    @Mock RolePermissionRepository rolePermissionRepository;
    @Mock RoleRepository roleRepository;
    @Mock CustomerRepository customerRepository;
    @Mock EmployeeRepository employeeRepository;
    @Mock OrganizationMembershipRepository organizationMembershipRepository;
    @Mock MemberParkingLotScopeRepository memberParkingLotScopeRepository;
    private AccountAuthorizationPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AccountAuthorizationPersistenceAdapter(
                accountRepository, rolePermissionRepository, roleRepository, customerRepository,
                employeeRepository, organizationMembershipRepository, memberParkingLotScopeRepository);
    }

    @Test
    void activeCustomRoleKeepsAssignedPlatformPermission() {
        UUID accountId = UUID.randomUUID();
        stubAccount(accountId, "CUSTOM_PLATFORM", true, null, Set.of("ACCOUNT_READ_ALL"));
        CurrentAccountAccess access = adapter.findByAccountId(accountId).orElseThrow();
        assertEquals(Set.of("ACCOUNT_READ_ALL"), access.getEffectivePermissionCodes());
    }

    @Test
    void roleNameDoesNotGrantMissingPermission() {
        UUID accountId = UUID.randomUUID();
        stubAccount(accountId, "SYSTEM_ADMIN", true, null, Set.of());
        assertTrue(adapter.findByAccountId(accountId).orElseThrow().getEffectivePermissionCodes().isEmpty());
    }

    @Test
    void customerOwnPermissionRequiresApprovedActiveCustomer() {
        UUID accountId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        stubAccount(accountId, "CUSTOMER", true, profileId, Set.of("CUSTOMER_VEHICLE_READ_OWN"));
        CustomerEntity customer = new CustomerEntity();
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setApprovalStatus(CustomerApprovalStatus.PENDING);
        when(customerRepository.findByUserProfileId(profileId)).thenReturn(Optional.of(customer));
        when(employeeRepository.findByUserProfileId(profileId)).thenReturn(Optional.empty());

        assertTrue(adapter.findByAccountId(accountId).orElseThrow().getEffectivePermissionCodes().isEmpty());
    }

    @Test
    void assignedScopeLoadsOnlyActiveAssignmentsAndRequiresActiveEmployee() {
        UUID accountId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        stubAccount(accountId, "CUSTOM_OPERATOR", true, profileId, Set.of("PARKING_SCOPE_ASSIGNED"));
        EmployeeEntity employee = new EmployeeEntity();
        employee.setStatus(EmployeeStatus.ACTIVE);
        when(employeeRepository.findByUserProfileId(profileId)).thenReturn(Optional.of(employee));
        when(customerRepository.findByUserProfileId(profileId)).thenReturn(Optional.empty());
        when(memberParkingLotScopeRepository.findActiveParkingLotIdsByAccountId(
                accountId, OrganizationMembershipStatus.ACTIVE)).thenReturn(Set.of(lotId));

        CurrentAccountAccess access = adapter.findByAccountId(accountId).orElseThrow();
        assertEquals(Set.of(lotId), access.assignedParkingLotIds());
        assertEquals(Set.of("PARKING_SCOPE_ASSIGNED"), access.getEffectivePermissionCodes());
    }

    @Test
    void partnerScopeLoadsActiveMembershipForCustomRole() {
        UUID accountId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        stubAccount(accountId, "CUSTOM_PARTNER", true, null, Set.of("PARKING_SCOPE_PARTNER"));
        when(organizationMembershipRepository.findActiveOrganizationIdsByAccountId(
                accountId, OrganizationMembershipStatus.ACTIVE)).thenReturn(Set.of(organizationId));

        CurrentAccountAccess access = adapter.findByAccountId(accountId).orElseThrow();
        assertEquals(Set.of(organizationId), access.activeOrganizationIds());
        assertEquals(Set.of("PARKING_SCOPE_PARTNER"), access.getEffectivePermissionCodes());
    }

    @Test
    void membershipLookupFailurePropagatesInsteadOfFailingOpen() {
        UUID accountId = UUID.randomUUID();
        stubAccount(accountId, "CUSTOM_PARTNER", true, null, Set.of("PARKING_SCOPE_PARTNER"));
        when(organizationMembershipRepository.findActiveOrganizationIdsByAccountId(
                accountId, OrganizationMembershipStatus.ACTIVE))
                .thenThrow(new DataAccessResourceFailureException("Database unavailable"));
        assertThrows(DataAccessResourceFailureException.class, () -> adapter.findByAccountId(accountId));
    }

    private void stubAccount(
            UUID accountId, String roleCode, boolean roleActive, UUID profileId, Set<String> permissions) {
        UUID roleId = UUID.randomUUID();
        AccountEntity account = new AccountEntity();
        account.setAccountId(accountId);
        account.setRoleId(roleId);
        account.setUserProfileId(profileId);
        account.setStatus(AccountStatus.ACTIVE);
        RoleEntity role = new RoleEntity();
        role.setRoleId(roleId);
        role.setCode(roleCode);
        role.setIsActive(roleActive);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(rolePermissionRepository.findActivePermissionCodesByRoleId(roleId)).thenReturn(permissions);
    }
}
