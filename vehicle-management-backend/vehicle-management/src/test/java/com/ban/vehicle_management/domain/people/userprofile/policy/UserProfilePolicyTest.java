package com.ban.vehicle_management.domain.people.userprofile.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.shared.enumeration.UserProfileStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class UserProfilePolicyTest {

    private final UserProfilePolicy userProfilePolicy = new UserProfilePolicy();

    @Test
    void shouldInitializeUserProfileWithDefaults() {
        UserProfile userProfile = new UserProfile();
        userProfile.setFullName("  Nguyen Van A  ");
        userProfile.setAddress("  HCM  ");

        userProfilePolicy.initialize(userProfile);

        assertEquals("Nguyen Van A", userProfile.getFullName());
        assertEquals("HCM", userProfile.getAddress());
        assertEquals(UserProfileStatus.ACTIVE, userProfile.getStatus());
    }

    @Test
    void shouldRejectFutureDateOfBirth() {
        UserProfile userProfile = new UserProfile();
        userProfile.setFullName("Nguyen Van A");
        userProfile.setStatus(UserProfileStatus.ACTIVE);
        userProfile.setDateOfBirth(LocalDate.now().plusDays(1));

        assertThrows(BadRequestException.class, () -> userProfilePolicy.validateState(userProfile));
    }
}

