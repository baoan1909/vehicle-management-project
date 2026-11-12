package com.ban.vehicle_management.application.iam.account.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.out.AccountDeletionCleanupPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.AccountDeletionPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.OrganizationClosurePortOut;
import com.ban.vehicle_management.application.iam.account.port.out.DeletedIdentityFingerprintPortOut;
import com.ban.vehicle_management.infrastructure.security.config.AccountDeletionTimeProperties;
import com.ban.vehicle_management.shared.enumeration.iam.AccountDeletionCleanupResourceType;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountDeletionCompletionServiceTest {

    @Test
    void schedulesIdentityAndAvatarImmediatelyButTenantDocumentsAfterRetention() {
        AccountDeletionPortOut accountPort = mock(AccountDeletionPortOut.class);
        OrganizationClosurePortOut organizationPort = mock(OrganizationClosurePortOut.class);
        AccountDeletionCleanupPortOut cleanupPort = mock(AccountDeletionCleanupPortOut.class);
        DeletedIdentityFingerprintPortOut fingerprintPort = mock(DeletedIdentityFingerprintPortOut.class);
        AccountDeletionTimeProperties properties = new AccountDeletionTimeProperties();
        properties.setOrganizationDocumentRetention(Duration.ofDays(30));
        AccountDeletionCompletionService service = new AccountDeletionCompletionService(
                accountPort, organizationPort, cleanupPort, fingerprintPort, properties);
        UUID requestId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-20T00:00:00Z");
        when(organizationPort.complete(requestId, accountId, now))
                .thenReturn(List.of("tenant/document"));
        when(accountPort.completeWithTombstones(requestId, accountId, now))
                .thenReturn(List.of("avatar/object"));
        when(accountPort.findIdentityProviderSubjectSnapshot(requestId))
                .thenReturn("keycloak-subject");

        List<String> objectKeys = service.completeDatabase(requestId, accountId, now);

        assertEquals(List.of("tenant/document", "avatar/object"), objectKeys);
        verify(fingerprintPort).capture(requestId, accountId, now);
        verify(cleanupPort).schedule(requestId, null,
                AccountDeletionCleanupResourceType.IDENTITY_PROVIDER_USER,
                "keycloak-subject", now);
        verify(cleanupPort).schedule(requestId, null,
                AccountDeletionCleanupResourceType.AVATAR_OBJECT,
                "avatar/object", now);
        verify(cleanupPort).schedule(requestId, null,
                AccountDeletionCleanupResourceType.TENANT_PRIVATE_OBJECT,
                "tenant/document", now.plus(Duration.ofDays(30)));
    }
}
