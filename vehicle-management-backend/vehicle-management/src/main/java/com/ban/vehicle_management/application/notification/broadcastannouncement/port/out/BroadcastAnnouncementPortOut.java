package com.ban.vehicle_management.application.notification.broadcastannouncement.port.out;

import com.ban.vehicle_management.domain.notification.broadcastannouncement.model.BroadcastAnnouncement;
import com.ban.vehicle_management.shared.enumeration.notification.BroadcastAnnouncementStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BroadcastAnnouncementPortOut {

    BroadcastAnnouncement save(BroadcastAnnouncement announcement);

    Optional<BroadcastAnnouncement> findById(UUID broadcastId);

    List<BroadcastAnnouncement> findAll(BroadcastAnnouncementStatus status);

    void deleteById(UUID broadcastId);
}
