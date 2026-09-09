package com.ban.vehicle_management.infrastructure.persistence.database.repository.iam;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountIdentityEntity;
import com.ban.vehicle_management.shared.enumeration.iam.SocialIdentityProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountIdentityRepository extends JpaRepository<AccountIdentityEntity, UUID> {

    Optional<AccountIdentityEntity> findByProviderAndProviderSubject(
            SocialIdentityProvider provider,
            String providerSubject
    );

    Optional<AccountIdentityEntity> findByAccountIdAndProvider(
            UUID accountId,
            SocialIdentityProvider provider
    );
}
