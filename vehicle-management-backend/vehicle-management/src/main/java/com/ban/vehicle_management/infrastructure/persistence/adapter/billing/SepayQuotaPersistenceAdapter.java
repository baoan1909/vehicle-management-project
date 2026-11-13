package com.ban.vehicle_management.infrastructure.persistence.adapter.billing;

import com.ban.vehicle_management.application.billing.payment.port.out.SepayQuotaPortOut;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SepayQuotaPersistenceAdapter implements SepayQuotaPortOut {
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String LOCK_SQL =
            "SELECT pg_advisory_xact_lock(hashtext(?))";
    private static final String COUNT_ACTIVE_USAGE_SQL = """
            SELECT COUNT(*)
              FROM billing.payments
             WHERE payment_method = 'SEPAY'
               AND ((status = 'SUCCESS' AND paid_at >= ? AND paid_at < ?)
                    OR (status = 'PENDING'
                        AND expires_at > CURRENT_TIMESTAMP
                        AND created_at < ?))
            """;

    private final JdbcTemplate jdbcTemplate;

    public SepayQuotaPersistenceAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean tryAcquire(YearMonth period, int limit) {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (PreparedStatement statement = connection.prepareStatement(LOCK_SQL)) {
                statement.setString(1, "SEPAY:" + period);
                statement.executeQuery().close();
            }
            return null;
        });
        Instant from = period.atDay(1).atStartOfDay(VIETNAM_ZONE).toInstant();
        Instant to = period.plusMonths(1).atDay(1).atStartOfDay(VIETNAM_ZONE).toInstant();
        Long activeUsage = jdbcTemplate.queryForObject(
                COUNT_ACTIVE_USAGE_SQL,
                Long.class,
                Timestamp.from(from),
                Timestamp.from(to),
                Timestamp.from(to)
        );
        return activeUsage != null && activeUsage < limit;
    }
}
