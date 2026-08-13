package com.ban.vehicle_management.entrypoint.dto.notification.broadcastannouncement.request;

import com.ban.vehicle_management.shared.enumeration.notification.BroadcastAnnouncementStatus;

public record BroadcastAnnouncementFilterRequest(
        BroadcastAnnouncementStatus status
) {
}
