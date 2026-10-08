package com.ban.vehicle_management.infrastructure.persistence.database.repository.billing;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.PayoutRequestEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayoutRequestRepository extends JpaRepository<PayoutRequestEntity, UUID> {

    Optional<PayoutRequestEntity> findByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PayoutRequestEntity p where p.payoutRequestId = :id")
    Optional<PayoutRequestEntity> findByIdForUpdate(@Param("id") UUID id);

    List<PayoutRequestEntity> findByOrganizationIdOrderByRequestedAtDesc(UUID organizationId, Pageable pageable);
}
