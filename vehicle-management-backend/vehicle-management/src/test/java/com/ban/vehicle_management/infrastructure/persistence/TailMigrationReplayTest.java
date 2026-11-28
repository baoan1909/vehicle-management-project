package com.ban.vehicle_management.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@Isolated("Replay historical SQL only against a disposable PostgreSQL database")
class TailMigrationReplayTest {
    private static final Path MIGRATIONS = Path.of("src/main/resources/db/migration");
    private static final String FIRST_MIGRATION = "V20261116110000__";

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse(
            System.getenv().getOrDefault("POSTGRES_SPATIAL_TEST_IMAGE",
                    "vehicle-management/postgres-pgvector-postgis:17"))
            .asCompatibleSubstituteFor("postgres"));

    @Test
    void replaysTailWithoutDeletingLiveSupportDataOrResettingGuideMetadata() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .load();
        flyway.migrate();

        UUID profile = UUID.randomUUID();
        UUID account = UUID.randomUUID();
        UUID category = UUID.randomUUID();
        UUID ticket = UUID.randomUUID();
        UUID conversation = UUID.randomUUID();
        UUID message = UUID.randomUUID();
        try (Connection db = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var sql = db.createStatement()) {
            sql.executeUpdate("""
                    INSERT INTO people.user_profiles(user_profile_id, full_name)
                    VALUES ('%s', 'Migration replay requester')
                    """.formatted(profile));
            sql.executeUpdate("""
                    INSERT INTO iam.accounts(account_id, user_profile_id, username, email, role_id)
                    SELECT '%s', '%s', 'migration-replay', 'migration-replay@example.test', role_id
                    FROM iam.roles WHERE code = 'PARKING_MANAGER'
                    """.formatted(account, profile));
            sql.executeUpdate("""
                    INSERT INTO operations.support_ticket_categories(
                        category_id, code, name, audience_type, display_order)
                    VALUES ('%s', 'REPLAY_INTERNAL_CUSTOM', 'Keep custom settings', 'INTERNAL', 77)
                    """.formatted(category));
            sql.executeUpdate("""
                    INSERT INTO operations.support_tickets(support_ticket_id, category_id, title,
                        content, requester_account_id, requester_role_code)
                    VALUES ('%s', '%s', 'Keep ticket', 'Keep content', '%s', 'PARKING_MANAGER')
                    """.formatted(ticket, category, account));
            sql.executeUpdate("""
                    INSERT INTO operations.chat_conversations(
                        conversation_id, conversation_type, owner_account_id)
                    VALUES ('%s', 'ASSISTANT_SUPPORT', '%s')
                    """.formatted(conversation, account));
            sql.executeUpdate("""
                    INSERT INTO operations.support_ticket_conversation_links(
                        support_ticket_id, conversation_id, link_reason)
                    VALUES ('%s', '%s', 'FIRST_REPLY')
                    """.formatted(ticket, conversation));
            sql.executeUpdate("""
                    INSERT INTO operations.chat_messages(message_id, conversation_id,
                        message_type, content, context_ticket_id)
                    VALUES ('%s', '%s', 'TEXT', 'Keep context', '%s')
                    """.formatted(message, conversation, ticket));
            sql.executeUpdate("""
                    INSERT INTO notification.user_guide_versions(guide_id, version_number,
                        title, audience_mode, published_at)
                    SELECT guide_id, 3, 'Newer published guide', 'ALL_AUTHENTICATED', now()
                    FROM notification.user_guides WHERE code = 'PRICE_RULE_SETUP'
                    """);
            sql.executeUpdate("""
                    UPDATE notification.user_guides SET current_version = 3
                    WHERE code = 'PRICE_RULE_SETUP'
                    """);
            String stepsQuery = """
                    SELECT string_agg(step_id::text || ':' || display_order, ',' ORDER BY step_id)
                    FROM notification.user_guide_steps
                    WHERE guide_version_id IN (
                        md5('price-rule-guide:PRICE_RULE_SETUP:2')::uuid,
                        md5('price-rule-guide:WORKFLOW_RULE_DRAFT:2')::uuid)
                    """;
            String steps = text(db, stepsQuery);
            String archiveQuery = "SELECT jsonb_agg(to_jsonb(t) ORDER BY record_table, record_key)::text FROM ai.retired_provider_records t";
            String archived = text(db, archiveQuery);
            try (var paths = Files.list(MIGRATIONS)) {
                var migrations = paths.filter(path -> path.getFileName().toString().startsWith("V"))
                        .filter(path -> path.getFileName().toString().endsWith(".sql"))
                        .filter(path -> path.getFileName().toString().compareTo(FIRST_MIGRATION) >= 0)
                        .sorted().toList();
                assertFalse(migrations.isEmpty());
                for (int pass = 0; pass < 2; pass++) {
                    for (Path migration : migrations) {
                        db.setAutoCommit(false);
                        try {
                            sql.execute(Files.readString(migration));
                            db.commit();
                        } catch (SQLException error) {
                            db.rollback();
                            throw new AssertionError("Replay failed: " + migration.getFileName(), error);
                        } finally {
                            db.setAutoCommit(true);
                        }
                    }
                    assertEquals("1", text(db, "SELECT count(*) FROM operations.support_tickets WHERE support_ticket_id='%s'".formatted(ticket)));
                    assertEquals("1", text(db, "SELECT count(*) FROM operations.chat_conversations WHERE conversation_id='%s'".formatted(conversation)));
                    assertEquals("1", text(db, "SELECT count(*) FROM operations.support_ticket_conversation_links WHERE support_ticket_id='%s'".formatted(ticket)));
                    assertEquals(ticket.toString(), text(db, "SELECT context_ticket_id FROM operations.chat_messages WHERE message_id='%s'".formatted(message)));
                    assertEquals("INTERNAL:77", text(db, "SELECT audience_type || ':' || display_order FROM operations.support_ticket_categories WHERE category_id='%s'".formatted(category)));
                    assertEquals("3", text(db, "SELECT current_version FROM notification.user_guides WHERE code='PRICE_RULE_SETUP'"));
                    assertEquals(steps, text(db, stepsQuery));
                    assertEquals(archived, text(db, archiveQuery));
                    assertEquals("0", text(db, """
                            SELECT count(*) FROM information_schema.tables
                            WHERE table_schema='ai'
                              AND table_name IN ('runtime_mode','runtime_mode_events','provider_http_calls')
                            """));
                }
            }
            flyway.validate();
        }
    }

    private static String text(Connection db, String query) throws SQLException {
        try (var statement = db.createStatement(); var rows = statement.executeQuery(query)) {
            assertTrue(rows.next());
            return rows.getString(1);
        }
    }
}
