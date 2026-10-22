package com.ban.vehicle_management.entrypoint.dto.notification.userguide.response;

import com.ban.vehicle_management.shared.enumeration.notification.UserGuideDisplayType;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideReplayPolicy;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserGuideAdminResponse {

    private UUID guideId;
    private UUID featureId;
    private String code;
    private UserGuideStatus status;
    private UserGuideDisplayType displayType;
    private UserGuideReplayPolicy replayPolicy;
    private String contextKey;
    private Integer priority;
    private Integer displayOrder;
    private Instant startAt;
    private Instant endAt;
    private Integer currentVersion;
    private Long lockVersion;
    private Instant createdAt;
    private UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;
}
