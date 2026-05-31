package com.ban.vehicle_management.domain.iam.account.policy;

import com.ban.vehicle_management.application.iam.account.model.command.CompleteAccountProfileCommand;
import com.ban.vehicle_management.application.iam.account.model.command.UpdateAccountProfileCommand;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountProfilePolicyTest {

    private final AccountProfilePolicy policy = new AccountProfilePolicy();

    @Test
    void shouldNormalizeUpdateCommand() {
        UpdateAccountProfileCommand normalized = policy.normalizeForUpdate(new UpdateAccountProfileCommand(
                "  Nguyen Bao An  ",
                " +84901234567 ",
                null,
                "  MALE ",
                "  Ho Chi Minh City  ",
                " 079203001234 ",
                " https://cdn.example.com/avatar.jpg "
        ));

        assertEquals("Nguyen Bao An", normalized.fullName());
        assertEquals("+84901234567", normalized.phoneNumber());
        assertEquals("MALE", normalized.gender());
        assertEquals("Ho Chi Minh City", normalized.address());
        assertEquals("079203001234", normalized.identifyCard());
        assertEquals("https://cdn.example.com/avatar.jpg", normalized.avatarUrl());
    }

    @Test
    void shouldRejectEmptyPatch() {
        assertThrows(
                BadRequestException.class,
                () -> policy.ensurePatchHasAtLeastOneField(new UpdateAccountProfileCommand(
                        null, null, null, null, null, null, null
                ))
        );
    }

    @Test
    void shouldNormalizeCompleteCommand() {
        CompleteAccountProfileCommand normalized = policy.normalizeForComplete(new CompleteAccountProfileCommand(
                "  Nguyen Bao An  ",
                " +84901234567 ",
                null,
                " MALE ",
                " Ho Chi Minh City ",
                " 079203001234 ",
                " https://cdn.example.com/avatar.jpg "
        ));

        assertEquals("Nguyen Bao An", normalized.fullName());
        assertEquals("+84901234567", normalized.phoneNumber());
    }

    @Test
    void shouldRejectMissingRequiredPhone() {
        UserProfile userProfile = new UserProfile();
        userProfile.setFullName("Nguyen Bao An");
        userProfile.setPhoneNumber(" ");

        assertThrows(BadRequestException.class, () -> policy.validateRequiredProfileFields(userProfile));
    }
}
