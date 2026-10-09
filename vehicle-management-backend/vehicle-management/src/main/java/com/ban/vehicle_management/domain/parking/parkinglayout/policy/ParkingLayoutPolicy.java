package com.ban.vehicle_management.domain.parking.parkinglayout.policy;

import com.ban.vehicle_management.domain.parking.parkinglayout.model.ParkingLayoutVersion;
import com.ban.vehicle_management.domain.parking.parkinglayout.model.ParkingSpaceLayoutItem;
import com.ban.vehicle_management.domain.parking.parkinglayout.model.ParkingLayoutElement;
import com.ban.vehicle_management.shared.enumeration.parking.LayoutVersionStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class ParkingLayoutPolicy {

    public void initialize(ParkingLayoutVersion layoutVersion) {
        requireLayoutVersion(layoutVersion);
        if (layoutVersion.getVersion() == null) {
            throw new BadRequestException("version must not be null");
        }
        if (layoutVersion.getStatus() == null) {
            layoutVersion.setStatus(LayoutVersionStatus.DRAFT);
        }
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
        requireField(item.getLayoutVersionId(), "layoutVersionId");
        requireField(item.getParkingSpaceId(), "parkingSpaceId");
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
        requireField(element.getLayoutVersionId(), "layoutVersionId");
        requireField(element.getElementType(), "elementType");
        requireField(element.getGeometry(), "geometry");
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