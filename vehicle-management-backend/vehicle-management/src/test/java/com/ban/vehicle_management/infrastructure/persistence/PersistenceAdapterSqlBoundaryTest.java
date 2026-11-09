package com.ban.vehicle_management.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class PersistenceAdapterSqlBoundaryTest {

    private static final List<Pattern> FORBIDDEN_PATTERNS = List.of(
            Pattern.compile("jakarta\\.persistence\\.EntityManager"),
            Pattern.compile("org\\.springframework\\.jdbc"),
            Pattern.compile("javax\\.sql\\.DataSource"),
            Pattern.compile("java\\.sql\\.(Connection|PreparedStatement|ResultSet)"),
            Pattern.compile("createNativeQuery\\s*\\("),
            Pattern.compile("createQuery\\s*\\("),
            Pattern.compile("pg_advisory_xact_lock", Pattern.CASE_INSENSITIVE),
            Pattern.compile("nextval\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*(SELECT|INSERT\\s+INTO|UPDATE\\s+[a-z_]|DELETE\\s+FROM|WITH\\s+[a-z_]+\\s+AS)\\b",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE)
    );

    @Test
    void productionAdaptersMustNotOwnDatabaseQueries() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java");
        List<String> violations = new ArrayList<>();
        try (var paths = Files.walk(sourceRoot)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith("Adapter.java"))
                    .forEach(path -> inspect(path, violations));
        }
        assertTrue(violations.isEmpty(), () -> "SQL/database query execution found in adapters:\n"
                + String.join("\n", violations));
    }

    @Test
    void productionDatabaseAccessMustBeOwnedBySpringDataRepositories() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java");
        List<String> violations = new ArrayList<>();
        List<Pattern> directAccess = List.of(
                Pattern.compile("import\\s+jakarta\\.persistence\\.EntityManager"),
                Pattern.compile("import\\s+org\\.springframework\\.jdbc"),
                Pattern.compile("import\\s+javax\\.sql\\.DataSource"),
                Pattern.compile("import\\s+java\\.sql\\.(Connection|PreparedStatement|ResultSet)"),
                Pattern.compile("createNativeQuery\\s*\\("),
                Pattern.compile("createQuery\\s*\\(")
        );
        try (var paths = Files.walk(sourceRoot)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .forEach(path -> inspect(path, directAccess, violations));
        }
        assertTrue(violations.isEmpty(), () -> "Direct database access found outside Spring Data JPA:\n"
                + String.join("\n", violations));
    }

    private static void inspect(Path path, List<String> violations) {
        inspect(path, FORBIDDEN_PATTERNS, violations);
    }

    private static void inspect(Path path, List<Pattern> patterns, List<String> violations) {
        try {
            String source = Files.readString(path);
            patterns.stream()
                    .filter(pattern -> pattern.matcher(source).find())
                    .map(pattern -> path + " -> " + pattern.pattern())
                    .forEach(violations::add);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot inspect " + path, exception);
        }
    }
}
