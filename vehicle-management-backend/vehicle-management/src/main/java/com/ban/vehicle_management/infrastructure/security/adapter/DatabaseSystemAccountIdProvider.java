package com.ban.vehicle_management.infrastructure.security.adapter;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DatabaseSystemAccountIdProvider implements SystemAccountIdProvider {

    private static final String SYSTEM_USERNAME = "SYSTEM";
    private static final String FIND_SYSTEM_ACCOUNT_ID_SQL = """
            SELECT account_id
            FROM iam.accounts
            WHERE username = ?
            LIMIT 1
            """;

    private final JdbcTemplate jdbcTemplate;
    private volatile UUID cachedSystemAccountId;

    public DatabaseSystemAccountIdProvider(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public UUID getSystemAccountId() {
        UUID cached = cachedSystemAccountId;
        if (cached != null) {
            return cached;
        }

        UUID resolved = jdbcTemplate.query(
                FIND_SYSTEM_ACCOUNT_ID_SQL,
                preparedStatement -> preparedStatement.setString(1, SYSTEM_USERNAME),
                resultSet -> resultSet.next() ? resultSet.getObject("account_id", UUID.class) : null
        );

        if (resolved == null) {
            throw new IllegalStateException("SYSTEM account is not found.");
        }

        cachedSystemAccountId = resolved;
        return resolved;
    }
}

