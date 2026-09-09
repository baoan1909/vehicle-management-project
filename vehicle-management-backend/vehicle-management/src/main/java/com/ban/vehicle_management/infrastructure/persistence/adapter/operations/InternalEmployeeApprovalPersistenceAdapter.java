package com.ban.vehicle_management.infrastructure.persistence.adapter.operations;

import com.ban.vehicle_management.application.operations.approvalrequest.authorization.InternalEmployeeApprovalAccessGuard;
import com.ban.vehicle_management.application.operations.approvalrequest.model.command.InternalEmployeeApprovalFilterCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.InternalEmployeeApprovalCandidate;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.InternalEmployeeApprovalResult;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.InternalEmployeeApprovalPortOut;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.infrastructure.mapper.operations.ApprovalRequestPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.operations.OnboardingApprovalReadModelMapper;
import com.ban.vehicle_management.infrastructure.mapper.people.EmployeePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.RoleEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ApprovalRequestEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.EmployeeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.RoleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ApprovalRequestRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.EmployeeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.UserProfileRepository;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class InternalEmployeeApprovalPersistenceAdapter implements InternalEmployeeApprovalPortOut {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final UserProfileRepository userProfileRepository;
    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final ApprovalRequestPersistenceMapper approvalRequestPersistenceMapper;
    private final EmployeePersistenceMapper employeePersistenceMapper;
    private final OnboardingApprovalReadModelMapper onboardingApprovalReadModelMapper;

    public InternalEmployeeApprovalPersistenceAdapter(
            ApprovalRequestRepository approvalRequestRepository,
            EmployeeRepository employeeRepository,
            UserProfileRepository userProfileRepository,
            AccountRepository accountRepository,
            RoleRepository roleRepository,
            ApprovalRequestPersistenceMapper approvalRequestPersistenceMapper,
            EmployeePersistenceMapper employeePersistenceMapper,
            OnboardingApprovalReadModelMapper onboardingApprovalReadModelMapper
    ) {
        this.approvalRequestRepository = approvalRequestRepository;
        this.employeeRepository = employeeRepository;
        this.userProfileRepository = userProfileRepository;
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
        this.approvalRequestPersistenceMapper = approvalRequestPersistenceMapper;
        this.employeePersistenceMapper = employeePersistenceMapper;
        this.onboardingApprovalReadModelMapper = onboardingApprovalReadModelMapper;
    }

    @Override
    public void saveInternalEmployeeApprovalRequest(ApprovalRequest approvalRequest) {
        approvalRequestRepository.saveAndFlush(approvalRequestPersistenceMapper.toEntity(approvalRequest));
    }

    @Override
    public void saveInternalEmployeeApprovalDecision(ApprovalRequest approvalRequest, Employee employee) {
        approvalRequestRepository.save(approvalRequestPersistenceMapper.toEntity(approvalRequest));
        employeeRepository.saveAndFlush(employeePersistenceMapper.toEntity(employee));
    }

    @Override
    public boolean existsPendingInternalEmployeeApprovalForEmployee(UUID employeeId) {
        return approvalRequestRepository.existsByRequestTypeAndTargetSchemaAndTargetTableAndTargetIdAndStatus(
                InternalEmployeeApprovalAccessGuard.REQUEST_TYPE,
                InternalEmployeeApprovalAccessGuard.TARGET_SCHEMA,
                InternalEmployeeApprovalAccessGuard.TARGET_TABLE,
                employeeId,
                ApprovalRequestStatus.PENDING
        );
    }

    @Override
    public Optional<ApprovalRequest> findInternalEmployeeApprovalRequestById(UUID approvalRequestId) {
        return approvalRequestRepository.findByApprovalRequestIdAndRequestTypeAndTargetSchemaAndTargetTable(
                        approvalRequestId,
                        InternalEmployeeApprovalAccessGuard.REQUEST_TYPE,
                        InternalEmployeeApprovalAccessGuard.TARGET_SCHEMA,
                        InternalEmployeeApprovalAccessGuard.TARGET_TABLE
                )
                .map(approvalRequestPersistenceMapper::toDomain);
    }

    @Override
    public Optional<ApprovalRequest> findLatestInternalEmployeeApprovalRequest(UUID employeeId) {
        return approvalRequestRepository.findTopByRequestTypeAndTargetSchemaAndTargetTableAndTargetIdOrderByCreatedAtDesc(
                        InternalEmployeeApprovalAccessGuard.REQUEST_TYPE,
                        InternalEmployeeApprovalAccessGuard.TARGET_SCHEMA,
                        InternalEmployeeApprovalAccessGuard.TARGET_TABLE,
                        employeeId
                )
                .map(approvalRequestPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Employee> findEmployeeById(UUID employeeId) {
        return employeeRepository.findById(employeeId)
                .map(employeePersistenceMapper::toDomain);
    }

    @Override
    public Optional<InternalEmployeeApprovalCandidate> findCandidateByEmployeeId(UUID employeeId) {
        return employeeRepository.findById(employeeId)
                .flatMap(this::toCandidate);
    }

    @Override
    public Optional<InternalEmployeeApprovalCandidate> findCandidateByAccountId(UUID accountId) {
        return accountRepository.findById(accountId)
                .flatMap(this::toCandidate);
    }

    @Override
    public List<InternalEmployeeApprovalResult> findInternalEmployeeApprovalRequests(InternalEmployeeApprovalFilterCommand command) {
        List<ApprovalRequestEntity> approvalRequests = command.status() == null
                ? approvalRequestRepository.findByRequestTypeAndTargetSchemaAndTargetTableOrderByCreatedAtDesc(
                InternalEmployeeApprovalAccessGuard.REQUEST_TYPE,
                InternalEmployeeApprovalAccessGuard.TARGET_SCHEMA,
                InternalEmployeeApprovalAccessGuard.TARGET_TABLE
        )
                : approvalRequestRepository.findByRequestTypeAndTargetSchemaAndTargetTableAndStatusOrderByCreatedAtDesc(
                InternalEmployeeApprovalAccessGuard.REQUEST_TYPE,
                InternalEmployeeApprovalAccessGuard.TARGET_SCHEMA,
                InternalEmployeeApprovalAccessGuard.TARGET_TABLE,
                command.status()
        );

        return approvalRequests.stream()
                .map(this::toResult)
                .flatMap(Optional::stream)
                .filter(result -> matchesRole(command, result))
                .filter(result -> matchesKeyword(command, result))
                .toList();
    }

    @Override
    public Optional<InternalEmployeeApprovalResult> findInternalEmployeeApprovalResultById(UUID approvalRequestId) {
        return approvalRequestRepository.findByApprovalRequestIdAndRequestTypeAndTargetSchemaAndTargetTable(
                        approvalRequestId,
                        InternalEmployeeApprovalAccessGuard.REQUEST_TYPE,
                        InternalEmployeeApprovalAccessGuard.TARGET_SCHEMA,
                        InternalEmployeeApprovalAccessGuard.TARGET_TABLE
                )
                .flatMap(this::toResult);
    }

    @Override
    public Optional<InternalEmployeeApprovalResult> findLatestInternalEmployeeApprovalResultByAccountId(UUID accountId) {
        return findCandidateByAccountId(accountId)
                .flatMap(candidate -> approvalRequestRepository.findTopByRequestTypeAndTargetSchemaAndTargetTableAndTargetIdOrderByCreatedAtDesc(
                        InternalEmployeeApprovalAccessGuard.REQUEST_TYPE,
                        InternalEmployeeApprovalAccessGuard.TARGET_SCHEMA,
                        InternalEmployeeApprovalAccessGuard.TARGET_TABLE,
                        candidate.employeeId()
                ))
                .flatMap(this::toResult);
    }

    private Optional<InternalEmployeeApprovalCandidate> toCandidate(EmployeeEntity employeeEntity) {
        return accountRepository.findByUserProfileId(employeeEntity.getUserProfileId())
                .flatMap(accountEntity -> buildCandidate(accountEntity, employeeEntity));
    }

    private Optional<InternalEmployeeApprovalCandidate> toCandidate(AccountEntity accountEntity) {
        if (accountEntity.getUserProfileId() == null) {
            return Optional.empty();
        }
        return employeeRepository.findByUserProfileId(accountEntity.getUserProfileId())
                .flatMap(employeeEntity -> buildCandidate(accountEntity, employeeEntity));
    }

    private Optional<InternalEmployeeApprovalCandidate> buildCandidate(
            AccountEntity accountEntity,
            EmployeeEntity employeeEntity
    ) {
        return resolveRole(accountEntity.getRoleId())
                .map(roleEntity -> new InternalEmployeeApprovalCandidate(
                        accountEntity.getAccountId(),
                        employeeEntity.getUserProfileId(),
                        employeeEntity.getEmployeeId(),
                        roleEntity.getCode(),
                        accountEntity.getStatus(),
                        employeeEntity.getStatus()
                ));
    }

    private Optional<InternalEmployeeApprovalResult> toResult(ApprovalRequestEntity approvalRequestEntity) {
        Optional<EmployeeEntity> employeeEntity = employeeRepository.findById(approvalRequestEntity.getTargetId());
        if (employeeEntity.isEmpty()) {
            return Optional.empty();
        }

        Optional<UserProfileEntity> userProfileEntity = userProfileRepository.findById(employeeEntity.get().getUserProfileId());
        Optional<AccountEntity> accountEntity = accountRepository.findByUserProfileId(employeeEntity.get().getUserProfileId());
        if (userProfileEntity.isEmpty() || accountEntity.isEmpty()) {
            return Optional.empty();
        }

        Optional<RoleEntity> roleEntity = resolveRole(accountEntity.get().getRoleId());
        if (roleEntity.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(onboardingApprovalReadModelMapper.toInternalEmployeeResult(
                approvalRequestEntity,
                accountEntity.get(),
                roleEntity.get(),
                userProfileEntity.get(),
                employeeEntity.get()
        ));
    }

    private Optional<RoleEntity> resolveRole(UUID roleId) {
        return roleRepository.findById(roleId);
    }

    private boolean matchesRole(
            InternalEmployeeApprovalFilterCommand command,
            InternalEmployeeApprovalResult result
    ) {
        return command.roleCode() == null || command.roleCode().equals(result.account().roleCode());
    }

    private boolean matchesKeyword(
            InternalEmployeeApprovalFilterCommand command,
            InternalEmployeeApprovalResult result
    ) {
        if (command.keyword() == null || command.keyword().isBlank()) {
            return true;
        }
        String keyword = command.keyword().toLowerCase(Locale.ROOT);
        return containsIgnoreCase(result.account().username(), keyword)
                || containsIgnoreCase(result.account().email(), keyword)
                || containsIgnoreCase(result.profile().fullName(), keyword)
                || containsIgnoreCase(result.profile().phoneNumber(), keyword)
                || containsIgnoreCase(result.employee().employeeCode(), keyword)
                || containsIgnoreCase(result.employee().jobTitle(), keyword);
    }

    private boolean containsIgnoreCase(String value, String normalizedKeyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }

}
