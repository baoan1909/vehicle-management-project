package com.ban.vehicle_management.application.notification.userguide.port.out;

import java.util.UUID;

/** Ensures the unique account/guide state row exists before transition locking. */
public interface UserGuideUserStateInitializerPortOut {

    void initializeIfAbsent(UUID accountId, UUID guideId);
}
