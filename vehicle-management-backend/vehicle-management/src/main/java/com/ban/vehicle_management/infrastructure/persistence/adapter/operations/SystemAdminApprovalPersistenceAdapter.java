package com.ban.vehicle_management.infrastructure.persistence.adapter.operations;

import com.ban.vehicle_management.application.operations.approvalrequest.authorization.SystemAdminApprovalAccessGuard;
import com.ban.vehicle_management.application.operations.approvalrequest.model.command.SystemAdminApprovalFilterCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.SystemAdminApprovalResult;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.SystemAdminApprovalPortOut;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.infrastructure.mapper.operations.ApprovalRequestPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.RoleEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ApprovalRequestEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.RoleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ApprovalRequestRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.UserProfileRepository;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SystemAdminApprovalPersistenceAdapter implements SystemAdminApprovalPortOut {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final UserProfileRepository userProfileRepository;
    private final ApprovalRequestPersistenceMapper approvalRequestPersistenceMapper;

    public SystemAdminApprovalPersistenceAdapter(
            ApprovalRequestRepository approvalRequestRepository,
            AccountRepository accountRepository,
            RoleRepository roleRepository,
            UserProfileRepository userProfileRepository,
            ApprovalRequestPersistenceMapper approvalRequestPersistenceMapper
    ) {
        this.approvalRequestRepository = approvalRequestRepository;
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
        this.userProfileRepository = userProfileRepository;
        this.approvalRequestPersistenceMapper = approvalRequestPersistenceMapper;
    }

    @Override
    public void saveSystemAdminApprovalRequest(ApprovalRequest approvalRequest) {
        approvalRequestRepository.saveAndFlush(approvalRequestPersistenceMapper.toEntity(approvalRequest));
    }

    @Override
    public void saveSystemAdminApprovalDecision(
            ApprovalRequest approvalRequest,
            UUID accountId,
            AccountStatus accountStatus
    ) {
        AccountEntity accountEntity = accountRepository.findById(accountId)
                .orElseThrow();
        accountEntity.setStatus(accountStatus);
        approvalRequestRepository.save(approvalRequestPersistenceMapper.toEntity(approvalRequest));
        accountRepository.saveAndFlush(accountEntity);
    }

    @Override
    public boolean existsPendingSystemAdminApprovalForAccount(UUID accountId) {
        return approvalRequestRepository.existsByRequestTypeAndTargetSchemaAndTargetTableAndTargetIdAndStatus(
                SystemAdminApprovalAccessGuard.REQUEST_TYPE,
                SystemAdminApprovalAccessGuard.TARGET_SCHEMA,
                SystemAdminApprovalAccessGuard.TARGET_TABLE,
                accountId,
                ApprovalRequestStatus.PENDING
        );
    }

    @Override
    public Optional<ApprovalRequest> findSystemAdminApprovalRequestById(UUID approvalRequestId) {
        return approvalRequestRepository.findByApprovalRequestIdAndRequestTypeAndTargetSchemaAndTargetTable(
                        approvalRequestId,
                        SystemAdminApprovalAccessGuard.REQUEST_TYPE,
                        SystemAdminApprovalAccessGuard.TARGET_SCHEMA,
                        SystemAdminApprovalAccessGuard.TARGET_TABLE
                )
                .map(approvalRequestPersistenceMapper::toDomain);
    }

    @Override
    public Optional<ApprovalRequest> findLatestSystemAdminApprovalRequest(UUID accountId) {
        return approvalRequestRepository.findTopByRequestTypeAndTargetSchemaAndTargetTableAndTargetIdOrderByCreatedAtDesc(
                        SystemAdminApprovalAccessGuard.REQUEST_TYPE,
                        SystemAdminApprovalAccessGuard.TARGET_SCHEMA,
                        SystemAdminApprovalAccessGuard.TARGET_TABLE,
                        accountId
                )
                .map(approvalRequestPersistenceMapper::toDomain);
    }

    @Override
    public List<SystemAdminApprovalResult> findSystemAdminApprovalRequests(SystemAdminApprovalFilterCommand command) {
        List<ApprovalRequestEntity> approvalRequests = command.status() == null
                ? approvalRequestRepository.findByRequestTypeAndTargetSchemaAndTargetTableOrderByCreatedAtDesc(
                SystemAdminApprovalAccessGuard.REQUEST_TYPE,
                SystemAdminApprovalAccessGuard.TARGET_SCHEMA,
                SystemAdminApprovalAccessGuard.TARGET_TABLE
        )
                : approvalRequestRepository.findByRequestTypeAndTargetSchemaAndTargetTableAndStatusOrderByCreatedAtDesc(
                SystemAdminApprovalAccessGuard.REQUEST_TYPE,
                SystemAdminApprovalAccessGuard.TARGET_SCHEMA,
                SystemAdminApprovalAccessGuard.TARGET_TABLE,
                command.status()
        );

        return approvalRequests.stream()
                .map(this::toResult)
                .flatMap(Optional::stream)
                .filter(result -> matchesKeyword(command, result))
                .toList();
    }

    @Override
    public Optional<SystemAdminApprovalResult> findSystemAdminApprovalResultById(UUID approvalRequestId) {
        return approvalRequestRepository.findByApprovalRequestIdAndRequestTypeAndTargetSchemaAndTargetTable(
                        approvalRequestId,
                        SystemAdminApprovalAccessGuard.REQUEST_TYPE,
                        SystemAdminApprovalAccessGuard.TARGET_SCHEMA,
                        SystemAdminApprovalAccessGuard.TARGET_TABLE
                )
                .flatMap(this::toResult);
    }

    @Override
    public Optional<SystemAdminApprovalResult> findLatestSystemAdminApprovalResultByAccountId(UUID accountId) {
        return approvalRequestRepository.findTopByRequestTypeAndTargetSchemaAndTargetTableAndTargetIdOrderByCreatedAtDesc(
                        SystemAdminApprovalAccessGuard.REQUEST_TYPE,
                        SystemAdminApprovalAccessGuard.TARGET_SCHEMA,
                        SystemAdminApprovalAccessGuard.TARGET_TABLE,
                        accountId
                )
                .flatMap(this::toResult);
    }

    private Optional<SystemAdminApprovalResult> toResult(ApprovalRequestEntity approvalRequestEntity) {
        Optional<AccountEntity> accountEntity = accountRepository.findById(approvalRequestEntity.getTargetId());
        if (accountEntity.isEmpty()) {
            return Optional.empty();
        }

        Optional<RoleEntity> roleEntity = roleRepository.findById(accountEntity.get().getRoleId());
        if (roleEntity.isEmpty()) {
            return Optional.empty();
        }

        Optional<UserProfileEntity> userProfileEntity = accountEntity.get().getUserProfileId() == null
                ? Optional.empty()
                : userProfileRepository.findById(accountEntity.get().getUserProfileId());

        return Optional.of(new SystemAdminApprovalResult(
                new SystemAdminApprovalResult.RequestInfoResult(
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
                new SystemAdminApprovalResult.AccountInfoResult(
                        accountEntity.get().getAccountId(),
                        accountEntity.get().getUsername(),
                        accountEntity.get().getEmail(),
                        roleEntity.get().getCode(),
                        enumName(accountEntity.get().getStatus())
                ),
                new SystemAdminApprovalResult.ProfileInfoResult(
                        userProfileEntity.map(UserProfileEntity::getUserProfileId).orElse(null),
                        userProfileEntity.map(UserProfileEntity::getFullName).orElse(null),
                        userProfileEntity.map(UserProfileEntity::getPhoneNumber).orElse(null)
                )
        ));
    }

    private boolean matchesKeyword(SystemAdminApprovalFilterCommand command, SystemAdminApprovalResult result) {
        if (command.keyword() == null || command.keyword().isBlank()) {
            return true;
        }
        String keyword = command.keyword().toLowerCase(Locale.ROOT);
        return containsIgnoreCase(result.account().username(), keyword)
                || containsIgnoreCase(result.account().email(), keyword)
                || containsIgnoreCase(result.profile().fullName(), keyword)
                || containsIgnoreCase(result.profile().phoneNumber(), keyword);
    }

    private boolean containsIgnoreCase(String value, String normalizedKeyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
