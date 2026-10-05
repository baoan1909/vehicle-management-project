package com.ban.vehicle_management.infrastructure.persistence.adapter.people;

import com.ban.vehicle_management.application.people.userprofile.port.out.AvatarModerationPortOut;
import com.ban.vehicle_management.application.people.userprofile.usecase.AvatarModerationUseCaseImpl;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.infrastructure.mapper.operations.ApprovalRequestPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ApprovalRequestRepository;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AvatarModerationPersistenceAdapter implements AvatarModerationPortOut {
    private final ApprovalRequestRepository approvalRepository;
    private final ApprovalRequestPersistenceMapper mapper;
    private final AccountRepository accountRepository;

    public AvatarModerationPersistenceAdapter(ApprovalRequestRepository approvalRepository,
                                               ApprovalRequestPersistenceMapper mapper,
                                               AccountRepository accountRepository) {
        this.approvalRepository = approvalRepository;
        this.mapper = mapper;
        this.accountRepository = accountRepository;
    }

    @Override public UUID findOwnerAccountId(UUID userProfileId) {
        return accountRepository.findByUserProfileId(userProfileId)
                .map(account -> account.getAccountId())
                .orElseThrow(() -> new NotFoundException("Avatar owner account not found"));
    }
    @Override public ApprovalRequest save(ApprovalRequest request) {
        return mapper.toDomain(approvalRepository.saveAndFlush(mapper.toEntity(request)));
    }
    @Override public Optional<ApprovalRequest> findForUpdate(UUID id) {
        return approvalRepository.findAvatarApprovalForUpdate(id, AvatarModerationUseCaseImpl.REQUEST_TYPE,
                AvatarModerationUseCaseImpl.TARGET_SCHEMA, AvatarModerationUseCaseImpl.TARGET_TABLE).map(mapper::toDomain);
    }
    @Override public Optional<ApprovalRequest> findLatestByAvatarId(UUID avatarId) {
        return approvalRepository.findTopByRequestTypeAndTargetSchemaAndTargetTableAndTargetIdOrderByCreatedAtDesc(
                AvatarModerationUseCaseImpl.REQUEST_TYPE, AvatarModerationUseCaseImpl.TARGET_SCHEMA,
                AvatarModerationUseCaseImpl.TARGET_TABLE, avatarId).map(mapper::toDomain);
    }
    @Override public Optional<ApprovalRequest> findLatestByOwnerAccountId(UUID accountId) {
        return approvalRepository.findTopByRequestedByAndRequestTypeAndTargetSchemaAndTargetTableOrderByCreatedAtDesc(
                accountId, AvatarModerationUseCaseImpl.REQUEST_TYPE, AvatarModerationUseCaseImpl.TARGET_SCHEMA,
                AvatarModerationUseCaseImpl.TARGET_TABLE).map(mapper::toDomain);
    }
    @Override public List<ApprovalRequest> findAll(ApprovalRequestStatus status) {
        var entities = status == null
                ? approvalRepository.findByRequestTypeAndTargetSchemaAndTargetTableOrderByCreatedAtDesc(
                    AvatarModerationUseCaseImpl.REQUEST_TYPE, AvatarModerationUseCaseImpl.TARGET_SCHEMA,
                    AvatarModerationUseCaseImpl.TARGET_TABLE)
                : approvalRepository.findByRequestTypeAndTargetSchemaAndTargetTableAndStatusOrderByCreatedAtDesc(
                    AvatarModerationUseCaseImpl.REQUEST_TYPE, AvatarModerationUseCaseImpl.TARGET_SCHEMA,
                    AvatarModerationUseCaseImpl.TARGET_TABLE, status);
        return entities.stream().map(mapper::toDomain).toList();
    }
}
