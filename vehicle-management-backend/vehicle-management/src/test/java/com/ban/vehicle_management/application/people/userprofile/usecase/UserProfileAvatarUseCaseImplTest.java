package com.ban.vehicle_management.application.people.userprofile.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.people.userprofile.port.out.UserProfilePortOut;
import com.ban.vehicle_management.application.storage.model.StoreFileCommand;
import com.ban.vehicle_management.application.storage.model.StoredFile;
import com.ban.vehicle_management.application.storage.port.out.FileStoragePort;
import com.ban.vehicle_management.application.storage.service.StorageUrlResolver;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.shared.enumeration.storage.StorageBucket;
import com.ban.vehicle_management.shared.enumeration.storage.StorageFolder;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class UserProfileAvatarUseCaseImplTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private UserProfilePortOut userProfilePort;

    @Mock
    private FileStoragePort fileStoragePort;

    @Mock
    private StorageUrlResolver storageUrlResolver;

    @InjectMocks
    private UserProfileUseCaseImpl useCase;

    @Test
    void shouldUploadAdminAvatarAndDeleteOldManagedAvatar() {
        UUID userProfileId = UUID.randomUUID();
        UUID uploaderAccountId = UUID.randomUUID();
        String oldAvatar = "av/2026/06/11/" + userProfileId + "/pb-old-avatar.jpg";
        String newAvatar = "av/2026/06/11/" + userProfileId + "/pb-new-avatar.jpg";
        String publicAvatar = "https://cdn.example.com/files/" + newAvatar;
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{1, 2, 3});

        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(uploaderAccountId);
        when(userProfilePort.findById(userProfileId)).thenReturn(Optional.of(profile(userProfileId, oldAvatar)));
        when(fileStoragePort.store(any(StoreFileCommand.class)))
                .thenReturn(new StoredFile(newAvatar, "avatar.png", "image/png", 3, "checksum"));
        when(userProfilePort.updateAvatar(userProfileId, newAvatar)).thenReturn(profile(userProfileId, newAvatar));
        when(storageUrlResolver.isManagedAvatarObjectKey(oldAvatar)).thenReturn(true);
        when(storageUrlResolver.isManagedAvatarObjectKey(newAvatar)).thenReturn(true);
        when(storageUrlResolver.resolvePublicAvatarUrl(newAvatar)).thenReturn(publicAvatar);

        UserProfile result = useCase.uploadAvatar(userProfileId, file);

        ArgumentCaptor<StoreFileCommand> commandCaptor = ArgumentCaptor.forClass(StoreFileCommand.class);
        verify(currentAccountPortIn).requirePermission("USER_PROFILE_UPDATE_ALL");
        verify(fileStoragePort).store(commandCaptor.capture());
        assertEquals(StorageBucket.PUBLIC, commandCaptor.getValue().bucket());
        assertEquals(StorageFolder.AVATAR, commandCaptor.getValue().folder());
        assertEquals(userProfileId, commandCaptor.getValue().resourceId());
        assertEquals(uploaderAccountId, commandCaptor.getValue().ownerAccountId());
        verify(userProfilePort).updateAvatar(userProfileId, newAvatar);
        verify(fileStoragePort).delete(oldAvatar);
        assertEquals(publicAvatar, result.getAvatarUrl());
    }

    @Test
    void shouldDeleteAdminAvatar() {
        UUID userProfileId = UUID.randomUUID();
        String oldAvatar = "av/2026/06/11/" + userProfileId + "/pb-old-avatar.jpg";

        when(userProfilePort.findById(userProfileId)).thenReturn(Optional.of(profile(userProfileId, oldAvatar)));
        when(userProfilePort.updateAvatar(userProfileId, null)).thenReturn(profile(userProfileId, null));
        when(storageUrlResolver.isManagedAvatarObjectKey(oldAvatar)).thenReturn(true);

        UserProfile result = useCase.deleteAvatar(userProfileId);

        verify(currentAccountPortIn).requirePermission("USER_PROFILE_UPDATE_ALL");
        verify(userProfilePort).updateAvatar(userProfileId, null);
        verify(fileStoragePort).delete(oldAvatar);
        assertNull(result.getAvatarUrl());
    }

    @Test
    void shouldRejectAdminAvatarUploadWhenProfileDoesNotExist() {
        UUID userProfileId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{1, 2, 3});

        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(UUID.randomUUID());
        when(userProfilePort.findById(userProfileId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.uploadAvatar(userProfileId, file));
        verify(fileStoragePort, never()).store(any(StoreFileCommand.class));
    }

    private UserProfile profile(UUID userProfileId, String avatarUrl) {
        UserProfile userProfile = new UserProfile();
        userProfile.setUserProfileId(userProfileId);
        userProfile.setFullName("Nguyen Bao An");
        userProfile.setAvatarUrl(avatarUrl);
        return userProfile;
    }
}
