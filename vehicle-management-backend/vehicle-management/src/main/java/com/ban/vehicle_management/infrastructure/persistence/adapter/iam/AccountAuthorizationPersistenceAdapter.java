package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import com.ban.vehicle_management.application.iam.account.port.out.AccountAuthorizationPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.RoleEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.EmployeeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.OrganizationMembershipRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.MemberParkingLotScopeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.RoleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.CustomerRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.EmployeeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.RolePermissionRepository;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationMembershipStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class AccountAuthorizationPersistenceAdapter implements AccountAuthorizationPortOut {

    private final AccountRepository accountRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RoleRepository roleRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final OrganizationMembershipRepository organizationMembershipRepository;
    private final MemberParkingLotScopeRepository memberParkingLotScopeRepository;

    public AccountAuthorizationPersistenceAdapter(
            AccountRepository accountRepository,
            RolePermissionRepository rolePermissionRepository,
            RoleRepository roleRepository,
            CustomerRepository customerRepository,
            EmployeeRepository employeeRepository,
            OrganizationMembershipRepository organizationMembershipRepository,
            MemberParkingLotScopeRepository memberParkingLotScopeRepository
    ) {
        this.accountRepository = accountRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.roleRepository = roleRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.organizationMembershipRepository = organizationMembershipRepository;
        this.memberParkingLotScopeRepository = memberParkingLotScopeRepository;
    }

    @Override
    public Optional<CurrentAccountAccess> findByAccountId(UUID accountId) {
        return accountRepository.findById(accountId)
                .map(this::toCurrentAccountAccess);
    }

    @Override
    public Optional<CurrentAccountAccess> findByKeycloakUserId(String keycloakUserId) {
        return accountRepository.findByKeycloakUserId(keycloakUserId)
                .map(this::toCurrentAccountAccess);
    }

    @Override
    public Optional<UUID> findCreatedByAccountId(UUID accountId) {
        return accountRepository.findById(accountId)
                .map(AccountEntity::getCreatedBy);
    }

    @Override
    public Set<String> findPermissionCodesByAccountId(UUID accountId) {
        return accountRepository.findById(accountId)
                .map(AccountEntity::getRoleId)
                .map(rolePermissionRepository::findActivePermissionCodesByRoleId)
                .orElseGet(Set::of);
    }

    private CurrentAccountAccess toCurrentAccountAccess(AccountEntity accountEntity) {
        Set<String> permissionCodes = rolePermissionRepository
                .findActivePermissionCodesByRoleId(accountEntity.getRoleId());
        RoleEntity role = roleRepository.findById(accountEntity.getRoleId()).orElse(null);
        String roleCode = role == null ? null : role.getCode();
        Set<UUID> activeOrganizationIds = permissionCodes.stream()
                .anyMatch(this::requiresOrganizationScope)
                ? safeSet(organizationMembershipRepository.findActiveOrganizationIdsByAccountId(
                        accountEntity.getAccountId(),
                        OrganizationMembershipStatus.ACTIVE
                ))
                : Set.of();
        Set<UUID> assignedParkingLotIds = permissionCodes.contains("PARKING_SCOPE_ASSIGNED")
                ? safeSet(memberParkingLotScopeRepository.findActiveParkingLotIdsByAccountId(
                        accountEntity.getAccountId(),
                        OrganizationMembershipStatus.ACTIVE
                ))
                : Set.of();
        EmployeeEntity employee = accountEntity.getUserProfileId() == null ? null
                : employeeRepository.findByUserProfileId(accountEntity.getUserProfileId()).orElse(null);
        CustomerEntity customer = accountEntity.getUserProfileId() == null ? null
                : customerRepository.findByUserProfileId(accountEntity.getUserProfileId()).orElse(null);
        boolean onboardingRequired = resolveOnboardingRequired(
                roleCode,
                accountEntity.getUserProfileId() != null,
                employee != null,
                customer != null,
                !activeOrganizationIds.isEmpty()
        );

        return new CurrentAccountAccess(
                accountEntity.getAccountId(),
                accountEntity.getKeycloakUserId(),
                accountEntity.getUsername(),
                accountEntity.getEmail(),
                accountEntity.getRoleId(),
                roleCode,
                role != null && Boolean.TRUE.equals(role.getIsActive()),
                !activeOrganizationIds.isEmpty(),
                activeOrganizationIds,
                assignedParkingLotIds,
                accountEntity.getStatus(),
                employee == null ? null : employee.getStatus(),
                customer == null ? null : customer.getStatus(),
                customer == null ? null : customer.getApprovalStatus(),
                onboardingRequired,
                permissionCodes
        );
    }

    private boolean resolveOnboardingRequired(
            String roleCode,
            boolean profilePresent,
            boolean employeePresent,
            boolean customerPresent,
            boolean activeOrganizationMembership
    ) {
        if (!profilePresent) {
            return true;
        }
        AdminProvisionableAccountRoleCode role = resolveProvisionableRole(roleCode);
        if (AdminProvisionableAccountRoleCode.CUSTOMER.equals(role)) {
            return !customerPresent;
        }
        if (AdminProvisionableAccountRoleCode.EMPLOYEE.equals(role)
                || AdminProvisionableAccountRoleCode.PARKING_MANAGER.equals(role)) {
            return !employeePresent;
        }
        if (AdminProvisionableAccountRoleCode.PARTNER_ADMIN.equals(role)) {
            return !activeOrganizationMembership;
        }
        return false;
    }

    private AdminProvisionableAccountRoleCode resolveProvisionableRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return null;
        }
        try {
            return AdminProvisionableAccountRoleCode.valueOf(roleCode);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private boolean requiresOrganizationScope(String permissionCode) {
        return "PARKING_SCOPE_PARTNER".equals(permissionCode)
                || "WALLET_SCOPE_PARTNER".equals(permissionCode)
                || (permissionCode != null && permissionCode.endsWith("_PARTNER"))
                || "ORGANIZATION_PROFILE_READ_OWN".equals(permissionCode)
                || "ORGANIZATION_PROFILE_UPDATE_REQUEST_OWN".equals(permissionCode);
    }

    private <T> Set<T> safeSet(Set<T> values) {
        return values == null ? Set.of() : Set.copyOf(values);
    }

}
