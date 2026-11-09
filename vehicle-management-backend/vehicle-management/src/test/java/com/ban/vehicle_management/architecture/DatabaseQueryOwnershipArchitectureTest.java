package com.ban.vehicle_management.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class DatabaseQueryOwnershipArchitectureTest {

    private static final Path PRODUCTION_ROOT = Path.of("src/main/java");
    private static final String REPOSITORY_SEGMENT =
            "infrastructure/persistence/database/repository/";

    private static final Pattern FORBIDDEN_DATABASE_API = Pattern.compile(
            "import\\s+.*\\.(?:JdbcTemplate|NamedParameterJdbcTemplate|EntityManager|DataSource)\\s*;|"
                    + "\\b(?:createNativeQuery|createQuery|prepareStatement|createStatement)\\s*\\(");
    private static final Pattern QUERY_ANNOTATION = Pattern.compile(
            "@(?:Query|NativeQuery|Modifying)\\b");
    private static final Pattern SQL_IN_ADAPTER = Pattern.compile(
            "(?is)(?:\\\"\\\"\\\"|\\\")\\s*(?:select|insert\\s+into|update\\s+[a-z_]"
                    + "|delete\\s+from|with\\s+[a-z_]+\\s+as)\\b");

    @Test
    void databaseApisAndQueryAnnotationsMustStayInSpringDataRepositories() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path source : productionSources()) {
            if (isRepository(source)) {
                continue;
            }
            String content = Files.readString(source);
            collect(source, content, FORBIDDEN_DATABASE_API, violations);
            collect(source, content, QUERY_ANNOTATION, violations);
        }
        assertTrue(violations.isEmpty(), () -> String.join(System.lineSeparator(), violations));
    }

    @Test
    void adaptersMustNotOwnSqlText() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path source : productionSources()) {
            if (!source.getFileName().toString().endsWith("Adapter.java")) {
                continue;
            }
            collect(source, Files.readString(source), SQL_IN_ADAPTER, violations);
        }
        assertTrue(violations.isEmpty(), () -> String.join(System.lineSeparator(), violations));
    }

    private void collect(
            Path source, String content, Pattern pattern, List<String> violations) {
        var matcher = pattern.matcher(content);
        while (matcher.find()) {
            violations.add(source + " -> " + matcher.group());
        }
    }

    private boolean isRepository(Path source) {
        return source.toString().replace('\\', '/').contains(REPOSITORY_SEGMENT);
    }

    private List<Path> productionSources() throws IOException {
        try (Stream<Path> files = Files.walk(PRODUCTION_ROOT)) {
            return files.filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted()
                    .toList();
        }
    }
}
