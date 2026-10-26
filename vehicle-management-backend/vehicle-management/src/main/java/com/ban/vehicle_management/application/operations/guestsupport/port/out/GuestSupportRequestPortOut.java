package com.ban.vehicle_management.application.operations.guestsupport.port.out;

import com.ban.vehicle_management.domain.operations.guestsupport.model.GuestSupportRequest;
import java.time.Instant;
import java.util.Optional;

public interface GuestSupportRequestPortOut {
    GuestSupportRequest save(GuestSupportRequest request);
    Optional<GuestSupportRequest> findByContactHashAndIdempotencyHash(String contactHash, String idempotencyHash);
    Optional<GuestSupportRequest> findByPublicReferenceCode(String publicReferenceCode);
    long countByIpHashSince(String ipHash, Instant since);
    long countByContactHashSince(String contactHash, Instant since);
    long countBySessionHashSince(String sessionHash, Instant since);
    void lockContact(String contactHash);
    long deleteExpiredBefore(Instant now);
}
