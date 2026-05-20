package com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.SubscriptionEntity;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, UUID> {

    boolean existsByCardId(UUID cardId);

    boolean existsByCardIdAndStatusIn(UUID cardId, Collection<SubscriptionStatus> statuses);
}


