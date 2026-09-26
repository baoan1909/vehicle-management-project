package com.ban.vehicle_management.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ApiTimeContractTest {

    private static final Pattern STRING_TIME_FIELD = Pattern.compile(
            "\\bString\\s+[A-Za-z0-9_]*(?:At|Time)\\b"
    );

    @Test
    void apiDtosMustNotExposeAbsoluteTimeAsString() throws IOException {
        Path dtoRoot = Path.of("src/main/java/com/ban/vehicle_management/entrypoint/dto");
        List<String> violations = new ArrayList<>();

        try (var paths = Files.walk(dtoRoot)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> findMatches(path, STRING_TIME_FIELD, violations));
        }

        assertTrue(violations.isEmpty(), () ->
                "Absolute API timestamps must use Instant, not String:\n"
                        + String.join("\n", violations));
    }

    @Test
    void applicationMappersMustNotPreformatInstants() throws IOException {
        Path applicationRoot = Path.of("src/main/java/com/ban/vehicle_management/application");
        List<String> violations = new ArrayList<>();
        Pattern formatter = Pattern.compile(
                "DateTimeUtils\\.formatInstant|qualifiedByName\\s*=\\s*\"format(?:Vietnam)?Instant\""
        );

        try (var paths = Files.walk(applicationRoot)) {
            paths.filter(path -> path.getFileName().toString().endsWith("Mapper.java"))
                    .forEach(path -> findMatches(path, formatter, violations));
        }

        assertTrue(violations.isEmpty(), () ->
                "Application mappers must return Instant and leave JSON formatting to Jackson:\n"
                        + String.join("\n", violations));
    }

    private void findMatches(Path path, Pattern pattern, List<String> violations) {
        try {
            List<String> lines = Files.readAllLines(path);
            for (int index = 0; index < lines.size(); index++) {
                if (pattern.matcher(lines.get(index)).find()) {
                    violations.add(path + ":" + (index + 1) + " " + lines.get(index).trim());
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot inspect " + path, exception);
        }
    }
}
