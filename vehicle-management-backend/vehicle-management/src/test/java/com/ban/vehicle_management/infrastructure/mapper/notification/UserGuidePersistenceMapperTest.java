package com.ban.vehicle_management.infrastructure.mapper.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.ban.vehicle_management.domain.notification.userguide.model.UserGuide;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideAudience;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideStep;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideVersion;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideAudienceMode;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideAudienceTargetType;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideDisplayType;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideReplayPolicy;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideStatus;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideStepPlacement;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class UserGuidePersistenceMapperTest {

    private final UserGuidePersistenceMapper mapper = Mappers.getMapper(UserGuidePersistenceMapper.class);

    @Test
    void shouldRoundTripGuideWithoutLosingFields() {
        UUID guideId = UUID.randomUUID();
        UUID featureId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-16T01:02:03Z");
        Instant updatedAt = Instant.parse("2026-10-16T02:03:04Z");

        UserGuide source = new UserGuide();
        source.setGuideId(guideId);
        source.setFeatureId(featureId);
        source.setCode("PARKING_ENTRY");
        source.setStatus(UserGuideStatus.PUBLISHED);
        source.setDisplayType(UserGuideDisplayType.MODAL);
        source.setReplayPolicy(UserGuideReplayPolicy.ONCE_PER_VERSION);
        source.setContextKey("PARKING_DASHBOARD");
        source.setPriority(10);
        source.setDisplayOrder(20);
        source.setStartAt(createdAt);
        source.setEndAt(updatedAt);
        source.setCurrentVersion(3);
        source.setLockVersion(7L);
        source.setCreatedAt(createdAt);
        source.setCreatedBy(actorId);
        source.setUpdatedAt(updatedAt);
        source.setUpdatedBy(actorId);

        UserGuide roundTrip = mapper.toDomain(mapper.toEntity(source));

        assertEquals(source.getGuideId(), roundTrip.getGuideId());
        assertEquals(source.getFeatureId(), roundTrip.getFeatureId());
        assertEquals(source.getCode(), roundTrip.getCode());
        assertEquals(source.getStatus(), roundTrip.getStatus());
        assertEquals(source.getDisplayType(), roundTrip.getDisplayType());
        assertEquals(source.getReplayPolicy(), roundTrip.getReplayPolicy());
        assertEquals(source.getContextKey(), roundTrip.getContextKey());
        assertEquals(source.getPriority(), roundTrip.getPriority());
        assertEquals(source.getDisplayOrder(), roundTrip.getDisplayOrder());
        assertEquals(source.getStartAt(), roundTrip.getStartAt());
        assertEquals(source.getEndAt(), roundTrip.getEndAt());
        assertEquals(source.getCurrentVersion(), roundTrip.getCurrentVersion());
        assertEquals(source.getLockVersion(), roundTrip.getLockVersion());
        assertEquals(source.getCreatedAt(), roundTrip.getCreatedAt());
        assertEquals(source.getCreatedBy(), roundTrip.getCreatedBy());
        assertEquals(source.getUpdatedAt(), roundTrip.getUpdatedAt());
        assertEquals(source.getUpdatedBy(), roundTrip.getUpdatedBy());
    }

    @Test
    void shouldMapVersionChildrenExactlyOnce() {
        UUID versionId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        UserGuideStep step = new UserGuideStep(
                UUID.randomUUID(), versionId, 0, "Open dashboard", "Choose parking lot",
                "user-guides/dashboard.png", "PARKING_LOT_CARD", UserGuideStepPlacement.BOTTOM, 500);
        UserGuideAudience audience = new UserGuideAudience(
                UUID.randomUUID(), versionId, UserGuideAudienceTargetType.ROLE,
                roleId, null, Instant.parse("2026-10-16T00:00:00Z"), UUID.randomUUID());

        var stepDomains = mapper.toStepDomains(mapper.toStepEntities(List.of(step)));
        var audienceDomains = mapper.toAudienceDomains(mapper.toAudienceEntities(List.of(audience)));

        assertEquals(1, stepDomains.size());
        assertEquals(step.getStepId(), stepDomains.getFirst().getStepId());
        assertEquals(step.getAnchorKey(), stepDomains.getFirst().getAnchorKey());
        assertEquals(1, audienceDomains.size());
        assertEquals(audience.getAudienceId(), audienceDomains.getFirst().getAudienceId());
        assertEquals(roleId, audienceDomains.getFirst().getRoleId());
        assertNull(audienceDomains.getFirst().getPermissionId());
    }

    @Test
    void versionScalarMappingShouldNotInventAuditOrAggregateFields() {
        UserGuideVersion source = new UserGuideVersion();
        source.setGuideVersionId(UUID.randomUUID());
        source.setGuideId(UUID.randomUUID());
        source.setVersionNumber(1);
        source.setTitle("First version");
        source.setAudienceMode(UserGuideAudienceMode.PUBLIC);

        UserGuideVersion roundTrip = mapper.toDomain(mapper.toEntity(source));

        assertEquals(source.getGuideVersionId(), roundTrip.getGuideVersionId());
        assertEquals(source.getGuideId(), roundTrip.getGuideId());
        assertEquals(source.getTitle(), roundTrip.getTitle());
        assertEquals(0, roundTrip.getSteps().size());
        assertEquals(0, roundTrip.getAudiences().size());
        assertNull(roundTrip.getCreatedBy());
        assertNull(roundTrip.getUpdatedBy());
    }
}
