package com.ban.vehicle_management.entrypoint.dto.people.userprofile.request;

import com.ban.vehicle_management.shared.enumeration.UserProfileStatus;

public record UserProfileFilterRequest(
        UserProfileStatus status,
        String keyword
) {
}
