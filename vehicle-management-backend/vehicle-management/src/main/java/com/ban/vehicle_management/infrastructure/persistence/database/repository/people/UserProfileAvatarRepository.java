package com.ban.vehicle_management.infrastructure.persistence.database.repository.people;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileAvatarEntity;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileAvatarStatus;

public interface UserProfileAvatarRepository extends JpaRepository<UserProfileAvatarEntity, UUID> {

    Optional<UserProfileAvatarEntity> findByUserProfileIdAndCurrentTrue(UUID userProfileId);

    Optional<UserProfileAvatarEntity> findByUserProfileIdAndStatus(UUID userProfileId, UserProfileAvatarStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select avatar from UserProfileAvatarEntity avatar where avatar.avatarId = :avatarId")
    Optional<UserProfileAvatarEntity> findByIdForUpdate(@Param("avatarId") UUID avatarId);

    List<UserProfileAvatarEntity> findByUserProfileIdInAndCurrentTrue(Set<UUID> userProfileIds);

    List<UserProfileAvatarEntity> findByUserProfileId(UUID userProfileId);
}
