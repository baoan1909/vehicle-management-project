package com.ban.vehicle_management.application.iam.account.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.mapper.AccountProfileResultMapper;
import com.ban.vehicle_management.application.iam.account.model.result.AccountProfileStatusResult;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountProfilePortOut;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.CustomerOnboardingApprovalPortOut;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.InternalEmployeeApprovalPortOut;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.SystemAdminApprovalPortOut;
import com.ban.vehicle_management.application.storage.model.StoreFileCommand;
import com.ban.vehicle_management.application.storage.model.StoredFile;
import com.ban.vehicle_management.application.storage.port.out.FileStoragePort;
import com.ban.vehicle_management.application.storage.service.StorageUrlResolver;
import com.ban.vehicle_management.domain.iam.account.model.AccountProfileState;
import com.ban.vehicle_management.domain.iam.account.policy.AccountProfilePolicy;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import com.ban.vehicle_management.shared.enumeration.storage.StorageBucket;
import com.ban.vehicle_management.shared.enumeration.storage.StorageFolder;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.time.LocalDate;
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
class AccountProfileAvatarUseCaseImplTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private AccountProfilePortOut accountProfilePortOut;

    @Mock
    private CustomerOnboardingApprovalPortOut customerOnboardingApprovalPortOut;

    @Mock
    private InternalEmployeeApprovalPortOut internalEmployeeApprovalPortOut;

    @Mock
    private SystemAdminApprovalPortOut systemAdminApprovalPortOut;

    @Mock
    private FileStoragePort fileStoragePort;

    @Mock
    private StorageUrlResolver storageUrlResolver;

    @Mock
    private AccountProfileResultMapper accountProfileResultMapper;

    @Mock
    private AccountProfilePolicy accountProfilePolicy;

    @InjectMocks
    private AccountProfileUseCaseImpl useCase;

    @Test
    void shouldUploadCurrentUserAvatarAndDeleteOldManagedAvatar() {
        UUID accountId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        String oldAvatar = "av/2026/06/11/" + userProfileId + "/pb-old-avatar.jpg";
        String newAvatar = "av/2026/06/11/" + userProfileId + "/pb-new-avatar.jpg";
        String publicAvatar = "https://cdn.example.com/files/" + newAvatar;
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", new byte[]{1, 2, 3});
        AccountProfileState initialState = state(accountId, userProfileId, oldAvatar);
        AccountProfileState updatedState = state(accountId, userProfileId, newAvatar);
        AccountProfileStatusResult mappedResult = result(accountId, userProfileId, newAvatar);

        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(accountProfilePortOut.findProfileStateByAccountId(accountId)).thenReturn(Optional.of(initialState));
        when(fileStoragePort.store(any(StoreFileCommand.class)))
                .thenReturn(new StoredFile(newAvatar, "avatar.jpg", "image/jpeg", 3, "checksum"));
        when(accountProfilePortOut.updateAvatar(accountId, newAvatar)).thenReturn(updatedState);
        when(accountProfileResultMapper.toStatusResult(updatedState, false)).thenReturn(mappedResult);
        when(storageUrlResolver.isManagedAvatarObjectKey(oldAvatar)).thenReturn(true);
        when(storageUrlResolver.isManagedAvatarObjectKey(newAvatar)).thenReturn(true);
        when(storageUrlResolver.resolvePublicAvatarUrl(newAvatar)).thenReturn(publicAvatar);

        AccountProfileStatusResult result = useCase.uploadMyAvatar(file);

        ArgumentCaptor<StoreFileCommand> commandCaptor = ArgumentCaptor.forClass(StoreFileCommand.class);
        verify(fileStoragePort).store(commandCaptor.capture());
        StoreFileCommand command = commandCaptor.getValue();
        assertEquals(StorageBucket.PUBLIC, command.bucket());
        assertEquals(StorageFolder.AVATAR, command.folder());
        assertEquals("people.user_profiles", command.resourceType());
        assertEquals(userProfileId, command.resourceId());
        assertEquals(accountId, command.ownerAccountId());
        verify(accountProfilePortOut).updateAvatar(accountId, newAvatar);
        verify(fileStoragePort).delete(oldAvatar);
        assertEquals(publicAvatar, result.profile().avatarUrl());
    }

    @Test
    void shouldCleanupNewAvatarWhenDatabaseUpdateFails() {
        UUID accountId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        String oldAvatar = "av/2026/06/11/" + userProfileId + "/pb-old-avatar.jpg";
        String newAvatar = "av/2026/06/11/" + userProfileId + "/pb-new-avatar.jpg";
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", new byte[]{1, 2, 3});

        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(accountProfilePortOut.findProfileStateByAccountId(accountId))
                .thenReturn(Optional.of(state(accountId, userProfileId, oldAvatar)));
        when(fileStoragePort.store(any(StoreFileCommand.class)))
                .thenReturn(new StoredFile(newAvatar, "avatar.jpg", "image/jpeg", 3, "checksum"));
        when(accountProfilePortOut.updateAvatar(accountId, newAvatar))
                .thenThrow(new ConflictException("DB update failed"));

        assertThrows(ConflictException.class, () -> useCase.uploadMyAvatar(file));

        verify(fileStoragePort).delete(newAvatar);
        verify(fileStoragePort, never()).delete(oldAvatar);
    }

    @Test
    void shouldDeleteCurrentUserAvatar() {
        UUID accountId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        String oldAvatar = "av/2026/06/11/" + userProfileId + "/pb-old-avatar.jpg";
        AccountProfileState updatedState = state(accountId, userProfileId, null);
        AccountProfileStatusResult mappedResult = result(accountId, userProfileId, null);

        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(accountProfilePortOut.findProfileStateByAccountId(accountId))
                .thenReturn(Optional.of(state(accountId, userProfileId, oldAvatar)));
        when(accountProfilePortOut.updateAvatar(accountId, null)).thenReturn(updatedState);
        when(accountProfileResultMapper.toStatusResult(updatedState, false)).thenReturn(mappedResult);
        when(storageUrlResolver.isManagedAvatarObjectKey(oldAvatar)).thenReturn(true);

        AccountProfileStatusResult result = useCase.deleteMyAvatar();

        verify(accountProfilePortOut).updateAvatar(accountId, null);
        verify(fileStoragePort).delete(oldAvatar);
        assertNull(result.profile().avatarUrl());
    }

    @Test
    void shouldRejectAvatarUploadWhenProfileIsNotReady() {
        UUID accountId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", new byte[]{1, 2, 3});

        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(accountProfilePortOut.findProfileStateByAccountId(accountId))
                .thenReturn(Optional.of(state(accountId, null, null)));

        assertThrows(ConflictException.class, () -> useCase.uploadMyAvatar(file));
        verify(fileStoragePort, never()).store(any(StoreFileCommand.class));
    }

    private AccountProfileState state(UUID accountId, UUID userProfileId, String avatarUrl) {
        return new AccountProfileState(
                accountId,
                "baoan3236",
                "baoan3236@example.com",
                "keycloak-user-id",
                null,
                userProfileId,
                userProfileId == null ? null : "Nguyen Bao An",
                userProfileId == null ? null : LocalDate.of(2003, 9, 19),
                "MALE",
                "0901234567",
                "Ho Chi Minh City",
                "079203001234",
                avatarUrl,
                userProfileId == null ? null : UserProfileStatus.ACTIVE,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                AccountStatus.ACTIVE
        );
    }

    private AccountProfileStatusResult result(UUID accountId, UUID userProfileId, String avatarUrl) {
        return new AccountProfileStatusResult(
                false,
                new AccountProfileStatusResult.AccountInfoResult(
                        accountId,
                        "ACTIVE",
                        "baoan3236",
                        "baoan3236@example.com",
                        "keycloak-user-id"
                ),
                new AccountProfileStatusResult.ProfileInfoResult(
                        userProfileId,
                        "Nguyen Bao An",
                        LocalDate.of(2003, 9, 19),
                        "MALE",
                        "0901234567",
                        "Ho Chi Minh City",
                        "079203001234",
                        avatarUrl,
                        "ACTIVE"
                ),
                null,
                null
        );
    }
}
