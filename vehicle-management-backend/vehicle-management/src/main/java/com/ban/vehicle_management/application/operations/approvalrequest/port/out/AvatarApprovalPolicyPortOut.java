package com.ban.vehicle_management.application.operations.approvalrequest.port.out;

import com.ban.vehicle_management.domain.operations.approvalrequest.model.AvatarApprovalPolicy;
import java.util.Optional;

public interface AvatarApprovalPolicyPortOut {
    Optional<AvatarApprovalPolicy> findPolicy();
    Optional<AvatarApprovalPolicy> findPolicyForUpdate();
    AvatarApprovalPolicy save(AvatarApprovalPolicy policy);
}
