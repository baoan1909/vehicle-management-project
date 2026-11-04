package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import com.ban.vehicle_management.application.iam.account.model.command.ProvisionedAccountFilterCommand;
import com.ban.vehicle_management.application.iam.account.model.result.ProvisionedAccountResult;
import com.ban.vehicle_management.application.iam.account.port.out.ProvisionedAccountPortOut;
import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.domain.iam.account.model.AccountStatusHistory;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.iam.account.policy.CurrentAccessStatePolicy;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.infrastructure.mapper.iam.AccountPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.iam.AccountStatusHistoryPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.iam.ProvisionedAccountReadModelMapper;
import com.ban.vehicle_management.infrastructure.mapper.people.UserProfilePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountStatusHistoryEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.EmployeeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository.ProvisionedAccountLifecycleProjection;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountStatusHistoryRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.RoleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.CustomerRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.EmployeeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.UserProfileRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.iam.ProvisionedAccountSpecifications;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class ProvisionedAccountPersistenceAdapter implements ProvisionedAccountPortOut {

    private static final CurrentAccessStatePolicy CURRENT_ACCESS_STATE_POLICY = new CurrentAccessStatePolicy();

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final UserProfileRepository userProfileRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final AccountStatusHistoryRepository accountStatusHistoryRepository;
    private final AccountPersistenceMapper accountPersistenceMapper;
    private final AccountStatusHistoryPersistenceMapper accountStatusHistoryPersistenceMapper;
    private final ProvisionedAccountReadModelMapper provisionedAccountReadModelMapper;
    private final UserProfilePersistenceMapper userProfilePersistenceMapper;
    private final EntityManager entityManager;

    public ProvisionedAccountPersistenceAdapter(
            AccountRepository accountRepository,
            RoleRepository roleRepository,
            UserProfileRepository userProfileRepository,
            CustomerRepository customerRepository,
            EmployeeRepository employeeRepository,
            AccountStatusHistoryRepository accountStatusHistoryRepository,
            AccountPersistenceMapper accountPersistenceMapper,
            AccountStatusHistoryPersistenceMapper accountStatusHistoryPersistenceMapper,
            ProvisionedAccountReadModelMapper provisionedAccountReadModelMapper,
            UserProfilePersistenceMapper userProfilePersistenceMapper,
            EntityManager entityManager
    ) {
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
        this.userProfileRepository = userProfileRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.accountStatusHistoryRepository = accountStatusHistoryRepository;
        this.accountPersistenceMapper = accountPersistenceMapper;
        this.accountStatusHistoryPersistenceMapper = accountStatusHistoryPersistenceMapper;
        this.provisionedAccountReadModelMapper = provisionedAccountReadModelMapper;
        this.userProfilePersistenceMapper = userProfilePersistenceMapper;
        this.entityManager = entityManager;
    }

    @Override
    public boolean existsByUsername(String username) {
        return accountRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return accountRepository.existsByEmail(email);
    }

    @Override
    public UUID findActiveRoleIdByCode(AdminProvisionableAccountRoleCode roleCode) {
        return roleRepository.findByCodeAndIsActiveTrue(roleCode.name())
                .orElseThrow(() -> new NotFoundException("Role " + roleCode.name() + " is not configured"))
                .getRoleId();
    }

    @Override
    public void provisionAccount(Account account, UserProfile userProfile) {
        userProfileRepository.save(userProfilePersistenceMapper.toEntity(userProfile));
        accountRepository.save(accountPersistenceMapper.toEntity(account));
        flushAndClear();
    }

    @Override
    public List<ProvisionedAccountResult> findProvisionedAccounts(ProvisionedAccountFilterCommand command) {
        Specification<AccountEntity> specification = ProvisionedAccountSpecifications.withFilters(command);
        List<AccountEntity> entities = accountRepository.findAll(specification);
        Map<UUID, ProvisionedAccountLifecycleProjection> lifecycleByAccountId = loadLifecycle(entities);
        return entities.stream()
                .map(entity -> withLifecycle(
                        provisionedAccountReadModelMapper.toSummaryResult(entity),
                        entity,
                        lifecycleByAccountId.get(entity.getAccountId())))
                .toList();
    }

    @Override
    public Optional<ProvisionedAccountResult> findProvisionedAccountById(UUID accountId) {
        return accountRepository.findById(accountId)
                .filter(entity -> entity.getStatus() != AccountStatus.DELETED)
                .map(entity -> withLifecycle(
                        provisionedAccountReadModelMapper.toResult(entity),
                        entity,
                        loadLifecycle(List.of(entity)).get(entity.getAccountId())));
    }

    private Map<UUID, ProvisionedAccountLifecycleProjection> loadLifecycle(List<AccountEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Map.of();
        }
        return accountRepository.findProvisionedAccountLifecycleByAccountIds(
                        entities.stream().map(AccountEntity::getAccountId).toList())
                .stream()
                .collect(Collectors.toUnmodifiableMap(
                        ProvisionedAccountLifecycleProjection::getAccountId,
                        Function.identity()));
    }

    private ProvisionedAccountResult withLifecycle(
            ProvisionedAccountResult result,
            AccountEntity entity,
            ProvisionedAccountLifecycleProjection projection
    ) {
        if (result == null || entity == null) {
            return result;
        }
        boolean hasActiveOrganization = projection != null
                && Boolean.TRUE.equals(projection.getHasActiveOrganizationMembership());
        boolean hasActiveParkingLotScope = projection != null
                && Boolean.TRUE.equals(projection.getHasActiveParkingLotScope());
        EmployeeStatus employeeStatus = enumValue(
                projection == null ? null : projection.getEmployeeStatus(), EmployeeStatus.class);
        CustomerStatus customerStatus = enumValue(
                projection == null ? null : projection.getCustomerStatus(), CustomerStatus.class);
        CustomerApprovalStatus customerApprovalStatus = enumValue(
                projection == null ? null : projection.getCustomerApprovalStatus(), CustomerApprovalStatus.class);
        ApprovalRequestStatus internalApprovalStatus = enumValue(
                projection == null ? null : projection.getInternalApprovalStatus(), ApprovalRequestStatus.class);

        String roleCode = result.role() == null ? null : result.role().roleCode();
        Set<String> assignedPermissions = result.role() == null || result.role().permissionCodes() == null
                ? Set.of()
                : Set.copyOf(result.role().permissionCodes());
        boolean onboardingRequired = resolveOnboardingRequired(
                roleCode,
                entity.getUserProfileId() != null,
                employeeStatus != null,
                customerStatus != null,
                hasActiveOrganization);
        CurrentAccountAccess access = new CurrentAccountAccess(
                entity.getAccountId(),
                entity.getKeycloakUserId(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getRoleId(),
                roleCode,
                entity.getRole() != null && Boolean.TRUE.equals(entity.getRole().getIsActive()),
                hasActiveOrganization,
                hasActiveOrganization ? Set.of(entity.getAccountId()) : Set.of(),
                hasActiveParkingLotScope ? Set.of(entity.getAccountId()) : Set.of(),
                entity.getStatus(),
                employeeStatus,
                customerStatus,
                customerApprovalStatus,
                onboardingRequired,
                internalApprovalStatus,
                assignedPermissions);
        CurrentAccessStatePolicy.Decision decision = CURRENT_ACCESS_STATE_POLICY.resolve(access);
        String approvalState = resolveApprovalState(
                roleCode, projection, employeeStatus, customerApprovalStatus, hasActiveOrganization);

        return new ProvisionedAccountResult(
                result.account(),
                result.role(),
                new ProvisionedAccountResult.LifecycleInfoResult(
                        onboardingRequired ? "PROFILE_REQUIRED" : "PROFILE_COMPLETED",
                        approvalState,
                        employeeStatus == null ? "NOT_CREATED" : employeeStatus.name(),
                        resolveScopeState(assignedPermissions, hasActiveOrganization, hasActiveParkingLotScope),
                        decision.state().name(),
                        decision.requiredAction() == null ? null : decision.requiredAction().name(),
                        access.getEffectivePermissionCodes().size()));
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

    private String resolveApprovalState(
            String roleCode,
            ProvisionedAccountLifecycleProjection projection,
            EmployeeStatus employeeStatus,
            CustomerApprovalStatus customerApprovalStatus,
            boolean hasActiveOrganization
    ) {
        if (customerApprovalStatus != null) {
            return customerApprovalStatus.name();
        }
        String internalApproval = projection == null ? null : projection.getInternalApprovalStatus();
        if (internalApproval != null && !internalApproval.isBlank()) {
            return internalApproval;
        }
        String partnerApproval = projection == null ? null : projection.getPartnerApprovalStatus();
        if (partnerApproval != null && !partnerApproval.isBlank()) {
            return partnerApproval;
        }
        AdminProvisionableAccountRoleCode role = resolveProvisionableRole(roleCode);
        if ((AdminProvisionableAccountRoleCode.EMPLOYEE.equals(role)
                || AdminProvisionableAccountRoleCode.PARKING_MANAGER.equals(role))
                && EmployeeStatus.ACTIVE.equals(employeeStatus)) {
            // Legacy internal accounts may predate approval requests. Only an
            // active workforce record is sufficient evidence of prior approval;
            // organization membership alone is not an onboarding decision.
            return "APPROVED";
        }
        if (AdminProvisionableAccountRoleCode.PARTNER_ADMIN.equals(role) && hasActiveOrganization) {
            // Legacy Partner Admin accounts may predate partner-registration
            // requests. Their active organization is the compatible approval proof.
            return "APPROVED";
        }
        return "NOT_SUBMITTED";
    }

    private String resolveScopeState(
            Set<String> assignedPermissions,
            boolean hasActiveOrganization,
            boolean hasActiveParkingLotScope
    ) {
        if (assignedPermissions.contains("PARKING_SCOPE_ASSIGNED")) {
            if (!hasActiveOrganization) {
                return "ORGANIZATION_REQUIRED";
            }
            return hasActiveParkingLotScope ? "SCOPE_READY" : "PARKING_LOT_ASSIGNMENT_REQUIRED";
        }
        boolean requiresOrganization = assignedPermissions.stream().anyMatch(permission ->
                "PARKING_SCOPE_PARTNER".equals(permission)
                        || "WALLET_SCOPE_PARTNER".equals(permission)
                        || (permission != null && permission.endsWith("_PARTNER")));
        return requiresOrganization && !hasActiveOrganization
                ? "ORGANIZATION_REQUIRED"
                : "SCOPE_READY";
    }

    private <E extends Enum<E>> E enumValue(String value, Class<E> enumType) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @Override
    public void updateProvisionedAccountStatus(
            UUID accountId,
            AccountStatus accountStatus,
            UserProfileStatus userProfileStatus,
            CustomerStatus customerStatus,
            EmployeeStatus employeeStatus,
            UUID changedBy,
            String reason
    ) {
        AccountEntity accountEntity = accountRepository.findById(accountId)
                .filter(entity -> entity.getStatus() != AccountStatus.DELETED)
                .orElseThrow(() -> new NotFoundException("Provisioned account not found"));
        UserProfileEntity userProfileEntity = resolveUserProfile(accountEntity.getUserProfileId());
        CustomerEntity customerEntity = resolveCustomer(accountEntity.getUserProfileId());
        EmployeeEntity employeeEntity = resolveEmployee(accountEntity.getUserProfileId());

        AccountStatus previousStatus = accountEntity.getStatus();
        accountEntity.setStatus(accountStatus);
        if (userProfileEntity != null) {
            userProfileEntity.setStatus(userProfileStatus);
        }
        if (customerEntity != null) {
            customerEntity.setStatus(customerStatus);
        }
        if (employeeEntity != null && employeeStatus != null) {
            employeeEntity.setStatus(employeeStatus);
        }

        if (!previousStatus.equals(accountStatus)) {
            AccountStatusHistory statusHistory = new AccountStatusHistory(
                    UUID.randomUUID(),
                    accountId,
                    previousStatus,
                    accountStatus,
                    reason,
                    Instant.now(),
                    changedBy
            );
            AccountStatusHistoryEntity statusHistoryEntity =
                    accountStatusHistoryPersistenceMapper.toEntity(statusHistory);
            accountStatusHistoryRepository.save(statusHistoryEntity);
        }

        accountRepository.save(accountEntity);
        if (userProfileEntity != null) {
            userProfileRepository.save(userProfileEntity);
        }
        if (customerEntity != null) {
            customerRepository.save(customerEntity);
        }
        if (employeeEntity != null && employeeStatus != null) {
            employeeRepository.save(employeeEntity);
        }
        flushAndClear();
    }

    @Override
    public void updateProvisionedAccountRole(UUID accountId, UUID roleId) {
        AccountEntity accountEntity = accountRepository.findById(accountId)
                .filter(entity -> entity.getStatus() != AccountStatus.DELETED)
                .orElseThrow(() -> new NotFoundException("Provisioned account not found"));
        accountEntity.setRoleId(roleId);
        accountRepository.save(accountEntity);
        flushAndClear();
    }

    private void flushAndClear() {
        accountRepository.flush();
        entityManager.clear();
    }

    private UserProfileEntity resolveUserProfile(UUID userProfileId) {
        if (userProfileId == null) {
            return null;
        }
        return userProfileRepository.findById(userProfileId).orElse(null);
    }

    private EmployeeEntity resolveEmployee(UUID userProfileId) {
        if (userProfileId == null) {
            return null;
        }
        return employeeRepository.findByUserProfileId(userProfileId).orElse(null);
    }

    private CustomerEntity resolveCustomer(UUID userProfileId) {
        if (userProfileId == null) {
            return null;
        }
        return customerRepository.findByUserProfileId(userProfileId).orElse(null);
    }
}
