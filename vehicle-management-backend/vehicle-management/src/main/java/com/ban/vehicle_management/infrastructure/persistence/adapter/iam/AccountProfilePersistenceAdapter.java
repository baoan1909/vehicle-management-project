package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import com.ban.vehicle_management.application.iam.account.port.out.AccountProfilePortOut;
import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.domain.iam.account.model.AccountProfileState;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.infrastructure.mapper.iam.AccountPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.people.CustomerPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.people.EmployeePersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.people.UserProfilePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.EmployeeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.RolePermissionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.RoleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.CustomerRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.EmployeeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.UserProfileRepository;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.IdentifierGenerationUtils;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AccountProfilePersistenceAdapter implements AccountProfilePortOut {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserProfileRepository userProfileRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final AccountPersistenceMapper accountPersistenceMapper;
    private final UserProfilePersistenceMapper userProfilePersistenceMapper;
    private final CustomerPersistenceMapper customerPersistenceMapper;
    private final EmployeePersistenceMapper employeePersistenceMapper;

    public AccountProfilePersistenceAdapter(
            AccountRepository accountRepository,
            RoleRepository roleRepository,
            RolePermissionRepository rolePermissionRepository,
            UserProfileRepository userProfileRepository,
            CustomerRepository customerRepository,
            EmployeeRepository employeeRepository,
            AccountPersistenceMapper accountPersistenceMapper,
            UserProfilePersistenceMapper userProfilePersistenceMapper,
            CustomerPersistenceMapper customerPersistenceMapper,
            EmployeePersistenceMapper employeePersistenceMapper
    ) {
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userProfileRepository = userProfileRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.accountPersistenceMapper = accountPersistenceMapper;
        this.userProfilePersistenceMapper = userProfilePersistenceMapper;
        this.customerPersistenceMapper = customerPersistenceMapper;
        this.employeePersistenceMapper = employeePersistenceMapper;
    }

    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        return userProfileRepository.existsByPhoneNumber(phoneNumber);
    }

    @Override
    public boolean existsByIdentifyCard(String identifyCard) {
        return userProfileRepository.existsByIdentifyCard(identifyCard);
    }

    @Override
    public boolean existsByPhoneNumberAndUserProfileIdNot(String phoneNumber, UUID userProfileId) {
        return userProfileRepository.existsByPhoneNumberAndUserProfileIdNot(phoneNumber, userProfileId);
    }

    @Override
    public boolean existsByIdentifyCardAndUserProfileIdNot(String identifyCard, UUID userProfileId) {
        return userProfileRepository.existsByIdentifyCardAndUserProfileIdNot(identifyCard, userProfileId);
    }

    @Override
    public Optional<AccountProfileState> findProfileStateByAccountId(UUID accountId) {
        return accountRepository.findById(accountId)
                .map(this::toProfileState);
    }

    @Override
    public Account completeProfileOnly(UUID accountId, UserProfile userProfile) {
        AccountEntity accountEntity = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account does not exist"));

        saveUserProfile(userProfile);
        accountEntity.setUserProfileId(userProfile.getUserProfileId());

        return accountPersistenceMapper.toDomain(accountRepository.save(accountEntity));
    }

    @Override
    public Account completeProfile(UUID accountId, UserProfile userProfile, Customer customer) {
        AccountEntity accountEntity = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account does not exist"));

        saveUserProfile(userProfile);

        customer.setCustomerCode(IdentifierGenerationUtils.generateCustomerCode(customer.getCustomerId()));
        customerRepository.save(customerPersistenceMapper.toEntity(customer));

        accountEntity.setUserProfileId(userProfile.getUserProfileId());
        accountEntity.setStatus(AccountStatus.ACTIVE);

        return accountPersistenceMapper.toDomain(accountRepository.save(accountEntity));
    }

    @Override
    public Account completeInternalProfile(UUID accountId, UserProfile userProfile, Employee employee) {
        AccountEntity accountEntity = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account does not exist"));

        saveUserProfile(userProfile);
        employeeRepository.save(employeePersistenceMapper.toEntity(employee));

        accountEntity.setUserProfileId(userProfile.getUserProfileId());
        return accountPersistenceMapper.toDomain(accountRepository.save(accountEntity));
    }

    @Override
    public AccountProfileState updateProfile(UUID accountId, UserProfile userProfile) {
        AccountEntity accountEntity = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account does not exist"));

        UUID userProfileId = accountEntity.getUserProfileId();
        if (userProfileId == null) {
            throw new NotFoundException("User profile does not exist");
        }

        UserProfileEntity existingUserProfileEntity = userProfileRepository.findById(userProfileId)
                .orElseThrow(() -> new NotFoundException("User profile does not exist"));

        existingUserProfileEntity.setFullName(userProfile.getFullName());
        existingUserProfileEntity.setPhoneNumber(userProfile.getPhoneNumber());
        existingUserProfileEntity.setDateOfBirth(userProfile.getDateOfBirth());
        existingUserProfileEntity.setGender(userProfile.getGender());
        existingUserProfileEntity.setAddress(userProfile.getAddress());
        existingUserProfileEntity.setIdentifyCard(userProfile.getIdentifyCard());
        existingUserProfileEntity.setStatus(userProfile.getStatus());

        userProfileRepository.save(existingUserProfileEntity);
        return toProfileState(accountEntity);
    }

    private void saveUserProfile(UserProfile userProfile) {
        UserProfileEntity userProfileEntity = userProfileRepository.findById(userProfile.getUserProfileId())
                .map(existingUserProfileEntity -> applyUserProfileChanges(existingUserProfileEntity, userProfile))
                .orElseGet(() -> userProfilePersistenceMapper.toEntity(userProfile));
        userProfileRepository.save(userProfileEntity);
    }

    private UserProfileEntity applyUserProfileChanges(
            UserProfileEntity userProfileEntity,
            UserProfile userProfile
    ) {
        userProfileEntity.setFullName(userProfile.getFullName());
        userProfileEntity.setPhoneNumber(userProfile.getPhoneNumber());
        userProfileEntity.setDateOfBirth(userProfile.getDateOfBirth());
        userProfileEntity.setGender(userProfile.getGender());
        userProfileEntity.setAddress(userProfile.getAddress());
        userProfileEntity.setIdentifyCard(userProfile.getIdentifyCard());
        userProfileEntity.setStatus(userProfile.getStatus());
        return userProfileEntity;
    }

    private AccountProfileState toProfileState(AccountEntity accountEntity) {
        UUID userProfileId = accountEntity.getUserProfileId();
        UserProfileEntity userProfileEntity = resolveUserProfile(userProfileId);
        EmployeeEntity employeeEntity = resolveEmployee(userProfileId);
        CustomerEntity customerEntity = resolveCustomer(userProfileId);
        String roleCode = resolveRoleCode(accountEntity.getRoleId());

        return new AccountProfileState(
                accountEntity.getAccountId(),
                accountEntity.getUsername(),
                accountEntity.getEmail(),
                accountEntity.getKeycloakUserId(),
                roleCode,
                userProfileId,
                userProfileEntity == null ? null : userProfileEntity.getFullName(),
                userProfileEntity == null ? null : userProfileEntity.getDateOfBirth(),
                userProfileEntity == null ? null : userProfileEntity.getGender(),
                userProfileEntity == null ? null : userProfileEntity.getPhoneNumber(),
                userProfileEntity == null ? null : userProfileEntity.getAddress(),
                userProfileEntity == null ? null : userProfileEntity.getIdentifyCard(),
                null,
                userProfileEntity == null ? null : userProfileEntity.getStatus(),
                employeeEntity == null ? null : employeeEntity.getEmployeeId(),
                employeeEntity == null ? null : employeeEntity.getEmployeeCode(),
                employeeEntity == null ? null : employeeEntity.getJobTitle(),
                employeeEntity == null ? null : employeeEntity.getHiredAt(),
                employeeEntity == null ? null : employeeEntity.getStatus(),
                customerEntity == null ? null : customerEntity.getCustomerId(),
                customerEntity == null ? null : customerEntity.getCustomerCode(),
                customerEntity == null ? null : customerEntity.getCustomerType(),
                customerEntity == null ? null : customerEntity.getStatus(),
                customerEntity == null ? null : customerEntity.getApprovalStatus(),
                accountEntity.getStatus(),
                resolveEffectivePermissionCodes(accountEntity, roleCode, employeeEntity)
        );
    }

    private String resolveRoleCode(UUID roleId) {
        if (roleId == null) {
            return null;
        }

        return roleRepository.findById(roleId)
                .map(role -> role.getCode())
                .orElse(null);
    }

    private UserProfileEntity resolveUserProfile(UUID userProfileId) {
        if (userProfileId == null) {
            return null;
        }
        return userProfileRepository.findById(userProfileId).orElse(null);
    }

    private CustomerEntity resolveCustomer(UUID userProfileId) {
        if (userProfileId == null) {
            return null;
        }
        return customerRepository.findByUserProfileId(userProfileId).orElse(null);
    }

    private EmployeeEntity resolveEmployee(UUID userProfileId) {
        if (userProfileId == null) {
            return null;
        }
        return employeeRepository.findByUserProfileId(userProfileId).orElse(null);
    }

    private List<String> resolveEffectivePermissionCodes(
            AccountEntity accountEntity,
            String roleCode,
            EmployeeEntity employeeEntity
    ) {
        if (accountEntity.getRoleId() == null || !canUseBusinessPermissions(accountEntity, roleCode, employeeEntity)) {
            return List.of();
        }

        return rolePermissionRepository.findActivePermissionCodesByRoleId(accountEntity.getRoleId())
                .stream()
                .sorted()
                .toList();
    }

    private boolean canUseBusinessPermissions(
            AccountEntity accountEntity,
            String roleCode,
            EmployeeEntity employeeEntity
    ) {
        if (!AccountStatus.ACTIVE.equals(accountEntity.getStatus())) {
            return false;
        }

        AdminProvisionableAccountRoleCode provisionableRole = resolveProvisionableRole(roleCode);
        if (provisionableRole == null || !provisionableRole.requiresEmployeeRecord()) {
            return true;
        }

        return employeeEntity != null && EmployeeStatus.ACTIVE.equals(employeeEntity.getStatus());
    }

    private AdminProvisionableAccountRoleCode resolveProvisionableRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return null;
        }

        try {
            return AdminProvisionableAccountRoleCode.valueOf(roleCode);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
