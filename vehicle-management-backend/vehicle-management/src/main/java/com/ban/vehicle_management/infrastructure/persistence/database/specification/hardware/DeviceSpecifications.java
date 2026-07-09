package com.ban.vehicle_management.infrastructure.persistence.database.specification.hardware;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.hardware.DeviceEntity;
import com.ban.vehicle_management.shared.enumeration.hardware.DeviceStatus;
import com.ban.vehicle_management.shared.enumeration.hardware.DeviceType;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class DeviceSpecifications {

    private DeviceSpecifications() {
    }

    public static Specification<DeviceEntity> withFilters(
            UUID parkingLotId,
            UUID laneId,
            DeviceType deviceType,
            DeviceStatus status,
            String keyword
    ) {
        return Specification
                .where(hasParkingLotId(parkingLotId))
                .and(hasLaneId(laneId))
                .and(hasDeviceType(deviceType))
                .and(hasStatus(status))
                .and(containsKeyword(keyword));
    }

    private static Specification<DeviceEntity> hasParkingLotId(UUID parkingLotId) {
        return (root, query, criteriaBuilder) ->
                parkingLotId == null
                        ? null
                        : criteriaBuilder.equal(root.get("parkingLotId"), parkingLotId);
    }

    private static Specification<DeviceEntity> hasLaneId(UUID laneId) {
        return (root, query, criteriaBuilder) ->
                laneId == null
                        ? null
                        : criteriaBuilder.equal(root.get("laneId"), laneId);
    }

    private static Specification<DeviceEntity> hasDeviceType(DeviceType deviceType) {
        return (root, query, criteriaBuilder) ->
                deviceType == null
                        ? null
                        : criteriaBuilder.equal(root.get("deviceType"), deviceType);
    }

    private static Specification<DeviceEntity> hasStatus(DeviceStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null
                        ? null
                        : criteriaBuilder.equal(root.get("status"), status);
    }

    private static Specification<DeviceEntity> containsKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }

            String pattern = "%"
                    + keyword.trim().toLowerCase(Locale.ROOT)
                    + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("deviceCode")),
                            pattern
                    ),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("name")),
                            pattern
                    ),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("ipAddress")),
                            pattern
                    )
            );
        };
    }
}