package com.ban.vehicle_management.domain.iam.account.policy;

import com.ban.vehicle_management.application.iam.account.model.command.RegisterAccountCommand;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class PublicAuthPolicy {

    private static final int USERNAME_MIN_LENGTH = 4;
    private static final int USERNAME_MAX_LENGTH = 50;
    private static final int EMAIL_MAX_LENGTH = 255;
    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int PASSWORD_MAX_LENGTH = 64;
    private static final int FULL_NAME_MIN_LENGTH = 2;
    private static final int FULL_NAME_MAX_LENGTH = 150;
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
            "^[A-Za-z][A-Za-z0-9]*(?:[._][A-Za-z0-9]+)*$"
    );
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern FULL_NAME_PATTERN = Pattern.compile(
            "^[\\p{L}\\p{M}]+(?:[ '\\-\\u2019][\\p{L}\\p{M}]+)*$"
    );

    public RegisterAccountCommand normalizeRegisterCommand(RegisterAccountCommand command) {
        String username = normalizeUsername(command.username());
        String email = normalizeRequiredEmail(command.email());
        String password = normalizePassword(command.password());
        String fullName = normalizeFullName(command.fullName());
        return new RegisterAccountCommand(username, email, password, fullName);
    }

    private String normalizeUsername(String username) {
        String normalizedUsername = normalizeRequiredAuthText(
                username,
                "Tên đăng nhập",
                USERNAME_MAX_LENGTH,
                "Vui lòng nhập tên đăng nhập."
        );
        if (normalizedUsername.length() < USERNAME_MIN_LENGTH) {
            throw new BadRequestException("Tên đăng nhập phải có từ 4 đến 50 ký tự.");
        }
        if (!USERNAME_PATTERN.matcher(normalizedUsername).matches()) {
            throw new BadRequestException(
                    "Tên đăng nhập phải bắt đầu bằng chữ cái và chỉ gồm chữ cái không dấu, chữ số, dấu chấm hoặc dấu gạch dưới."
            );
        }
        return normalizedUsername;
    }

    public String normalizeRequiredEmail(String email) {
        String normalizedEmail = normalizeRequiredAuthText(
                email,
                "Email",
                EMAIL_MAX_LENGTH,
                "Vui lòng nhập email."
        ).toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new BadRequestException("Địa chỉ email không hợp lệ.");
        }
        return normalizedEmail;
    }

    private String normalizePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new BadRequestException("Vui lòng nhập mật khẩu.");
        }
        if (password.length() < PASSWORD_MIN_LENGTH || password.length() > PASSWORD_MAX_LENGTH) {
            throw new BadRequestException("Mật khẩu phải có từ 8 đến 64 ký tự.");
        }
        if (password.chars().anyMatch(Character::isWhitespace)) {
            throw new BadRequestException("Mật khẩu không được chứa khoảng trắng.");
        }

        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasDigit = false;
        boolean hasSpecialCharacter = false;
        for (int index = 0; index < password.length(); index++) {
            char character = password.charAt(index);
            if (Character.isISOControl(character)) {
                throw new BadRequestException("Mật khẩu chứa ký tự không được hỗ trợ.");
            }
            hasUppercase |= Character.isUpperCase(character);
            hasLowercase |= Character.isLowerCase(character);
            hasDigit |= Character.isDigit(character);
            hasSpecialCharacter |= !Character.isLetterOrDigit(character);
        }

        if (!hasUppercase || !hasLowercase || !hasDigit || !hasSpecialCharacter) {
            throw new BadRequestException(
                    "Mật khẩu phải có chữ hoa, chữ thường, chữ số và ký tự đặc biệt."
            );
        }
        return password;
    }

    private String normalizeFullName(String fullName) {
        String normalizedFullName = normalizeRequiredAuthText(
                fullName,
                "Họ và tên",
                FULL_NAME_MAX_LENGTH,
                "Vui lòng nhập họ và tên."
        );
        if (normalizedFullName.length() < FULL_NAME_MIN_LENGTH) {
            throw new BadRequestException("Họ và tên phải có từ 2 đến 150 ký tự.");
        }
        if (!FULL_NAME_PATTERN.matcher(normalizedFullName).matches()) {
            throw new BadRequestException(
                    "Họ và tên chỉ được gồm chữ cái, khoảng trắng, dấu nháy đơn hoặc dấu gạch nối."
            );
        }
        return normalizedFullName;
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
