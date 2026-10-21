package com.ban.vehicle_management.application.notification.userguide.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.permission.port.out.PermissionPortOut;
import com.ban.vehicle_management.application.iam.role.port.out.RolePortOut;
import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuideFeaturePortOut;
import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuidePortOut;
import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuideVersionPortOut;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuide;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideFeature;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideStep;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideVersion;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideAudienceMode;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideDisplayType;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideErrorCode;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideReplayPolicy;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideStatus;
import com.ban.vehicle_management.shared.exception.UserGuideException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserGuideAdminUseCaseImplTest {

    @Mock
    private UserGuidePortOut guidePortOut;

    @Mock
    private UserGuideFeaturePortOut featurePortOut;

    @Mock
    private UserGuideVersionPortOut versionPortOut;

    @Mock
    private RolePortOut rolePortOut;

    @Mock
    private PermissionPortOut permissionPortOut;

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private UserGuideAuditWriter auditWriter;

    @InjectMocks
    private UserGuideAdminUseCaseImpl useCase;

    @Test
    void shouldCreateGuideWithInitialVersion() {
        UserGuideFeature feature = enabledFeature();
        when(featurePortOut.findById(feature.getFeatureId())).thenReturn(Optional.of(feature));
        when(guidePortOut.existsByCode("GUIDE_ONE")).thenReturn(false);
        when(guidePortOut.save(any(UserGuide.class))).thenAnswer(invocation -> {
            UserGuide saved = invocation.getArgument(0);
            if (saved.getGuideId() == null) {
                saved.setGuideId(UUID.randomUUID());
            }
            return saved;
        });
        when(versionPortOut.save(any(UserGuideVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserGuide created = useCase.createGuide(validGuide(feature), validVersion());

        assertEquals(UserGuideStatus.DRAFT, created.getStatus());
        assertEquals(Integer.valueOf(0), created.getCurrentVersion());
        verify(guidePortOut).save(any(UserGuide.class));
        verify(versionPortOut).save(any(UserGuideVersion.class));
    }

    @Test
    void shouldRejectDuplicateGuideCode() {
        UserGuideFeature feature = enabledFeature();
        when(featurePortOut.findById(feature.getFeatureId())).thenReturn(Optional.of(feature));
        when(guidePortOut.existsByCode("GUIDE_ONE")).thenReturn(true);

        UserGuideException exception = assertThrows(
                UserGuideException.class, () -> useCase.createGuide(validGuide(feature), validVersion()));
        assertEquals(UserGuideErrorCode.USER_GUIDE_CODE_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    void shouldRejectCreateWhenFeatureMissing() {
        when(featurePortOut.findById(any(UUID.class))).thenReturn(Optional.empty());

        UserGuideException exception = assertThrows(
                UserGuideException.class, () -> useCase.createGuide(validGuide(), validVersion()));
        assertEquals(UserGuideErrorCode.USER_GUIDE_FEATURE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void shouldRejectStaleLockOnUpdate() {
        UserGuide existing = publishedGuide();
        existing.setLockVersion(7L);
        when(guidePortOut.findById(existing.getGuideId())).thenReturn(Optional.of(existing));

        UserGuideException exception = assertThrows(
                UserGuideException.class,
                () -> useCase.updateGuide(existing.getGuideId(), new UserGuide(), 6L));
        assertEquals(UserGuideErrorCode.USER_GUIDE_OPTIMISTIC_LOCK_CONFLICT, exception.getErrorCode());
    }

    @Test
    void shouldRejectPublishedVersionUpdate() {
        UserGuide guide = publishedGuide();
        guide.setCurrentVersion(2);
        when(guidePortOut.findById(guide.getGuideId())).thenReturn(Optional.of(guide));
        UserGuideVersion published = validVersion();
        published.setVersionNumber(2);
        when(versionPortOut.findByGuideIdAndVersionNumber(guide.getGuideId(), 2))
                .thenReturn(Optional.of(published));

        UserGuideException exception = assertThrows(
                UserGuideException.class,
                () -> useCase.updateDraftVersion(guide.getGuideId(), 2, new UserGuideVersion(), 1L));
        assertEquals(UserGuideErrorCode.USER_GUIDE_VERSION_IMMUTABLE, exception.getErrorCode());
    }

    @Test
    void shouldPublishDraftAtomically() {
        UserGuide guide = draftGuide();
        UserGuideVersion draft = validVersion();
        draft.setGuideId(guide.getGuideId());
        draft.setVersionNumber(1);
        when(guidePortOut.findByIdForUpdate(guide.getGuideId())).thenReturn(Optional.of(guide));
        when(versionPortOut.findByGuideIdAndVersionNumber(guide.getGuideId(), 1))
                .thenReturn(Optional.of(draft));
        when(featurePortOut.findById(guide.getFeatureId())).thenReturn(Optional.of(enabledFeature()));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(UUID.randomUUID());
        when(versionPortOut.save(any(UserGuideVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(guidePortOut.save(any(UserGuide.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserGuideVersion published = useCase.publishVersion(guide.getGuideId(), 1);

        assertEquals(Integer.valueOf(1), guide.getCurrentVersion());
        assertEquals(UserGuideStatus.PUBLISHED, guide.getStatus());
        verify(versionPortOut).save(draft);
        verify(guidePortOut).save(guide);
    }

    @Test
    void shouldRejectRepublishOfOldVersion() {
        UserGuide guide = publishedGuide();
        guide.setCurrentVersion(2);
        when(guidePortOut.findByIdForUpdate(guide.getGuideId())).thenReturn(Optional.of(guide));
        UserGuideVersion old = validVersion();
        old.setVersionNumber(1);
        when(versionPortOut.findByGuideIdAndVersionNumber(guide.getGuideId(), 1))
                .thenReturn(Optional.of(old));

        UserGuideException exception = assertThrows(
                UserGuideException.class, () -> useCase.publishVersion(guide.getGuideId(), 1));
        assertEquals(UserGuideErrorCode.USER_GUIDE_PUBLISH_CONFLICT, exception.getErrorCode());
    }

    @Test
    void shouldArchivePublishedGuide() {
        UserGuide guide = publishedGuide();
        when(guidePortOut.findById(guide.getGuideId())).thenReturn(Optional.of(guide));
        when(guidePortOut.save(any(UserGuide.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserGuide archived = useCase.archiveGuide(guide.getGuideId());

        assertEquals(UserGuideStatus.ARCHIVED, archived.getStatus());
    }

    private UserGuideFeature enabledFeature() {
        UserGuideFeature feature = new UserGuideFeature();
        feature.setFeatureId(UUID.randomUUID());
        feature.setCode("PARKING_CHECKIN");
        feature.setName("Parking check-in");
        feature.setEnabled(true);
        return feature;
    }

    private UserGuide validGuide() {
        return validGuide(null);
    }

    private UserGuide validGuide(UserGuideFeature feature) {
        UserGuide guide = new UserGuide();
        guide.setFeatureId(feature == null ? UUID.randomUUID() : feature.getFeatureId());
        guide.setCode("GUIDE_ONE");        guide.setContextKey("PARKING_CHECKIN");
        guide.setDisplayType(UserGuideDisplayType.MODAL);
        guide.setReplayPolicy(UserGuideReplayPolicy.ONCE_PER_VERSION);
        guide.setStartAt(Instant.parse("2026-01-01T00:00:00Z"));
        guide.setLockVersion(0L);
        return guide;
    }

    private UserGuide draftGuide() {
        UserGuide guide = validGuide();
        guide.setGuideId(UUID.randomUUID());
        guide.setStatus(UserGuideStatus.DRAFT);
        guide.setCurrentVersion(0);
        return guide;
    }

    private UserGuide publishedGuide() {
        UserGuide guide = validGuide();
        guide.setGuideId(UUID.randomUUID());
        guide.setStatus(UserGuideStatus.PUBLISHED);
        guide.setCurrentVersion(1);
        guide.setLockVersion(1L);
        return guide;
    }

    private UserGuideVersion validVersion() {
        UserGuideStep step = new UserGuideStep();
        step.setDisplayOrder(0);
        step.setTitle("Step one");
        step.setBody("Do this first");
        UserGuideVersion version = new UserGuideVersion();
        version.setTitle("Version one");
        version.setAudienceMode(UserGuideAudienceMode.ALL_AUTHENTICATED);
        version.setSteps(List.of(step));
        version.setAudiences(List.of());
        return version;
    }
}
