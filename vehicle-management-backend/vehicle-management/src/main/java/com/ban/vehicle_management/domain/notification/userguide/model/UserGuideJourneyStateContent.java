package com.ban.vehicle_management.domain.notification.userguide.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserGuideJourneyStateContent {
    private String description;
    private String ownerHint;
    private String actionLabel;
}
