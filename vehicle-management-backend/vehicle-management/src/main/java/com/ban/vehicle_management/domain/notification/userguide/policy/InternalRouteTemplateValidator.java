package com.ban.vehicle_management.domain.notification.userguide.policy;

import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InternalRouteTemplateValidator {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Z0-9_]+)}");
    private static final Set<String> ALLOWED_PLACEHOLDERS = Set.of("PARKING_LOT_ID", "ORGANIZATION_ID");

    private InternalRouteTemplateValidator() {
    }

    public static String normalize(String value, String fieldName) {
        String normalized = TextValidationUtils.normalizeNullableText(value, fieldName, 1000);
        if (normalized == null) return null;
        String routeForUri = normalized;
        Matcher matcher = PLACEHOLDER.matcher(normalized);
        while (matcher.find()) {
            if (!ALLOWED_PLACEHOLDERS.contains(matcher.group(1))) {
                throw new BadRequestException(fieldName + " contains an unsupported placeholder: " + matcher.group());
            }
            routeForUri = routeForUri.replace(matcher.group(), "00000000-0000-0000-0000-000000000000");
        }
        if (routeForUri.contains("{") || routeForUri.contains("}")) {
            throw new BadRequestException(fieldName + " contains an invalid placeholder");
        }
        try {
            URI uri = URI.create(routeForUri);
            String rawPath = uri.getRawPath();
            String decodedPath = rawPath == null ? "" : URLDecoder.decode(rawPath, StandardCharsets.UTF_8);
            boolean traversal = Arrays.stream(decodedPath.split("/", -1))
                    .anyMatch(segment -> segment.equals(".") || segment.equals(".."));
            if (!routeForUri.startsWith("/") || routeForUri.startsWith("//") || routeForUri.contains("\\")
                    || decodedPath.startsWith("//") || traversal
                    || uri.isAbsolute() || uri.getHost() != null) {
                throw new BadRequestException(fieldName + " must be a safe internal route");
            }
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(fieldName + " must be a valid internal route");
        }
        return normalized;
    }
}
