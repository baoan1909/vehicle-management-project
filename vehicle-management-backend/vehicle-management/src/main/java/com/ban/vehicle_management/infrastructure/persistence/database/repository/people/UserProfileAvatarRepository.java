package com.ban.vehicle_management.infrastructure.persistence.database.repository.people;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileAvatarEntity;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileAvatarStatus;

public interface UserProfileAvatarRepository extends JpaRepository<UserProfileAvatarEntity, UUID> {

    @Query(value = """
        select object_key from people.user_profile_avatars
        where user_profile_id=:profileId and object_key not like '_deleted_%'
        """, nativeQuery = true)
    List<String> findDeletionObjectKeys(@Param("profileId") UUID profileId);

    @Modifying
    @Query(value = """
        update people.user_profile_avatars set status='DELETED', is_current=false, updated_at=:now
        where user_profile_id=:profileId
        """, nativeQuery = true)
    int markDeletedByUserProfileId(@Param("profileId") UUID profileId, @Param("now") Instant now);

    @Modifying
    @Query(value = """
        update people.user_profile_avatars
           set object_key=concat('_deleted_', avatar_id), original_filename=null,
               content_type=null, size_bytes=null, checksum_sha256=null, updated_at=:now
         where user_profile_id=:profileId
        """, nativeQuery = true)
    int completeDeletionCleanup(@Param("profileId") UUID profileId, @Param("now") Instant now);

    Optional<UserProfileAvatarEntity> findByUserProfileIdAndCurrentTrue(UUID userProfileId);

    Optional<UserProfileAvatarEntity> findByUserProfileIdAndStatus(UUID userProfileId, UserProfileAvatarStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select avatar from UserProfileAvatarEntity avatar where avatar.avatarId = :avatarId")
    Optional<UserProfileAvatarEntity> findByIdForUpdate(@Param("avatarId") UUID avatarId);

    List<UserProfileAvatarEntity> findByUserProfileIdInAndCurrentTrue(Set<UUID> userProfileIds);

    List<UserProfileAvatarEntity> findByUserProfileId(UUID userProfileId);
}
