package com.ban.vehicle_management.domain.iam.account.policy;

import com.ban.vehicle_management.shared.enumeration.iam.SocialIdentityProvider;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SocialAccountPolicy {

    private static final String ENABLED_SELF_REGISTRATION_PROVIDER = "google";
    private static final String USERNAME_SUFFIX_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int USERNAME_MAX_LENGTH = 50;
    private static final int USERNAME_SUFFIX_LENGTH = 6;
    private static final Pattern DIACRITIC_PATTERN = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALPHANUMERIC_PATTERN = Pattern.compile("[^a-z0-9]");

    private final PublicAuthPolicy publicAuthPolicy;

    public SocialAccountPolicy(PublicAuthPolicy publicAuthPolicy) {
        this.publicAuthPolicy = publicAuthPolicy;
    }

    public SocialIdentityProvider requireEnabledProvider(String providerAlias) {
        String normalizedAlias = TextValidationUtils.normalizeRequiredText(
                providerAlias,
                "identityProvider",
                50
        ).toLowerCase(Locale.ROOT);
        if (!ENABLED_SELF_REGISTRATION_PROVIDER.equals(normalizedAlias)) {
            throw new BadRequestException("Nhà cung cấp đăng nhập này chưa được phép tự đăng ký.");
        }
        return SocialIdentityProvider.GOOGLE;
    }

    public String requireVerifiedEmail(String email, boolean emailVerified) {
        if (!emailVerified) {
            throw new BadRequestException("Google chưa xác minh địa chỉ email.");
        }
        return publicAuthPolicy.normalizeRequiredEmail(email);
    }

    public String normalizeProviderSubject(String providerSubject) {
        return TextValidationUtils.normalizeRequiredText(providerSubject, "providerSubject", 255);
    }

    public String normalizeProviderUsername(String providerUsername) {
        return TextValidationUtils.normalizeNullableText(providerUsername, "providerUsername", 255);
    }

    public String resolveFullName(String fullName, String email) {
        String normalizedFullName = TextValidationUtils.normalizeNullableText(fullName, "fullName", 150);
        if (normalizedFullName != null) {
            return normalizedFullName;
        }
        int separatorIndex = email.indexOf('@');
        String fallback = separatorIndex > 0 ? email.substring(0, separatorIndex) : email;
        return TextValidationUtils.normalizeRequiredText(fallback, "fullName", 150);
    }

    public String buildUsername(
            SocialIdentityProvider provider,
            String fullName,
            String keycloakUserId
    ) {
        String normalizedSubject = TextValidationUtils.normalizeRequiredText(
                keycloakUserId,
                "keycloakUserId",
                255
        );
        String usernamePrefix = normalizeFullNameForUsername(fullName);
        if (usernamePrefix.isEmpty()) {
            usernamePrefix = provider.name().toLowerCase(Locale.ROOT) + "user";
        } else if (!Character.isLetter(usernamePrefix.charAt(0))) {
            usernamePrefix = provider.name().toLowerCase(Locale.ROOT) + usernamePrefix;
        }

        int maximumPrefixLength = USERNAME_MAX_LENGTH - USERNAME_SUFFIX_LENGTH - 1;
        if (usernamePrefix.length() > maximumPrefixLength) {
            usernamePrefix = usernamePrefix.substring(0, maximumPrefixLength);
        }
        return usernamePrefix + "_" + stableAlphanumericSuffix(normalizedSubject);
    }

    private String normalizeFullNameForUsername(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        String vietnameseDNormalized = fullName.trim()
                .replace('đ', 'd')
                .replace('Đ', 'D');
        String decomposed = Normalizer.normalize(vietnameseDNormalized, Normalizer.Form.NFD);
        String withoutDiacritics = DIACRITIC_PATTERN.matcher(decomposed).replaceAll("");
        return NON_ALPHANUMERIC_PATTERN.matcher(withoutDiacritics.toLowerCase(Locale.ROOT)).replaceAll("");
    }

    private String stableAlphanumericSuffix(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder suffix = new StringBuilder(USERNAME_SUFFIX_LENGTH);
            for (int index = 0; index < USERNAME_SUFFIX_LENGTH; index++) {
                int alphabetIndex = Byte.toUnsignedInt(hash[index]) % USERNAME_SUFFIX_ALPHABET.length();
                suffix.append(USERNAME_SUFFIX_ALPHABET.charAt(alphabetIndex));
            }
            return suffix.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
