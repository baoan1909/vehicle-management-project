package com.ban.vehicle_management.infrastructure.security.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;

@ExtendWith(MockitoExtension.class)
class DatabaseSystemAccountIdProviderTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DatabaseSystemAccountIdProvider databaseSystemAccountIdProvider;

    @Test
    void shouldResolveAndCacheSystemAccountId() {
        UUID systemAccountId = UUID.fromString("20000000-0000-0000-0000-00000000a001");
        when(jdbcTemplate.query(
                anyString(),
                any(PreparedStatementSetter.class),
                any(ResultSetExtractor.class)
        )).thenReturn(systemAccountId);

        UUID first = databaseSystemAccountIdProvider.getSystemAccountId();
        UUID second = databaseSystemAccountIdProvider.getSystemAccountId();

        assertEquals(systemAccountId, first);
        assertEquals(systemAccountId, second);
        verify(jdbcTemplate, times(1)).query(
                anyString(),
                any(PreparedStatementSetter.class),
                any(ResultSetExtractor.class)
        );
    }

    @Test
    void shouldThrowWhenSystemAccountMissing() {
        when(jdbcTemplate.query(
                anyString(),
                any(PreparedStatementSetter.class),
                any(ResultSetExtractor.class)
        )).thenReturn(null);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> databaseSystemAccountIdProvider.getSystemAccountId()
        );

        assertEquals(
                "SYSTEM account is not found. Ensure migration V7 has been applied.",
                exception.getMessage()
        );
    }
}

