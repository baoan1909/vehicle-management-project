package com.ban.vehicle_management.infrastructure.health;

import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeEmbeddingRepository;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.stereotype.Component;

@Component("pgVector")
public class PgVectorHealthIndicator implements HealthIndicator {

    public static final String MINIMUM_VERSION = "0.8.0";
    public static final String EXPECTED_COLUMN_TYPE = "vector(768)";

    private final KnowledgeEmbeddingRepository repository;

    public PgVectorHealthIndicator(KnowledgeEmbeddingRepository repository) {
        this.repository = repository;
    }

    @Override
    public Health health() {
        try {
            String extensionVersion = repository.findPgVectorExtensionVersion();
            String columnType = repository.findEmbeddingColumnType();

            if (extensionVersion == null) {
                return Health.down().withDetail("reason", "PGVECTOR_EXTENSION_MISSING").build();
            }
            if (compareVersions(extensionVersion, MINIMUM_VERSION) < 0) {
                return Health.down()
                        .withDetail("reason", "PGVECTOR_VERSION_UNSUPPORTED")
                        .withDetail("installedVersion", extensionVersion)
                        .withDetail("minimumVersion", MINIMUM_VERSION)
                        .build();
            }
            if (!EXPECTED_COLUMN_TYPE.equalsIgnoreCase(columnType)) {
                return Health.down()
                        .withDetail("reason", "PGVECTOR_COLUMN_TYPE_INVALID")
                        .withDetail("actualColumnType", columnType == null ? "missing" : columnType)
                        .withDetail("expectedColumnType", EXPECTED_COLUMN_TYPE)
                        .build();
            }
            return Health.up()
                    .withDetail("extensionVersion", extensionVersion)
                    .withDetail("columnType", columnType)
                    .build();
        } catch (Exception exception) {
            return Health.down()
                    .withDetail("reason", "PGVECTOR_HEALTH_CHECK_FAILED")
                    .withException(exception)
                    .build();
        }
    }

    public void assertReady() {
        Health health = health();
        if (!Status.UP.equals(health.getStatus())) {
            throw new IllegalStateException("pgvector chưa sẵn sàng: " + health.getDetails());
        }
    }

    static int compareVersions(String left, String right) {
        String[] leftParts = left.split("\\.");
        String[] rightParts = right.split("\\.");
        int length = Math.max(leftParts.length, rightParts.length);
        for (int index = 0; index < length; index++) {
            int leftValue = index < leftParts.length ? numericPrefix(leftParts[index]) : 0;
            int rightValue = index < rightParts.length ? numericPrefix(rightParts[index]) : 0;
            int comparison = Integer.compare(leftValue, rightValue);
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }

    private static int numericPrefix(String value) {
        String digits = value.replaceFirst("^(\\d+).*$", "$1");
        return digits.matches("\\d+") ? Integer.parseInt(digits) : 0;
    }
}
