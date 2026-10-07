package com.ban.vehicle_management.entrypoint.dto.people.userprofile.response;

import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import java.time.Instant;
import java.util.UUID;

public record AvatarModerationResponse(
        UUID avatarId,
        UUID approvalRequestId,
        UUID userProfileId,
        UUID ownerAccountId,
        ApprovalRequestStatus approvalStatus,
        String reviewNote,
        String displayedAvatarUrl,
        String candidatePreviewUrl,
        Instant submittedAt
) {}
