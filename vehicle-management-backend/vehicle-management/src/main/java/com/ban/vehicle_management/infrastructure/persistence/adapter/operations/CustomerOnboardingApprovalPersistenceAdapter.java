package com.ban.vehicle_management.infrastructure.persistence.adapter.operations;

import com.ban.vehicle_management.application.operations.approvalrequest.authorization.CustomerOnboardingApprovalAccessGuard;
import com.ban.vehicle_management.application.operations.approvalrequest.model.command.CustomerOnboardingApprovalFilterCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.CustomerOnboardingApprovalCandidate;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.CustomerOnboardingApprovalResult;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.CustomerOnboardingApprovalPortOut;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.infrastructure.mapper.operations.ApprovalRequestPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.people.CustomerPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.RoleEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ApprovalRequestEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.RoleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ApprovalRequestRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.CustomerRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.UserProfileRepository;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CustomerOnboardingApprovalPersistenceAdapter implements CustomerOnboardingApprovalPortOut {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final CustomerRepository customerRepository;
    private final UserProfileRepository userProfileRepository;
    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final ApprovalRequestPersistenceMapper approvalRequestPersistenceMapper;
    private final CustomerPersistenceMapper customerPersistenceMapper;

    public CustomerOnboardingApprovalPersistenceAdapter(
            ApprovalRequestRepository approvalRequestRepository,
            CustomerRepository customerRepository,
            UserProfileRepository userProfileRepository,
            AccountRepository accountRepository,
            RoleRepository roleRepository,
            ApprovalRequestPersistenceMapper approvalRequestPersistenceMapper,
            CustomerPersistenceMapper customerPersistenceMapper
    ) {
        this.approvalRequestRepository = approvalRequestRepository;
        this.customerRepository = customerRepository;
        this.userProfileRepository = userProfileRepository;
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
        this.approvalRequestPersistenceMapper = approvalRequestPersistenceMapper;
        this.customerPersistenceMapper = customerPersistenceMapper;
    }

    @Override
    public void saveCustomerOnboardingApprovalRequest(ApprovalRequest approvalRequest) {
        approvalRequestRepository.saveAndFlush(approvalRequestPersistenceMapper.toEntity(approvalRequest));
    }

    @Override
    public void saveCustomerOnboardingApprovalDecision(ApprovalRequest approvalRequest, Customer customer) {
        approvalRequestRepository.save(approvalRequestPersistenceMapper.toEntity(approvalRequest));
        customerRepository.saveAndFlush(customerPersistenceMapper.toEntity(customer));
    }

    @Override
    public boolean existsPendingCustomerOnboardingApprovalForCustomer(UUID customerId) {
        return approvalRequestRepository.existsByRequestTypeAndTargetSchemaAndTargetTableAndTargetIdAndStatus(
                CustomerOnboardingApprovalAccessGuard.REQUEST_TYPE,
                CustomerOnboardingApprovalAccessGuard.TARGET_SCHEMA,
                CustomerOnboardingApprovalAccessGuard.TARGET_TABLE,
                customerId,
                ApprovalRequestStatus.PENDING
        );
    }

    @Override
    public Optional<ApprovalRequest> findCustomerOnboardingApprovalRequestById(UUID approvalRequestId) {
        return approvalRequestRepository.findByApprovalRequestIdAndRequestTypeAndTargetSchemaAndTargetTable(
                        approvalRequestId,
                        CustomerOnboardingApprovalAccessGuard.REQUEST_TYPE,
                        CustomerOnboardingApprovalAccessGuard.TARGET_SCHEMA,
                        CustomerOnboardingApprovalAccessGuard.TARGET_TABLE
                )
                .map(approvalRequestPersistenceMapper::toDomain);
    }

    @Override
    public Optional<ApprovalRequest> findLatestCustomerOnboardingApprovalRequest(UUID customerId) {
        return approvalRequestRepository.findTopByRequestTypeAndTargetSchemaAndTargetTableAndTargetIdOrderByCreatedAtDesc(
                        CustomerOnboardingApprovalAccessGuard.REQUEST_TYPE,
                        CustomerOnboardingApprovalAccessGuard.TARGET_SCHEMA,
                        CustomerOnboardingApprovalAccessGuard.TARGET_TABLE,
                        customerId
                )
                .map(approvalRequestPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Customer> findCustomerById(UUID customerId) {
        return customerRepository.findById(customerId)
                .map(customerPersistenceMapper::toDomain);
    }

    @Override
    public Optional<CustomerOnboardingApprovalCandidate> findCandidateByCustomerId(UUID customerId) {
        return customerRepository.findById(customerId)
                .flatMap(this::toCandidate);
    }

    @Override
    public Optional<CustomerOnboardingApprovalCandidate> findCandidateByAccountId(UUID accountId) {
        return accountRepository.findById(accountId)
                .flatMap(this::toCandidate);
    }

    @Override
    public List<CustomerOnboardingApprovalResult> findCustomerOnboardingApprovalRequests(
            CustomerOnboardingApprovalFilterCommand command
    ) {
        List<ApprovalRequestEntity> approvalRequests = command.status() == null
                ? approvalRequestRepository.findByRequestTypeAndTargetSchemaAndTargetTableOrderByCreatedAtDesc(
                CustomerOnboardingApprovalAccessGuard.REQUEST_TYPE,
                CustomerOnboardingApprovalAccessGuard.TARGET_SCHEMA,
                CustomerOnboardingApprovalAccessGuard.TARGET_TABLE
        )
                : approvalRequestRepository.findByRequestTypeAndTargetSchemaAndTargetTableAndStatusOrderByCreatedAtDesc(
                CustomerOnboardingApprovalAccessGuard.REQUEST_TYPE,
                CustomerOnboardingApprovalAccessGuard.TARGET_SCHEMA,
                CustomerOnboardingApprovalAccessGuard.TARGET_TABLE,
                command.status()
        );

        return approvalRequests.stream()
                .map(this::toResult)
                .flatMap(Optional::stream)
                .filter(result -> matchesKeyword(command, result))
                .toList();
    }

    @Override
    public Optional<CustomerOnboardingApprovalResult> findCustomerOnboardingApprovalResultById(UUID approvalRequestId) {
        return approvalRequestRepository.findByApprovalRequestIdAndRequestTypeAndTargetSchemaAndTargetTable(
                        approvalRequestId,
                        CustomerOnboardingApprovalAccessGuard.REQUEST_TYPE,
                        CustomerOnboardingApprovalAccessGuard.TARGET_SCHEMA,
                        CustomerOnboardingApprovalAccessGuard.TARGET_TABLE
                )
                .flatMap(this::toResult);
    }

    @Override
    public Optional<CustomerOnboardingApprovalResult> findLatestCustomerOnboardingApprovalResultByAccountId(UUID accountId) {
        return findCandidateByAccountId(accountId)
                .flatMap(candidate -> approvalRequestRepository.findTopByRequestTypeAndTargetSchemaAndTargetTableAndTargetIdOrderByCreatedAtDesc(
                        CustomerOnboardingApprovalAccessGuard.REQUEST_TYPE,
                        CustomerOnboardingApprovalAccessGuard.TARGET_SCHEMA,
                        CustomerOnboardingApprovalAccessGuard.TARGET_TABLE,
                        candidate.customerId()
                ))
                .flatMap(this::toResult);
    }

    private Optional<CustomerOnboardingApprovalCandidate> toCandidate(CustomerEntity customerEntity) {
        return accountRepository.findByUserProfileId(customerEntity.getUserProfileId())
                .flatMap(accountEntity -> buildCandidate(accountEntity, customerEntity));
    }

    private Optional<CustomerOnboardingApprovalCandidate> toCandidate(AccountEntity accountEntity) {
        if (accountEntity.getUserProfileId() == null) {
            return Optional.empty();
        }
        return customerRepository.findByUserProfileId(accountEntity.getUserProfileId())
                .flatMap(customerEntity -> buildCandidate(accountEntity, customerEntity));
    }

    private Optional<CustomerOnboardingApprovalCandidate> buildCandidate(
            AccountEntity accountEntity,
            CustomerEntity customerEntity
    ) {
        return resolveRole(accountEntity.getRoleId())
                .map(roleEntity -> new CustomerOnboardingApprovalCandidate(
                        accountEntity.getAccountId(),
                        customerEntity.getUserProfileId(),
                        customerEntity.getCustomerId(),
                        roleEntity.getCode(),
                        accountEntity.getStatus(),
                        customerEntity.getStatus(),
                        customerEntity.getApprovalStatus()
                ));
    }

    private Optional<CustomerOnboardingApprovalResult> toResult(ApprovalRequestEntity approvalRequestEntity) {
        Optional<CustomerEntity> customerEntity = customerRepository.findById(approvalRequestEntity.getTargetId());
        if (customerEntity.isEmpty()) {
            return Optional.empty();
        }

        Optional<UserProfileEntity> userProfileEntity = userProfileRepository.findById(customerEntity.get().getUserProfileId());
        Optional<AccountEntity> accountEntity = accountRepository.findByUserProfileId(customerEntity.get().getUserProfileId());
        if (userProfileEntity.isEmpty() || accountEntity.isEmpty()) {
            return Optional.empty();
        }

        Optional<RoleEntity> roleEntity = resolveRole(accountEntity.get().getRoleId());
        if (roleEntity.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new CustomerOnboardingApprovalResult(
                new CustomerOnboardingApprovalResult.RequestInfoResult(
                        approvalRequestEntity.getApprovalRequestId(),
                        approvalRequestEntity.getRequestType(),
                        enumName(approvalRequestEntity.getStatus()),
                        approvalRequestEntity.getNote(),
                        approvalRequestEntity.getRequestedBy(),
                        approvalRequestEntity.getApprovedBy(),
                        approvalRequestEntity.getApprovedAt(),
                        approvalRequestEntity.getCreatedAt(),
                        approvalRequestEntity.getUpdatedAt()
                ),
                new CustomerOnboardingApprovalResult.AccountInfoResult(
                        accountEntity.get().getAccountId(),
                        accountEntity.get().getUsername(),
                        accountEntity.get().getEmail(),
                        roleEntity.get().getCode(),
                        enumName(accountEntity.get().getStatus())
                ),
                new CustomerOnboardingApprovalResult.ProfileInfoResult(
                        userProfileEntity.get().getUserProfileId(),
                        userProfileEntity.get().getFullName(),
                        userProfileEntity.get().getPhoneNumber()
                ),
                new CustomerOnboardingApprovalResult.CustomerInfoResult(
                        customerEntity.get().getCustomerId(),
                        customerEntity.get().getCustomerCode(),
                        enumName(customerEntity.get().getCustomerType()),
                        enumName(customerEntity.get().getStatus()),
                        enumName(customerEntity.get().getApprovalStatus()),
                        customerEntity.get().getApprovedBy(),
                        customerEntity.get().getApprovedAt()
                )
        ));
    }

    private Optional<RoleEntity> resolveRole(UUID roleId) {
        return roleRepository.findById(roleId);
    }

    private boolean matchesKeyword(
            CustomerOnboardingApprovalFilterCommand command,
            CustomerOnboardingApprovalResult result
    ) {
        if (command.keyword() == null || command.keyword().isBlank()) {
            return true;
        }
        String keyword = command.keyword().toLowerCase(Locale.ROOT);
        return containsIgnoreCase(result.account().username(), keyword)
                || containsIgnoreCase(result.account().email(), keyword)
                || containsIgnoreCase(result.profile().fullName(), keyword)
                || containsIgnoreCase(result.profile().phoneNumber(), keyword)
                || containsIgnoreCase(result.customer().customerCode(), keyword);
    }

    private boolean containsIgnoreCase(String value, String normalizedKeyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
