package com.ban.vehicle_management.application.storage.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.storage.port.out.FileAccessPort;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StorageUrlResolverTest {

    @Mock
    private FileAccessPort fileAccessPort;

    @Test
    void shouldResolveManagedAvatarToConfiguredPublicUrl() {
        StorageUrlResolver resolver = new StorageUrlResolver(fileAccessPort);
        String objectKey = "av/2026/06/11/" + UUID.randomUUID() + "/pb-new-avatar.jpg";
        String publicUrl = "https://cdn.example.com/files/" + objectKey;
        when(fileAccessPort.createPublicUrl(objectKey)).thenReturn(Optional.of(publicUrl));

        assertTrue(resolver.isManagedAvatarObjectKey(objectKey));
        assertEquals(publicUrl, resolver.resolvePublicAvatarUrl(objectKey));
    }

    @Test
    void shouldKeepExternalAvatarUrlUnchanged() {
        StorageUrlResolver resolver = new StorageUrlResolver(fileAccessPort);
        String externalUrl = "https://example.com/avatar.jpg";

        assertFalse(resolver.isManagedAvatarObjectKey(externalUrl));
        assertEquals(externalUrl, resolver.resolvePublicAvatarUrl(externalUrl));
        verify(fileAccessPort, never()).createPublicUrl(externalUrl);
    }
}
