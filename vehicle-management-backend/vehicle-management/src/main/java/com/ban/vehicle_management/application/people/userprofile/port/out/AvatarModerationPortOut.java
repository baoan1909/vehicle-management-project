package com.ban.vehicle_management.application.people.userprofile.port.out;

import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AvatarModerationPortOut {
    UUID findOwnerAccountId(UUID userProfileId);
    ApprovalRequest save(ApprovalRequest request);
    Optional<ApprovalRequest> findForUpdate(UUID approvalRequestId);
    Optional<ApprovalRequest> findLatestByAvatarId(UUID avatarId);
    Optional<ApprovalRequest> findLatestByOwnerAccountId(UUID accountId);
    List<ApprovalRequest> findAll(ApprovalRequestStatus status);
}
