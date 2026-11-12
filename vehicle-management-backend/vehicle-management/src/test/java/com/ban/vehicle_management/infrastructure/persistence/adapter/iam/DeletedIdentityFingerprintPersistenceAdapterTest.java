package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.audit.DeletedIdentityFingerprintEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.audit.DeletedIdentityFingerprintRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountIdentityRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import com.ban.vehicle_management.shared.enumeration.iam.DeletedIdentityType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DeletedIdentityFingerprintPersistenceAdapterTest {

    @Test
    void capturesOnlyHmacAndNormalizesLookupUsingTheSameRule() {
        DeletedIdentityFingerprintRepository fingerprintRepository =
                mock(DeletedIdentityFingerprintRepository.class);
        AccountRepository accountRepository = mock(AccountRepository.class);
        AccountIdentityRepository identityRepository = mock(AccountIdentityRepository.class);
        DeletedIdentityFingerprintPersistenceAdapter adapter =
                new DeletedIdentityFingerprintPersistenceAdapter(
                        fingerprintRepository, accountRepository, identityRepository,
                        "01234567890123456789012345678901", "v1");
        UUID requestId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-11T00:00:00Z");
        AccountRepository.DeletionIdentityProjection projection =
                mock(AccountRepository.DeletionIdentityProjection.class);
        when(projection.getEmail()).thenReturn(" Customer@Example.COM ");
        when(accountRepository.findDeletionIdentityByAccountId(accountId))
                .thenReturn(Optional.of(projection));
        when(identityRepository.findAllByAccountId(accountId)).thenReturn(List.of());
        when(fingerprintRepository.existsByRequestIdAndIdentityTypeAndFingerprintAndKeyVersion(
                eq(requestId), eq(DeletedIdentityType.EMAIL), any(), eq("v1"))).thenReturn(false);

        adapter.capture(requestId, accountId, now);

        ArgumentCaptor<DeletedIdentityFingerprintEntity> captor =
                ArgumentCaptor.forClass(DeletedIdentityFingerprintEntity.class);
        verify(fingerprintRepository).save(captor.capture());
        DeletedIdentityFingerprintEntity saved = captor.getValue();
        assertEquals(64, saved.getFingerprint().length());
        assertEquals(DeletedIdentityType.EMAIL, saved.getIdentityType());
        adapter.lookup(DeletedIdentityType.EMAIL, "customer@example.com");
        verify(fingerprintRepository).findAllByIdentityTypeAndFingerprintAndKeyVersion(
                DeletedIdentityType.EMAIL, saved.getFingerprint(), "v1");
    }
}
