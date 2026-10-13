package com.ban.vehicle_management.domain.parking.parkinglayout.policy;

import com.ban.vehicle_management.domain.parking.parkinglayout.model.ParkingLayoutVersion;
import com.ban.vehicle_management.domain.parking.parkinglayout.model.ParkingSpaceLayoutItem;
import com.ban.vehicle_management.domain.parking.parkinglayout.model.ParkingLayoutElement;
import com.ban.vehicle_management.shared.enumeration.parking.LayoutVersionStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class ParkingLayoutPolicy {

    public static final Set<String> ALLOWED_ELEMENT_TYPES = Set.of(
            "WALL", "COLUMN", "DRIVE_LANE", "ENTRY", "EXIT", "TEXT", "LABEL",
            "BACKGROUND", "IMAGE_OVERLAY");
    private static final int MAX_GEOMETRY_JSON_CHARS = 8192;
    private static final int MAX_STYLE_JSON_CHARS = 4096;
    private static final int MAX_LABEL_CHARS = 200;

    public void initialize(ParkingLayoutVersion layoutVersion) {
        requireLayoutVersion(layoutVersion);
        if (layoutVersion.getVersion() == null) {
            throw new BadRequestException("version must not be null");
        }
        if (layoutVersion.getStatus() == null) {
            layoutVersion.setStatus(LayoutVersionStatus.DRAFT);
        }
        // Revision maps to the JPA @Version column and must stay null until the
        // first persist so Hibernate can seed it. Defaulting it to 0L here would
        // make Spring Data treat the insert as a merge and fail with an
        // optimistic-locking conflict on brand-new drafts.
        validateState(layoutVersion);
    }

    public void publish(ParkingLayoutVersion layoutVersion, UUID publishedBy) {
        requireLayoutVersion(layoutVersion);
        if (layoutVersion.getStatus() != LayoutVersionStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT layout can be published");
        }
        layoutVersion.setStatus(LayoutVersionStatus.PUBLISHED);
        layoutVersion.setPublishedAt(java.time.Instant.now());
        layoutVersion.setPublishedBy(publishedBy);
        validateState(layoutVersion);
    }

    public void archive(ParkingLayoutVersion layoutVersion) {
        requireLayoutVersion(layoutVersion);
        layoutVersion.setStatus(LayoutVersionStatus.ARCHIVED);
        validateState(layoutVersion);
    }

    public void validateState(ParkingLayoutVersion layoutVersion) {
        requireLayoutVersion(layoutVersion);
        requireField(layoutVersion.getZoneId(), "zoneId");
        requireField(layoutVersion.getVersion(), "version");
        requireField(layoutVersion.getStatus(), "status");
    }

    public void validateLayoutItem(ParkingSpaceLayoutItem item) {
        requireLayoutItem(item);
        item.setCode(TextValidationUtils.normalizeCode(item.getCode(), "code", 50));
        requireField(item.getX(), "x");
        requireField(item.getY(), "y");
        requireField(item.getWidth(), "width");
        requireField(item.getHeight(), "height");
        if (item.getRotation() == null) {
            item.setRotation(BigDecimal.ZERO);
        } else {
            item.setRotation(normalizeRotation(item.getRotation()));
        }

        if (item.getWidth().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("width must be positive");
        }
        if (item.getHeight().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("height must be positive");
        }
        if (Double.isNaN(item.getX().doubleValue()) || Double.isInfinite(item.getX().doubleValue())
                || Double.isNaN(item.getY().doubleValue()) || Double.isInfinite(item.getY().doubleValue())
                || Double.isNaN(item.getWidth().doubleValue()) || Double.isInfinite(item.getWidth().doubleValue())
                || Double.isNaN(item.getHeight().doubleValue()) || Double.isInfinite(item.getHeight().doubleValue())
                || Double.isNaN(item.getRotation().doubleValue()) || Double.isInfinite(item.getRotation().doubleValue())) {
            throw new BadRequestException("Geometry values must be finite numbers");
        }
    }

    /**
     * Geometry source of truth: {@code parking_space_layout_items} is authoritative
     * per layout version (draft/published). {@code parking_spaces} keeps an operational
     * copy synced from the published layout so check-in/out reads stay cheap.
     */
    public void validateWithinCanvas(List<ParkingSpaceLayoutItem> items,
            BigDecimal canvasWidth, BigDecimal canvasHeight) {
        requireField(canvasWidth, "canvasWidth");
        requireField(canvasHeight, "canvasHeight");
        double width = canvasWidth.doubleValue();
        double height = canvasHeight.doubleValue();
        for (ParkingSpaceLayoutItem item : items) {
            validateLayoutItem(item);
            for (double[] corner : rotatedCorners(item)) {
                if (corner[0] < 0 || corner[1] < 0 || corner[0] > width || corner[1] > height) {
                    throw new BadRequestException(
                            "Space " + item.getParkingSpaceId() + " is outside the level canvas");
                }
            }
        }
    }

    public void validateUniqueCodes(List<ParkingSpaceLayoutItem> items) {
        Set<String> seen = new HashSet<>();
        for (ParkingSpaceLayoutItem item : items) {
            requireLayoutItem(item);
            String code = item.getCode() == null
                    ? null
                    : item.getCode().trim().toUpperCase(Locale.ROOT);
            if (code == null || code.isEmpty()) {
                throw new BadRequestException("code must not be null");
            }
            item.setCode(code);
            if (!seen.add(code)) {
                throw new BadRequestException("Duplicate space code in layout: " + code);
            }
        }
    }

    public void validateNoOverlap(List<ParkingSpaceLayoutItem> items) {
        for (int i = 0; i < items.size(); i++) {
            for (int j = i + 1; j < items.size(); j++) {
                if (overlaps(items.get(i), items.get(j))) {
                    throw new BadRequestException("Space " + items.get(i).getParkingSpaceId() + " overlaps with space " + items.get(j).getParkingSpaceId());
                }
            }
        }
    }

    public void validateLayoutElement(ParkingLayoutElement element) {
        requireLayoutElement(element);
        String elementType = element.getElementType() == null
                ? null
                : element.getElementType().trim().toUpperCase(Locale.ROOT);
        if (elementType == null || !ALLOWED_ELEMENT_TYPES.contains(elementType)) {
            throw new BadRequestException("Unsupported layout element type: " + element.getElementType());
        }
        element.setElementType(elementType);
        requireField(element.getGeometry(), "geometry");
        String geometryText = element.getGeometry().toString();
        if (geometryText.length() > MAX_GEOMETRY_JSON_CHARS) {
            throw new BadRequestException("Layout element geometry is too large");
        }
        assertFiniteJsonNumbers(element.getGeometry(), "geometry");
        if (element.getStyle() != null) {
            String styleText = element.getStyle().toString();
            if (styleText.length() > MAX_STYLE_JSON_CHARS) {
                throw new BadRequestException("Layout element style is too large");
            }
            assertSafeStyle(styleText);
        }
        if (element.getLabel() != null) {
            element.setLabel(TextValidationUtils.normalizeRequiredText(
                    element.getLabel(), "label", MAX_LABEL_CHARS));
        }
        if (element.getDisplayOrder() == null) {
            element.setDisplayOrder(0);
        } else if (element.getDisplayOrder() < 0) {
            throw new BadRequestException("displayOrder must not be negative");
        }
    }

    private void assertFiniteJsonNumbers(com.fasterxml.jackson.databind.JsonNode node, String fieldName) {
        if (node.isNumber() && !Double.isFinite(node.doubleValue())) {
            throw new BadRequestException(fieldName + " must contain only finite numbers");
        }
        if (node.isTextual()) {
            String text = node.asText().trim();
            if ("NaN".equalsIgnoreCase(text) || "Infinity".equalsIgnoreCase(text)
                    || "+Infinity".equalsIgnoreCase(text) || "-Infinity".equalsIgnoreCase(text)) {
                throw new BadRequestException(fieldName + " must contain only finite numbers");
            }
        }
        node.forEach(child -> assertFiniteJsonNumbers(child, fieldName));
    }

    private void assertSafeStyle(String styleText) {
        String lowered = styleText.toLowerCase(Locale.ROOT);
        for (String forbidden : new String[]{"<script", "javascript:", "expression(", "onload=",
                "onerror=", "onclick=", "<svg", "<img", "<iframe"}) {
            if (lowered.contains(forbidden)) {
                throw new BadRequestException("Layout element style contains unsafe content");
            }
        }
    }

    /**
     * Oriented-bounding-box overlap via the separating axis theorem, so rotated
     * spaces in the drag/rotate editor are checked precisely instead of AABB.
     */
    private boolean overlaps(ParkingSpaceLayoutItem a, ParkingSpaceLayoutItem b) {
        double[][] cornersA = rotatedCorners(a);
        double[][] cornersB = rotatedCorners(b);
        double[][] axes = {
                edgeNormal(cornersA[0], cornersA[1]),
                edgeNormal(cornersA[1], cornersA[2]),
                edgeNormal(cornersB[0], cornersB[1]),
                edgeNormal(cornersB[1], cornersB[2])
        };
        for (double[] axis : axes) {
            double[] projectionA = project(cornersA, axis);
            double[] projectionB = project(cornersB, axis);
            if (projectionA[1] <= projectionB[0] || projectionB[1] <= projectionA[0]) {
                return false;
            }
        }
        return true;
    }

    private double[][] rotatedCorners(ParkingSpaceLayoutItem item) {
        double x = item.getX().doubleValue();
        double y = item.getY().doubleValue();
        double width = item.getWidth().doubleValue();
        double height = item.getHeight().doubleValue();
        double radians = Math.toRadians(item.getRotation().doubleValue());
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double centerX = x + width / 2.0;
        double centerY = y + height / 2.0;
        double[][] corners = new double[4][2];
        double[] localX = {-width / 2.0, width / 2.0, width / 2.0, -width / 2.0};
        double[] localY = {-height / 2.0, -height / 2.0, height / 2.0, height / 2.0};
        for (int i = 0; i < 4; i++) {
            corners[i][0] = centerX + localX[i] * cos - localY[i] * sin;
            corners[i][1] = centerY + localX[i] * sin + localY[i] * cos;
        }
        return corners;
    }

    private double[] edgeNormal(double[] from, double[] to) {
        double dx = to[0] - from[0];
        double dy = to[1] - from[1];
        double length = Math.hypot(dx, dy);
        if (length == 0) {
            return new double[]{1, 0};
        }
        return new double[]{-dy / length, dx / length};
    }

    private double[] project(double[][] corners, double[] axis) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (double[] corner : corners) {
            double dot = corner[0] * axis[0] + corner[1] * axis[1];
            min = Math.min(min, dot);
            max = Math.max(max, dot);
        }
        return new double[]{min, max};
    }

    private BigDecimal normalizeRotation(BigDecimal rotation) {
        BigDecimal normalized = rotation.remainder(BigDecimal.valueOf(360));
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            normalized = normalized.add(BigDecimal.valueOf(360));
        }
        return normalized.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private void requireLayoutVersion(ParkingLayoutVersion layoutVersion) {
        requireField(layoutVersion, "layoutVersion");
    }

    private void requireLayoutItem(ParkingSpaceLayoutItem item) {
        requireField(item, "layoutItem");
    }

    private void requireLayoutElement(ParkingLayoutElement element) {
        requireField(element, "layoutElement");
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }
}