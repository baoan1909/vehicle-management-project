package com.ban.vehicle_management.infrastructure.persistence.database.repository.iam;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountDeletionRequestEntity;
import com.ban.vehicle_management.shared.enumeration.iam.AccountDeletionRequestStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountDeletionRequestRepository extends JpaRepository<AccountDeletionRequestEntity, UUID> {

    Optional<AccountDeletionRequestEntity> findFirstByAccountIdAndStatusInOrderByRequestedAtDesc(
            UUID accountId,
            Collection<AccountDeletionRequestStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select request from AccountDeletionRequestEntity request
        where request.accountId = :accountId and request.status in :statuses
        """)
    Optional<AccountDeletionRequestEntity> findActiveForUpdate(
            @Param("accountId") UUID accountId,
            @Param("statuses") Collection<AccountDeletionRequestStatus> statuses
    );

    Optional<AccountDeletionRequestEntity> findByRequestIdAndClaimToken(UUID requestId, String claimToken);
}
