package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import com.ban.vehicle_management.application.iam.account.model.result.AccountDeletionAuditResult;
import com.ban.vehicle_management.application.iam.account.model.result.ClosedOrganizationArchiveResult;
import com.ban.vehicle_management.application.iam.account.model.result.DeletedAccountArchiveResult;
import com.ban.vehicle_management.application.iam.account.port.out.AccountDeletionArchivePortOut;
import com.ban.vehicle_management.infrastructure.mapper.iam.AccountDeletionArchivePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountDeletionCleanupItemRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountDeletionRequestRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.OrganizationRepository;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AccountDeletionArchivePersistenceAdapter implements AccountDeletionArchivePortOut {
    private final AccountDeletionRequestRepository requestRepository;
    private final AccountDeletionCleanupItemRepository cleanupRepository;
    private final OrganizationRepository organizationRepository;
    private final AccountDeletionArchivePersistenceMapper mapper;

    public AccountDeletionArchivePersistenceAdapter(
            AccountDeletionRequestRepository requestRepository,
            AccountDeletionCleanupItemRepository cleanupRepository,
            OrganizationRepository organizationRepository,
            AccountDeletionArchivePersistenceMapper mapper) {
        this.requestRepository = requestRepository;
        this.cleanupRepository = cleanupRepository;
        this.organizationRepository = organizationRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeletedAccountArchiveResult> findDeletedAccounts(int limit) {
        return mapper.toDeletedAccountResults(
                requestRepository.findDeletedAccountArchive(PageRequest.of(0, limit)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DeletedAccountArchiveResult> findDeletedAccount(UUID accountId) {
        return requestRepository.findDeletedAccountArchiveByAccountId(accountId).map(mapper::toResult);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClosedOrganizationArchiveResult> findClosedOrganizations(int limit) {
        return mapper.toClosedOrganizationResults(organizationRepository
                .findAllByStatusOrderByClosedAtDesc(OrganizationStatus.CLOSED, PageRequest.of(0, limit)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClosedOrganizationArchiveResult> findClosedOrganization(UUID organizationId) {
        return organizationRepository.findByOrganizationIdAndStatus(organizationId, OrganizationStatus.CLOSED)
                .map(mapper::toResult);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountDeletionAuditResult> findDeletionAudit(UUID requestId) {
        return requestRepository.findById(requestId).map(request -> mapper.toAuditResult(
                request, mapper.toCleanupResults(
                        cleanupRepository.findAllByRequestIdOrderByScheduledAtAscCleanupItemIdAsc(requestId))));
    }
}
