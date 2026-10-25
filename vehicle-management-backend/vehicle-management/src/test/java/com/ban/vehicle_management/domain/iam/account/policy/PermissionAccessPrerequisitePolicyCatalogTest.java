package com.ban.vehicle_management.domain.iam.account.policy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.ban.vehicle_management.domain.iam.account.policy.PermissionAccessPrerequisitePolicy.PermissionPrerequisite;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class PermissionAccessPrerequisitePolicyCatalogTest {

    private static final Pattern SENSITIVE_PERMISSION = Pattern.compile(
            "'([A-Z][A-Z0-9_]+_(?:OWN|PARTNER)|PARKING_SCOPE_ASSIGNED)'"
    );
    private final PermissionAccessPrerequisitePolicy policy = new PermissionAccessPrerequisitePolicy();

    @Test
    void lifecycleAndOwnershipSensitiveSeedPermissionsMustBeExplicitlyClassified() throws IOException {
        Set<String> sensitivePermissions = new LinkedHashSet<>();
        try (Stream<Path> migrations = Files.walk(Path.of("src/main/resources/db/migration"))) {
            migrations.filter(path -> path.toString().endsWith(".sql"))
                    .forEach(path -> extractSensitivePermissions(path, sensitivePermissions));
        }

        assertFalse(sensitivePermissions.isEmpty(), "Permission seed/catalog could not be inspected");
        sensitivePermissions.forEach(permissionCode -> assertNotEquals(
                PermissionPrerequisite.COMMON_ACCOUNT_ROLE,
                policy.classificationFor(permissionCode),
                () -> "Lifecycle/scope-sensitive permission is not classified: " + permissionCode
        ));
    }

    private void extractSensitivePermissions(Path migration, Set<String> result) {
        try {
            Matcher matcher = SENSITIVE_PERMISSION.matcher(Files.readString(migration));
            while (matcher.find()) {
                result.add(matcher.group(1));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not inspect permission migration " + migration, exception);
        }
    }
}
