package com.ban.vehicle_management.application.notification.userguide.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.notification.userguide.port.in.UserGuideAdminPortIn;
import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuideFeaturePortOut;
import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuidePortOut;
import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuideVersionPortOut;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuide;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideFeature;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideStep;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideVersion;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideAudienceMode;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideDisplayType;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideReplayPolicy;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideStatus;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideStepPlacement;
import com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport;
import java.time.Instant;
import java.util.List;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class UserGuideConcurrentPublishIT extends PostgresIntegrationTestSupport {

    private static final UUID SYSTEM_ACCOUNT_ID =
            UUID.fromString("20000000-0000-0000-0000-00000000a001");

    @Autowired
    private UserGuideAdminPortIn adminPortIn;

    @Autowired
    private UserGuideFeaturePortOut featurePortOut;

    @Autowired
    private UserGuidePortOut guidePortOut;

    @Autowired
    private UserGuideVersionPortOut versionPortOut;

    @Autowired
    private DataSource dataSource;

    @MockitoBean
    private CurrentAccountPortIn currentAccountPortIn;

    @MockitoBean
    private AuditLogPortOut auditLogPortOut;

    @AfterEach
    void cleanUp() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("DELETE FROM notification.user_guide_user_states");
        jdbc.update("DELETE FROM notification.user_guide_audiences");
        jdbc.update("DELETE FROM notification.user_guide_steps");
        jdbc.update("DELETE FROM notification.user_guide_versions");
        jdbc.update("DELETE FROM notification.user_guides");
        jdbc.update("DELETE FROM notification.user_guide_features");
    }

    @Test
    void concurrentPublishOfSameDraftShouldHaveExactlyOneWinner() throws Exception {
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(SYSTEM_ACCOUNT_ID);

        UserGuideFeature feature = new UserGuideFeature();
        feature.setCode("CONCURRENT_PUBLISH");
        feature.setName("Concurrent publish");
        feature.setEnabled(true);
        feature = featurePortOut.save(feature);

        UserGuide guide = new UserGuide();
        guide.setFeatureId(feature.getFeatureId());
        guide.setCode("CONCURRENT_PUBLISH_GUIDE");
        guide.setStatus(UserGuideStatus.DRAFT);
        guide.setDisplayType(UserGuideDisplayType.MODAL);
        guide.setReplayPolicy(UserGuideReplayPolicy.ONCE_PER_VERSION);
        guide.setContextKey("CONCURRENT_PUBLISH");
        guide.setPriority(1);
        guide.setDisplayOrder(1);
        guide.setStartAt(Instant.now().minusSeconds(60));
        guide.setCurrentVersion(0);
        guide = guidePortOut.save(guide);

        UserGuideStep step = new UserGuideStep();
        step.setDisplayOrder(0);
        step.setTitle("First step");
        step.setBody("Concurrent publish step");
        step.setPlacement(UserGuideStepPlacement.CENTER);
        step.setCollapseMs(0);
        UserGuideVersion version = new UserGuideVersion();
        version.setGuideId(guide.getGuideId());
        version.setVersionNumber(1);
        version.setTitle("Version one");
        version.setAudienceMode(UserGuideAudienceMode.ALL_AUTHENTICATED);
        version.setSteps(List.of(step));
        version.setAudiences(List.of());
        versionPortOut.save(version);

        UUID guideId = guide.getGuideId();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> publish(start, guideId));
            Future<Boolean> second = executor.submit(() -> publish(start, guideId));
            start.countDown();
            long successes = List.of(first.get(), second.get()).stream().filter(Boolean::booleanValue).count();
            assertEquals(1, successes);
        } finally {
            executor.shutdownNow();
        }

        UserGuide published = guidePortOut.findById(guideId).orElseThrow();
        assertEquals(UserGuideStatus.PUBLISHED, published.getStatus());
        assertEquals(1, published.getCurrentVersion());
        assertTrue(versionPortOut.findByGuideIdAndVersionNumber(guideId, 1).orElseThrow()
                .getPublishedAt() != null);
    }

    private boolean publish(CountDownLatch start, UUID guideId) {
        try {
            start.await();
            adminPortIn.publishVersion(guideId, 1);
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        } catch (RuntimeException conflict) {
            return false;
        }
    }
}
