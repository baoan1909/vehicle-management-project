package com.ban.vehicle_management.infrastructure.persistence.database.repository.iam;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountDeletionOrganizationScopeEntity;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationClosureStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountDeletionOrganizationScopeRepository
        extends JpaRepository<AccountDeletionOrganizationScopeEntity, UUID> {

    List<AccountDeletionOrganizationScopeEntity> findAllByRequestIdOrderByOrganizationId(UUID requestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select scope from AccountDeletionOrganizationScopeEntity scope
        where scope.requestId = :requestId and scope.closureStatus in :statuses
        order by scope.organizationId
        """)
    List<AccountDeletionOrganizationScopeEntity> findAllByRequestIdForUpdate(
            @Param("requestId") UUID requestId,
            @Param("statuses") Collection<OrganizationClosureStatus> statuses);
}
