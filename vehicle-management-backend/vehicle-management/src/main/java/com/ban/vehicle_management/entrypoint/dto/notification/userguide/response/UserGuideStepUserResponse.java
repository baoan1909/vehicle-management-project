package com.ban.vehicle_management.entrypoint.dto.notification.userguide.response;

import com.ban.vehicle_management.shared.enumeration.notification.UserGuideStepPlacement;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserGuideStepUserResponse {

    private UUID stepId;
    private Integer displayOrder;
    private String title;
    private String body;
    private String mediaUrl;
    private String anchorKey;
    private UserGuideStepPlacement placement;
    private Integer collapseMs;
}
