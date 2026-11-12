package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import com.ban.vehicle_management.application.iam.account.model.result.DeletedIdentityMatch;
import com.ban.vehicle_management.application.iam.account.port.out.DeletedIdentityFingerprintPortOut;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.audit.DeletedIdentityFingerprintEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.audit.DeletedIdentityFingerprintRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountIdentityRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import com.ban.vehicle_management.shared.enumeration.iam.DeletedIdentityType;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeletedIdentityFingerprintPersistenceAdapter implements DeletedIdentityFingerprintPortOut {
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final DeletedIdentityFingerprintRepository fingerprintRepository;
    private final AccountRepository accountRepository;
    private final AccountIdentityRepository accountIdentityRepository;
    private final byte[] secret;
    private final String keyVersion;

    public DeletedIdentityFingerprintPersistenceAdapter(
            DeletedIdentityFingerprintRepository fingerprintRepository,
            AccountRepository accountRepository,
            AccountIdentityRepository accountIdentityRepository,
            @Value("${app.iam.account-deletion.identity-fingerprint-secret:}") String secret,
            @Value("${app.iam.account-deletion.identity-fingerprint-key-version:v1}") String keyVersion) {
        this.fingerprintRepository = fingerprintRepository;
        this.accountRepository = accountRepository;
        this.accountIdentityRepository = accountIdentityRepository;
        this.secret = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        this.keyVersion = keyVersion;
    }

    @Override
    @Transactional
    public void capture(UUID requestId, UUID accountId, Instant capturedAt) {
        requireConfiguredSecret();
        AccountRepository.DeletionIdentityProjection identity = accountRepository
                .findDeletionIdentityByAccountId(accountId)
                .orElseThrow(() -> new IllegalStateException("Account identity no longer exists"));
        persist(requestId, accountId, DeletedIdentityType.USERNAME, identity.getUsername(), capturedAt);
        persist(requestId, accountId, DeletedIdentityType.EMAIL, identity.getEmail(), capturedAt);
        persist(requestId, accountId, DeletedIdentityType.PHONE_NUMBER, identity.getPhoneNumber(), capturedAt);
        persist(requestId, accountId, DeletedIdentityType.IDENTIFY_CARD, identity.getIdentifyCard(), capturedAt);
        persist(requestId, accountId, DeletedIdentityType.CUSTOMER_CODE, identity.getCustomerCode(), capturedAt);
        persist(requestId, accountId, DeletedIdentityType.EMPLOYEE_CODE, identity.getEmployeeCode(), capturedAt);
        accountIdentityRepository.findAllByAccountId(accountId).forEach(identityEntity -> persist(
                requestId, accountId, DeletedIdentityType.PROVIDER_SUBJECT,
                identityEntity.getProviderSubject(), capturedAt));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeletedIdentityMatch> lookup(DeletedIdentityType identityType, String rawValue) {
        requireConfiguredSecret();
        String fingerprint = fingerprint(identityType, rawValue);
        return fingerprintRepository.findAllByIdentityTypeAndFingerprintAndKeyVersion(
                        identityType, fingerprint, keyVersion).stream()
                .map(entity -> new DeletedIdentityMatch(entity.getAccountId(), entity.getRequestId(),
                        entity.getIdentityType(), entity.getCreatedAt()))
                .toList();
    }

    private void persist(
            UUID requestId,
            UUID accountId,
            DeletedIdentityType identityType,
            String rawValue,
            Instant capturedAt) {
        if (rawValue == null || rawValue.isBlank() || rawValue.startsWith("_deleted_")) return;
        String fingerprint = fingerprint(identityType, rawValue);
        if (fingerprintRepository.existsByRequestIdAndIdentityTypeAndFingerprintAndKeyVersion(
                requestId, identityType, fingerprint, keyVersion)) return;
        DeletedIdentityFingerprintEntity entity = new DeletedIdentityFingerprintEntity();
        entity.setIdentityFingerprintId(UUID.randomUUID());
        entity.setRequestId(requestId);
        entity.setAccountId(accountId);
        entity.setIdentityType(identityType);
        entity.setFingerprint(fingerprint);
        entity.setKeyVersion(keyVersion);
        entity.setCreatedAt(capturedAt);
        fingerprintRepository.save(entity);
    }

    private String fingerprint(DeletedIdentityType identityType, String rawValue) {
        String normalized = normalize(identityType, rawValue);
        if (normalized.isBlank()) throw new IllegalArgumentException("Identity value must not be blank");
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to calculate identity fingerprint", exception);
        }
    }

    private String normalize(DeletedIdentityType type, String rawValue) {
        if (rawValue == null) return "";
        String normalized = Normalizer.normalize(rawValue, Normalizer.Form.NFKC).trim();
        return switch (type) {
            case EMAIL, USERNAME, PROVIDER_SUBJECT -> normalized.toLowerCase(Locale.ROOT);
            case PHONE_NUMBER -> normalized.replaceAll("[^0-9]", "");
            case IDENTIFY_CARD, CUSTOMER_CODE, EMPLOYEE_CODE ->
                    normalized.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
        };
    }

    private void requireConfiguredSecret() {
        if (secret.length < 32) {
            throw new IllegalStateException(
                    "ACCOUNT_DELETION_IDENTITY_FINGERPRINT_SECRET must contain at least 32 bytes");
        }
    }
}
