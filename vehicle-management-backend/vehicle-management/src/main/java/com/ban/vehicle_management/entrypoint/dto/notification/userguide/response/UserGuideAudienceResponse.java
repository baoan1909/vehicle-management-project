package com.ban.vehicle_management.entrypoint.dto.notification.userguide.response;

import com.ban.vehicle_management.shared.enumeration.notification.UserGuideAudienceTargetType;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserGuideAudienceResponse {

    private UUID audienceId;
    private UserGuideAudienceTargetType targetType;
    private UUID roleId;
    private UUID permissionId;
}
