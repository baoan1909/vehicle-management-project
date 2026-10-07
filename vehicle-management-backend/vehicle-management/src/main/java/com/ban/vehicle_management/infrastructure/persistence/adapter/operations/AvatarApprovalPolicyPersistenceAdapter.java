package com.ban.vehicle_management.infrastructure.persistence.adapter.operations;

import com.ban.vehicle_management.application.operations.approvalrequest.port.out.AvatarApprovalPolicyPortOut;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.AvatarApprovalPolicy;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.AvatarApprovalPolicyEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.AvatarApprovalPolicyRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AvatarApprovalPolicyPersistenceAdapter implements AvatarApprovalPolicyPortOut {
    private static final UUID POLICY_ID = UUID.fromString("62000000-0000-4000-8000-000000000003");

    private final AvatarApprovalPolicyRepository repository;

    public AvatarApprovalPolicyPersistenceAdapter(AvatarApprovalPolicyRepository repository) {
        this.repository = repository;
    }

    @Override public Optional<AvatarApprovalPolicy> findPolicy() {
        return repository.findFirstByOrderByCreatedAtAsc().map(this::toDomain);
    }
    @Override public Optional<AvatarApprovalPolicy> findPolicyForUpdate() {
        return repository.findPolicyForUpdate(POLICY_ID).map(this::toDomain);
    }
    @Override public AvatarApprovalPolicy save(AvatarApprovalPolicy policy) {
        AvatarApprovalPolicyEntity entity = new AvatarApprovalPolicyEntity();
        entity.setPolicyId(policy.getPolicyId());
        entity.setAutoApproveEnabled(policy.isAutoApproveEnabled());
        entity.setEffectiveFrom(policy.getEffectiveFrom());
        entity.setVersion(policy.getVersion());
        entity.setCreatedAt(policy.getCreatedAt());
        entity.setCreatedBy(policy.getCreatedBy());
        entity.setUpdatedAt(policy.getUpdatedAt());
        entity.setUpdatedBy(policy.getUpdatedBy());
        return toDomain(repository.saveAndFlush(entity));
    }

    private AvatarApprovalPolicy toDomain(AvatarApprovalPolicyEntity entity) {
        AvatarApprovalPolicy policy = new AvatarApprovalPolicy();
        policy.setPolicyId(entity.getPolicyId());
        policy.setAutoApproveEnabled(entity.isAutoApproveEnabled());
        policy.setEffectiveFrom(entity.getEffectiveFrom());
        policy.setVersion(entity.getVersion());
        policy.setCreatedAt(entity.getCreatedAt());
        policy.setCreatedBy(entity.getCreatedBy());
        policy.setUpdatedAt(entity.getUpdatedAt());
        policy.setUpdatedBy(entity.getUpdatedBy());
        return policy;
    }
}
