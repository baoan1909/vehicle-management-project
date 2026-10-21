package com.ban.vehicle_management.domain.notification.userguide.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserGuideFeature extends AuditableDomainModel {

    private UUID featureId;
    private String code;
    private String name;
    private String description;
    private Boolean enabled;
}
