package com.ban.vehicle_management.domain.notification.userguide.model;

import com.ban.vehicle_management.shared.enumeration.notification.UserGuideAudienceTargetType;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserGuideAudience {

    private UUID audienceId;
    private UUID guideVersionId;
    private UserGuideAudienceTargetType targetType;
    private UUID roleId;
    private UUID permissionId;
    private Instant createdAt;
    private UUID createdBy;
}
