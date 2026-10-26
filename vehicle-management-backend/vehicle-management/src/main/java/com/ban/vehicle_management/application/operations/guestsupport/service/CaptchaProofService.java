package com.ban.vehicle_management.application.operations.guestsupport.service;

import com.ban.vehicle_management.application.operations.guestsupport.config.GuestSupportProperties;
import com.ban.vehicle_management.application.operations.guestsupport.model.CaptchaProof;
import com.ban.vehicle_management.application.operations.guestsupport.model.CaptchaPurpose;
import com.ban.vehicle_management.application.operations.guestsupport.model.CaptchaVerificationResult;
import com.ban.vehicle_management.application.operations.guestsupport.model.GuestRateLimitBucket;
import com.ban.vehicle_management.application.operations.guestsupport.model.GuestRateLimitDecision;
import com.ban.vehicle_management.application.operations.guestsupport.port.out.CaptchaProofStorePortOut;
import com.ban.vehicle_management.application.operations.guestsupport.port.out.GuestRateLimitPortOut;
import com.ban.vehicle_management.application.operations.guestsupport.port.out.PublicChallengeVerificationPort;
import com.ban.vehicle_management.infrastructure.security.guestsupport.GuestSupportSecurityMetrics;
import com.ban.vehicle_management.shared.exception.CaptchaValidationException;
import com.ban.vehicle_management.shared.exception.PublicSupportRateLimitException;
import com.ban.vehicle_management.shared.exception.ServiceUnavailableException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CaptchaProofService {

    private static final Duration PROVIDER_TOKEN_TTL = Duration.ofMinutes(5);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final GuestSupportProperties properties;
    private final PublicChallengeVerificationPort verificationPort;
    private final CaptchaProofStorePortOut proofStore;
    private final GuestRateLimitPortOut rateLimitPort;
    private final GuestSupportTokenService tokenService;
    private final GuestSupportSecurityMetrics metrics;

    public CaptchaProofService(
            GuestSupportProperties properties,
            PublicChallengeVerificationPort verificationPort,
            CaptchaProofStorePortOut proofStore,
            GuestRateLimitPortOut rateLimitPort,
            GuestSupportTokenService tokenService,
            GuestSupportSecurityMetrics metrics
    ) {
        this.properties = properties;
        this.verificationPort = verificationPort;
        this.proofStore = proofStore;
        this.rateLimitPort = rateLimitPort;
        this.tokenService = tokenService;
        this.metrics = metrics;
    }

    public CaptchaProof verifyAndIssue(
            CaptchaPurpose purpose,
            String challengeToken,
            String sessionId,
            String remoteAddress
    ) {
        if (!properties.isCaptchaEnabled()) {
            throw new ServiceUnavailableException("CAPTCHA chưa được bật cho public support");
        }
        if (challengeToken == null || challengeToken.isBlank()) {
            reject("missing_token", "Vui lòng hoàn thành CAPTCHA");
        }
        String ipHash = tokenService.hash(normalize(remoteAddress, "unknown"));
        enforceRateLimit(GuestRateLimitBucket.CAPTCHA_IP, ipHash);
        String sessionHash = tokenService.hash(normalize(sessionId, "anonymous"));
        enforceRateLimit(GuestRateLimitBucket.CAPTCHA_SESSION, sessionHash);

        CaptchaVerificationResult result = verificationPort.verify(challengeToken.strip(), remoteAddress);
        validateProviderResult(purpose, result);
        String tokenHash = tokenService.hash(challengeToken.strip());
        if (!proofStore.claimChallengeToken(tokenHash, PROVIDER_TOKEN_TTL)) {
            reject("replayed_token", "CAPTCHA đã được sử dụng. Vui lòng tải lại thử thách");
        }

        byte[] random = new byte[32];
        SECURE_RANDOM.nextBytes(random);
        String proof = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        String bindingHash = bindingHash(purpose, sessionId, remoteAddress);
        proofStore.storeProof(tokenService.hash(proof), bindingHash, properties.getCaptchaProofTtl());
        return new CaptchaProof(proof, Instant.now().plus(properties.getCaptchaProofTtl()));
    }

    public void requireProof(
            CaptchaPurpose purpose,
            String proof,
            String sessionId,
            String remoteAddress
    ) {
        if (!isRequired(purpose)) return;
        if (!properties.isCaptchaEnabled()) {
            throw new ServiceUnavailableException("CAPTCHA bắt buộc nhưng chưa được cấu hình");
        }
        if (proof == null || proof.isBlank()) {
            reject("proof_required", "Vui lòng hoàn thành CAPTCHA trước khi gửi");
        }
        boolean consumed = proofStore.consumeProof(
                tokenService.hash(proof.strip()),
                bindingHash(purpose, sessionId, remoteAddress));
        if (!consumed) {
            reject("invalid_or_expired_proof", "CAPTCHA không hợp lệ, đã hết hạn hoặc đã được sử dụng");
        }
    }

    public boolean isRequired(CaptchaPurpose purpose) {
        return switch (purpose) {
            case INTAKE -> properties.isCaptchaRequiredForIntake();
            case ASSISTANT -> properties.isCaptchaRequiredForAssistant();
        };
    }

    private void validateProviderResult(CaptchaPurpose purpose, CaptchaVerificationResult result) {
        if (result == null || !result.success()) {
            reject("provider_rejected", "CAPTCHA không hợp lệ. Vui lòng thử lại");
        }
        if (!purpose.providerAction().equals(result.action())) {
            reject("wrong_action", "CAPTCHA không đúng mục đích sử dụng");
        }
        Set<String> expectedHosts = properties.getCaptchaExpectedHostnames() == null
                ? Set.of()
                : properties.getCaptchaExpectedHostnames().stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.strip().toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        if (!expectedHosts.isEmpty()
                && (result.hostname() == null
                || !expectedHosts.contains(result.hostname().toLowerCase(Locale.ROOT)))) {
            reject("wrong_hostname", "CAPTCHA không hợp lệ cho tên miền này");
        }
        Instant challengedAt = result.challengedAt();
        Instant now = Instant.now();
        if (challengedAt == null
                || challengedAt.isBefore(now.minus(PROVIDER_TOKEN_TTL))
                || challengedAt.isAfter(now.plusSeconds(60))) {
            reject("expired", "CAPTCHA đã hết hạn. Vui lòng thử lại");
        }
    }

    private void enforceRateLimit(GuestRateLimitBucket bucket, String digest) {
        GuestRateLimitDecision decision = rateLimitPort.consume(
                bucket,
                digest,
                properties.getMaxCaptchaVerificationsPerWindow(),
                properties.getRateWindow());
        if (!decision.allowed()) {
            throw new PublicSupportRateLimitException(
                    "CAPTCHA_RATE_LIMITED",
                    "Bạn đã xác minh CAPTCHA quá nhiều lần. Vui lòng thử lại sau",
                    decision.retryAfterSeconds());
        }
    }

    private String bindingHash(CaptchaPurpose purpose, String sessionId, String remoteAddress) {
        return tokenService.hash("captcha-proof-v1|" + purpose.name() + '|'
                + tokenService.hash(normalize(sessionId, "anonymous")) + '|'
                + tokenService.hash(normalize(remoteAddress, "unknown")));
    }

    private String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }

    private void reject(String reason, String message) {
        metrics.captchaRejected(reason);
        throw new CaptchaValidationException("CAPTCHA_" + reason.toUpperCase(Locale.ROOT), message);
    }
}
