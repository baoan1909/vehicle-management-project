package com.ban.vehicle_management.application.notification.broadcastannouncement.port.in;

import com.ban.vehicle_management.domain.notification.broadcastannouncement.model.BroadcastAnnouncement;
import com.ban.vehicle_management.shared.enumeration.notification.BroadcastAnnouncementStatus;
import java.util.List;
import java.util.UUID;

public interface BroadcastAnnouncementPortIn {

    BroadcastAnnouncement createBroadcastAnnouncement(BroadcastAnnouncement announcement);

    BroadcastAnnouncement getBroadcastAnnouncementById(UUID broadcastId);

    List<BroadcastAnnouncement> getBroadcastAnnouncements(BroadcastAnnouncementStatus status);

    BroadcastAnnouncement updateBroadcastAnnouncement(UUID broadcastId, BroadcastAnnouncement request);

    BroadcastAnnouncement publishBroadcastAnnouncement(UUID broadcastId);

    BroadcastAnnouncement cancelBroadcastAnnouncement(UUID broadcastId);

    void deleteBroadcastAnnouncement(UUID broadcastId);
}
