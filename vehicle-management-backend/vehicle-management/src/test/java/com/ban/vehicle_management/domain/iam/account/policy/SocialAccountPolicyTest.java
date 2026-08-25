package com.ban.vehicle_management.domain.iam.account.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.shared.enumeration.iam.SocialIdentityProvider;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import org.junit.jupiter.api.Test;

class SocialAccountPolicyTest {

    private final SocialAccountPolicy policy = new SocialAccountPolicy(new PublicAuthPolicy());

    @Test
    void shouldAllowOnlyGoogleForCustomerSelfRegistration() {
        assertEquals(SocialIdentityProvider.GOOGLE, policy.requireEnabledProvider(" Google "));
        assertThrows(BadRequestException.class, () -> policy.requireEnabledProvider("facebook"));
    }

    @Test
    void shouldRequireVerifiedEmail() {
        assertEquals("customer@example.com", policy.requireVerifiedEmail(" Customer@Example.com ", true));
        assertThrows(
                BadRequestException.class,
                () -> policy.requireVerifiedEmail("customer@example.com", false)
        );
    }

    @Test
    void shouldBuildReadableStableUsernameFromFullNameAndKeycloakSubject() {
        String first = policy.buildUsername(
                SocialIdentityProvider.GOOGLE,
                "Nguyễn Bảo An",
                "keycloak-subject-1"
        );
        String second = policy.buildUsername(
                SocialIdentityProvider.GOOGLE,
                "Nguyễn Bảo An",
                "keycloak-subject-1"
        );
        String another = policy.buildUsername(
                SocialIdentityProvider.GOOGLE,
                "Nguyễn Bảo An",
                "keycloak-subject-2"
        );

        assertEquals(first, second);
        assertNotEquals(first, another);
        assertTrue(first.matches("nguyenbaoan_[a-z0-9]{6}"));
    }

    @Test
    void shouldFallbackAndKeepGeneratedUsernameWithinLimit() {
        String fallback = policy.buildUsername(
                SocialIdentityProvider.GOOGLE,
                "用户",
                "keycloak-subject-1"
        );
        String longUsername = policy.buildUsername(
                SocialIdentityProvider.GOOGLE,
                "Nguyễn Văn A Nguyễn Văn B Nguyễn Văn C Nguyễn Văn D Nguyễn Văn E",
                "keycloak-subject-1"
        );

        assertTrue(fallback.matches("googleuser_[a-z0-9]{6}"));
        assertEquals(50, longUsername.length());
    }
}
