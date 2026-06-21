package com.ban.vehicle_management.application.people.userprofile.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.people.userprofile.port.out.UserProfileAvatarPortOut;
import com.ban.vehicle_management.application.people.userprofile.port.out.UserProfilePortOut;
import com.ban.vehicle_management.application.storage.model.StoreFileCommand;
import com.ban.vehicle_management.application.storage.model.StoredFile;
import com.ban.vehicle_management.application.storage.port.out.FileStoragePort;
import com.ban.vehicle_management.application.storage.service.StorageUrlResolver;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfileAvatar;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileAvatarStatus;
import com.ban.vehicle_management.shared.enumeration.storage.StorageBucket;
import com.ban.vehicle_management.shared.enumeration.storage.StorageFolder;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class UserProfileAvatarUseCaseImplTest {

    @Mock
    private UserProfilePortOut userProfilePortOut;

    @Mock
    private UserProfileAvatarPortOut userProfileAvatarPortOut;

    @Mock
    private FileStoragePort fileStoragePort;

    @Mock
    private StorageUrlResolver storageUrlResolver;

    @InjectMocks
    private UserProfileAvatarUseCaseImpl useCase;

    @Test
    void shouldUploadAvatarAndDeleteOldManagedAvatar() {
        UUID userProfileId = UUID.randomUUID();
        UUID uploaderAccountId = UUID.randomUUID();
        String oldAvatar = "av/2026/06/11/" + userProfileId + "/pb-old-avatar.jpg";
        String newAvatar = "av/2026/06/11/" + userProfileId + "/pb-new-avatar.jpg";
        String publicAvatar = "https://cdn.example.com/files/" + newAvatar;
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{1, 2, 3});

        when(userProfilePortOut.findById(userProfileId)).thenReturn(Optional.of(profile(userProfileId, null)));
        when(userProfileAvatarPortOut.findCurrentByUserProfileId(userProfileId))
                .thenReturn(Optional.of(avatar(userProfileId, oldAvatar)));
        when(fileStoragePort.store(any(StoreFileCommand.class)))
                .thenReturn(new StoredFile(newAvatar, "avatar.png", "image/png", 3, "checksum"));
        when(userProfileAvatarPortOut.save(any(UserProfileAvatar.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(storageUrlResolver.isManagedAvatarObjectKey(oldAvatar)).thenReturn(true);
        when(storageUrlResolver.isManagedAvatarObjectKey(newAvatar)).thenReturn(true);
        when(storageUrlResolver.resolvePublicAvatarUrl(newAvatar)).thenReturn(publicAvatar);

        UserProfile result = useCase.uploadAvatar(userProfileId, file, uploaderAccountId);

        ArgumentCaptor<StoreFileCommand> commandCaptor = ArgumentCaptor.forClass(StoreFileCommand.class);
        verify(fileStoragePort).store(commandCaptor.capture());
        assertEquals(StorageBucket.PUBLIC, commandCaptor.getValue().bucket());
        assertEquals(StorageFolder.AVATAR, commandCaptor.getValue().folder());
        assertEquals(userProfileId, commandCaptor.getValue().resourceId());
        assertEquals(uploaderAccountId, commandCaptor.getValue().ownerAccountId());

        ArgumentCaptor<UserProfileAvatar> avatarCaptor = ArgumentCaptor.forClass(UserProfileAvatar.class);
        verify(userProfileAvatarPortOut).save(avatarCaptor.capture());
        UserProfileAvatar avatar = avatarCaptor.getValue();
        assertEquals(userProfileId, avatar.getUserProfileId());
        assertEquals(newAvatar, avatar.getObjectKey());
        assertEquals("avatar.png", avatar.getOriginalFilename());
        assertEquals("image/png", avatar.getContentType());
        assertEquals(3L, avatar.getSizeBytes());
        assertEquals("checksum", avatar.getChecksumSha256());
        assertEquals(StorageBucket.PUBLIC, avatar.getBucket());
        assertEquals(UserProfileAvatarStatus.ACTIVE, avatar.getStatus());
        assertEquals(true, avatar.getCurrent());
        assertEquals(uploaderAccountId, avatar.getUploadedByAccountId());

        InOrder inOrder = org.mockito.Mockito.inOrder(userProfileAvatarPortOut);
        inOrder.verify(userProfileAvatarPortOut).findCurrentByUserProfileId(userProfileId);
        inOrder.verify(userProfileAvatarPortOut).markCurrentAsReplaced(userProfileId);
        inOrder.verify(userProfileAvatarPortOut).save(any(UserProfileAvatar.class));
        verify(fileStoragePort).delete(oldAvatar);
        assertEquals(publicAvatar, result.getAvatarUrl());
    }

    @Test
    void shouldCleanupNewAvatarWhenDatabaseUpdateFails() {
        UUID userProfileId = UUID.randomUUID();
        UUID uploaderAccountId = UUID.randomUUID();
        String oldAvatar = "av/2026/06/11/" + userProfileId + "/pb-old-avatar.jpg";
        String newAvatar = "av/2026/06/11/" + userProfileId + "/pb-new-avatar.jpg";
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{1, 2, 3});

        when(userProfilePortOut.findById(userProfileId)).thenReturn(Optional.of(profile(userProfileId, null)));
        when(userProfileAvatarPortOut.findCurrentByUserProfileId(userProfileId))
                .thenReturn(Optional.of(avatar(userProfileId, oldAvatar)));
        when(fileStoragePort.store(any(StoreFileCommand.class)))
                .thenReturn(new StoredFile(newAvatar, "avatar.png", "image/png", 3, "checksum"));
        when(userProfileAvatarPortOut.save(any(UserProfileAvatar.class)))
                .thenThrow(new ConflictException("DB update failed"));

        assertThrows(ConflictException.class, () -> useCase.uploadAvatar(userProfileId, file, uploaderAccountId));

        verify(userProfileAvatarPortOut).markCurrentAsReplaced(userProfileId);
        verify(userProfileAvatarPortOut).save(any(UserProfileAvatar.class));
        verify(fileStoragePort).delete(newAvatar);
        verify(fileStoragePort, never()).delete(oldAvatar);
    }

    @Test
    void shouldDeleteAvatar() {
        UUID userProfileId = UUID.randomUUID();
        String oldAvatar = "av/2026/06/11/" + userProfileId + "/pb-old-avatar.jpg";

        when(userProfilePortOut.findById(userProfileId)).thenReturn(Optional.of(profile(userProfileId, null)));
        when(userProfileAvatarPortOut.findCurrentByUserProfileId(userProfileId))
                .thenReturn(Optional.of(avatar(userProfileId, oldAvatar)));
        when(storageUrlResolver.isManagedAvatarObjectKey(oldAvatar)).thenReturn(true);

        UserProfile result = useCase.deleteAvatar(userProfileId);

        verify(userProfileAvatarPortOut).markCurrentAsDeleted(userProfileId);
        verify(fileStoragePort).delete(oldAvatar);
        assertNull(result.getAvatarUrl());
    }

    @Test
    void shouldRejectUploadWhenProfileDoesNotExist() {
        UUID userProfileId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{1, 2, 3});

        when(userProfilePortOut.findById(userProfileId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.uploadAvatar(userProfileId, file, UUID.randomUUID()));
        verify(fileStoragePort, never()).store(any(StoreFileCommand.class));
    }

    @Test
    void shouldResolveAvatarFromCurrentAvatarTableBeforeProfileColumn() {
        UUID userProfileId = UUID.randomUUID();
        String tableAvatar = "av/2026/06/11/" + userProfileId + "/pb-table-avatar.jpg";
        String columnAvatar = "av/2026/06/10/" + userProfileId + "/pb-column-avatar.jpg";
        String publicAvatar = "https://cdn.example.com/files/" + tableAvatar;

        when(userProfileAvatarPortOut.findCurrentByUserProfileId(userProfileId))
                .thenReturn(Optional.of(avatar(userProfileId, tableAvatar)));
        when(storageUrlResolver.isManagedAvatarObjectKey(tableAvatar)).thenReturn(true);
        when(storageUrlResolver.resolvePublicAvatarUrl(tableAvatar)).thenReturn(publicAvatar);

        UserProfile result = useCase.withResolvedAvatarUrl(profile(userProfileId, columnAvatar));

        assertEquals(publicAvatar, result.getAvatarUrl());
    }

    @Test
    void shouldReturnNullWhenCurrentAvatarDoesNotExist() {
        UUID userProfileId = UUID.randomUUID();
        String columnAvatar = "av/2026/06/10/" + userProfileId + "/pb-column-avatar.jpg";

        when(userProfileAvatarPortOut.findCurrentByUserProfileId(userProfileId)).thenReturn(Optional.empty());

        UserProfile result = useCase.withResolvedAvatarUrl(profile(userProfileId, columnAvatar));

        assertNull(result.getAvatarUrl());
    }

    @Test
    void shouldIgnoreExternalProfileAvatarWhenCurrentAvatarDoesNotExist() {
        UUID userProfileId = UUID.randomUUID();
        String externalAvatar = "https://example.com/avatar.jpg";

        when(userProfileAvatarPortOut.findCurrentByUserProfileId(userProfileId)).thenReturn(Optional.empty());

        UserProfile result = useCase.withResolvedAvatarUrl(profile(userProfileId, externalAvatar));

        assertNull(result.getAvatarUrl());
    }

    @Test
    void shouldResolveAvatarUrlsInBulk() {
        UUID firstProfileId = UUID.randomUUID();
        UUID secondProfileId = UUID.randomUUID();
        String firstAvatar = "av/2026/06/11/" + firstProfileId + "/pb-first-avatar.jpg";
        String secondAvatar = "av/2026/06/10/" + secondProfileId + "/pb-second-avatar.jpg";
        String firstPublicAvatar = "https://cdn.example.com/files/" + firstAvatar;
        UserProfile firstProfile = profile(firstProfileId, null);
        UserProfile secondProfile = profile(secondProfileId, secondAvatar);

        when(userProfileAvatarPortOut.findCurrentByUserProfileIds(Set.of(firstProfileId, secondProfileId)))
                .thenReturn(Map.of(firstProfileId, avatar(firstProfileId, firstAvatar)));
        when(storageUrlResolver.isManagedAvatarObjectKey(firstAvatar)).thenReturn(true);
        when(storageUrlResolver.resolvePublicAvatarUrl(firstAvatar)).thenReturn(firstPublicAvatar);

        List<UserProfile> result = useCase.withResolvedAvatarUrls(List.of(firstProfile, secondProfile));

        assertEquals(firstPublicAvatar, result.get(0).getAvatarUrl());
        assertNull(result.get(1).getAvatarUrl());
        verify(userProfileAvatarPortOut).findCurrentByUserProfileIds(Set.of(firstProfileId, secondProfileId));
    }

    private UserProfile profile(UUID userProfileId, String avatarUrl) {
        UserProfile userProfile = new UserProfile();
        userProfile.setUserProfileId(userProfileId);
        userProfile.setFullName("Nguyen Bao An");
        userProfile.setAvatarUrl(avatarUrl);
        return userProfile;
    }

    private UserProfileAvatar avatar(UUID userProfileId, String objectKey) {
        UserProfileAvatar avatar = new UserProfileAvatar();
        avatar.setAvatarId(UUID.randomUUID());
        avatar.setUserProfileId(userProfileId);
        avatar.setObjectKey(objectKey);
        avatar.setBucket(StorageBucket.PUBLIC);
        avatar.setStatus(UserProfileAvatarStatus.ACTIVE);
        avatar.setCurrent(true);
        return avatar;
    }
}
