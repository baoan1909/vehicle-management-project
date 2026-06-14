package com.ban.vehicle_management.application.iam.account.usecase;

import com.ban.vehicle_management.application.iam.account.mapper.AccountProfileResultMapper;
import com.ban.vehicle_management.application.iam.account.model.command.UpdateAccountProfileCommand;
import com.ban.vehicle_management.application.iam.account.model.result.AccountProfileStatusResult;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountProfilePortOut;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.CustomerOnboardingApprovalPortOut;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.InternalEmployeeApprovalPortOut;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.SystemAdminApprovalPortOut;
import com.ban.vehicle_management.application.storage.service.StorageUrlResolver;
import com.ban.vehicle_management.domain.iam.account.model.AccountProfileState;
import com.ban.vehicle_management.domain.iam.account.policy.AccountProfilePolicy;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerType;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateAccountProfileUseCaseImplTest {

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
    private AccountProfileResultMapper accountProfileResultMapper;

    @Mock
    private AccountProfilePolicy accountProfilePolicy;

    @Mock
    private StorageUrlResolver storageUrlResolver;

    @InjectMocks
    private AccountProfileUseCaseImpl useCase;

    @Test
    void shouldThrowConflictWhenProfileIsNotReady() {
        UUID accountId = UUID.randomUUID();
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(accountProfilePortOut.findProfileStateByAccountId(accountId))
                .thenReturn(Optional.of(stateWithoutProfile(accountId)));

        assertThrows(
                ConflictException.class,
                () -> useCase.updateMyProfile(new UpdateAccountProfileCommand(
                        "Nguyen Bao An",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ))
        );
    }

    @Test
    void shouldThrowBadRequestWhenPatchBodyIsEmpty() {
        UUID accountId = UUID.randomUUID();
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(accountProfilePortOut.findProfileStateByAccountId(accountId))
                .thenReturn(Optional.of(stateWithProfile(accountId)));
        doThrow(new BadRequestException("At least one profile field must be provided"))
                .when(accountProfilePolicy)
                .ensurePatchHasAtLeastOneField(org.mockito.ArgumentMatchers.any(UpdateAccountProfileCommand.class));

        assertThrows(
                BadRequestException.class,
                () -> useCase.updateMyProfile(new UpdateAccountProfileCommand(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ))
        );
    }

    @Test
    void shouldThrowConflictWhenPhoneNumberAlreadyExists() {
        UUID accountId = UUID.randomUUID();
        UUID userProfileId = UUID.fromString("ec761405-c091-4a65-b1dd-c8fb23f0d6bd");
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(accountProfilePortOut.findProfileStateByAccountId(accountId))
                .thenReturn(Optional.of(stateWithProfile(accountId)));
        when(accountProfilePolicy.normalizeForUpdate(org.mockito.ArgumentMatchers.any(UpdateAccountProfileCommand.class)))
                .thenReturn(new UpdateAccountProfileCommand(
                        null,
                        "+84909999999",
                        null,
                        null,
                        null,
                        null,
                        null
                ));
        when(accountProfileResultMapper.mergeProfile(
                org.mockito.ArgumentMatchers.any(AccountProfileState.class),
                org.mockito.ArgumentMatchers.any(UpdateAccountProfileCommand.class)
        )).thenReturn(buildUserProfile(
                UUID.fromString("ec761405-c091-4a65-b1dd-c8fb23f0d6bd"),
                "Nguyen Bao An",
                "+84909999999",
                LocalDate.of(2003, 9, 19),
                "MALE",
                "Ho Chi Minh City",
                "079203001234",
                "https://cdn.example.com/avatars/bao-an.jpg",
                UserProfileStatus.ACTIVE
        ));
        when(accountProfilePortOut.existsByPhoneNumberAndUserProfileIdNot("+84909999999", userProfileId))
                .thenReturn(true);
        doThrow(new ConflictException("Phone number already exists"))
                .when(accountProfilePolicy)
                .ensureUniqueForUpdate(eq(true), anyBoolean());

        assertThrows(
                ConflictException.class,
                () -> useCase.updateMyProfile(new UpdateAccountProfileCommand(
                        null,
                        "+84909999999",
                        null,
                        null,
                        null,
                        null,
                        null
                ))
        );
    }

    @Test
    void shouldUpdateProfileWithPartialAddressPatch() {
        UUID accountId = UUID.randomUUID();
        UUID userProfileId = UUID.fromString("ec761405-c091-4a65-b1dd-c8fb23f0d6bd");
        AccountProfileState initialState = stateWithProfile(accountId);
        AccountProfileState updatedState = new AccountProfileState(
                accountId,
                initialState.username(),
                initialState.email(),
                initialState.keycloakUserId(),
                initialState.roleCode(),
                userProfileId,
                initialState.fullName(),
                initialState.dateOfBirth(),
                initialState.gender(),
                initialState.phoneNumber(),
                "Thu Duc, Ho Chi Minh City",
                initialState.identifyCard(),
                initialState.avatarUrl(),
                initialState.userProfileStatus(),
                initialState.employeeId(),
                initialState.employeeCode(),
                initialState.jobTitle(),
                initialState.employeeHiredAt(),
                initialState.employeeStatus(),
                initialState.customerId(),
                initialState.customerCode(),
                initialState.customerType(),
                initialState.customerStatus(),
                initialState.customerApprovalStatus(),
                initialState.accountStatus()
        );

        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(accountProfilePortOut.findProfileStateByAccountId(accountId)).thenReturn(Optional.of(initialState));
        when(accountProfilePolicy.normalizeForUpdate(org.mockito.ArgumentMatchers.any(UpdateAccountProfileCommand.class)))
                .thenReturn(new UpdateAccountProfileCommand(
                        null,
                        null,
                        null,
                        null,
                        "Thu Duc, Ho Chi Minh City",
                        null,
                        null
                ));
        when(accountProfileResultMapper.mergeProfile(
                org.mockito.ArgumentMatchers.any(AccountProfileState.class),
                org.mockito.ArgumentMatchers.any(UpdateAccountProfileCommand.class)
        )).thenReturn(buildUserProfile(
                userProfileId,
                "Nguyen Bao An",
                "+84901234567",
                LocalDate.of(2003, 9, 19),
                "MALE",
                "Thu Duc, Ho Chi Minh City",
                "079203001234",
                "https://cdn.example.com/avatars/bao-an.jpg",
                UserProfileStatus.ACTIVE
        ));
        when(accountProfilePortOut.updateProfile(eq(accountId), any(UserProfile.class))).thenReturn(updatedState);
        when(accountProfileResultMapper.toStatusResult(updatedState, false)).thenReturn(new AccountProfileStatusResult(
                false,
                new AccountProfileStatusResult.AccountInfoResult(
                        accountId,
                        "ACTIVE",
                        "baoan3236",
                        "baoan3236@gmail.com",
                        "23d493f8-e9f8-4843-917c-9e6c431bfeea"
                ),
                new AccountProfileStatusResult.ProfileInfoResult(
                        userProfileId,
                        "Nguyen Bao An",
                        LocalDate.of(2003, 9, 19),
                        "MALE",
                        "+84901234567",
                        "Thu Duc, Ho Chi Minh City",
                        "079203001234",
                        "https://cdn.example.com/avatars/bao-an.jpg",
                        "ACTIVE"
                ),
                null,
                new AccountProfileStatusResult.CustomerInfoResult(
                        UUID.fromString("1f53b3c1-1ca4-4898-b35f-80ddf8745ae3"),
                        "CUS-ABC123",
                        "REGISTERED",
                        "INACTIVE",
                        "PENDING"
                )
        ));

        AccountProfileStatusResult result = useCase.updateMyProfile(new UpdateAccountProfileCommand(
                null,
                null,
                null,
                null,
                "Thu Duc, Ho Chi Minh City",
                null,
                null
        ));

        ArgumentCaptor<UserProfile> userProfileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(accountProfilePortOut).updateProfile(eq(accountId), userProfileCaptor.capture());

        UserProfile mergedProfile = userProfileCaptor.getValue();
        assertEquals("Nguyen Bao An", mergedProfile.getFullName());
        assertEquals("+84901234567", mergedProfile.getPhoneNumber());
        assertEquals("Thu Duc, Ho Chi Minh City", mergedProfile.getAddress());

        assertEquals(accountId, result.account().accountId());
        assertEquals(userProfileId, result.profile().userProfileId());
        assertEquals("Thu Duc, Ho Chi Minh City", result.profile().address());
    }

    private AccountProfileState stateWithoutProfile(UUID accountId) {
        return new AccountProfileState(
                accountId,
                "baoan3236",
                "baoan3236@gmail.com",
                "23d493f8-e9f8-4843-917c-9e6c431bfeea",
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
                AccountStatus.PENDING
        );
    }

    private AccountProfileState stateWithProfile(UUID accountId) {
        return new AccountProfileState(
                accountId,
                "baoan3236",
                "baoan3236@gmail.com",
                "23d493f8-e9f8-4843-917c-9e6c431bfeea",
                null,
                UUID.fromString("ec761405-c091-4a65-b1dd-c8fb23f0d6bd"),
                "Nguyen Bao An",
                LocalDate.of(2003, 9, 19),
                "MALE",
                "+84901234567",
                "Ho Chi Minh City",
                "079203001234",
                "https://cdn.example.com/avatars/bao-an.jpg",
                UserProfileStatus.ACTIVE,
                null,
                null,
                null,
                null,
                null,
                UUID.fromString("1f53b3c1-1ca4-4898-b35f-80ddf8745ae3"),
                "CUS-ABC123",
                CustomerType.REGISTERED,
                CustomerStatus.INACTIVE,
                CustomerApprovalStatus.PENDING,
                AccountStatus.ACTIVE
        );
    }

    private UserProfile buildUserProfile(
            UUID userProfileId,
            String fullName,
            String phoneNumber,
            LocalDate dateOfBirth,
            String gender,
            String address,
            String identifyCard,
            String avatarUrl,
            UserProfileStatus status
    ) {
        UserProfile userProfile = new UserProfile();
        userProfile.setUserProfileId(userProfileId);
        userProfile.setFullName(fullName);
        userProfile.setPhoneNumber(phoneNumber);
        userProfile.setDateOfBirth(dateOfBirth);
        userProfile.setGender(gender);
        userProfile.setAddress(address);
        userProfile.setIdentifyCard(identifyCard);
        userProfile.setAvatarUrl(avatarUrl);
        userProfile.setStatus(status);
        return userProfile;
    }
}
