package com.ban.vehicle_management.infrastructure.mapper.iam;

import com.ban.vehicle_management.application.iam.account.model.result.AccountDeletionAuditResult;
import com.ban.vehicle_management.application.iam.account.model.result.ClosedOrganizationArchiveResult;
import com.ban.vehicle_management.application.iam.account.model.result.DeletedAccountArchiveResult;
import com.ban.vehicle_management.application.iam.account.model.result.DeletionCleanupArchiveResult;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountDeletionCleanupItemEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountDeletionRequestEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.OrganizationEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountDeletionRequestRepository;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountDeletionArchivePersistenceMapper {
    DeletedAccountArchiveResult toResult(
            AccountDeletionRequestRepository.DeletedAccountArchiveProjection projection);

    List<DeletedAccountArchiveResult> toDeletedAccountResults(
            List<AccountDeletionRequestRepository.DeletedAccountArchiveProjection> projections);

    ClosedOrganizationArchiveResult toResult(OrganizationEntity entity);

    List<ClosedOrganizationArchiveResult> toClosedOrganizationResults(List<OrganizationEntity> entities);

    DeletionCleanupArchiveResult toResult(AccountDeletionCleanupItemEntity entity);

    List<DeletionCleanupArchiveResult> toCleanupResults(List<AccountDeletionCleanupItemEntity> entities);

    @Mapping(target = "cleanupItems", source = "cleanupItems")
    AccountDeletionAuditResult toAuditResult(
            AccountDeletionRequestEntity request,
            List<DeletionCleanupArchiveResult> cleanupItems);
}
