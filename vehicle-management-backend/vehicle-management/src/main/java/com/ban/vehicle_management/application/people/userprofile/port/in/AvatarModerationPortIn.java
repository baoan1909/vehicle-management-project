package com.ban.vehicle_management.application.people.userprofile.port.in;

import com.ban.vehicle_management.application.people.userprofile.model.AvatarModerationResult;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfileAvatar;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AvatarModerationPortIn {
    AvatarModerationResult submitCandidate(UserProfileAvatar avatar, UUID uploaderAccountId);
    void cancelPendingCandidate(UUID userProfileId);
    Optional<AvatarModerationResult> getMyAvatarModerationStatus();
    List<AvatarModerationResult> getAvatarApprovals(ApprovalRequestStatus status);
    AvatarModerationResult approve(UUID approvalRequestId, String note);
    AvatarModerationResult reject(UUID approvalRequestId, String note);
}
