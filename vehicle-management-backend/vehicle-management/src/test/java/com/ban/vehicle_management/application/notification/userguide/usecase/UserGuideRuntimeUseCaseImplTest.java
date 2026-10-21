package com.ban.vehicle_management.application.notification.userguide.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.permission.port.out.PermissionPortOut;
import com.ban.vehicle_management.application.notification.userguide.model.result.ResolvedUserGuide;
import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuideFeaturePortOut;
import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuidePortOut;
import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuideUserStatePortOut;
import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuideVersionPortOut;
import com.ban.vehicle_management.application.storage.port.out.FileAccessPort;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.iam.permission.model.Permission;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuide;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideAudience;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideFeature;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideStep;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideUserState;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideVersion;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideAudienceMode;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideAudienceTargetType;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideDisplayType;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideErrorCode;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideEventType;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideReplayPolicy;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideStatus;
import com.ban.vehicle_management.shared.exception.UserGuideException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserGuideRuntimeUseCaseImplTest {

    private static final UUID ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-000000000701");
    private static final UUID ROLE_ID = UUID.fromString("00000000-0000-0000-0000-000000000702");

    @Mock
    private UserGuidePortOut guidePortOut;

    @Mock
    private UserGuideFeaturePortOut featurePortOut;

    @Mock
    private UserGuideVersionPortOut versionPortOut;

    @Mock
    private UserGuideUserStatePortOut statePortOut;

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private PermissionPortOut permissionPortOut;

    @Mock
    private FileAccessPort fileAccessPort;

    @Mock
    private UserGuideAuditWriter auditWriter;

    @InjectMocks
    private UserGuideRuntimeUseCaseImpl useCase;

    @Test
    void shouldResolvePublicGuideWithoutState() {
        mockAccount(Set.of());
        when(featurePortOut.findByCode("PARKING_CHECKIN")).thenReturn(Optional.of(enabledFeature()));
        UserGuide guide = publishedGuide(UserGuideReplayPolicy.ONCE_PER_VERSION);
        when(guidePortOut.findByFeatureIdAndStatus(any(UUID.class), eq(UserGuideStatus.PUBLISHED)))
                .thenReturn(List.of(guide));
        UserGuideVersion version = publishedVersion(UserGuideAudienceMode.PUBLIC);
        when(versionPortOut.findByGuideIdAndVersionNumber(guide.getGuideId(), 1))
                .thenReturn(Optional.of(version));
        when(statePortOut.findByAccountIdAndGuideId(ACCOUNT_ID, guide.getGuideId()))
                .thenReturn(Optional.empty());

        List<ResolvedUserGuide> resolved = useCase.resolveGuides("parking_checkin", null);

        assertEquals(1, resolved.size());
        assertEquals(guide.getGuideId(), resolved.get(0).guide().getGuideId());
    }

    @Test
    void shouldExcludeManualOnlyFromAutoResolve() {
        mockAccount(Set.of());
        when(featurePortOut.findByCode("PARKING_CHECKIN")).thenReturn(Optional.of(enabledFeature()));
        UserGuide guide = publishedGuide(UserGuideReplayPolicy.MANUAL_ONLY);
        when(guidePortOut.findByFeatureIdAndStatus(any(UUID.class), eq(UserGuideStatus.PUBLISHED)))
                .thenReturn(List.of(guide));

        assertTrue(useCase.resolveGuides("PARKING_CHECKIN", null).isEmpty());
    }

    @Test
    void shouldEnforceRoleAudienceFromServer() {
        mockAccount(Set.of(), UUID.randomUUID());
        when(featurePortOut.findByCode("PARKING_CHECKIN")).thenReturn(Optional.of(enabledFeature()));
        UserGuide guide = publishedGuide(UserGuideReplayPolicy.EVERY_SESSION);
        when(guidePortOut.findByFeatureIdAndStatus(any(UUID.class), eq(UserGuideStatus.PUBLISHED)))
                .thenReturn(List.of(guide));
        UserGuideVersion version = publishedVersion(UserGuideAudienceMode.ROLE);
        version.setAudiences(List.of(roleAudience(ROLE_ID)));
        when(versionPortOut.findByGuideIdAndVersionNumber(guide.getGuideId(), 1))
                .thenReturn(Optional.of(version));

        assertTrue(useCase.resolveGuides("PARKING_CHECKIN", null).isEmpty());
    }

    @Test
    void shouldEnforcePermissionAllAudience() {
        mockAccount(Set.of("VOUCHER_READ_ALL"));
        UUID permissionId = UUID.randomUUID();
        Permission permission = new Permission();
        permission.setPermissionId(permissionId);
        permission.setPermissionCode("VOUCHER_READ_ALL");
        when(permissionPortOut.findById(permissionId)).thenReturn(Optional.of(permission));
        when(featurePortOut.findByCode("PARKING_CHECKIN")).thenReturn(Optional.of(enabledFeature()));
        UserGuide guide = publishedGuide(UserGuideReplayPolicy.EVERY_SESSION);
        when(guidePortOut.findByFeatureIdAndStatus(any(UUID.class), eq(UserGuideStatus.PUBLISHED)))
                .thenReturn(List.of(guide));
        UserGuideVersion version = publishedVersion(UserGuideAudienceMode.PERMISSION_ALL);
        version.setAudiences(List.of(permissionAudience(permissionId)));
        when(versionPortOut.findByGuideIdAndVersionNumber(guide.getGuideId(), 1))
                .thenReturn(Optional.of(version));
        when(statePortOut.findByAccountIdAndGuideId(ACCOUNT_ID, guide.getGuideId()))
                .thenReturn(Optional.empty());

        assertEquals(1, useCase.resolveGuides("PARKING_CHECKIN", null).size());
    }

    @Test
    void shouldHideCompletedOncePerVersionGuide() {
        mockAccount(Set.of());
        when(featurePortOut.findByCode("PARKING_CHECKIN")).thenReturn(Optional.of(enabledFeature()));
        UserGuide guide = publishedGuide(UserGuideReplayPolicy.ONCE_PER_VERSION);
        when(guidePortOut.findByFeatureIdAndStatus(any(UUID.class), eq(UserGuideStatus.PUBLISHED)))
                .thenReturn(List.of(guide));
        UserGuideVersion version = publishedVersion(UserGuideAudienceMode.PUBLIC);
        when(versionPortOut.findByGuideIdAndVersionNumber(guide.getGuideId(), 1))
                .thenReturn(Optional.of(version));
        UserGuideUserState state = new UserGuideUserState();
        state.setCompletedVersion(1);
        state.setCompletedAt(Instant.now());
        when(statePortOut.findByAccountIdAndGuideId(ACCOUNT_ID, guide.getGuideId()))
                .thenReturn(Optional.of(state));

        assertTrue(useCase.resolveGuides("PARKING_CHECKIN", null).isEmpty());
    }

    @Test
    void shouldCompleteGuideIdempotently() {
        mockAccount(Set.of());
        UserGuide guide = publishedGuide(UserGuideReplayPolicy.ONCE_PER_VERSION);
        when(guidePortOut.findById(guide.getGuideId())).thenReturn(Optional.of(guide));
        UserGuideVersion version = publishedVersion(UserGuideAudienceMode.PUBLIC);
        when(versionPortOut.findByGuideIdAndVersionNumber(guide.getGuideId(), 1))
                .thenReturn(Optional.of(version));
        UserGuideUserState existing = new UserGuideUserState();
        existing.setAccountId(ACCOUNT_ID);
        existing.setGuideId(guide.getGuideId());
        existing.setCompletedVersion(1);
        existing.setCompletedAt(Instant.parse("2026-01-02T00:00:00Z"));
        when(statePortOut.findByAccountIdAndGuideIdForUpdate(ACCOUNT_ID, guide.getGuideId()))
                .thenReturn(Optional.of(existing));
        when(statePortOut.save(any(UserGuideUserState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserGuideUserState saved =
                useCase.recordEvent(guide.getGuideId(), UserGuideEventType.COMPLETED, 1, null);

        assertEquals(Integer.valueOf(1), saved.getCompletedVersion());
        verify(statePortOut).save(any(UserGuideUserState.class));
    }

    @Test
    void shouldRejectStepOutsidePublishedVersion() {
        mockAccount(Set.of());
        UserGuide guide = publishedGuide(UserGuideReplayPolicy.EVERY_SESSION);
        when(guidePortOut.findById(guide.getGuideId())).thenReturn(Optional.of(guide));
        UserGuideVersion version = publishedVersion(UserGuideAudienceMode.PUBLIC);
        when(versionPortOut.findByGuideIdAndVersionNumber(guide.getGuideId(), 1))
                .thenReturn(Optional.of(version));
        when(statePortOut.findByAccountIdAndGuideIdForUpdate(ACCOUNT_ID, guide.getGuideId()))
                .thenReturn(Optional.empty());

        UserGuideException exception = assertThrows(
                UserGuideException.class,
                () -> useCase.recordEvent(guide.getGuideId(), UserGuideEventType.STEP_VIEWED, 1, UUID.randomUUID()));
        assertEquals(UserGuideErrorCode.USER_GUIDE_INVALID_STEP, exception.getErrorCode());
    }

    @Test
    void shouldRejectReplayOfCompletedOncePerVersion() {
        mockAccount(Set.of());
        UserGuide guide = publishedGuide(UserGuideReplayPolicy.ONCE_PER_VERSION);
        when(guidePortOut.findById(guide.getGuideId())).thenReturn(Optional.of(guide));
        UserGuideVersion version = publishedVersion(UserGuideAudienceMode.PUBLIC);
        when(versionPortOut.findByGuideIdAndVersionNumber(guide.getGuideId(), 1))
                .thenReturn(Optional.of(version));
        UserGuideUserState state = new UserGuideUserState();
        state.setAccountId(ACCOUNT_ID);
        state.setGuideId(guide.getGuideId());
        state.setCompletedVersion(1);
        state.setCompletedAt(Instant.now());
        when(statePortOut.findByAccountIdAndGuideIdForUpdate(ACCOUNT_ID, guide.getGuideId()))
                .thenReturn(Optional.of(state));

        UserGuideException exception = assertThrows(
                UserGuideException.class,
                () -> useCase.recordEvent(guide.getGuideId(), UserGuideEventType.REPLAYED, 1, null));
        assertEquals(UserGuideErrorCode.USER_GUIDE_INVALID_STATE_TRANSITION, exception.getErrorCode());
    }

    @Test
    void shouldRejectEventForArchivedGuide() {
        mockAccount(Set.of());
        UserGuide guide = publishedGuide(UserGuideReplayPolicy.EVERY_SESSION);
        guide.setStatus(UserGuideStatus.ARCHIVED);
        when(guidePortOut.findById(guide.getGuideId())).thenReturn(Optional.of(guide));

        UserGuideException exception = assertThrows(
                UserGuideException.class,
                () -> useCase.recordEvent(guide.getGuideId(), UserGuideEventType.STARTED, 1, null));
        assertEquals(UserGuideErrorCode.USER_GUIDE_NOT_FOUND, exception.getErrorCode());
    }

    private void mockAccount(Set<String> permissions) {
        mockAccount(permissions, ROLE_ID);
    }

    private void mockAccount(Set<String> permissions, UUID roleId) {
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(new CurrentAccountAccess(
                ACCOUNT_ID, "sub", "user", "user@example.com", roleId, "CUSTOMER",
                AccountStatus.ACTIVE, null, null, null, permissions));
    }

    private UserGuideFeature enabledFeature() {
        UserGuideFeature feature = new UserGuideFeature();
        feature.setFeatureId(UUID.randomUUID());
        feature.setCode("PARKING_CHECKIN");
        feature.setEnabled(true);
        return feature;
    }

    private UserGuide publishedGuide(UserGuideReplayPolicy policy) {
        UserGuide guide = new UserGuide();
        guide.setGuideId(UUID.randomUUID());
        guide.setFeatureId(UUID.randomUUID());
        guide.setCode("GUIDE_ONE");
        guide.setStatus(UserGuideStatus.PUBLISHED);
        guide.setDisplayType(UserGuideDisplayType.MODAL);
        guide.setReplayPolicy(policy);
        guide.setContextKey("PARKING_CHECKIN");
        guide.setPriority(100);
        guide.setDisplayOrder(100);
        guide.setStartAt(Instant.parse("2026-01-01T00:00:00Z"));
        guide.setCurrentVersion(1);
        return guide;
    }

    private UserGuideVersion publishedVersion(UserGuideAudienceMode mode) {
        UserGuideStep step = new UserGuideStep();
        step.setStepId(UUID.randomUUID());
        step.setDisplayOrder(0);
        step.setTitle("Step one");
        step.setBody("Body");
        UserGuideVersion version = new UserGuideVersion();
        version.setGuideVersionId(UUID.randomUUID());
        version.setVersionNumber(1);
        version.setTitle("Title");
        version.setAudienceMode(mode);
        version.setPublishedAt(Instant.parse("2026-01-02T00:00:00Z"));
        version.setSteps(List.of(step));
        version.setAudiences(List.of());
        return version;
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
