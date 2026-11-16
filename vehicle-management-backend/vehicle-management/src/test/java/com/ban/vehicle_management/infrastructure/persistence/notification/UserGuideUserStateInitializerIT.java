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
    private UUID createdFeatureId;
    private UUID createdGuideId;

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
        if (createdGuideId != null) {
            jdbcTemplate.update(
                    "DELETE FROM notification.user_guide_user_states WHERE guide_id = ?",
                    createdGuideId);
            jdbcTemplate.update(
                    "DELETE FROM notification.user_guides WHERE guide_id = ?",
                    createdGuideId);
        }
        if (createdFeatureId != null) {
            jdbcTemplate.update(
                    "DELETE FROM notification.user_guide_features WHERE feature_id = ?",
                    createdFeatureId);
        }
        createdGuideId = null;
        createdFeatureId = null;
    }

    @Test
    void concurrentInitializationShouldCreateExactlyOneStateRow() throws Exception {
        createdFeatureId = UUID.randomUUID();
        createdGuideId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO notification.user_guide_features
                    (feature_id, code, name, enabled, created_at)
                VALUES (?, ?, ?, true, now())
                """, createdFeatureId, "CONCURRENT_STATE", "Concurrent state");
        jdbcTemplate.update("""
                INSERT INTO notification.user_guides
                    (guide_id, feature_id, code, status, display_type, replay_policy,
                     context_key, priority, display_order, start_at, current_version, created_at)
                VALUES (?, ?, ?, 'PUBLISHED', 'MODAL', 'ONCE_PER_VERSION',
                        ?, 1, 1, now() - interval '1 minute', 1, now())
                """, createdGuideId, createdFeatureId, "CONCURRENT_STATE_GUIDE", "CONCURRENT_STATE");

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> initializeInTransaction(start, createdGuideId));
            Future<?> second = executor.submit(() -> initializeInTransaction(start, createdGuideId));
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
                createdGuideId);
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
