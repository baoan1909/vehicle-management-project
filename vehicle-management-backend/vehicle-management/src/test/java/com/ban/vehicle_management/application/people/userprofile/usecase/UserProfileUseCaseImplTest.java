package com.ban.vehicle_management.application.people.userprofile.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.people.userprofile.port.out.UserProfilePortOut;
import com.ban.vehicle_management.application.storage.service.StorageUrlResolver;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileUseCaseImplTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private UserProfilePortOut userProfilePort;

    @Mock
    private StorageUrlResolver storageUrlResolver;

    @InjectMocks
    private UserProfileUseCaseImpl userProfileUseCase;

    @Test
    void shouldCreateUserProfileWithDefaultActiveStatus() {
        UserProfile requestUserProfile = new UserProfile();
        requestUserProfile.setFullName("  Nguyen Van A  ");
        requestUserProfile.setGender(" male ");
        requestUserProfile.setPhoneNumber(" 0901234567 ");
        requestUserProfile.setAddress("  Ho Chi Minh City ");
        requestUserProfile.setIdentifyCard(" 079123456789 ");
        requestUserProfile.setAvatarUrl(" https://example.com/avatar.jpg ");
        requestUserProfile.setDateOfBirth(LocalDate.of(1995, 1, 10));

        when(userProfilePort.existsByPhoneNumber("0901234567")).thenReturn(false);
        when(userProfilePort.existsByIdentifyCard("079123456789")).thenReturn(false);
        when(userProfilePort.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile createdUserProfile = userProfileUseCase.createUserProfile(requestUserProfile);

        assertEquals("Nguyen Van A", createdUserProfile.getFullName());
        assertEquals("male", createdUserProfile.getGender());
        assertEquals("0901234567", createdUserProfile.getPhoneNumber());
        assertEquals("Ho Chi Minh City", createdUserProfile.getAddress());
        assertEquals("079123456789", createdUserProfile.getIdentifyCard());
        assertEquals("https://example.com/avatar.jpg", createdUserProfile.getAvatarUrl());
        assertEquals(UserProfileStatus.ACTIVE, createdUserProfile.getStatus());
        verify(userProfilePort).save(any(UserProfile.class));
    }

    @Test
    void shouldRejectDuplicatePhoneNumberOnCreate() {
        UserProfile requestUserProfile = new UserProfile();
        requestUserProfile.setFullName("Nguyen Van A");
        requestUserProfile.setPhoneNumber("0901234567");

        when(userProfilePort.existsByPhoneNumber("0901234567")).thenReturn(true);

        assertThrows(ConflictException.class, () -> userProfileUseCase.createUserProfile(requestUserProfile));
        verify(userProfilePort, never()).save(any(UserProfile.class));
    }

    @Test
    void shouldRejectDuplicateIdentifyCardOnCreate() {
        UserProfile requestUserProfile = new UserProfile();
        requestUserProfile.setFullName("Nguyen Van A");
        requestUserProfile.setIdentifyCard("079123456789");

        when(userProfilePort.existsByIdentifyCard("079123456789")).thenReturn(true);

        assertThrows(ConflictException.class, () -> userProfileUseCase.createUserProfile(requestUserProfile));
        verify(userProfilePort, never()).save(any(UserProfile.class));
    }

    @Test
    void shouldUpdateUserProfile() {
        UUID userProfileId = UUID.randomUUID();
        UserProfile existingUserProfile = new UserProfile();
        existingUserProfile.setUserProfileId(userProfileId);
        existingUserProfile.setFullName("Nguyen Van A");
        existingUserProfile.setDateOfBirth(LocalDate.of(1995, 1, 10));
        existingUserProfile.setGender("male");
        existingUserProfile.setPhoneNumber("0901234567");
        existingUserProfile.setAddress("Old address");
        existingUserProfile.setIdentifyCard("079123456789");
        existingUserProfile.setAvatarUrl("https://example.com/old.jpg");
        existingUserProfile.setStatus(UserProfileStatus.ACTIVE);

        UserProfile requestUserProfile = new UserProfile();
        requestUserProfile.setFullName("Tran Thi B");
        requestUserProfile.setDateOfBirth(LocalDate.of(1998, 6, 15));
        requestUserProfile.setGender("female");
        requestUserProfile.setPhoneNumber("0912345678");
        requestUserProfile.setAddress("New address");
        requestUserProfile.setIdentifyCard("012345678901");
        requestUserProfile.setAvatarUrl("https://example.com/new.jpg");
        requestUserProfile.setStatus(UserProfileStatus.SUSPENDED);

        when(userProfilePort.findById(userProfileId)).thenReturn(Optional.of(existingUserProfile));
        when(userProfilePort.existsByPhoneNumberAndUserProfileIdNot("0912345678", userProfileId)).thenReturn(false);
        when(userProfilePort.existsByIdentifyCardAndUserProfileIdNot("012345678901", userProfileId)).thenReturn(false);
        when(userProfilePort.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile updatedUserProfile = userProfileUseCase.updateUserProfile(userProfileId, requestUserProfile);

        assertEquals("Tran Thi B", updatedUserProfile.getFullName());
        assertEquals(LocalDate.of(1998, 6, 15), updatedUserProfile.getDateOfBirth());
        assertEquals("female", updatedUserProfile.getGender());
        assertEquals("0912345678", updatedUserProfile.getPhoneNumber());
        assertEquals("New address", updatedUserProfile.getAddress());
        assertEquals("012345678901", updatedUserProfile.getIdentifyCard());
        assertEquals("https://example.com/new.jpg", updatedUserProfile.getAvatarUrl());
        assertEquals(UserProfileStatus.SUSPENDED, updatedUserProfile.getStatus());
    }

    @Test
    void shouldKeepExistingStatusWhenUpdateRequestDoesNotProvideStatus() {
        UUID userProfileId = UUID.randomUUID();
        UserProfile existingUserProfile = new UserProfile();
        existingUserProfile.setUserProfileId(userProfileId);
        existingUserProfile.setFullName("Nguyen Van A");
        existingUserProfile.setStatus(UserProfileStatus.SUSPENDED);

        UserProfile requestUserProfile = new UserProfile();
        requestUserProfile.setFullName("Tran Thi B");

        when(userProfilePort.findById(userProfileId)).thenReturn(Optional.of(existingUserProfile));
        when(userProfilePort.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile updatedUserProfile = userProfileUseCase.updateUserProfile(userProfileId, requestUserProfile);

        assertEquals(UserProfileStatus.SUSPENDED, updatedUserProfile.getStatus());
    }

    @Test
    void shouldReturnFilteredUserProfiles() {
        when(userProfilePort.findAll(UserProfileStatus.ACTIVE, "nguyen"))
                .thenReturn(List.of(new UserProfile(), new UserProfile()));

        List<UserProfile> userProfiles = userProfileUseCase.getUserProfiles(UserProfileStatus.ACTIVE, "nguyen");

        assertEquals(2, userProfiles.size());
        verify(userProfilePort).findAll(UserProfileStatus.ACTIVE, "nguyen");
    }

    @Test
    void shouldThrowWhenUserProfileDoesNotExist() {
        UUID userProfileId = UUID.randomUUID();
        when(userProfilePort.findById(userProfileId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userProfileUseCase.getUserProfileById(userProfileId));
    }

    @Test
    void shouldNormalizeBlankOptionalFieldsToNullOnUpdate() {
        UUID userProfileId = UUID.randomUUID();
        UserProfile existingUserProfile = new UserProfile();
        existingUserProfile.setUserProfileId(userProfileId);
        existingUserProfile.setFullName("Nguyen Van A");
        existingUserProfile.setStatus(UserProfileStatus.ACTIVE);
        existingUserProfile.setPhoneNumber("0901234567");
        existingUserProfile.setIdentifyCard("079123456789");

        UserProfile requestUserProfile = new UserProfile();
        requestUserProfile.setFullName("Tran Thi B");
        requestUserProfile.setPhoneNumber("   ");
        requestUserProfile.setIdentifyCard("   ");

        when(userProfilePort.findById(userProfileId)).thenReturn(Optional.of(existingUserProfile));
        when(userProfilePort.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile updatedUserProfile = userProfileUseCase.updateUserProfile(userProfileId, requestUserProfile);

        assertNull(updatedUserProfile.getPhoneNumber());
        assertNull(updatedUserProfile.getIdentifyCard());
    }
}
