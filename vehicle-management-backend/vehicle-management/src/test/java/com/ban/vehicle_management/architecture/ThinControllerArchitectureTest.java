package com.ban.vehicle_management.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

class ThinControllerArchitectureTest {

    private static final Path CONTROLLER_ROOT = Path.of(
            "src/main/java/com/ban/vehicle_management/entrypoint/controller");
    private static final Pattern FORBIDDEN_SOURCE = Pattern.compile(
            "import\\s+.*\\.port\\.out\\.|"
                    + "import\\s+.*\\.infrastructure\\.|"
                    + "\\b(?:JdbcTemplate|EntityManager|ObjectMapper)\\b|"
                    + "\\bMap\\s*<|"
                    + "\\bnew\\s+\\w*(?:Request|Response|Command|Dto)\\s*\\(|"
                    + "\\.(?:stream|map)\\s*\\(|"
                    + "\\b(?:Instant|LocalDateTime|OffsetDateTime)\\.now\\s*\\(|"
                    + "\\bUUID\\.randomUUID\\s*\\(|"
                    + "\\btry\\s*\\{|\\bcatch\\s*\\(");

    @Test
    void controllersMustNotContainForbiddenDependenciesOrMappingLogic() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path source : controllerSources()) {
            String content = Files.readString(source);
            var matcher = FORBIDDEN_SOURCE.matcher(content);
            while (matcher.find()) {
                violations.add(source + " -> " + matcher.group());
            }
        }
        assertTrue(violations.isEmpty(), () -> String.join(System.lineSeparator(), violations));
    }

    @Test
    void everyDeclaredControllerMethodMustBeAnHttpEndpoint() throws Exception {
        List<String> violations = new ArrayList<>();
        for (Path source : controllerSources()) {
            Class<?> controller = Class.forName(toClassName(source));
            for (Method method : controller.getDeclaredMethods()) {
                if (method.isSynthetic() || Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (!Modifier.isPublic(method.getModifiers()) || !hasHttpMapping(method)) {
                    violations.add(controller.getName() + "#" + method.getName());
                }
            }
        }
        assertTrue(violations.isEmpty(), () -> "Non-endpoint controller methods: " + violations);
    }

    private boolean hasHttpMapping(Method method) {
        Set<Class<?>> mappings = Set.of(
                GetMapping.class,
                PostMapping.class,
                PutMapping.class,
                PatchMapping.class,
                DeleteMapping.class);
        return Stream.of(method.getAnnotations())
                .anyMatch(annotation -> mappings.contains(annotation.annotationType()));
    }

    private List<Path> controllerSources() throws IOException {
        try (Stream<Path> files = Files.walk(CONTROLLER_ROOT)) {
            return files.filter(path -> path.getFileName().toString().endsWith("Controller.java"))
                    .sorted()
                    .toList();
        }
    }

    private String toClassName(Path source) {
        String relative = Path.of("src/main/java").relativize(source).toString();
        return relative.substring(0, relative.length() - ".java".length())
                .replace('/', '.')
                .replace('\\', '.');
    }
}
