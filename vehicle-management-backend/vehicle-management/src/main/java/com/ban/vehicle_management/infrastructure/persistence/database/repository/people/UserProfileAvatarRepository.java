package com.ban.vehicle_management.infrastructure.persistence.database.repository.people;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileAvatarEntity;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileAvatarRepository extends JpaRepository<UserProfileAvatarEntity, UUID> {

    Optional<UserProfileAvatarEntity> findByUserProfileIdAndCurrentTrue(UUID userProfileId);

    List<UserProfileAvatarEntity> findByUserProfileIdInAndCurrentTrue(Set<UUID> userProfileIds);
}
