package com.ban.vehicle_management.infrastructure.security.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatabaseSystemAccountIdProviderTest {

    @Mock AccountRepository accountRepository;
    @InjectMocks DatabaseSystemAccountIdProvider provider;

    @Test
    void resolvesWithNonAutoFlushJpaQueryAndCachesSystemAccountId() {
        UUID systemAccountId = UUID.fromString("20000000-0000-0000-0000-00000000a001");
        when(accountRepository.findAccountIdForAuditByUsername("SYSTEM"))
                .thenReturn(Optional.of(systemAccountId));

        assertEquals(systemAccountId, provider.getSystemAccountId());
        assertEquals(systemAccountId, provider.getSystemAccountId());
        verify(accountRepository, times(1)).findAccountIdForAuditByUsername("SYSTEM");
    }

    @Test
    void failsFastWhenMigrationOwnedSystemAccountIsMissing() {
        when(accountRepository.findAccountIdForAuditByUsername("SYSTEM"))
                .thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                provider::getSystemAccountId
        );

        assertEquals("SYSTEM account is not found.", exception.getMessage());
    }
}
