package com.ban.vehicle_management.application.operations.approvalrequest.port.out;

import com.ban.vehicle_management.domain.operations.approvalrequest.model.OnboardingApprovalPolicy;
import com.ban.vehicle_management.shared.enumeration.operations.OnboardingApprovalPolicyType;
import java.util.Optional;

public interface OnboardingApprovalPolicyPortOut {

    Optional<OnboardingApprovalPolicy> findByType(OnboardingApprovalPolicyType policyType);

    Optional<OnboardingApprovalPolicy> findByTypeForUpdate(OnboardingApprovalPolicyType policyType);

    OnboardingApprovalPolicy save(OnboardingApprovalPolicy policy);
}
