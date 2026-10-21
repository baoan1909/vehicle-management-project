package com.ban.vehicle_management.domain.notification.userguide.policy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.notification.userguide.model.UserGuide;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideAudience;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideFeature;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideStep;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideVersion;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideAudienceMode;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideAudienceTargetType;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideDisplayType;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideReplayPolicy;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserGuidePolicyTest {

    private final UserGuidePolicy policy = new UserGuidePolicy();

    @Test
    void shouldNormalizeCodesAndDefaults() {
        UserGuideFeature feature = new UserGuideFeature();
        feature.setCode(" parking-guide ");
        feature.setName(" Parking guide ");
        policy.normalizeFeature(feature);

        UserGuide guide = validGuide();
        guide.setCode("parking-entry");
        guide.setContextKey(" parking_dashboard ");
        guide.setPriority(null);
        guide.setDisplayOrder(null);
        guide.setCurrentVersion(null);
        policy.normalizeGuide(guide);

        assertEquals("PARKING-GUIDE", feature.getCode());
        assertEquals("PARKING-ENTRY", guide.getCode());
        assertEquals("PARKING_DASHBOARD", guide.getContextKey());
        assertEquals(100, guide.getPriority());
        assertEquals(100, guide.getDisplayOrder());
        assertEquals(0, guide.getCurrentVersion());
    }

    @Test
    void shouldRejectInvalidCodeAndUnsafeOrdinaryText() {
        UserGuideFeature feature = new UserGuideFeature();
        feature.setCode("bad code");
        feature.setName("Valid");
        assertThrows(BadRequestException.class, () -> policy.normalizeFeature(feature));

        feature.setCode("VALID_CODE");
        feature.setName("<script>");
        assertThrows(BadRequestException.class, () -> policy.normalizeFeature(feature));
    }

    @Test
    void shouldValidateSchedule() {
        Instant start = Instant.parse("2026-10-16T00:00:00Z");
        assertDoesNotThrow(() -> policy.validateSchedule(start, start));
        assertDoesNotThrow(() -> policy.validateSchedule(start, null));
        assertThrows(BadRequestException.class, () -> policy.validateSchedule(null, null));
        assertThrows(BadRequestException.class,
                () -> policy.validateSchedule(start, start.minusSeconds(1)));
    }

    @Test
    void shouldAllowOnlyDefinedStateTransitions() {
        assertDoesNotThrow(() -> policy.validateTransition(UserGuideStatus.DRAFT, UserGuideStatus.PUBLISHED));
        assertDoesNotThrow(() -> policy.validateTransition(UserGuideStatus.PUBLISHED, UserGuideStatus.INACTIVE));
        assertDoesNotThrow(() -> policy.validateTransition(UserGuideStatus.INACTIVE, UserGuideStatus.PUBLISHED));
        assertDoesNotThrow(() -> policy.validateTransition(UserGuideStatus.DRAFT, UserGuideStatus.ARCHIVED));
        assertDoesNotThrow(() -> policy.validateTransition(UserGuideStatus.PUBLISHED, UserGuideStatus.ARCHIVED));
        assertDoesNotThrow(() -> policy.validateTransition(UserGuideStatus.INACTIVE, UserGuideStatus.ARCHIVED));

        assertThrows(ConflictException.class,
                () -> policy.validateTransition(UserGuideStatus.DRAFT, UserGuideStatus.INACTIVE));
        assertThrows(ConflictException.class,
                () -> policy.validateTransition(UserGuideStatus.PUBLISHED, UserGuideStatus.DRAFT));
        assertThrows(ConflictException.class,
                () -> policy.validateTransition(UserGuideStatus.INACTIVE, UserGuideStatus.DRAFT));
    }

    @Test
    void archivedShouldBeTerminal() {
        for (UserGuideStatus target : UserGuideStatus.values()) {
            assertThrows(ConflictException.class,
                    () -> policy.validateTransition(UserGuideStatus.ARCHIVED, target));
        }
    }

    @Test
    void shouldRejectEmptyDuplicateAndOverLimitSteps() {
        assertThrows(BadRequestException.class, () -> policy.normalizeSteps(List.of()));

        UserGuideStep first = validStep(0);
        UserGuideStep duplicate = validStep(0);
        assertThrows(BadRequestException.class, () -> policy.normalizeSteps(List.of(first, duplicate)));

        List<UserGuideStep> tooMany = new ArrayList<>();
        for (int index = 0; index <= UserGuidePolicy.MAX_STEPS; index++) {
            tooMany.add(validStep(index));
        }
        assertThrows(BadRequestException.class, () -> policy.normalizeSteps(tooMany));
    }

    @Test
    void shouldValidateCollapseBoundariesAndNormalizeNull() {
        UserGuideStep invalid = validStep(0);
        invalid.setCollapseMs(-1);
        assertThrows(BadRequestException.class, () -> policy.normalizeSteps(List.of(invalid)));

        UserGuideStep zero = validStep(0);
        zero.setCollapseMs(0);
        assertDoesNotThrow(() -> policy.normalizeSteps(List.of(zero)));

        UserGuideStep maximum = validStep(0);
        maximum.setCollapseMs(600_000);
        assertDoesNotThrow(() -> policy.normalizeSteps(List.of(maximum)));

        UserGuideStep nullable = validStep(0);
        nullable.setCollapseMs(null);
        nullable.setAnchorKey(null);
        nullable.setMediaObjectKey(null);
        policy.normalizeSteps(List.of(nullable));
        assertEquals(0, nullable.getCollapseMs());
        assertNull(nullable.getAnchorKey());
    }

    @Test
    void shouldValidateEveryAudienceMode() {
        assertDoesNotThrow(() -> policy.validateAudience(UserGuideAudienceMode.PUBLIC, null));
        assertDoesNotThrow(() -> policy.validateAudience(UserGuideAudienceMode.ALL_AUTHENTICATED, List.of()));

        UserGuideAudience role = roleAudience(UUID.randomUUID());
        assertDoesNotThrow(() -> policy.validateAudience(UserGuideAudienceMode.ROLE, List.of(role)));
        assertThrows(BadRequestException.class,
                () -> policy.validateAudience(UserGuideAudienceMode.ROLE, List.of()));

        UserGuideAudience permission = permissionAudience(UUID.randomUUID());
        assertDoesNotThrow(() -> policy.validateAudience(UserGuideAudienceMode.PERMISSION_ANY,
                List.of(permission)));
        assertDoesNotThrow(() -> policy.validateAudience(UserGuideAudienceMode.PERMISSION_ALL,
                List.of(permission)));
        assertThrows(BadRequestException.class,
                () -> policy.validateAudience(UserGuideAudienceMode.PUBLIC, List.of(role)));
        assertThrows(BadRequestException.class,
                () -> policy.validateAudience(UserGuideAudienceMode.PERMISSION_ANY, List.of(role)));
        assertThrows(BadRequestException.class,
                () -> policy.validateAudience(UserGuideAudienceMode.ROLE,
                        List.of(role, roleAudience(role.getRoleId()))));
    }

    @Test
    void shouldRejectUnsafeDeepLinksAndAcceptInternalPath() {
        UserGuideVersion version = validVersion();
        version.setDeepLink("/parking/lots?tab=active");
        assertDoesNotThrow(() -> policy.normalizeVersion(version));

        for (String unsafe : List.of(
                "javascript:alert(1)",
                "data:text/plain,hello",
                "https://example.com/guide",
                "//example.com/guide",
                "/parking\\admin"
        )) {
            UserGuideVersion unsafeVersion = validVersion();
            unsafeVersion.setDeepLink(unsafe);
            assertThrows(BadRequestException.class, () -> policy.normalizeVersion(unsafeVersion));
        }
    }

    @Test
    void shouldAcceptNullOptionalVersionFields() {
        UserGuideVersion version = validVersion();
        version.setSummary(null);
        version.setDeepLink(null);
        version.getSteps().getFirst().setAnchorKey(null);
        version.getSteps().getFirst().setMediaObjectKey(null);

        assertDoesNotThrow(() -> policy.normalizeVersion(version));
        assertNull(version.getSummary());
        assertNull(version.getDeepLink());
    }

    private UserGuide validGuide() {
        UserGuide guide = new UserGuide();
        guide.setFeatureId(UUID.randomUUID());
        guide.setCode("PARKING_ENTRY");
        guide.setDisplayType(UserGuideDisplayType.MODAL);
        guide.setReplayPolicy(UserGuideReplayPolicy.ONCE_PER_VERSION);
        guide.setContextKey("PARKING_DASHBOARD");
        guide.setStartAt(Instant.parse("2026-10-16T00:00:00Z"));
        return guide;
    }

    private UserGuideVersion validVersion() {
        UserGuideVersion version = new UserGuideVersion();
        version.setGuideId(UUID.randomUUID());
        version.setVersionNumber(1);
        version.setTitle("Parking entry guide");
        version.setAudienceMode(UserGuideAudienceMode.ALL_AUTHENTICATED);
        version.setSteps(List.of(validStep(0)));
        return version;
    }

    private UserGuideStep validStep(int order) {
        UserGuideStep step = new UserGuideStep();
        step.setDisplayOrder(order);
        step.setTitle("Step " + order);
        step.setBody("Body " + order);
        step.setCollapseMs(0);
        return step;
    }

    private UserGuideAudience roleAudience(UUID roleId) {
        UserGuideAudience audience = new UserGuideAudience();
        audience.setTargetType(UserGuideAudienceTargetType.ROLE);
        audience.setRoleId(roleId);
        return audience;
    }

    private UserGuideAudience permissionAudience(UUID permissionId) {
        UserGuideAudience audience = new UserGuideAudience();
        audience.setTargetType(UserGuideAudienceTargetType.PERMISSION);
        audience.setPermissionId(permissionId);
        return audience;
    }
}
