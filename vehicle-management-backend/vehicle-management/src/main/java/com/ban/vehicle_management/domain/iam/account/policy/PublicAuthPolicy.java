package com.ban.vehicle_management.domain.iam.account.policy;

import com.ban.vehicle_management.application.iam.account.model.command.RegisterAccountCommand;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class PublicAuthPolicy {

    public RegisterAccountCommand normalizeRegisterCommand(RegisterAccountCommand command) {
        String username = TextValidationUtils.normalizeRequiredText(command.username(), "username", 100);
        String email = normalizeRequiredEmail(command.email());
        String password = normalizePassword(command.password());
        return new RegisterAccountCommand(username, email, password);
    }

    public String normalizeRequiredEmail(String email) {
        return TextValidationUtils.normalizeRequiredText(email, "email", 255)
                .toLowerCase(Locale.ROOT);
    }

    private String normalizePassword(String password) {
        String normalizedPassword = TextValidationUtils.normalizeRequiredText(password, "password", 255);
        if (normalizedPassword.length() < 8) {
            throw new BadRequestException("password must be at least 8 characters");
        }
        return normalizedPassword;
    }
}
