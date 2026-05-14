package com.ban.vehicle_management.infrastructure.persistence.accesscontrol.subscription;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, UUID> {
}
