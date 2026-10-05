package com.ban.vehicle_management.application.operations.approvalrequest.usecase;

import com.ban.vehicle_management.application.operations.approvalrequest.port.out.OnboardingApprovalPolicyPortOut;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.OnboardingApprovalPolicy;
import com.ban.vehicle_management.shared.enumeration.operations.OnboardingApprovalPolicyType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class OnboardingApprovalPolicyEvaluator {

    private final OnboardingApprovalPolicyPortOut policyPortOut;

    public OnboardingApprovalPolicyEvaluator(OnboardingApprovalPolicyPortOut policyPortOut) {
        this.policyPortOut = policyPortOut;
    }

    public Optional<OnboardingApprovalPolicy> findEligiblePolicy(
            OnboardingApprovalPolicyType policyType,
            Instant requestCreatedAt
    ) {
        return policyPortOut.findByType(policyType).filter(policy -> policy.isEligible(requestCreatedAt));
    }

    public Optional<Instant> findEffectiveFrom(OnboardingApprovalPolicyType policyType) {
        return policyPortOut.findByType(policyType)
                .filter(OnboardingApprovalPolicy::isAutoApproveEnabled)
                .map(OnboardingApprovalPolicy::getEffectiveFrom);
    }
}
