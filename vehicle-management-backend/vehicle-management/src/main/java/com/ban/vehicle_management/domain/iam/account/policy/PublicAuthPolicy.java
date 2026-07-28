package com.ban.vehicle_management.domain.iam.account.policy;

import com.ban.vehicle_management.application.iam.account.model.command.RegisterAccountCommand;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class PublicAuthPolicy {

    private static final int USERNAME_MAX_LENGTH = 100;
    private static final int EMAIL_MAX_LENGTH = 255;
    private static final int PASSWORD_MAX_LENGTH = 255;
    private static final int FULL_NAME_MAX_LENGTH = 150;

    public RegisterAccountCommand normalizeRegisterCommand(RegisterAccountCommand command) {
        String username = normalizeRequiredAuthText(
                command.username(),
                "Tên đăng nhập",
                USERNAME_MAX_LENGTH,
                "Vui lòng nhập tên đăng nhập."
        );
        String email = normalizeRequiredEmail(command.email());
        String password = normalizePassword(command.password());
        String fullName = normalizeRequiredAuthText(
                command.fullName(),
                "Họ và tên",
                FULL_NAME_MAX_LENGTH,
                "Vui lòng nhập họ và tên."
        );
        return new RegisterAccountCommand(username, email, password, fullName);
    }

    public String normalizeRequiredEmail(String email) {
        return normalizeRequiredAuthText(
                email,
                "Email",
                EMAIL_MAX_LENGTH,
                "Vui lòng nhập email."
        )
                .toLowerCase(Locale.ROOT);
    }

    private String normalizePassword(String password) {
        String normalizedPassword = normalizeRequiredAuthText(
                password,
                "Mật khẩu",
                PASSWORD_MAX_LENGTH,
                "Vui lòng nhập mật khẩu."
        );
        if (normalizedPassword.length() < 8) {
            throw new BadRequestException("Mật khẩu phải có ít nhất 8 ký tự.");
        }
        return normalizedPassword;
    }

    private String normalizeRequiredAuthText(
            String value,
            String fieldLabel,
            int maxLength,
            String blankMessage
    ) {
        if (value == null) {
            throw new BadRequestException(blankMessage);
        }

        String normalizedValue = value.trim();
        if (normalizedValue.isEmpty()) {
            throw new BadRequestException(blankMessage);
        }

        if (normalizedValue.length() > maxLength) {
            throw new BadRequestException(fieldLabel + " không được vượt quá " + maxLength + " ký tự.");
        }

        for (int index = 0; index < normalizedValue.length(); index++) {
            char character = normalizedValue.charAt(index);
            if (Character.isISOControl(character) || character == '<' || character == '>') {
                throw new BadRequestException(fieldLabel + " chứa ký tự không được hỗ trợ.");
            }
        }

        return normalizedValue;
    }
}
