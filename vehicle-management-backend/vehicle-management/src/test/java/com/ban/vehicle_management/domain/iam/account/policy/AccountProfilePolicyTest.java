package com.ban.vehicle_management.domain.iam.account.policy;

import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountProfilePolicyTest {

    private final AccountProfilePolicy policy = new AccountProfilePolicy();

    @Test
    void shouldNormalizeProfileFields() {
        assertEquals("Nguyen Bao An", policy.normalizeNullableFullName("  Nguyen Bao An  "));
        assertEquals("+84901234567", policy.normalizeNullablePhoneNumber(" +84901234567 "));
        assertEquals("MALE", policy.normalizeNullableGender("  MALE "));
        assertEquals("Ho Chi Minh City", policy.normalizeNullableAddress("  Ho Chi Minh City  "));
        assertEquals("079203001234", policy.normalizeNullableIdentifyCard(" 079203001234 "));
    }

    @Test
    void shouldRejectEmptyPatch() {
        assertThrows(
                BadRequestException.class,
                () -> policy.ensurePatchHasAtLeastOneField(null, null, null, null, null, null)
        );
    }

    @Test
    void shouldNormalizeRequiredCompleteFields() {
        assertEquals("Nguyen Bao An", policy.normalizeRequiredFullName("  Nguyen Bao An  "));
        assertEquals("+84901234567", policy.normalizeRequiredPhoneNumber(" +84901234567 "));
        assertNull(policy.normalizeNullableIdentifyCard(null));
    }

    @Test
    void shouldRejectMissingRequiredPhone() {
        UserProfile userProfile = new UserProfile();
        userProfile.setFullName("Nguyen Bao An");
        userProfile.setPhoneNumber(" ");

        assertThrows(BadRequestException.class, () -> policy.validateRequiredProfileFields(userProfile));
    }
}
