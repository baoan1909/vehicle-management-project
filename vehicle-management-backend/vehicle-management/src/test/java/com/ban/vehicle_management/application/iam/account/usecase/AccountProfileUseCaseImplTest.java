package com.ban.vehicle_management.application.iam.account.usecase;

import com.ban.vehicle_management.application.iam.account.mapper.AccountProfileResultMapper;
import com.ban.vehicle_management.application.iam.account.model.command.CompleteAccountProfileCommand;
import com.ban.vehicle_management.application.iam.account.model.result.AccountProfileStatusResult;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountProfilePortOut;
import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.domain.iam.account.model.AccountProfileState;
import com.ban.vehicle_management.domain.iam.account.policy.AccountProfilePolicy;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerType;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountProfileUseCaseImplTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private AccountProfilePortOut accountProfilePortOut;

    @Mock
    private AccountProfileResultMapper accountProfileResultMapper;

    @Mock
    private AccountProfilePolicy accountProfilePolicy;

    @InjectMocks
    private AccountProfileUseCaseImpl accountProfileUseCase;

    @Test
    void shouldReturnDetailedOnboardingStatusWhenProfileAndCustomerExist() {
        UUID accountId = UUID.fromString("c6e12a53-e72b-441a-8a1e-bb84b49e0ca4");
        UUID userProfileId = UUID.fromString("ec761405-c091-4a65-b1dd-c8fb23f0d6bd");
        UUID customerId = UUID.fromString("1f53b3c1-1ca4-4898-b35f-80ddf8745ae3");
        AccountProfileState state = new AccountProfileState(
                accountId,
                "baoan3236",
                "baoan3236@gmail.com",
                "23d493f8-e9f8-4843-917c-9e6c431bfeea",
                userProfileId,
                "Nguyen Bao An",
                LocalDate.of(2003, 9, 19),
                "MALE",
                "+84901234567",
                "Ho Chi Minh City",
                "079203001234",
                "https://cdn.example.com/avatars/bao-an.jpg",
                UserProfileStatus.ACTIVE,
                customerId,
                "CUS-ABC123",
                CustomerType.REGISTERED,
                CustomerStatus.INACTIVE,
                CustomerApprovalStatus.PENDING,
                AccountStatus.ACTIVE
        );

        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(accountProfilePortOut.findProfileStateByAccountId(accountId)).thenReturn(Optional.of(state));
        when(accountProfileResultMapper.toStatusResult(state, false)).thenReturn(new AccountProfileStatusResult(
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
                        "Ho Chi Minh City",
                        "079203001234",
                        "https://cdn.example.com/avatars/bao-an.jpg",
                        "ACTIVE"
                ),
                new AccountProfileStatusResult.CustomerInfoResult(
                        customerId,
                        "CUS-ABC123",
                        "REGISTERED",
                        "INACTIVE",
                        "PENDING"
                )
        ));

        AccountProfileStatusResult result = accountProfileUseCase.getMyProfile();

        assertFalse(result.onboardingRequired());
        assertEquals(accountId, result.account().accountId());
        assertEquals("ACTIVE", result.account().accountStatus());
        assertEquals("baoan3236", result.account().username());
        assertEquals("baoan3236@gmail.com", result.account().email());
        assertEquals("23d493f8-e9f8-4843-917c-9e6c431bfeea", result.account().keycloakUserId());
        assertEquals(userProfileId, result.profile().userProfileId());
        assertEquals("Nguyen Bao An", result.profile().fullName());
        assertEquals(LocalDate.of(2003, 9, 19), result.profile().dateOfBirth());
        assertEquals("MALE", result.profile().gender());
        assertEquals("+84901234567", result.profile().phoneNumber());
        assertEquals("Ho Chi Minh City", result.profile().address());
        assertEquals("079203001234", result.profile().identifyCard());
        assertEquals("https://cdn.example.com/avatars/bao-an.jpg", result.profile().avatarUrl());
        assertEquals("ACTIVE", result.profile().userProfileStatus());
        assertEquals(customerId, result.customer().customerId());
        assertEquals("CUS-ABC123", result.customer().customerCode());
        assertEquals("REGISTERED", result.customer().customerType());
        assertEquals("INACTIVE", result.customer().customerStatus());
        assertEquals("PENDING", result.customer().customerApprovalStatus());
    }

    @Test
    void shouldMarkOnboardingRequiredWhenUserProfileAndCustomerAreMissing() {
        UUID accountId = UUID.randomUUID();
        AccountProfileState state = new AccountProfileState(
                accountId,
                "pending-user",
                "pending@example.com",
                "sub-123",
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

        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(accountProfilePortOut.findProfileStateByAccountId(accountId)).thenReturn(Optional.of(state));
        when(accountProfileResultMapper.toStatusResult(state, true)).thenReturn(new AccountProfileStatusResult(
                true,
                new AccountProfileStatusResult.AccountInfoResult(
                        accountId,
                        "PENDING",
                        "pending-user",
                        "pending@example.com",
                        "sub-123"
                ),
                new AccountProfileStatusResult.ProfileInfoResult(
                        null, null, null, null, null, null, null, null, null
                ),
                new AccountProfileStatusResult.CustomerInfoResult(
                        null, null, null, null, null
                )
        ));

        AccountProfileStatusResult result = accountProfileUseCase.getMyProfile();

        assertTrue(result.onboardingRequired());
        assertEquals("PENDING", result.account().accountStatus());
        assertEquals("pending-user", result.account().username());
        assertEquals("pending@example.com", result.account().email());
        assertEquals("sub-123", result.account().keycloakUserId());
        assertNull(result.profile().userProfileId());
        assertNull(result.customer().customerId());
    }

    @Test
    void shouldReturnDetailedResultWhenCompleteOnboardingSuccessfully() {
        UUID accountId = UUID.fromString("c6e12a53-e72b-441a-8a1e-bb84b49e0ca4");
        UUID userProfileId = UUID.fromString("ec761405-c091-4a65-b1dd-c8fb23f0d6bd");
        UUID customerId = UUID.fromString("1f53b3c1-1ca4-4898-b35f-80ddf8745ae3");

        AccountProfileState initialState = new AccountProfileState(
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
                AccountStatus.PENDING
        );
        AccountProfileState completedState = new AccountProfileState(
                accountId,
                "baoan3236",
                "baoan3236@gmail.com",
                "23d493f8-e9f8-4843-917c-9e6c431bfeea",
                userProfileId,
                "Nguyen Bao An",
                LocalDate.of(2003, 9, 19),
                "MALE",
                "+84901234567",
                "Ho Chi Minh City",
                "079203001234",
                "https://cdn.example.com/avatars/bao-an.jpg",
                UserProfileStatus.ACTIVE,
                customerId,
                "CUS-ABC123",
                CustomerType.REGISTERED,
                CustomerStatus.INACTIVE,
                CustomerApprovalStatus.PENDING,
                AccountStatus.ACTIVE
        );
        Account updatedAccount = new Account();
        updatedAccount.setAccountId(accountId);
        updatedAccount.setUsername("baoan3236");
        updatedAccount.setEmail("baoan3236@gmail.com");
        updatedAccount.setKeycloakUserId("23d493f8-e9f8-4843-917c-9e6c431bfeea");
        updatedAccount.setStatus(AccountStatus.ACTIVE);

        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(accountProfilePortOut.findProfileStateByAccountId(accountId))
                .thenReturn(Optional.of(initialState), Optional.of(completedState));
        when(accountProfilePolicy.normalizeForComplete(org.mockito.ArgumentMatchers.any(CompleteAccountProfileCommand.class)))
                .thenReturn(new CompleteAccountProfileCommand(
                        "Nguyen Bao An",
                        "+84901234567",
                        LocalDate.of(2003, 9, 19),
                        "MALE",
                        "Ho Chi Minh City",
                        "079203001234",
                        "https://cdn.example.com/avatars/bao-an.jpg"
                ));
        when(accountProfilePortOut.existsByPhoneNumber("+84901234567")).thenReturn(false);
        when(accountProfilePortOut.existsByIdentifyCard("079203001234")).thenReturn(false);
        when(accountProfilePortOut.completeProfile(
                org.mockito.ArgumentMatchers.eq(accountId),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(updatedAccount);
        when(accountProfileResultMapper.toStatusResult(completedState, false)).thenReturn(new AccountProfileStatusResult(
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
                        "Ho Chi Minh City",
                        "079203001234",
                        "https://cdn.example.com/avatars/bao-an.jpg",
                        "ACTIVE"
                ),
                new AccountProfileStatusResult.CustomerInfoResult(
                        customerId,
                        "CUS-ABC123",
                        "REGISTERED",
                        "INACTIVE",
                        "PENDING"
                )
        ));

        AccountProfileStatusResult result = accountProfileUseCase.completeMyProfile(
                new CompleteAccountProfileCommand(
                        "Nguyen Bao An",
                        "+84901234567",
                        LocalDate.of(2003, 9, 19),
                        "MALE",
                        "Ho Chi Minh City",
                        "079203001234",
                        "https://cdn.example.com/avatars/bao-an.jpg"
                )
        );

        assertFalse(result.onboardingRequired());
        assertEquals(accountId, result.account().accountId());
        assertEquals("ACTIVE", result.account().accountStatus());
        assertEquals("baoan3236", result.account().username());
        assertEquals("baoan3236@gmail.com", result.account().email());
        assertEquals("23d493f8-e9f8-4843-917c-9e6c431bfeea", result.account().keycloakUserId());
        assertEquals(userProfileId, result.profile().userProfileId());
        assertEquals("Nguyen Bao An", result.profile().fullName());
        assertEquals(LocalDate.of(2003, 9, 19), result.profile().dateOfBirth());
        assertEquals("MALE", result.profile().gender());
        assertEquals("+84901234567", result.profile().phoneNumber());
        assertEquals("Ho Chi Minh City", result.profile().address());
        assertEquals("079203001234", result.profile().identifyCard());
        assertEquals("https://cdn.example.com/avatars/bao-an.jpg", result.profile().avatarUrl());
        assertEquals("ACTIVE", result.profile().userProfileStatus());
        assertEquals(customerId, result.customer().customerId());
        assertEquals("CUS-ABC123", result.customer().customerCode());
        assertEquals("REGISTERED", result.customer().customerType());
        assertEquals("INACTIVE", result.customer().customerStatus());
        assertEquals("PENDING", result.customer().customerApprovalStatus());
    }
}
