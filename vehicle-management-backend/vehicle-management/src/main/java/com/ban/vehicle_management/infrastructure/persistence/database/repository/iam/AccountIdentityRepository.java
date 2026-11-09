package com.ban.vehicle_management.infrastructure.persistence.database.repository.iam;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountIdentityEntity;
import com.ban.vehicle_management.shared.enumeration.iam.SocialIdentityProvider;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountIdentityRepository extends JpaRepository<AccountIdentityEntity, UUID> {

    @Modifying
    @Query(value = """
        update iam.account_identities set provider_subject=:marker, updated_at=:now
        where account_id=:accountId
        """, nativeQuery = true)
    int tombstoneProviderSubjects(
            @Param("accountId") UUID accountId,
            @Param("marker") String marker,
            @Param("now") Instant now);

    Optional<AccountIdentityEntity> findByProviderAndProviderSubject(
            SocialIdentityProvider provider,
            String providerSubject
    );

    Optional<AccountIdentityEntity> findByAccountIdAndProvider(
            UUID accountId,
            SocialIdentityProvider provider
    );
}
