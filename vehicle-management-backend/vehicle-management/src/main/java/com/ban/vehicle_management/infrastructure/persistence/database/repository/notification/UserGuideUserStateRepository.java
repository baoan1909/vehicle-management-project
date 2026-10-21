package com.ban.vehicle_management.infrastructure.persistence.database.repository.notification;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.notification.UserGuideUserStateEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserGuideUserStateRepository extends JpaRepository<UserGuideUserStateEntity, UUID> {

    Optional<UserGuideUserStateEntity> findByAccountIdAndGuideId(UUID accountId, UUID guideId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select state from UserGuideUserStateEntity state"
            + " where state.accountId = :accountId and state.guideId = :guideId")
    Optional<UserGuideUserStateEntity> findByAccountIdAndGuideIdForUpdate(
            @Param("accountId") UUID accountId, @Param("guideId") UUID guideId);
}
