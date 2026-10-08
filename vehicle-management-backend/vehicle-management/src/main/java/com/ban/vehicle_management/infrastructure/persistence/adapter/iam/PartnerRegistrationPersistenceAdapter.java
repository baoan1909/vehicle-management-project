package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import com.ban.vehicle_management.application.iam.partnerregistration.model.result.PartnerRegistrationResult;
import com.ban.vehicle_management.application.iam.partnerregistration.port.out.PartnerRegistrationPortOut;
import com.ban.vehicle_management.application.iam.partnerregistration.usecase.PartnerRegistrationUseCaseImpl;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.infrastructure.mapper.operations.ApprovalRequestPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ApprovalRequestEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ApprovalRequestRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.UserProfileRepository;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Comparator;
import java.util.stream.Stream;
import java.time.Instant;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class PartnerRegistrationPersistenceAdapter implements PartnerRegistrationPortOut {
    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalRequestPersistenceMapper approvalRequestPersistenceMapper;
    private final AccountRepository accountRepository;
    private final UserProfileRepository userProfileRepository;

    public PartnerRegistrationPersistenceAdapter(ApprovalRequestRepository approvalRequestRepository, ApprovalRequestPersistenceMapper approvalRequestPersistenceMapper, AccountRepository accountRepository, UserProfileRepository userProfileRepository) {
        this.approvalRequestRepository = approvalRequestRepository;
        this.approvalRequestPersistenceMapper = approvalRequestPersistenceMapper;
        this.accountRepository = accountRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Override public void save(ApprovalRequest approvalRequest) {
        approvalRequestRepository.saveAndFlush(approvalRequestPersistenceMapper.toEntity(approvalRequest));
    }

    @Override public boolean existsPendingByOrganizationCode(String organizationCode) {
        return entities(ApprovalRequestStatus.PENDING).stream()
                .anyMatch(item -> organizationCode.equalsIgnoreCase(value(item.getRequestData(), "organizationCode")));
    }

    @Override public List<PartnerRegistrationResult> findAll(ApprovalRequestStatus status) {
        return entities(status).stream().map(this::toResult).toList();
    }

    @Override public Optional<ApprovalRequest> findById(UUID approvalRequestId) {
        Optional<ApprovalRequestEntity> current = approvalRequestRepository.findByApprovalRequestIdAndRequestTypeAndTargetSchemaAndTargetTable(
                approvalRequestId,
                PartnerRegistrationUseCaseImpl.REQUEST_TYPE,
                PartnerRegistrationUseCaseImpl.TARGET_SCHEMA,
                PartnerRegistrationUseCaseImpl.TARGET_TABLE
        );
        return current.or(() -> approvalRequestRepository.findByApprovalRequestIdAndRequestTypeAndTargetSchemaAndTargetTable(
                approvalRequestId,
                PartnerRegistrationUseCaseImpl.REQUEST_TYPE,
                PartnerRegistrationUseCaseImpl.TARGET_SCHEMA,
                PartnerRegistrationUseCaseImpl.LEGACY_TARGET_TABLE
        )).map(approvalRequestPersistenceMapper::toDomain);
    }

    @Override public Optional<ApprovalRequest> findByIdForUpdate(UUID approvalRequestId) {
        Optional<ApprovalRequestEntity> current = approvalRequestRepository.findPartnerRegistrationForUpdate(
                approvalRequestId,
                PartnerRegistrationUseCaseImpl.REQUEST_TYPE,
                PartnerRegistrationUseCaseImpl.TARGET_SCHEMA,
                PartnerRegistrationUseCaseImpl.TARGET_TABLE
        );
        return current.or(() -> approvalRequestRepository.findPartnerRegistrationForUpdate(
                approvalRequestId,
                PartnerRegistrationUseCaseImpl.REQUEST_TYPE,
                PartnerRegistrationUseCaseImpl.TARGET_SCHEMA,
                PartnerRegistrationUseCaseImpl.LEGACY_TARGET_TABLE
        )).map(approvalRequestPersistenceMapper::toDomain);
    }

    @Override
    public List<UUID> findPendingIdsCreatedAtOrAfter(Instant effectiveFrom, int limit) {
        return approvalRequestRepository.findPartnerRegistrationIdsEligibleForAutoApproval(
                PartnerRegistrationUseCaseImpl.REQUEST_TYPE,
                PartnerRegistrationUseCaseImpl.TARGET_SCHEMA,
                PartnerRegistrationUseCaseImpl.TARGET_TABLE,
                ApprovalRequestStatus.PENDING,
                effectiveFrom,
                PageRequest.of(0, Math.max(1, limit))
        );
    }

    @Override public Optional<ApprovalRequest> findLatestByApplicantAccountId(UUID accountId) {
        Optional<ApprovalRequestEntity> current = approvalRequestRepository
                .findTopByRequestedByAndRequestTypeAndTargetSchemaAndTargetTableOrderByCreatedAtDesc(
                        accountId,
                        PartnerRegistrationUseCaseImpl.REQUEST_TYPE,
                        PartnerRegistrationUseCaseImpl.TARGET_SCHEMA,
                        PartnerRegistrationUseCaseImpl.TARGET_TABLE
                );
        Optional<ApprovalRequestEntity> legacy = approvalRequestRepository
                .findTopByRequestedByAndRequestTypeAndTargetSchemaAndTargetTableOrderByCreatedAtDesc(
                        accountId,
                        PartnerRegistrationUseCaseImpl.REQUEST_TYPE,
                        PartnerRegistrationUseCaseImpl.TARGET_SCHEMA,
                        PartnerRegistrationUseCaseImpl.LEGACY_TARGET_TABLE
                );
        return Stream.of(current, legacy)
                .flatMap(Optional::stream)
                .max(Comparator.comparing(ApprovalRequestEntity::getCreatedAt))
                .map(approvalRequestPersistenceMapper::toDomain);
    }

    private List<ApprovalRequestEntity> entities(ApprovalRequestStatus status) {
        return Stream.concat(
                        entitiesForTable(PartnerRegistrationUseCaseImpl.TARGET_TABLE, status).stream(),
                        entitiesForTable(PartnerRegistrationUseCaseImpl.LEGACY_TARGET_TABLE, status).stream()
                )
                .sorted(Comparator.comparing(ApprovalRequestEntity::getCreatedAt).reversed())
                .toList();
    }

    private List<ApprovalRequestEntity> entitiesForTable(String targetTable, ApprovalRequestStatus status) {
        return status == null
                ? approvalRequestRepository.findByRequestTypeAndTargetSchemaAndTargetTableOrderByCreatedAtDesc(
                        PartnerRegistrationUseCaseImpl.REQUEST_TYPE,
                        PartnerRegistrationUseCaseImpl.TARGET_SCHEMA,
                        targetTable
                )
                : approvalRequestRepository.findByRequestTypeAndTargetSchemaAndTargetTableAndStatusOrderByCreatedAtDesc(
                        PartnerRegistrationUseCaseImpl.REQUEST_TYPE,
                        PartnerRegistrationUseCaseImpl.TARGET_SCHEMA,
                        targetTable,
                        status
                );
    }

    private PartnerRegistrationResult toResult(ApprovalRequestEntity entity) {
        Map<String, String> data = entity.getRequestData();
        ApplicantInfo applicant = resolveApplicant(entity.getRequestedBy());
        return new PartnerRegistrationResult(
                entity.getApprovalRequestId(),
                value(data, "organizationCode"),
                value(data, "organizationName"),
                value(data, "organizationAddressDisplay"),
                applicant.fullName(),
                applicant.phoneNumber(),
                applicant.email(),
                entity.getCreatedAt(),
                entity.getStatus(),
                entity.getNote()
        );
    }

    private ApplicantInfo resolveApplicant(UUID accountId) {
        if (accountId == null) {
            return new ApplicantInfo("", "", "");
        }
        return accountRepository.findById(accountId)
                .map(account -> {
                    String email = defaultString(account.getEmail());
                    UUID userProfileId = account.getUserProfileId();
                    if (userProfileId == null) {
                        return new ApplicantInfo("", "", email);
                    }
                    return userProfileRepository.findById(userProfileId)
                            .map(profile -> new ApplicantInfo(
                                    defaultString(profile.getFullName()),
                                    defaultString(profile.getPhoneNumber()),
                                    email
                            ))
                            .orElse(new ApplicantInfo("", "", email));
                })
                .orElse(new ApplicantInfo("", "", ""));
    }

    private record ApplicantInfo(String fullName, String phoneNumber, String email) {
    }

    private String defaultString(String value) { return value == null ? "" : value; }

    private String value(Map<String, String> data, String key) { return data == null ? "" : data.getOrDefault(key, ""); }
}
