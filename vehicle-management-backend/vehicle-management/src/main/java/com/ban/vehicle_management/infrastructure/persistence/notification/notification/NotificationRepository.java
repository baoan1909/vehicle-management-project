package com.ban.vehicle_management.infrastructure.persistence.notification.notification;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {
}
