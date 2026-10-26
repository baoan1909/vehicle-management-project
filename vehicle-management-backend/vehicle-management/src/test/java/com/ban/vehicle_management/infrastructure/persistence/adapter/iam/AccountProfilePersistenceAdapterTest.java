package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.domain.iam.account.model.AccountProfileState;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.infrastructure.mapper.iam.AccountPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.people.CustomerPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.people.EmployeePersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.people.UserProfilePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.RoleEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.EmployeeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.OrganizationMembershipRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.MemberParkingLotScopeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.OrganizationRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.RolePermissionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.RoleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ApprovalRequestRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.CustomerRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.EmployeeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.UserProfileRepository;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountProfilePersistenceAdapterTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ApprovalRequestRepository approvalRequestRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationMembershipRepository organizationMembershipRepository;

    @Mock
    private MemberParkingLotScopeRepository memberParkingLotScopeRepository;

    @Mock
    private AccountPersistenceMapper accountPersistenceMapper;

    @Mock
    private UserProfilePersistenceMapper userProfilePersistenceMapper;

    @Mock
    private CustomerPersistenceMapper customerPersistenceMapper;

    @Mock
    private EmployeePersistenceMapper employeePersistenceMapper;

    @InjectMocks
    private AccountProfilePersistenceAdapter adapter;

    @ParameterizedTest
    @MethodSource("permissionParityStates")
    void onboardingPermissionsShouldMatchCurrentAccessForSameLifecycleState(
            String roleCode,
            boolean roleActive,
            boolean hasActivePartnerOrganizationMembership,
            AccountStatus accountStatus,
            EmployeeStatus employeeStatus,
            CustomerStatus customerStatus,
            CustomerApprovalStatus customerApprovalStatus
    ) {
        UUID accountId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        Set<String> assignedPermissions = Set.of("ROLE_UPDATE_ALL", "ACCOUNT_READ_ALL");

        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setAccountId(accountId);
        accountEntity.setRoleId(roleId);
        accountEntity.setUserProfileId(userProfileId);
        accountEntity.setStatus(accountStatus);

        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setRoleId(roleId);
        roleEntity.setCode(roleCode);
        roleEntity.setIsActive(roleActive);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(accountEntity));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(roleEntity));

        if (employeeStatus != null) {
            EmployeeEntity employeeEntity = new EmployeeEntity();
            employeeEntity.setEmployeeId(UUID.randomUUID());
            employeeEntity.setUserProfileId(userProfileId);
            employeeEntity.setStatus(employeeStatus);
            when(employeeRepository.findByUserProfileId(userProfileId)).thenReturn(Optional.of(employeeEntity));
        }
        if (customerStatus != null || customerApprovalStatus != null) {
            CustomerEntity customerEntity = new CustomerEntity();
            customerEntity.setCustomerId(UUID.randomUUID());
            customerEntity.setUserProfileId(userProfileId);
            customerEntity.setStatus(customerStatus);
            customerEntity.setApprovalStatus(customerApprovalStatus);
            when(customerRepository.findByUserProfileId(userProfileId)).thenReturn(Optional.of(customerEntity));
        }
        CurrentAccountAccess currentAccess = new CurrentAccountAccess(
                accountId,
                "subject",
                "user",
                "user@example.com",
                roleId,
                roleCode,
                roleActive,
                hasActivePartnerOrganizationMembership,
                accountStatus,
                employeeStatus,
                customerStatus,
                customerApprovalStatus,
                assignedPermissions
        );
        if (currentAccess.canUseBusinessPermissions()) {
            when(rolePermissionRepository.findActivePermissionCodesByRoleId(roleId))
                    .thenReturn(assignedPermissions);
        }

        AccountProfileState onboarding = adapter.findProfileStateByAccountId(accountId).orElseThrow();

        assertEquals(List.copyOf(currentAccess.getEffectivePermissionCodes()), onboarding.permissionCodes());
    }

    @Test
    void completeProfileShouldUpdateExistingUserProfileWithoutReplacingAuditFields() {
        UUID accountId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-06-20T10:00:00Z");

        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setAccountId(accountId);
        accountEntity.setUserProfileId(userProfileId);
        accountEntity.setStatus(AccountStatus.PENDING);

        UserProfileEntity existingUserProfileEntity = new UserProfileEntity();
        existingUserProfileEntity.setUserProfileId(userProfileId);
        existingUserProfileEntity.setFullName("Old Name");
        existingUserProfileEntity.setCreatedAt(createdAt);
        existingUserProfileEntity.setStatus(UserProfileStatus.ACTIVE);

        UserProfile userProfile = new UserProfile();
        userProfile.setUserProfileId(userProfileId);
        userProfile.setFullName("New Name");
        userProfile.setPhoneNumber("0901002003");
        userProfile.setDateOfBirth(LocalDate.of(1998, 3, 15));
        userProfile.setGender("MALE");
        userProfile.setAddressDisplay("25 Nguyễn Đỗ Cung, Phường Tây Thạnh, Quận Tân Phú, Thành phố Hồ Chí Minh");
        userProfile.setAddressDetail("25 Nguyễn Đỗ Cung");
        userProfile.setProvinceCode("79");
        userProfile.setDistrictCode("760");
        userProfile.setWardCode("26740");
        userProfile.setAddressDisplay("25 Nguyễn Đỗ Cung, Phường Tây Thạnh, Quận Tân Phú, Thành phố Hồ Chí Minh");
        userProfile.setIdentifyCard("079100200003");
        userProfile.setStatus(UserProfileStatus.ACTIVE);

        Customer customer = new Customer();
        customer.setCustomerId(UUID.randomUUID());
        customer.setUserProfileId(userProfileId);

        CustomerEntity customerEntity = new CustomerEntity();
        Account account = new Account();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(accountEntity));
        when(userProfileRepository.findById(userProfileId)).thenReturn(Optional.of(existingUserProfileEntity));
        when(customerPersistenceMapper.toEntity(customer)).thenReturn(customerEntity);
        when(accountRepository.save(accountEntity)).thenReturn(accountEntity);
        when(accountPersistenceMapper.toDomain(accountEntity)).thenReturn(account);

        adapter.completeProfile(accountId, userProfile, customer);

        verify(userProfileRepository).save(existingUserProfileEntity);
        verify(userProfilePersistenceMapper, never()).toEntity(any());
        verify(customerRepository).save(customerEntity);
        assertSame(createdAt, existingUserProfileEntity.getCreatedAt());
        assertEquals("New Name", existingUserProfileEntity.getFullName());
        assertEquals("0901002003", existingUserProfileEntity.getPhoneNumber());
        assertEquals("25 Nguyễn Đỗ Cung", existingUserProfileEntity.getAddressDetail());
        assertEquals("79", existingUserProfileEntity.getProvinceCode());
        assertEquals("760", existingUserProfileEntity.getDistrictCode());
        assertEquals("26740", existingUserProfileEntity.getWardCode());
        assertEquals("25 Nguyễn Đỗ Cung, Phường Tây Thạnh, Quận Tân Phú, Thành phố Hồ Chí Minh",
                existingUserProfileEntity.getAddressDisplay());
        assertEquals(AccountStatus.ACTIVE, accountEntity.getStatus());
    }

    @Test
    void updateProfileShouldPersistAndReturnLegacyStructuredAddress() {
        UUID accountId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();

        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setAccountId(accountId);
        accountEntity.setUserProfileId(userProfileId);
        accountEntity.setStatus(AccountStatus.PENDING);

        UserProfileEntity existingUserProfileEntity = new UserProfileEntity();
        existingUserProfileEntity.setUserProfileId(userProfileId);
        existingUserProfileEntity.setStatus(UserProfileStatus.ACTIVE);

        UserProfile userProfile = new UserProfile();
        userProfile.setUserProfileId(userProfileId);
        userProfile.setFullName("Nguyễn Văn A");
        userProfile.setPhoneNumber("0384679271");
        userProfile.setAddressDisplay("25 Nguyễn Đỗ Cung, Phường Tây Thạnh, Quận Tân Phú, Thành phố Hồ Chí Minh");
        userProfile.setAddressDetail("25 Nguyễn Đỗ Cung");
        userProfile.setProvinceCode("79");
        userProfile.setDistrictCode("760");
        userProfile.setWardCode("26740");
        userProfile.setAddressDisplay("25 Nguyễn Đỗ Cung, Phường Tây Thạnh, Quận Tân Phú, Thành phố Hồ Chí Minh");
        userProfile.setStatus(UserProfileStatus.ACTIVE);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(accountEntity));
        when(userProfileRepository.findById(userProfileId))
                .thenReturn(Optional.of(existingUserProfileEntity));
        when(customerRepository.findByUserProfileId(userProfileId)).thenReturn(Optional.empty());
        when(employeeRepository.findByUserProfileId(userProfileId)).thenReturn(Optional.empty());

        AccountProfileState result = adapter.updateProfile(accountId, userProfile);

        verify(userProfileRepository).save(existingUserProfileEntity);
        assertEquals("25 Nguyễn Đỗ Cung", existingUserProfileEntity.getAddressDetail());
        assertEquals("79", existingUserProfileEntity.getProvinceCode());
        assertEquals("760", existingUserProfileEntity.getDistrictCode());
        assertEquals("26740", existingUserProfileEntity.getWardCode());
        assertEquals("79", result.provinceCode());
        assertEquals("760", result.districtCode());
        assertEquals("26740", result.wardCode());
    }

    private static java.util.stream.Stream<Arguments> permissionParityStates() {
        return java.util.stream.Stream.of(
                Arguments.of("SYSTEM_ADMIN", true, false, AccountStatus.ACTIVE, null, null, null),
                Arguments.of("SYSTEM_ADMIN", false, false, AccountStatus.ACTIVE, null, null, null),
                Arguments.of("PARTNER_ADMIN", true, true, AccountStatus.ACTIVE, null, null, null),
                Arguments.of("PARTNER_ADMIN", true, false, AccountStatus.ACTIVE, null, null, null),
                Arguments.of("PARTNER_ADMIN", true, true, AccountStatus.PENDING, null, null, null),
                Arguments.of("PARKING_MANAGER", true, false, AccountStatus.ACTIVE,
                        EmployeeStatus.ACTIVE, null, null),
                Arguments.of("PARKING_MANAGER", true, true, AccountStatus.ACTIVE,
                        EmployeeStatus.SUSPENDED, null, null),
                Arguments.of("EMPLOYEE", true, false, AccountStatus.ACTIVE, EmployeeStatus.ACTIVE, null, null),
                Arguments.of("EMPLOYEE", true, true, AccountStatus.ACTIVE, EmployeeStatus.INACTIVE, null, null),
                Arguments.of("EMPLOYEE", true, true, AccountStatus.ACTIVE, null, null, null),
                Arguments.of("CUSTOMER", true, false, AccountStatus.ACTIVE, null,
                        CustomerStatus.ACTIVE, CustomerApprovalStatus.APPROVED),
                Arguments.of("CUSTOMER", true, true, AccountStatus.ACTIVE, null,
                        CustomerStatus.INACTIVE, CustomerApprovalStatus.APPROVED),
                Arguments.of("CUSTOMER", true, true, AccountStatus.ACTIVE, null,
                        CustomerStatus.ACTIVE, CustomerApprovalStatus.PENDING),
                Arguments.of("CUSTOMER", true, true, AccountStatus.ACTIVE, null,
                        CustomerStatus.ACTIVE, CustomerApprovalStatus.REJECTED),
                Arguments.of("CUSTOMER", true, true, AccountStatus.ACTIVE, null,
                        CustomerStatus.ACTIVE, CustomerApprovalStatus.SUSPENDED),
                Arguments.of("CUSTOMER", true, true, AccountStatus.ACTIVE, null, null, null),
                Arguments.of("SYSTEM_ADMIN", true, false, AccountStatus.PENDING, null, null, null),
                Arguments.of("SYSTEM_ADMIN", true, false, AccountStatus.LOCKED, null, null, null),
                Arguments.of("SYSTEM_ADMIN", true, true, AccountStatus.DISABLED, null, null, null),
                Arguments.of("CUSTOM_OPERATOR", true, false, AccountStatus.ACTIVE, null, null, null),
                Arguments.of("CUSTOM_OPERATOR", true, true, AccountStatus.ACTIVE,
                        EmployeeStatus.INACTIVE, null, null),
                Arguments.of("custom_operator", true, true, AccountStatus.ACTIVE, null, null, null)
        );
    }
}
