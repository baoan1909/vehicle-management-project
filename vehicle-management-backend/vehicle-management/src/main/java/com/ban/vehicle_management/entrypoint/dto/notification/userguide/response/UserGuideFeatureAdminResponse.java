package com.ban.vehicle_management.entrypoint.dto.notification.userguide.response;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserGuideFeatureAdminResponse {

    private UUID featureId;
    private String code;
    private String name;
    private String description;
    private Boolean enabled;
    private Instant createdAt;
    private UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;
}
