package com.ban.vehicle_management.infrastructure.persistence.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuideUserStateInitializerPortOut;
import com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class UserGuideUserStateInitializerIT extends PostgresIntegrationTestSupport {

    private static final UUID SYSTEM_ACCOUNT_ID =
            UUID.fromString("20000000-0000-0000-0000-00000000a001");

    private final JdbcTemplate jdbcTemplate;
    private final PlatformTransactionManager transactionManager;
    private final UserGuideUserStateInitializerPortOut initializer;

    @Autowired
    UserGuideUserStateInitializerIT(
            DataSource dataSource,
            PlatformTransactionManager transactionManager,
            UserGuideUserStateInitializerPortOut initializer
    ) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.transactionManager = transactionManager;
        this.initializer = initializer;
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM notification.user_guide_user_states");
        jdbcTemplate.update("DELETE FROM notification.user_guides");
        jdbcTemplate.update("DELETE FROM notification.user_guide_features");
    }

    @Test
    void concurrentInitializationShouldCreateExactlyOneStateRow() throws Exception {
        UUID featureId = UUID.randomUUID();
        UUID guideId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO notification.user_guide_features
                    (feature_id, code, name, enabled, created_at)
                VALUES (?, ?, ?, true, now())
                """, featureId, "CONCURRENT_STATE", "Concurrent state");
        jdbcTemplate.update("""
                INSERT INTO notification.user_guides
                    (guide_id, feature_id, code, status, display_type, replay_policy,
                     context_key, priority, display_order, start_at, current_version, created_at)
                VALUES (?, ?, ?, 'PUBLISHED', 'MODAL', 'ONCE_PER_VERSION',
                        ?, 1, 1, now() - interval '1 minute', 1, now())
                """, guideId, featureId, "CONCURRENT_STATE_GUIDE", "CONCURRENT_STATE");

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> initializeInTransaction(start, guideId));
            Future<?> second = executor.submit(() -> initializeInTransaction(start, guideId));
            start.countDown();
            first.get();
            second.get();
        } finally {
            executor.shutdownNow();
        }

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM notification.user_guide_user_states"
                        + " WHERE account_id = ? AND guide_id = ?",
                Integer.class,
                SYSTEM_ACCOUNT_ID,
                guideId);
        assertEquals(1, count);
    }

    private void initializeInTransaction(CountDownLatch start, UUID guideId) {
        try {
            start.await();
            new TransactionTemplate(transactionManager).executeWithoutResult(
                    ignored -> initializer.initializeIfAbsent(SYSTEM_ACCOUNT_ID, guideId));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
