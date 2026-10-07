package com.ban.vehicle_management.infrastructure.persistence.database.repository.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.OnboardingApprovalPolicyEntity;
import com.ban.vehicle_management.shared.enumeration.operations.OnboardingApprovalPolicyType;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface OnboardingApprovalPolicyRepository
        extends JpaRepository<OnboardingApprovalPolicyEntity, UUID> {

    Optional<OnboardingApprovalPolicyEntity> findByPolicyType(OnboardingApprovalPolicyType policyType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OnboardingApprovalPolicyEntity> findForUpdateByPolicyType(OnboardingApprovalPolicyType policyType);
}
