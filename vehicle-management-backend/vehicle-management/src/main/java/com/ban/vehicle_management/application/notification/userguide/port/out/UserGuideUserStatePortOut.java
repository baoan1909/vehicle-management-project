package com.ban.vehicle_management.application.notification.userguide.port.out;

import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideUserState;
import java.util.Optional;
import java.util.UUID;

public interface UserGuideUserStatePortOut {

    UserGuideUserState save(UserGuideUserState state);

    Optional<UserGuideUserState> findById(UUID stateId);

    Optional<UserGuideUserState> findByAccountIdAndGuideId(UUID accountId, UUID guideId);

    Optional<UserGuideUserState> findByAccountIdAndGuideIdForUpdate(UUID accountId, UUID guideId);
}
