package com.ban.vehicle_management.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class SpatialFreshDatabaseContainerTest {

    private static final String IMAGE = System.getenv().getOrDefault(
            "POSTGRES_SPATIAL_TEST_IMAGE",
            "vehicle-management/postgres-pgvector-postgis:17"
    );

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse(IMAGE).asCompatibleSubstituteFor("postgres")
    ).withDatabaseName("vehicle_management_fresh")
            .withUsername("migration_owner")
            .withPassword("migration_password");

    @BeforeAll
    static void migrateFreshDatabase() {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .defaultSchema("public")
                .schemas("public")
                .validateMigrationNaming(true)
                .cleanDisabled(true)
                .load();

        flyway.migrate();
        assertEquals(0, flyway.info().pending().length);
    }

    @Test
    void shouldInstallExtensionsAndSeedBothAdministrativeDatasets() throws SQLException {
        try (Connection connection = connection()) {
            assertEquals(2, scalar(connection, "SELECT count(*) FROM pg_extension WHERE extname IN ('vector', 'postgis')"));
            assertEquals(34, scalar(connection, "SELECT count(*) FROM reference.provinces"));
            assertEquals(3_321, scalar(connection, "SELECT count(*) FROM reference.wards"));
            assertEquals(63, scalar(connection, "SELECT count(*) FROM reference.legacy_provinces"));
            assertEquals(696, scalar(connection, "SELECT count(*) FROM reference.legacy_districts"));
            assertEquals(10_035, scalar(connection, "SELECT count(*) FROM reference.legacy_wards"));
            assertEquals(3_321, scalar(connection, "SELECT count(*) FROM reference.gis_wards"));
        }
    }

    @Test
    void shouldEnforceReferenceForeignKeys() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            assertThrows(SQLException.class, () -> statement.executeUpdate("""
                    INSERT INTO reference.legacy_districts (
                        code, province_code, name, full_name, code_name,
                        administrative_unit_id, administrative_unit_name
                    ) VALUES ('999', '99', 'Invalid', 'Invalid', 'invalid-test', 1, 'Invalid')
                    """));
        }
    }

    @Test
    void shouldKeepOnlyCanonicalDisplayAddressColumns() throws SQLException {
        try (Connection connection = connection()) {
            assertEquals(0, scalar(connection, """
                    SELECT count(*)
                    FROM information_schema.columns
                    WHERE table_schema IN ('people', 'iam', 'parking')
                      AND table_name IN ('user_profiles', 'organizations', 'parking_lots')
                      AND column_name = 'address'
                    """));
            assertEquals(3, scalar(connection, """
                    SELECT count(*)
                    FROM information_schema.columns
                    WHERE table_schema IN ('people', 'iam', 'parking')
                      AND table_name IN ('user_profiles', 'organizations', 'parking_lots')
                      AND column_name = 'address_display'
                    """));
        }
    }

    @Test
    void shouldInstallWalletPermissionsWithDistinctIamTuples() throws SQLException {
        try (Connection connection = connection()) {
            assertEquals(1, scalar(connection, """
                    SELECT count(*)
                    FROM iam.permission_modules
                    WHERE module_id = '00000000-0000-0000-0000-000000001045'
                      AND code = 'WALLET'
                    """));
            assertEquals(23, scalar(connection, """
                    SELECT count(*)
                    FROM iam.permissions permission
                    JOIN iam.permission_modules module ON module.module_id = permission.module_id
                    WHERE module.code = 'WALLET'
                    """));
            assertEquals(23, scalar(connection, """
                    SELECT count(DISTINCT (permission.module_id, permission.action_id, permission.scope_id))
                    FROM iam.permissions permission
                    JOIN iam.permission_modules module ON module.module_id = permission.module_id
                    WHERE module.code = 'WALLET'
                    """));
            assertEquals(6, walletRolePermissionCount(connection, "CUSTOMER"));
            assertEquals(6, walletRolePermissionCount(connection, "PARTNER_ADMIN"));
            assertEquals(11, walletRolePermissionCount(connection, "SYSTEM_ADMIN"));
        }
    }

    @Test
    void shouldEnforceWalletOwnershipUniquenessAndVndAmounts() throws SQLException {
        UUID walletId = UUID.randomUUID();
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO billing.wallets (
                        wallet_id, owner_type, organization_id, wallet_purpose, currency
                    ) VALUES (
                        '%s', 'ORGANIZATION', '00000000-0000-0000-0000-000000009001',
                        'ORGANIZATION_SETTLEMENT', 'VND'
                    )
                    """.formatted(walletId));
        }

        assertSqlFails("""
                INSERT INTO billing.wallets (
                    wallet_id, owner_type, organization_id, wallet_purpose, currency
                ) VALUES (
                    '%s', 'ORGANIZATION', '00000000-0000-0000-0000-000000009001',
                    'ORGANIZATION_SETTLEMENT', 'VND'
                )
                """.formatted(UUID.randomUUID()));
        assertSqlFails("""
                INSERT INTO billing.wallets (
                    wallet_id, owner_type, organization_id, wallet_purpose, currency
                ) VALUES ('%s', 'ORGANIZATION', '%s', 'ORGANIZATION_SETTLEMENT', 'VND')
                """.formatted(UUID.randomUUID(), UUID.randomUUID()));
        assertSqlFails("""
                INSERT INTO billing.wallets (
                    wallet_id, owner_type, wallet_purpose, currency, available_balance
                ) VALUES ('%s', 'PLATFORM', 'DECIMAL_TEST', 'VND', 0.50)
                """.formatted(UUID.randomUUID()));
    }

    @Test
    void shouldKeepPostedLedgerEntriesImmutable() throws SQLException {
        UUID transactionId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO billing.financial_transactions (
                        financial_transaction_id, transaction_code, transaction_type, status,
                        idempotency_key, currency
                    ) VALUES ('%s', '%s', 'WALLET_ADJUSTMENT', 'POSTED', '%s', 'VND')
                    """.formatted(transactionId, "TEST-" + transactionId, "TEST-" + transactionId));
            statement.executeUpdate("""
                    INSERT INTO billing.ledger_entries (
                        ledger_entry_id, financial_transaction_id, ledger_account_id,
                        entry_side, amount, description
                    ) VALUES (
                        '%s', '%s', '00000000-0000-0000-0000-000000011003',
                        'DEBIT', 1000, 'immutability test'
                    )
                    """.formatted(entryId, transactionId));
        }

        assertSqlFails("""
                UPDATE billing.ledger_entries SET amount = 2000
                WHERE ledger_entry_id = '%s'
                """.formatted(entryId));
        assertSqlFails("""
                DELETE FROM billing.ledger_entries WHERE ledger_entry_id = '%s'
                """.formatted(entryId));
    }

    @Test
    void shouldProvisionOneWalletWhenTwoRequestsRace() throws Exception {
        String purpose = "RACE_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> first = executor.submit(() -> insertOrganizationWalletOnConflict(purpose, start));
            Future<Integer> second = executor.submit(() -> insertOrganizationWalletOnConflict(purpose, start));
            start.countDown();

            assertEquals(1, first.get() + second.get());
        }

        try (Connection connection = connection()) {
            assertEquals(1, scalar(connection, """
                    SELECT count(*) FROM billing.wallets
                    WHERE organization_id = '00000000-0000-0000-0000-000000009001'
                      AND currency = 'VND'
                      AND wallet_purpose = '%s'
                    """.formatted(purpose)));
        }
    }

    @Test
    void shouldSynchronizeLocationRunSpatialQueryAndUseGistIndex() throws SQLException {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID outside = UUID.randomUUID();
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            insertActiveParkingLot(statement, first, 106.700806, 10.776889);
            insertActiveParkingLot(statement, second, 106.709000, 10.780100);
            insertActiveParkingLot(statement, outside, 106.800000, 10.850000);

            assertEquals(3, scalar(connection, """
                    SELECT count(*) FROM parking.parking_lots
                    WHERE latitude IS NOT NULL
                      AND location IS NOT NULL
                      AND public.ST_SRID(location) = 4326
                    """));
            assertEquals(2, scalar(connection, """
                    SELECT count(*) FROM parking.parking_lots
                    WHERE status = 'ACTIVE'
                      AND public.ST_DWithin(
                          location,
                          public.ST_SetSRID(public.ST_MakePoint(106.700806, 10.776889), 4326)::geography,
                          5000
                      )
                    """));

            statement.execute("SET enable_seqscan = off");
            StringBuilder plan = new StringBuilder();
            try (ResultSet resultSet = statement.executeQuery("""
                    EXPLAIN (COSTS OFF)
                    SELECT parking_lot_id
                    FROM parking.parking_lots
                    WHERE status = 'ACTIVE'
                      AND location IS NOT NULL
                      AND public.ST_DWithin(
                          location,
                          public.ST_SetSRID(public.ST_MakePoint(106.700806, 10.776889), 4326)::geography,
                          5000
                      )
                    """)) {
                while (resultSet.next()) {
                    plan.append(resultSet.getString(1)).append(System.lineSeparator());
                }
            }
            assertTrue(plan.toString().contains("idx_parking_lots_active_location_gist"), plan.toString());
        }
    }

    private static void insertActiveParkingLot(
            Statement statement,
            UUID parkingLotId,
            double longitude,
            double latitude
    ) throws SQLException {
        statement.executeUpdate("""
                INSERT INTO parking.parking_lots (
                    parking_lot_id, organization_id, code, name, total_capacity, status,
                    latitude, longitude, location, address_input_scheme, address_display,
                    current_ward_code, geocoding_status
                )
                SELECT
                    '%s', '00000000-0000-0000-0000-000000009001', '%s', 'Fresh container test', 10, 'ACTIVE',
                    %s, %s, NULL, 'CURRENT', 'Fresh container test', ward_code, 'RESOLVED'
                FROM reference.current_ward_codes_for_point(
                    public.ST_SetSRID(public.ST_MakePoint(%s, %s), 4326)::geography
                )
                LIMIT 1
                """.formatted(parkingLotId, "FRESH-" + parkingLotId, latitude, longitude, longitude, latitude));
    }

    private static int scalar(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next());
            return resultSet.getInt(1);
        }
    }

    private static int walletRolePermissionCount(Connection connection, String roleCode) throws SQLException {
        return scalar(connection, """
                SELECT count(*)
                FROM iam.role_permissions role_permission
                JOIN iam.roles role ON role.role_id = role_permission.role_id
                JOIN iam.permissions permission ON permission.permission_id = role_permission.permission_id
                JOIN iam.permission_modules module ON module.module_id = permission.module_id
                WHERE role.code = '%s'
                  AND module.code = 'WALLET'
                  AND role_permission.is_active = true
                """.formatted(roleCode));
    }

    private static void assertSqlFails(String sql) {
        assertThrows(SQLException.class, () -> {
            try (Connection connection = connection(); Statement statement = connection.createStatement()) {
                statement.executeUpdate(sql);
            }
        });
    }

    private static int insertOrganizationWalletOnConflict(String purpose, CountDownLatch start) throws Exception {
        start.await();
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            return statement.executeUpdate("""
                    INSERT INTO billing.wallets (
                        wallet_id, owner_type, organization_id, wallet_purpose, currency
                    ) VALUES (
                        '%s', 'ORGANIZATION', '00000000-0000-0000-0000-000000009001', '%s', 'VND'
                    )
                    ON CONFLICT DO NOTHING
                    """.formatted(UUID.randomUUID(), purpose));
        }
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
