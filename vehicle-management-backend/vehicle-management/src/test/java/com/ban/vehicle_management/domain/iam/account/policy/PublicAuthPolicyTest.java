package com.ban.vehicle_management.domain.iam.account.policy;

import com.ban.vehicle_management.application.iam.account.model.command.RegisterAccountCommand;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PublicAuthPolicyTest {

    private final PublicAuthPolicy policy = new PublicAuthPolicy();

    @Test
    void shouldNormalizeRegisterCommand() {
        RegisterAccountCommand command = new RegisterAccountCommand(
                "  baoan3236  ",
                "  BaoAn3236@Gmail.Com  ",
                "MatKhau1!",
                "  Nguyen Bao An  "
        );

        RegisterAccountCommand normalized = policy.normalizeRegisterCommand(command);

        assertEquals("baoan3236", normalized.username());
        assertEquals("baoan3236@gmail.com", normalized.email());
        assertEquals("MatKhau1!", normalized.password());
        assertEquals("Nguyen Bao An", normalized.fullName());
    }

    @Test
    void shouldRejectShortPassword() {
        RegisterAccountCommand command = new RegisterAccountCommand(
                "baoan3236",
                "baoan3236@gmail.com",
                "Mk1!",
                "Nguyen Bao An"
        );

        assertThrows(BadRequestException.class, () -> policy.normalizeRegisterCommand(command));
    }

    @Test
    void shouldRejectInvalidUsername() {
        RegisterAccountCommand command = new RegisterAccountCommand(
                "1baoan",
                "baoan3236@gmail.com",
                "MatKhau1!",
                "Nguyen Bao An"
        );

        assertThrows(BadRequestException.class, () -> policy.normalizeRegisterCommand(command));
    }

    @Test
    void shouldRejectInvalidEmail() {
        RegisterAccountCommand command = new RegisterAccountCommand(
                "baoan3236",
                "baoan3236@gmail",
                "MatKhau1!",
                "Nguyen Bao An"
        );

        assertThrows(BadRequestException.class, () -> policy.normalizeRegisterCommand(command));
    }

    @Test
    void shouldRejectFullNameContainingDigits() {
        RegisterAccountCommand command = new RegisterAccountCommand(
                "baoan3236",
                "baoan3236@gmail.com",
                "MatKhau1!",
                "Nguyen Bao An 1"
        );

        assertThrows(BadRequestException.class, () -> policy.normalizeRegisterCommand(command));
    }

    @Test
    void shouldRejectPasswordWithoutRequiredCharacterGroups() {
        String[] invalidPasswords = {"matkhau1!", "MATKHAU1!", "MatKhau!!", "MatKhau12"};

        for (String password : invalidPasswords) {
            RegisterAccountCommand command = new RegisterAccountCommand(
                    "baoan3236",
                    "baoan3236@gmail.com",
                    password,
                    "Nguyen Bao An"
            );

            assertThrows(BadRequestException.class, () -> policy.normalizeRegisterCommand(command));
        }
    }

    @Test
    void shouldNotTrimPassword() {
        RegisterAccountCommand command = new RegisterAccountCommand(
                "baoan3236",
                "baoan3236@gmail.com",
                " MatKhau1! ",
                "Nguyen Bao An"
        );

        assertThrows(BadRequestException.class, () -> policy.normalizeRegisterCommand(command));
    }
}
