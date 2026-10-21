package com.ban.vehicle_management.infrastructure.persistence.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuideFeaturePortOut;
import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuidePortOut;
import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuideUserStatePortOut;
import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuideVersionPortOut;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuide;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideAudience;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideFeature;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideStep;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideUserState;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideVersion;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.notification.UserGuideEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.notification.UserGuideRepository;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideAudienceMode;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideAudienceTargetType;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideDisplayType;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideReplayPolicy;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideStatus;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideStepPlacement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class UserGuideFoundationIT extends com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport {

    private static final UUID SYSTEM_ACCOUNT_ID =
            UUID.fromString("20000000-0000-0000-0000-00000000a001");

    private final JdbcTemplate jdbcTemplate;
    private final UserGuideFeaturePortOut featurePortOut;
    private final UserGuidePortOut guidePortOut;
    private final UserGuideVersionPortOut versionPortOut;
    private final UserGuideUserStatePortOut statePortOut;
    private final UserGuideRepository guideRepository;
    private final PlatformTransactionManager transactionManager;

    @Autowired
    UserGuideFoundationIT(
            DataSource dataSource,
            UserGuideFeaturePortOut featurePortOut,
            UserGuidePortOut guidePortOut,
            UserGuideVersionPortOut versionPortOut,
            UserGuideUserStatePortOut statePortOut,
            UserGuideRepository guideRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.featurePortOut = featurePortOut;
        this.guidePortOut = guidePortOut;
        this.versionPortOut = versionPortOut;
        this.statePortOut = statePortOut;
        this.guideRepository = guideRepository;
        this.transactionManager = transactionManager;
    }

    @AfterEach
    void cleanUserGuideData() {
        jdbcTemplate.update("DELETE FROM notification.user_guide_user_states");
        jdbcTemplate.update("DELETE FROM notification.user_guide_audiences");
        jdbcTemplate.update("DELETE FROM notification.user_guide_steps");
        jdbcTemplate.update("DELETE FROM notification.user_guide_versions");
        jdbcTemplate.update("DELETE FROM notification.user_guides");
        jdbcTemplate.update("DELETE FROM notification.user_guide_features");
    }

    @Test
    void flywayShouldCreateAllSixTablesInNotificationSchema() {
        Integer tableCount = jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM information_schema.tables
                WHERE table_schema = 'notification'
                  AND table_name IN (
                    'user_guide_features', 'user_guides', 'user_guide_versions',
                    'user_guide_steps', 'user_guide_audiences', 'user_guide_user_states'
                  )
                """,
                Integer.class);

        assertEquals(6, tableCount);
    }

    @Test
    void shouldPersistAndReadCompleteFoundationAggregate() {
        UserGuideFeature savedFeature = featurePortOut.save(feature("PARKING_CORE"));
        UserGuide savedGuide = guidePortOut.save(guide(savedFeature.getFeatureId(), "PARKING_ENTRY"));

        UUID roleId = requiredUuid("SELECT role_id FROM iam.roles WHERE code = 'SYSTEM_ADMIN'");
        UserGuideVersion version = version(savedGuide.getGuideId(), 1, UserGuideAudienceMode.ROLE);
        version.setSteps(List.of(step(0)));
        version.setAudiences(List.of(roleAudience(roleId)));
        UserGuideVersion savedVersion = versionPortOut.save(version);

        Instant completedAt = Instant.parse("2026-10-16T03:04:05.123456Z");
        UserGuideUserState state = new UserGuideUserState();
        state.setAccountId(SYSTEM_ACCOUNT_ID);
        state.setGuideId(savedGuide.getGuideId());
        state.setCompletedVersion(1);
        state.setCompletedAt(completedAt);
        state.setLastOpenedAt(completedAt);
        UserGuideUserState savedState = statePortOut.save(state);

        UserGuideVersion loadedVersion = versionPortOut
                .findByGuideIdAndVersionNumber(savedGuide.getGuideId(), 1).orElseThrow();
        UserGuideUserState loadedState = statePortOut
                .findByAccountIdAndGuideId(SYSTEM_ACCOUNT_ID, savedGuide.getGuideId()).orElseThrow();

        assertNotNull(savedFeature.getFeatureId());
        assertNotNull(savedGuide.getGuideId());
        assertNotNull(savedVersion.getGuideVersionId());
        assertNotNull(savedState.getUserGuideStateId());
        assertTrue(featurePortOut.findByCode("PARKING_CORE").isPresent());
        assertTrue(guidePortOut.findByCode("PARKING_ENTRY").isPresent());
        assertEquals(1, loadedVersion.getSteps().size());
        assertEquals("PARKING_CREATE_BUTTON", loadedVersion.getSteps().getFirst().getAnchorKey());
        assertEquals(1, loadedVersion.getAudiences().size());
        assertEquals(roleId, loadedVersion.getAudiences().getFirst().getRoleId());
        assertEquals(completedAt.truncatedTo(ChronoUnit.MICROS),
                loadedState.getCompletedAt().truncatedTo(ChronoUnit.MICROS));
    }

    @Test
    void shouldEnforceUniqueFeatureAndGuideCodes() {
        UserGuideFeature first = featurePortOut.save(feature("DUPLICATE_FEATURE"));
        assertThrows(DataIntegrityViolationException.class,
                () -> featurePortOut.save(feature("DUPLICATE_FEATURE")));

        guidePortOut.save(guide(first.getFeatureId(), "DUPLICATE_GUIDE"));
        assertThrows(DataIntegrityViolationException.class,
                () -> guidePortOut.save(guide(first.getFeatureId(), "DUPLICATE_GUIDE")));
    }

    @Test
    void shouldEnforceUniqueVersionAndStepOrder() {
        UserGuide savedGuide = persistGuide("VERSION_STEP_UNIQUE");
        UserGuideVersion first = version(savedGuide.getGuideId(), 1, UserGuideAudienceMode.PUBLIC);
        first.setSteps(List.of(step(0)));
        versionPortOut.save(first);

        UserGuideVersion duplicateVersion = version(savedGuide.getGuideId(), 1, UserGuideAudienceMode.PUBLIC);
        duplicateVersion.setSteps(List.of(step(1)));
        assertThrows(DataIntegrityViolationException.class, () -> versionPortOut.save(duplicateVersion));

        UserGuideVersion duplicateOrder = version(savedGuide.getGuideId(), 2, UserGuideAudienceMode.PUBLIC);
        duplicateOrder.setSteps(List.of(step(0), step(0)));
        assertThrows(DataIntegrityViolationException.class, () -> versionPortOut.save(duplicateOrder));
    }

    @Test
    void shouldEnforceUniqueAccountGuideStateAndVersionTimestampConsistency() {
        UserGuide savedGuide = persistGuide("STATE_CONSTRAINTS");
        UserGuideUserState first = new UserGuideUserState();
        first.setAccountId(SYSTEM_ACCOUNT_ID);
        first.setGuideId(savedGuide.getGuideId());
        statePortOut.save(first);

        UserGuideUserState duplicate = new UserGuideUserState();
        duplicate.setAccountId(SYSTEM_ACCOUNT_ID);
        duplicate.setGuideId(savedGuide.getGuideId());
        assertThrows(DataIntegrityViolationException.class, () -> statePortOut.save(duplicate));

        UUID invalidStateId = UUID.randomUUID();
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                """
                INSERT INTO notification.user_guide_user_states (
                    user_guide_state_id, account_id, guide_id, completed_version, completed_at
                ) VALUES (?, ?, ?, 1, NULL)
                """,
                invalidStateId, SYSTEM_ACCOUNT_ID, savedGuide.getGuideId()));
    }

    @Test
    void shouldEnforceAudienceShapeUniquenessAndRolePermissionForeignKeys() {
        UserGuide savedGuide = persistGuide("AUDIENCE_CONSTRAINTS");
        UUID versionId = insertVersion(savedGuide.getGuideId(), 1, "ROLE");
        UUID roleId = requiredUuid("SELECT role_id FROM iam.roles WHERE code = 'SYSTEM_ADMIN'");
        UUID permissionId = requiredUuid(
                "SELECT permission_id FROM iam.permissions WHERE permission_code = 'USER_GUIDE_READ_ALL'");

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                """
                INSERT INTO notification.user_guide_audiences (
                    audience_id, guide_version_id, target_type, role_id, permission_id
                ) VALUES (?, ?, 'ROLE', ?, ?)
                """,
                UUID.randomUUID(), versionId, roleId, permissionId));

        jdbcTemplate.update(
                """
                INSERT INTO notification.user_guide_audiences (
                    audience_id, guide_version_id, target_type, role_id
                ) VALUES (?, ?, 'ROLE', ?)
                """,
                UUID.randomUUID(), versionId, roleId);
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                """
                INSERT INTO notification.user_guide_audiences (
                    audience_id, guide_version_id, target_type, role_id
                ) VALUES (?, ?, 'ROLE', ?)
                """,
                UUID.randomUUID(), versionId, roleId));

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                """
                INSERT INTO notification.user_guide_audiences (
                    audience_id, guide_version_id, target_type, permission_id
                ) VALUES (?, ?, 'PERMISSION', ?)
                """,
                UUID.randomUUID(), versionId, UUID.randomUUID()));

        jdbcTemplate.update(
                """
                INSERT INTO notification.user_guide_audiences (
                    audience_id, guide_version_id, target_type, permission_id
                ) VALUES (?, ?, 'PERMISSION', ?)
                """,
                UUID.randomUUID(), versionId, permissionId);
    }

    @Test
    void stateAccountForeignKeyShouldReferenceIamAccounts() {
        UserGuide savedGuide = persistGuide("ACCOUNT_FK");
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                """
                INSERT INTO notification.user_guide_user_states (
                    user_guide_state_id, account_id, guide_id
                ) VALUES (?, ?, ?)
                """,
                UUID.randomUUID(), UUID.randomUUID(), savedGuide.getGuideId()));

        String referencedTable = jdbcTemplate.queryForObject(
                """
                SELECT foreign_table.table_schema || '.' || foreign_table.table_name
                FROM information_schema.referential_constraints reference_constraint
                JOIN information_schema.table_constraints foreign_key
                  ON foreign_key.constraint_catalog = reference_constraint.constraint_catalog
                 AND foreign_key.constraint_schema = reference_constraint.constraint_schema
                 AND foreign_key.constraint_name = reference_constraint.constraint_name
                JOIN information_schema.table_constraints unique_constraint
                  ON unique_constraint.constraint_catalog = reference_constraint.unique_constraint_catalog
                 AND unique_constraint.constraint_schema = reference_constraint.unique_constraint_schema
                 AND unique_constraint.constraint_name = reference_constraint.unique_constraint_name
                JOIN information_schema.tables foreign_table
                  ON foreign_table.table_schema = unique_constraint.table_schema
                 AND foreign_table.table_name = unique_constraint.table_name
                WHERE foreign_key.constraint_schema = 'notification'
                  AND foreign_key.constraint_name = 'fk_user_guide_user_states_account'
                """,
                String.class);
        assertEquals("iam.accounts", referencedTable);
    }

    @Test
    void shouldApplyOptimisticLockingToGuide() {
        UserGuide saved = persistGuide("OPTIMISTIC_LOCK");
        TransactionTemplate requiresNew = new TransactionTemplate(transactionManager);
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        UserGuideEntity first = requiresNew.execute(
                status -> guideRepository.findById(saved.getGuideId()).orElseThrow());
        UserGuideEntity stale = requiresNew.execute(
                status -> guideRepository.findById(saved.getGuideId()).orElseThrow());

        first.setPriority(10);
        requiresNew.executeWithoutResult(status -> guideRepository.saveAndFlush(first));
        stale.setPriority(20);

        assertThrows(OptimisticLockingFailureException.class,
                () -> requiresNew.executeWithoutResult(status -> guideRepository.saveAndFlush(stale)));
    }

    private UserGuide persistGuide(String suffix) {
        UserGuideFeature savedFeature = featurePortOut.save(feature("FEATURE_" + suffix));
        return guidePortOut.save(guide(savedFeature.getFeatureId(), "GUIDE_" + suffix));
    }

    private UserGuideFeature feature(String code) {
        UserGuideFeature feature = new UserGuideFeature();
        feature.setCode(code);
        feature.setName(code);
        feature.setEnabled(true);
        return feature;
    }

    private UserGuide guide(UUID featureId, String code) {
        UserGuide guide = new UserGuide();
        guide.setFeatureId(featureId);
        guide.setCode(code);
        guide.setStatus(UserGuideStatus.DRAFT);
        guide.setDisplayType(UserGuideDisplayType.MODAL);
        guide.setReplayPolicy(UserGuideReplayPolicy.ONCE_PER_VERSION);
        guide.setContextKey("CONTEXT_" + code);
        guide.setPriority(100);
        guide.setDisplayOrder(100);
        guide.setStartAt(Instant.parse("2026-10-16T00:00:00Z"));
        guide.setCurrentVersion(0);
        return guide;
    }

    private UserGuideVersion version(UUID guideId, int versionNumber, UserGuideAudienceMode audienceMode) {
        UserGuideVersion version = new UserGuideVersion();
        version.setGuideId(guideId);
        version.setVersionNumber(versionNumber);
        version.setTitle("Version " + versionNumber);
        version.setAudienceMode(audienceMode);
        return version;
    }

    private UserGuideStep step(int order) {
        UserGuideStep step = new UserGuideStep();
        step.setDisplayOrder(order);
        step.setTitle("Step " + order);
        step.setBody("Body " + order);
        step.setMediaObjectKey("user-guides/step-" + order + ".png");
        step.setAnchorKey("PARKING_CREATE_BUTTON");
        step.setPlacement(UserGuideStepPlacement.BOTTOM);
        step.setCollapseMs(0);
        return step;
    }

    private UserGuideAudience roleAudience(UUID roleId) {
        UserGuideAudience audience = new UserGuideAudience();
        audience.setTargetType(UserGuideAudienceTargetType.ROLE);
        audience.setRoleId(roleId);
        return audience;
    }

    private UUID insertVersion(UUID guideId, int versionNumber, String audienceMode) {
        UUID versionId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO notification.user_guide_versions (
                    guide_version_id, guide_id, version_number, title, audience_mode
                ) VALUES (?, ?, ?, ?, ?)
                """,
                versionId, guideId, versionNumber, "Version " + versionNumber, audienceMode);
        return versionId;
    }

    private UUID requiredUuid(String sql) {
        return jdbcTemplate.queryForObject(sql, UUID.class);
    }
}
