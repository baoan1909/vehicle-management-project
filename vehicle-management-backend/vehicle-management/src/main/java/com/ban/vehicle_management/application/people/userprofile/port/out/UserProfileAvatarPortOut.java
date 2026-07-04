package com.ban.vehicle_management.application.people.userprofile.port.out;

import com.ban.vehicle_management.domain.people.userprofile.model.UserProfileAvatar;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface UserProfileAvatarPortOut {

    Optional<UserProfileAvatar> findCurrentByUserProfileId(UUID userProfileId);

    Map<UUID, UserProfileAvatar> findCurrentByUserProfileIds(Set<UUID> userProfileIds);

    UserProfileAvatar save(UserProfileAvatar userProfileAvatar);

    void markCurrentAsReplaced(UUID userProfileId);

    void markCurrentAsDeleted(UUID userProfileId);
}
