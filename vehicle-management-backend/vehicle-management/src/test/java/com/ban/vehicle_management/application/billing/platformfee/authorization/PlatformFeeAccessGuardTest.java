package com.ban.vehicle_management.application.billing.platformfee.authorization;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class PlatformFeeAccessGuardTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @InjectMocks
    private PlatformFeeAccessGuard guard;

    @Test
    void shouldRequireCreatePermissionWithoutRoleCheck() {
        guard.ensureCanCreate();

        verify(currentAccountPortIn).requirePermission("PLATFORM_FEE_CREATE_ALL");
    }

    @Test
    void shouldRequireReadPermission() {
        guard.ensureCanRead();

        verify(currentAccountPortIn).requirePermission("PLATFORM_FEE_READ_ALL");
    }

    @Test
    void shouldRequireUpdatePermission() {
        guard.ensureCanUpdate();

        verify(currentAccountPortIn).requirePermission("PLATFORM_FEE_UPDATE_ALL");
    }

    @Test
    void shouldRequireActivatePermission() {
        guard.ensureCanActivate();

        verify(currentAccountPortIn).requirePermission("PLATFORM_FEE_ACTIVATE_ALL");
    }

    @Test
    void shouldRequireArchivePermission() {
        guard.ensureCanArchive();

        verify(currentAccountPortIn).requirePermission("PLATFORM_FEE_ARCHIVE_ALL");
    }

    @Test
    void shouldRequirePreviewPermission() {
        guard.ensureCanPreview();

        verify(currentAccountPortIn).requirePermission("PLATFORM_FEE_PREVIEW_ALL");
    }

    @Test
    void shouldRequireConfigurePermission() {
        guard.ensureCanConfigure();

        verify(currentAccountPortIn).requirePermission("PLATFORM_FEE_CONFIGURE_ALL");
    }

    @Test
    void shouldPropagateDeniedWhenPermissionRevoked() {
        doThrow(new AccessDeniedException("Access is denied"))
                .when(currentAccountPortIn).requirePermission("PLATFORM_FEE_READ_ALL");

        assertThrows(AccessDeniedException.class, guard::ensureCanRead);
    }
}
