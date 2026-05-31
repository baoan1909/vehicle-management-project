package com.ban.vehicle_management.infrastructure.persistence.database.repository.iam;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {
    boolean existsByRoleId(UUID roleId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<AccountEntity> findByKeycloakUserId(String keycloakUserId);

    Optional<AccountEntity> findByUsername(String username);

    Optional<AccountEntity> findByEmail(String email);

}


