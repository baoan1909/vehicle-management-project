package com.ban.vehicle_management.entrypoint.dto.notification.userguide.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserGuideJourneyStateContentResponse {
    private String description;
    private String ownerHint;
    private String actionLabel;
}
