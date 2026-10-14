package com.ban.vehicle_management.testsupport;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared PostgreSQL integration-test infrastructure.
 *
 * <p>The production schema requires both PostGIS and pgvector, so Spring
 * integration tests must not fall back to a developer's local PostgreSQL.
 * A singleton container also keeps the Spring context cache valid across test
 * classes because the JDBC URL remains stable for the lifetime of the JVM.</p>
 */
public abstract class PostgresIntegrationTestSupport {

    private static final String IMAGE = System.getenv().getOrDefault(
            "POSTGRES_SPATIAL_TEST_IMAGE",
            "vehicle-management/postgres-pgvector-postgis:17"
    );

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse(IMAGE).asCompatibleSubstituteFor("postgres")
    ).withDatabaseName("vehicle_management_test")
            .withUsername("integration_test_owner")
            .withPassword("integration_test_password");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
    }
}
