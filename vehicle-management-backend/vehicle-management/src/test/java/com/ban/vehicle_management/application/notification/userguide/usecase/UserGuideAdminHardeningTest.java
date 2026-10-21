package com.ban.vehicle_management.application.notification.userguide.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.permission.port.out.PermissionPortOut;
import com.ban.vehicle_management.application.iam.role.port.out.RolePortOut;
import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuideFeaturePortOut;
import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuidePortOut;
import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuideVersionPortOut;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuide;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideVersion;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideErrorCode;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideStatus;
import com.ban.vehicle_management.shared.exception.UserGuideException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserGuideAdminHardeningTest {

    @Mock private UserGuidePortOut guidePortOut;
    @Mock private UserGuideFeaturePortOut featurePortOut;
    @Mock private UserGuideVersionPortOut versionPortOut;
    @Mock private RolePortOut rolePortOut;
    @Mock private PermissionPortOut permissionPortOut;
    @Mock private CurrentAccountPortIn currentAccountPortIn;
    @Mock private UserGuideAuditWriter auditWriter;

    private UserGuideAdminUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new UserGuideAdminUseCaseImpl(
                guidePortOut,
                featurePortOut,
                versionPortOut,
                rolePortOut,
                permissionPortOut,
                currentAccountPortIn,
                auditWriter);
    }

    @Test
    void shouldLockGuideBeforeAllocatingNextDraftNumberAndRejectArchivedGuide() {
        UUID guideId = UUID.randomUUID();
        UserGuide archived = archivedGuide(guideId);
        when(guidePortOut.findByIdForUpdate(guideId)).thenReturn(Optional.of(archived));

        UserGuideException exception = assertThrows(
                UserGuideException.class,
                () -> useCase.createDraftVersion(guideId, new UserGuideVersion(), null));

        assertEquals(UserGuideErrorCode.USER_GUIDE_VERSION_IMMUTABLE, exception.getErrorCode());
        verify(guidePortOut).findByIdForUpdate(guideId);
        verify(versionPortOut, never()).findByGuideIdOrderByVersionNumberDesc(guideId);
    }

    @Test
    void shouldRejectMetadataUpdateForArchivedGuide() {
        UUID guideId = UUID.randomUUID();
        UserGuide archived = archivedGuide(guideId);
        when(guidePortOut.findById(guideId)).thenReturn(Optional.of(archived));

        UserGuideException exception = assertThrows(
                UserGuideException.class,
                () -> useCase.updateGuide(guideId, new UserGuide(), archived.getLockVersion()));

        assertEquals(UserGuideErrorCode.USER_GUIDE_VERSION_IMMUTABLE, exception.getErrorCode());
        verify(guidePortOut, never()).save(archived);
    }

    private UserGuide archivedGuide(UUID guideId) {
        UserGuide guide = new UserGuide();
        guide.setGuideId(guideId);
        guide.setStatus(UserGuideStatus.ARCHIVED);
        guide.setLockVersion(3L);
        return guide;
    }
}
