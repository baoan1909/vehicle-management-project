package com.ban.vehicle_management.infrastructure.persistence.adapter.notification;

import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuideUserStateInitializerPortOut;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.notification.UserGuideUserStateInitializationRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UserGuideUserStateInitializerPersistenceAdapter
        implements UserGuideUserStateInitializerPortOut {

    private final UserGuideUserStateInitializationRepository repository;

    public UserGuideUserStateInitializerPersistenceAdapter(
            UserGuideUserStateInitializationRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public void initializeIfAbsent(UUID accountId, UUID guideId) {
        repository.insertIfAbsent(UUID.randomUUID(), accountId, guideId);
    }
}
