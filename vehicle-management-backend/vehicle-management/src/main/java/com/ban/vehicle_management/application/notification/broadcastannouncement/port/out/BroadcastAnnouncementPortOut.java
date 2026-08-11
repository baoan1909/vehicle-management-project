package com.ban.vehicle_management.application.notification.broadcastannouncement.port.out;

import com.ban.vehicle_management.domain.notification.broadcastannouncement.model.BroadcastAnnouncement;
import java.time.Instant;
import com.ban.vehicle_management.shared.enumeration.notification.BroadcastAnnouncementStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BroadcastAnnouncementPortOut {

    BroadcastAnnouncement save(BroadcastAnnouncement announcement);

    Optional<BroadcastAnnouncement> findById(UUID broadcastId);

    List<BroadcastAnnouncement> findAll(BroadcastAnnouncementStatus status);

    List<BroadcastAnnouncement> findActivePublishedForRole(String roleCode, Instant now);

    boolean existsByTitle(String title);

    boolean existsByTitleAndBroadcastIdNot(String title, UUID broadcastId);

    void deleteById(UUID broadcastId);
}
