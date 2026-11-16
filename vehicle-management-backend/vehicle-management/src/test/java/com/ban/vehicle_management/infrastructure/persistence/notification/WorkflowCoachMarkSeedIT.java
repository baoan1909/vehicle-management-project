package com.ban.vehicle_management.infrastructure.persistence.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WorkflowCoachMarkSeedIT extends com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport {
    private static final String SEED = "db/migration/V20261115120000__seed_workflow_coach_mark_coverage.sql";
    @Autowired JdbcTemplate jdbc;

    @Test
    void seedPublishesCompleteManualGuidesWithValidAudiencesAndOrderedSteps() {
        assertEquals(19, jdbc.queryForObject("""
                select count(*) from notification.user_guides
                where guide_id = md5('workflow-coverage-guide:' || code)::uuid
                and status='PUBLISHED' and display_type='COACH_MARK'
                and replay_policy='MANUAL_ONLY' and current_version=1
                """, Integer.class));
        assertEquals(79, jdbc.queryForObject("""
                select count(*) from notification.user_guide_steps s
                join notification.user_guide_versions v using(guide_version_id)
                join notification.user_guides g using(guide_id)
                where g.guide_id=md5('workflow-coverage-guide:' || g.code)::uuid
                """, Integer.class));
        assertEquals(0, jdbc.queryForObject("""
                select count(*) from notification.user_guide_versions v
                join notification.user_guides g using(guide_id)
                where g.guide_id=md5('workflow-coverage-guide:' || g.code)::uuid
                  and (v.published_at is null or not exists (
                    select 1 from notification.user_guide_audiences a where a.guide_version_id=v.guide_version_id))
                """, Integer.class));
        assertEquals(0, jdbc.queryForObject("""
                select count(*) from (
                  select s.guide_version_id from notification.user_guide_steps s
                  join notification.user_guide_versions v using(guide_version_id)
                  join notification.user_guides g using(guide_id)
                  where g.guide_id=md5('workflow-coverage-guide:' || g.code)::uuid
                  group by s.guide_version_id having min(s.display_order)<>0 or max(s.display_order)<>count(*)-1
                ) invalid
                """, Integer.class));
    }

    @Test
    void rerunPreservesEditedGuideContentAndEarlierDrafts() throws IOException {
        jdbc.update("""
                update notification.user_guide_versions set title='Nội dung quản trị đã sửa'
                where guide_version_id=md5('workflow-coverage-version:WORKFLOW_CARD_STOCK:1')::uuid
                """);
        jdbc.update("""
                update notification.user_guides set status='INACTIVE', lock_version=3
                where code='WORKFLOW_CARD_STOCK'
                """);
        Integer stepCount = jdbc.queryForObject("select count(*) from notification.user_guide_steps", Integer.class);
        // PostgreSQL parses dollar-quoted DO blocks; the generic Spring script splitter does not.
        jdbc.execute(new ClassPathResource(SEED).getContentAsString(StandardCharsets.UTF_8));
        assertEquals(stepCount, jdbc.queryForObject("select count(*) from notification.user_guide_steps", Integer.class));
        assertEquals("Nội dung quản trị đã sửa", jdbc.queryForObject("""
                select title from notification.user_guide_versions
                where guide_version_id=md5('workflow-coverage-version:WORKFLOW_CARD_STOCK:1')::uuid
                """, String.class));
        assertEquals("INACTIVE", jdbc.queryForObject(
                "select status from notification.user_guides where code='WORKFLOW_CARD_STOCK'", String.class));
        assertEquals("DRAFT", jdbc.queryForObject(
                "select status from notification.user_guides where code='PARKING_STRUCTURE_SETUP'", String.class));
        assertEquals(0, jdbc.queryForObject(
                "select current_version from notification.user_guides where code='PARKING_STRUCTURE_SETUP'", Integer.class));
    }

    @Test
    void audienceUsesExistingPermissionsWithoutRoleOnlyRestrictionOrPermissionGrants() {
        var audiences = jdbc.queryForList("""
                select p.permission_code from notification.user_guide_audiences a
                join iam.permissions p using(permission_id)
                join notification.user_guide_versions v using(guide_version_id)
                join notification.user_guides g using(guide_id)
                where g.code='WORKFLOW_STRUCTURE_SETUP' order by p.permission_code
                """, String.class);
        assertEquals(java.util.List.of("PARKING_SPACE_CONFIGURE_ALL", "PARKING_TOPOLOGY_CONFIGURE_ALL"), audiences);
        assertTrue(jdbc.queryForList("""
                select p.permission_code from notification.user_guide_audiences a
                join iam.permissions p using(permission_id)
                join notification.user_guide_versions v using(guide_version_id)
                join notification.user_guides g using(guide_id) where g.code='WORKFLOW_OWN_SHIFT'
                """, String.class).contains("SHIFT_ASSIGNMENT_READ_OWN"));
        assertEquals(0, jdbc.queryForObject("""
                select count(*) from notification.user_guide_audiences a
                join notification.user_guide_versions v using(guide_version_id)
                join notification.user_guides g using(guide_id)
                where g.guide_id=md5('workflow-coverage-guide:' || g.code)::uuid and a.target_type='ROLE'
                """, Integer.class));
    }
}

