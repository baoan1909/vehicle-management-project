package com.ban.vehicle_management.entrypoint.dto.notification.userguide.response;

import com.ban.vehicle_management.shared.enumeration.notification.UserGuideAudienceMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserGuideVersionAdminResponse {

    private UUID guideVersionId;
    private UUID guideId;
    private Integer versionNumber;
    private String title;
    private String summary;
    private String deepLink;
    private UserGuideAudienceMode audienceMode;
    private Instant publishedAt;
    private UUID publishedBy;
    private List<UserGuideStepResponse> steps;
    private List<UserGuideAudienceResponse> audiences;
    private Instant createdAt;
    private UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;
}
