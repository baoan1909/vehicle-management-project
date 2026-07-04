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
                " 12345678 ",
                "  Nguyen Bao An  "
        );

        RegisterAccountCommand normalized = policy.normalizeRegisterCommand(command);

        assertEquals("baoan3236", normalized.username());
        assertEquals("baoan3236@gmail.com", normalized.email());
        assertEquals("12345678", normalized.password());
        assertEquals("Nguyen Bao An", normalized.fullName());
    }

    @Test
    void shouldRejectShortPassword() {
        RegisterAccountCommand command = new RegisterAccountCommand(
                "baoan3236",
                "baoan3236@gmail.com",
                "1234567",
                "Nguyen Bao An"
        );

        assertThrows(BadRequestException.class, () -> policy.normalizeRegisterCommand(command));
    }
}
