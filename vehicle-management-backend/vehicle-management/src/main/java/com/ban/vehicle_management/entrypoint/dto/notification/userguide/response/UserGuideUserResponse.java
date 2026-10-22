package com.ban.vehicle_management.entrypoint.dto.notification.userguide.response;

import com.ban.vehicle_management.shared.enumeration.notification.UserGuideDisplayType;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideReplayPolicy;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserGuideUserResponse {

    private UUID guideId;
    private String code;
    private String title;
    private String summary;
    private String deepLink;
    private UserGuideDisplayType displayType;
    private UserGuideReplayPolicy replayPolicy;
    private Integer priority;
    private Integer displayOrder;
    private Integer versionNumber;
    private List<UserGuideStepUserResponse> steps;
    private UserGuideUserStateResponse userState;
    private Instant startAt;
    private Instant endAt;
}
