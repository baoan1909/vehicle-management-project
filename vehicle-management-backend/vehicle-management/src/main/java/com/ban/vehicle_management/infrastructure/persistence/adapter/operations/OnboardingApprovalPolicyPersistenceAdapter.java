package com.ban.vehicle_management.infrastructure.persistence.adapter.operations;

import com.ban.vehicle_management.application.operations.approvalrequest.port.out.OnboardingApprovalPolicyPortOut;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.OnboardingApprovalPolicy;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.OnboardingApprovalPolicyEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.OnboardingApprovalPolicyRepository;
import com.ban.vehicle_management.shared.enumeration.operations.OnboardingApprovalPolicyType;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class OnboardingApprovalPolicyPersistenceAdapter implements OnboardingApprovalPolicyPortOut {

    private final OnboardingApprovalPolicyRepository repository;

    public OnboardingApprovalPolicyPersistenceAdapter(OnboardingApprovalPolicyRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<OnboardingApprovalPolicy> findByType(OnboardingApprovalPolicyType policyType) {
        return repository.findByPolicyType(policyType).map(this::toDomain);
    }

    @Override
    public Optional<OnboardingApprovalPolicy> findByTypeForUpdate(OnboardingApprovalPolicyType policyType) {
        return repository.findForUpdateByPolicyType(policyType).map(this::toDomain);
    }

    @Override
    public OnboardingApprovalPolicy save(OnboardingApprovalPolicy policy) {
        return toDomain(repository.saveAndFlush(toEntity(policy)));
    }

    private OnboardingApprovalPolicy toDomain(OnboardingApprovalPolicyEntity entity) {
        OnboardingApprovalPolicy policy = new OnboardingApprovalPolicy();
        policy.setPolicyId(entity.getPolicyId());
        policy.setPolicyType(entity.getPolicyType());
        policy.setAutoApproveEnabled(entity.isAutoApproveEnabled());
        policy.setEffectiveFrom(entity.getEffectiveFrom());
        policy.setVersion(entity.getVersion());
        policy.setCreatedAt(entity.getCreatedAt());
        policy.setCreatedBy(entity.getCreatedBy());
        policy.setUpdatedAt(entity.getUpdatedAt());
        policy.setUpdatedBy(entity.getUpdatedBy());
        return policy;
    }

    private OnboardingApprovalPolicyEntity toEntity(OnboardingApprovalPolicy policy) {
        OnboardingApprovalPolicyEntity entity = new OnboardingApprovalPolicyEntity();
        entity.setPolicyId(policy.getPolicyId());
        entity.setPolicyType(policy.getPolicyType());
        entity.setAutoApproveEnabled(policy.isAutoApproveEnabled());
        entity.setEffectiveFrom(policy.getEffectiveFrom());
        entity.setVersion(policy.getVersion());
        entity.setCreatedAt(policy.getCreatedAt());
        entity.setCreatedBy(policy.getCreatedBy());
        entity.setUpdatedAt(policy.getUpdatedAt());
        entity.setUpdatedBy(policy.getUpdatedBy());
        return entity;
    }
}
