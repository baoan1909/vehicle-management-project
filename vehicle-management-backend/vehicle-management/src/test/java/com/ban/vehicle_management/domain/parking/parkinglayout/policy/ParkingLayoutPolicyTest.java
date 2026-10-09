package com.ban.vehicle_management.domain.parking.parkinglayout.policy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.parking.parkinglayout.model.ParkingSpaceLayoutItem;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ParkingLayoutPolicyTest {

    private final ParkingLayoutPolicy policy = new ParkingLayoutPolicy();

    @Test
    void shouldDetectRotatedOverlap() {
        ParkingSpaceLayoutItem first = item(0, 0, 10, 4, 45);
        ParkingSpaceLayoutItem second = item(5, 0, 10, 4, -45);

        assertThrows(BadRequestException.class, () -> policy.validateNoOverlap(List.of(first, second)));
    }

    @Test
    void shouldPassWhenRotatedBoxesAreSeparated() {
        ParkingSpaceLayoutItem first = item(0, 0, 10, 4, 45);
        ParkingSpaceLayoutItem second = item(40, 40, 10, 4, -45);

        assertDoesNotThrow(() -> policy.validateNoOverlap(List.of(first, second)));
    }

    @Test
    void shouldRejectRotatedCornersOutsideCanvas() {
        ParkingSpaceLayoutItem item = item(95, 95, 10, 10, 45);

        assertThrows(BadRequestException.class,
                () -> policy.validateWithinCanvas(List.of(item), BigDecimal.valueOf(100), BigDecimal.valueOf(100)));
    }

    @Test
    void shouldAcceptItemsWithinCanvas() {
        ParkingSpaceLayoutItem item = item(10, 10, 10, 5, 0);

        assertDoesNotThrow(() -> policy.validateWithinCanvas(
                List.of(item), BigDecimal.valueOf(100), BigDecimal.valueOf(100)));
    }

    private ParkingSpaceLayoutItem item(double x, double y, double width, double height, double rotation) {
        ParkingSpaceLayoutItem item = new ParkingSpaceLayoutItem();
        item.setLayoutItemId(UUID.randomUUID());
        item.setLayoutVersionId(UUID.randomUUID());
        item.setParkingSpaceId(UUID.randomUUID());
        item.setX(BigDecimal.valueOf(x));
        item.setY(BigDecimal.valueOf(y));
        item.setWidth(BigDecimal.valueOf(width));
        item.setHeight(BigDecimal.valueOf(height));
        item.setRotation(BigDecimal.valueOf(rotation));
        return item;
    }
}
