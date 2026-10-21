package com.ban.vehicle_management.infrastructure.persistence.database.repository.notification;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.notification.UserGuideUserStateEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface UserGuideUserStateInitializationRepository
        extends Repository<UserGuideUserStateEntity, UUID> {

    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO notification.user_guide_user_states (
                user_guide_state_id, account_id, guide_id, created_at
            ) VALUES (
                :stateId, :accountId, :guideId, now()
            )
            ON CONFLICT (account_id, guide_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("stateId") UUID stateId,
            @Param("accountId") UUID accountId,
            @Param("guideId") UUID guideId
    );
}
