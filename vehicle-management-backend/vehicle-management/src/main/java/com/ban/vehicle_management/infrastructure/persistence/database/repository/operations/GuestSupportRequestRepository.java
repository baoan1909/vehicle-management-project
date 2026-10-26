package com.ban.vehicle_management.infrastructure.persistence.database.repository.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.GuestSupportRequestEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GuestSupportRequestRepository extends JpaRepository<GuestSupportRequestEntity, UUID> {
    Optional<GuestSupportRequestEntity> findByContactHashAndIdempotencyHash(String contactHash, String idempotencyHash);
    Optional<GuestSupportRequestEntity> findByPublicReferenceCode(String publicReferenceCode);
    long countByIpHashAndCreatedAtGreaterThanEqual(String ipHash, Instant since);
    long countByContactHashAndCreatedAtGreaterThanEqual(String contactHash, Instant since);
    long countBySessionHashAndCreatedAtGreaterThanEqual(String sessionHash, Instant since);
    @Query(value = "SELECT 1::bigint FROM pg_advisory_xact_lock(hashtextextended(:contactHash, 0))", nativeQuery = true)
    Long lockContact(@Param("contactHash") String contactHash);
    long deleteByRetentionExpiresAtBefore(Instant now);
}
