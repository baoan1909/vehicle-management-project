package com.ban.vehicle_management.application.operations.guestsupport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.operations.guestsupport.config.GuestSupportProperties;
import com.ban.vehicle_management.application.operations.guestsupport.model.CaptchaProof;
import com.ban.vehicle_management.application.operations.guestsupport.model.CaptchaPurpose;
import com.ban.vehicle_management.application.operations.guestsupport.model.CaptchaVerificationResult;
import com.ban.vehicle_management.application.operations.guestsupport.model.GuestRateLimitDecision;
import com.ban.vehicle_management.application.operations.guestsupport.port.out.CaptchaProofStorePortOut;
import com.ban.vehicle_management.application.operations.guestsupport.port.out.GuestRateLimitPortOut;
import com.ban.vehicle_management.application.operations.guestsupport.port.out.PublicChallengeVerificationPort;
import com.ban.vehicle_management.infrastructure.security.guestsupport.GuestSupportSecurityMetrics;
import com.ban.vehicle_management.shared.exception.CaptchaValidationException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CaptchaProofServiceTest {

    @Mock PublicChallengeVerificationPort verificationPort;
    @Mock CaptchaProofStorePortOut proofStore;
    @Mock GuestRateLimitPortOut rateLimitPort;
    private GuestSupportProperties properties;
    private CaptchaProofService service;

    @BeforeEach
    void setUp() {
        properties = new GuestSupportProperties();
        properties.setCaptchaEnabled(true);
        properties.setCaptchaRequiredForIntake(true);
        properties.setCaptchaRequiredForAssistant(true);
        properties.setCaptchaExpectedHostnames(Set.of("app.example.com"));
        properties.setCapabilitySecret("captcha-test-secret-with-at-least-32-bytes");
        service = new CaptchaProofService(
                properties,
                verificationPort,
                proofStore,
                rateLimitPort,
                new GuestSupportTokenService(properties),
                new GuestSupportSecurityMetrics(new SimpleMeterRegistry()));
    }

    @Test
    void validChallengeIssuesBoundOneTimeProof() {
        allowCaptchaRateLimit();
        when(verificationPort.verify("provider-token", "127.0.0.1"))
                .thenReturn(valid(CaptchaPurpose.INTAKE));
        when(proofStore.claimChallengeToken(anyString(), any())).thenReturn(true);
        when(proofStore.consumeProof(anyString(), anyString())).thenReturn(true);

        CaptchaProof proof = service.verifyAndIssue(
                CaptchaPurpose.INTAKE, "provider-token", "session", "127.0.0.1");
        service.requireProof(CaptchaPurpose.INTAKE, proof.token(), "session", "127.0.0.1");

        verify(proofStore).storeProof(anyString(), anyString(), any());
        verify(proofStore).consumeProof(anyString(), anyString());
    }

    @Test
    void rejectsWrongPurposeHostnameExpiryAndReplay() {
        allowCaptchaRateLimit();
        when(verificationPort.verify("rejected", "127.0.0.1"))
                .thenReturn(new CaptchaVerificationResult(false, null, null, null, "invalid-input-response"));
        assertCode("CAPTCHA_PROVIDER_REJECTED", () -> service.verifyAndIssue(
                CaptchaPurpose.INTAKE, "rejected", "session", "127.0.0.1"));

        when(verificationPort.verify(anyString(), anyString()))
                .thenReturn(new CaptchaVerificationResult(true, CaptchaPurpose.ASSISTANT.providerAction(),
                        "app.example.com", Instant.now(), null));
        assertCode("CAPTCHA_WRONG_ACTION", () -> service.verifyAndIssue(
                CaptchaPurpose.INTAKE, "wrong-action", "session", "127.0.0.1"));

        when(verificationPort.verify("wrong-host", "127.0.0.1"))
                .thenReturn(new CaptchaVerificationResult(true, CaptchaPurpose.INTAKE.providerAction(),
                        "evil.example", Instant.now(), null));
        assertCode("CAPTCHA_WRONG_HOSTNAME", () -> service.verifyAndIssue(
                CaptchaPurpose.INTAKE, "wrong-host", "session", "127.0.0.1"));

        when(verificationPort.verify("expired", "127.0.0.1"))
                .thenReturn(new CaptchaVerificationResult(true, CaptchaPurpose.INTAKE.providerAction(),
                        "app.example.com", Instant.now().minusSeconds(301), null));
        assertCode("CAPTCHA_EXPIRED", () -> service.verifyAndIssue(
                CaptchaPurpose.INTAKE, "expired", "session", "127.0.0.1"));

        when(verificationPort.verify("replay", "127.0.0.1")).thenReturn(valid(CaptchaPurpose.INTAKE));
        when(proofStore.claimChallengeToken(anyString(), any())).thenReturn(false);
        assertCode("CAPTCHA_REPLAYED_TOKEN", () -> service.verifyAndIssue(
                CaptchaPurpose.INTAKE, "replay", "session", "127.0.0.1"));
    }

    @Test
    void missingOrConsumedProofCannotBypassRequiredCaptcha() {
        assertCode("CAPTCHA_PROOF_REQUIRED", () -> service.requireProof(
                CaptchaPurpose.ASSISTANT, null, "session", "127.0.0.1"));
        when(proofStore.consumeProof(anyString(), anyString())).thenReturn(false);
        assertCode("CAPTCHA_INVALID_OR_EXPIRED_PROOF", () -> service.requireProof(
                CaptchaPurpose.ASSISTANT, "proof", "session", "127.0.0.1"));
    }

    private CaptchaVerificationResult valid(CaptchaPurpose purpose) {
        return new CaptchaVerificationResult(
                true, purpose.providerAction(), "app.example.com", Instant.now(), null);
    }

    private void allowCaptchaRateLimit() {
        when(rateLimitPort.consume(any(), anyString(), anyInt(), any()))
                .thenReturn(new GuestRateLimitDecision(true, 60));
    }

    private void assertCode(String code, org.junit.jupiter.api.function.Executable executable) {
        CaptchaValidationException exception = assertThrows(CaptchaValidationException.class, executable);
        assertEquals(code, exception.code());
    }
}
