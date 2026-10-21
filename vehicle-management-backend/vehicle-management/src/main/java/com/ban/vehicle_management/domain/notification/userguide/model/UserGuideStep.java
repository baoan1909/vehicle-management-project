package com.ban.vehicle_management.domain.notification.userguide.model;

import com.ban.vehicle_management.shared.enumeration.notification.UserGuideStepPlacement;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserGuideStep {

    private UUID stepId;
    private UUID guideVersionId;
    private Integer displayOrder;
    private String title;
    private String body;
    private String mediaObjectKey;
    private String anchorKey;
    private UserGuideStepPlacement placement;
    private Integer collapseMs;
}
