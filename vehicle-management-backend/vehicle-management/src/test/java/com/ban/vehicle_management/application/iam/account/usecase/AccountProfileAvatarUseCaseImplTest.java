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
import com.ban.vehicle_management.application.people.userprofile.port.in.UserProfileAvatarPortIn;
import com.ban.vehicle_management.domain.iam.account.model.AccountProfileState;
import com.ban.vehicle_management.domain.iam.account.policy.AccountOnboardingPolicy;
import com.ban.vehicle_management.domain.iam.account.policy.AccountProfilePolicy;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

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
    private UserProfileAvatarPortIn userProfileAvatarPortIn;

    @Mock
    private AccountProfileResultMapper accountProfileResultMapper;

    @Spy
    private AccountProfilePolicy accountProfilePolicy = new AccountProfilePolicy();

    @Spy
    private AccountOnboardingPolicy accountOnboardingPolicy = new AccountOnboardingPolicy();

    @InjectMocks
    private AccountProfileUseCaseImpl useCase;

    @Test
    void shouldUploadCurrentUserAvatarThroughSharedUserProfileAvatarUseCase() {
        UUID accountId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        String oldAvatar = "av/2026/06/11/" + userProfileId + "/pb-old-avatar.jpg";
        String newAvatar = "av/2026/06/11/" + userProfileId + "/pb-new-avatar.jpg";
        String publicAvatar = "https://cdn.example.com/files/" + newAvatar;
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", new byte[]{1, 2, 3});
        AccountProfileState initialState = state(accountId, userProfileId, oldAvatar);
        AccountProfileState updatedState = state(accountId, userProfileId, newAvatar);
        AccountProfileStatusResult mappedResult = result(accountId, userProfileId, newAvatar);
        UserProfile resolvedProfile = new UserProfile();
        resolvedProfile.setUserProfileId(userProfileId);
        resolvedProfile.setAvatarUrl(publicAvatar);

        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(accountProfilePortOut.findProfileStateByAccountId(accountId))
                .thenReturn(Optional.of(initialState), Optional.of(updatedState));
        when(userProfileAvatarPortIn.uploadAvatar(userProfileId, file, accountId))
                .thenReturn(profile(userProfileId, publicAvatar));
        when(accountProfileResultMapper.toStatusResult(updatedState, false)).thenReturn(mappedResult);
        when(userProfileAvatarPortIn.withResolvedAvatarUrl(any(UserProfile.class))).thenReturn(resolvedProfile);

        AccountProfileStatusResult result = useCase.uploadMyAvatar(file);

        verify(userProfileAvatarPortIn).uploadAvatar(userProfileId, file, accountId);
        verify(accountProfilePortOut, never()).updateProfile(eq(accountId), any(UserProfile.class));
        assertEquals(publicAvatar, result.profile().avatarUrl());
    }

    @Test
    void shouldPropagateSharedAvatarUploadFailure() {
        UUID accountId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        String oldAvatar = "av/2026/06/11/" + userProfileId + "/pb-old-avatar.jpg";
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", new byte[]{1, 2, 3});

        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(accountProfilePortOut.findProfileStateByAccountId(accountId))
                .thenReturn(Optional.of(state(accountId, userProfileId, oldAvatar)));
        when(userProfileAvatarPortIn.uploadAvatar(userProfileId, file, accountId))
                .thenThrow(new ConflictException("DB update failed"));

        assertThrows(ConflictException.class, () -> useCase.uploadMyAvatar(file));

        verify(userProfileAvatarPortIn).uploadAvatar(userProfileId, file, accountId);
        verify(accountProfileResultMapper, never()).toStatusResult(any(AccountProfileState.class), eq(false));
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
                .thenReturn(Optional.of(state(accountId, userProfileId, oldAvatar)), Optional.of(updatedState));
        when(userProfileAvatarPortIn.deleteAvatar(userProfileId)).thenReturn(profile(userProfileId, null));
        when(accountProfileResultMapper.toStatusResult(updatedState, false)).thenReturn(mappedResult);

        AccountProfileStatusResult result = useCase.deleteMyAvatar();

        verify(userProfileAvatarPortIn).deleteAvatar(userProfileId);
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
        verify(userProfileAvatarPortIn, never()).uploadAvatar(
                any(UUID.class),
                any(MultipartFile.class),
                any(UUID.class)
        );
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

    private UserProfile profile(UUID userProfileId, String avatarUrl) {
        UserProfile userProfile = new UserProfile();
        userProfile.setUserProfileId(userProfileId);
        userProfile.setAvatarUrl(avatarUrl);
        return userProfile;
    }
}
