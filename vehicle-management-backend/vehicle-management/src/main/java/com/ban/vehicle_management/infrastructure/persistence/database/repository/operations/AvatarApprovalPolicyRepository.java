package com.ban.vehicle_management.infrastructure.persistence.database.repository.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.AvatarApprovalPolicyEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AvatarApprovalPolicyRepository extends JpaRepository<AvatarApprovalPolicyEntity, UUID> {
    Optional<AvatarApprovalPolicyEntity> findFirstByOrderByCreatedAtAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select policy from AvatarApprovalPolicyEntity policy where policy.policyId = :policyId")
    Optional<AvatarApprovalPolicyEntity> findPolicyForUpdate(@Param("policyId") UUID policyId);
}
