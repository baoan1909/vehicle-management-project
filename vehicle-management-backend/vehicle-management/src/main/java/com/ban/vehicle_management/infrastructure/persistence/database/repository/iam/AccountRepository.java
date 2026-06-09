package com.ban.vehicle_management.infrastructure.persistence.database.repository.iam;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {
    boolean existsByRoleId(UUID roleId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<AccountEntity> findByKeycloakUserId(String keycloakUserId);

    Optional<AccountEntity> findByUsername(String username);

    Optional<AccountEntity> findByEmail(String email);

    @Query("""
        select case when count(accountEntity) > 0 then true else false end
        from AccountEntity accountEntity
        join accountEntity.role roleEntity
        where accountEntity.accountId = :accountId
          and accountEntity.status = :status
          and roleEntity.code in :roleCodes
          and roleEntity.isActive = true
        """)
    boolean existsAssignableSupportTicketAccount(
            @Param("accountId") UUID accountId,
            @Param("status") AccountStatus status,
            @Param("roleCodes") Collection<String> roleCodes
    );

}


