package com.ban.vehicle_management.application.iam.account.usecase;

import com.ban.vehicle_management.application.iam.account.port.out.AccountDeletionPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.AccountDeletionCleanupPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.OrganizationClosurePortOut;
import com.ban.vehicle_management.application.iam.account.port.out.DeletedIdentityFingerprintPortOut;
import com.ban.vehicle_management.infrastructure.security.config.AccountDeletionTimeProperties;
import com.ban.vehicle_management.shared.enumeration.iam.AccountDeletionCleanupResourceType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountDeletionCompletionService {
    private final AccountDeletionPortOut accountDeletionPort;
    private final OrganizationClosurePortOut organizationClosurePort;
    private final AccountDeletionCleanupPortOut cleanupPort;
    private final DeletedIdentityFingerprintPortOut identityFingerprintPort;
    private final AccountDeletionTimeProperties properties;

    public AccountDeletionCompletionService(
            AccountDeletionPortOut accountDeletionPort,
            OrganizationClosurePortOut organizationClosurePort,
            AccountDeletionCleanupPortOut cleanupPort,
            DeletedIdentityFingerprintPortOut identityFingerprintPort,
            AccountDeletionTimeProperties properties) {
        this.accountDeletionPort = accountDeletionPort;
        this.organizationClosurePort = organizationClosurePort;
        this.cleanupPort = cleanupPort;
        this.identityFingerprintPort = identityFingerprintPort;
        this.properties = properties;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<String> completeDatabase(UUID requestId, UUID accountId, Instant completedAt) {
        identityFingerprintPort.capture(requestId, accountId, completedAt);
        List<String> organizationObjectKeys = organizationClosurePort.complete(
                requestId, accountId, completedAt);
        List<String> accountObjectKeys = accountDeletionPort.completeWithTombstones(
                requestId, accountId, completedAt);
        String identityProviderSubject = accountDeletionPort
                .findIdentityProviderSubjectSnapshot(requestId);
        if (identityProviderSubject != null && !identityProviderSubject.isBlank()
                && !identityProviderSubject.startsWith("_deleted_")) {
            cleanupPort.schedule(requestId, null,
                    AccountDeletionCleanupResourceType.IDENTITY_PROVIDER_USER,
                    identityProviderSubject, completedAt);
        }
        accountObjectKeys.forEach(objectKey -> cleanupPort.schedule(requestId, null,
                AccountDeletionCleanupResourceType.AVATAR_OBJECT, objectKey, completedAt));
        Instant documentDeletionAt = completedAt.plus(properties.getOrganizationDocumentRetention());
        organizationObjectKeys.forEach(objectKey -> cleanupPort.schedule(requestId, null,
                AccountDeletionCleanupResourceType.TENANT_PRIVATE_OBJECT,
                objectKey, documentDeletionAt));
        List<String> objectKeys = new ArrayList<>(organizationObjectKeys);
        objectKeys.addAll(accountObjectKeys);
        return objectKeys.stream()
                .filter(key -> key != null && !key.isBlank())
                .distinct()
                .toList();
    }
}
