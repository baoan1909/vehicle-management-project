package com.ban.vehicle_management.domain.parking.parkingspace.policy;

import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.shared.enumeration.parking.ManualOccupancyType;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatusSource;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.math.BigDecimal;

public class ParkingSpacePolicy {

    public void initialize(ParkingSpace parkingSpace) {
        requireParkingSpace(parkingSpace);
        parkingSpace.setCode(TextValidationUtils.normalizeCode(parkingSpace.getCode(), "code", 50));
        requireField(parkingSpace.getZoneId(), "zoneId");
        if (parkingSpace.getStatus() == null) {
            parkingSpace.setStatus(ParkingSpaceStatus.AVAILABLE);
        }
        if (parkingSpace.getLifecycleStatus() == null) {
            parkingSpace.setLifecycleStatus("ACTIVE");
        }
        if (parkingSpace.getStatusSource() == null) {
            parkingSpace.setStatusSource(ParkingSpaceStatusSource.MANUAL);
        }
        if (parkingSpace.getVersion() == null) {
            parkingSpace.setVersion(0L);
        }
        if (parkingSpace.getRotation() == null) {
            parkingSpace.setRotation(BigDecimal.ZERO);
        }
        validateState(parkingSpace);
    }

    public void occupy(ParkingSpace parkingSpace, ManualOccupancyType occupancyType, String reason) {
        requireStatus(parkingSpace, ParkingSpaceStatus.AVAILABLE, ParkingSpaceStatus.RESERVED);
        parkingSpace.setStatus(ParkingSpaceStatus.OCCUPIED);
        parkingSpace.setManualOccupancyType(occupancyType);
        parkingSpace.setStatusSource(ParkingSpaceStatusSource.MANUAL);
        parkingSpace.setStatusReason(reason);
        validateState(parkingSpace);
    }

    public void release(ParkingSpace parkingSpace) {
        requireStatus(parkingSpace, ParkingSpaceStatus.OCCUPIED, ParkingSpaceStatus.RESERVED);
        parkingSpace.setStatus(ParkingSpaceStatus.AVAILABLE);
        parkingSpace.setManualOccupancyType(null);
        parkingSpace.setStatusSource(ParkingSpaceStatusSource.MANUAL);
        validateState(parkingSpace);
    }

    public void reserve(ParkingSpace parkingSpace) {
        requireStatus(parkingSpace, ParkingSpaceStatus.AVAILABLE);
        parkingSpace.setStatus(ParkingSpaceStatus.RESERVED);
        parkingSpace.setManualOccupancyType(null);
        parkingSpace.setStatusSource(ParkingSpaceStatusSource.MANUAL);
        validateState(parkingSpace);
    }

    public void markMaintenance(ParkingSpace parkingSpace) {
        requireParkingSpace(parkingSpace);
        if (parkingSpace.getStatus() == ParkingSpaceStatus.OCCUPIED) {
            throw new BadRequestException("Occupied parkingSpace must not be moved to maintenance");
        }
        parkingSpace.setStatus(ParkingSpaceStatus.MAINTENANCE);
        parkingSpace.setManualOccupancyType(null);
        parkingSpace.setStatusSource(ParkingSpaceStatusSource.MANUAL);
        validateState(parkingSpace);
    }

    public void setManualStatus(ParkingSpace parkingSpace, ParkingSpaceStatus targetStatus, ManualOccupancyType occupancyType, String reason) {
        requireParkingSpace(parkingSpace);
        validateManualTransition(parkingSpace.getStatus(), targetStatus);
        parkingSpace.setStatus(targetStatus);
        parkingSpace.setManualOccupancyType(occupancyType);
        parkingSpace.setStatusSource(ParkingSpaceStatusSource.MANUAL);
        parkingSpace.setStatusReason(reason);
        validateState(parkingSpace);
    }

    public void setAutomaticStatus(ParkingSpace parkingSpace, ParkingSpaceStatus targetStatus, ManualOccupancyType occupancyType) {
        requireParkingSpace(parkingSpace);
        parkingSpace.setStatus(targetStatus);
        parkingSpace.setManualOccupancyType(occupancyType);
        parkingSpace.setStatusSource(ParkingSpaceStatusSource.AUTOMATIC);
        parkingSpace.setStatusReason(null);
        validateState(parkingSpace);
    }

    public void archive(ParkingSpace parkingSpace) {
        requireParkingSpace(parkingSpace);
        if (parkingSpace.getStatus() == ParkingSpaceStatus.OCCUPIED) {
            throw new BadRequestException("Cannot archive occupied parking space");
        }
        parkingSpace.setLifecycleStatus("ARCHIVED");
        validateState(parkingSpace);
    }

    public void validateState(ParkingSpace parkingSpace) {
        requireParkingSpace(parkingSpace);
        parkingSpace.setCode(TextValidationUtils.normalizeCode(parkingSpace.getCode(), "code", 50));
        requireField(parkingSpace.getZoneId(), "zoneId");
        requireField(parkingSpace.getStatus(), "status");
        requireField(parkingSpace.getLifecycleStatus(), "lifecycleStatus");
        requireField(parkingSpace.getStatusSource(), "statusSource");
        requireField(parkingSpace.getVersion(), "version");

        if (parkingSpace.getRotation() == null) {
            parkingSpace.setRotation(BigDecimal.ZERO);
        } else {
            parkingSpace.setRotation(normalizeRotation(parkingSpace.getRotation()));
        }

        boolean hasAnyGeometry = parkingSpace.getX() != null || parkingSpace.getY() != null
                || parkingSpace.getWidth() != null || parkingSpace.getHeight() != null;
        boolean hasCompleteGeometry = parkingSpace.getX() != null && parkingSpace.getY() != null
                && parkingSpace.getWidth() != null && parkingSpace.getHeight() != null;
        if (hasAnyGeometry && !hasCompleteGeometry) {
            throw new BadRequestException("x, y, width and height must be provided together");
        }
        if (parkingSpace.getX() != null && parkingSpace.getX().compareTo(BigDecimal.ZERO) < 0
                || parkingSpace.getY() != null && parkingSpace.getY().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("x and y must be non-negative");
        }
        if (parkingSpace.getWidth() != null && parkingSpace.getWidth().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("width must be positive");
        }
        if (parkingSpace.getHeight() != null && parkingSpace.getHeight().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("height must be positive");
        }

        if (parkingSpace.getStatusSource() == ParkingSpaceStatusSource.MANUAL) {
            if (parkingSpace.getStatus() == ParkingSpaceStatus.OCCUPIED) {
                requireField(parkingSpace.getManualOccupancyType(), "manualOccupancyType");
                requireField(parkingSpace.getStatusReason(), "statusReason");
            }
        } else {
            parkingSpace.setManualOccupancyType(null);
            parkingSpace.setStatusReason(null);
        }
    }

    private void validateManualTransition(ParkingSpaceStatus from, ParkingSpaceStatus to) {
        if (from == to) {
            throw new BadRequestException("ParkingSpace is already in status " + to);
        }
        boolean valid = switch (from) {
            case AVAILABLE -> to == ParkingSpaceStatus.OCCUPIED
                    || to == ParkingSpaceStatus.RESERVED
                    || to == ParkingSpaceStatus.MAINTENANCE;
            case RESERVED -> to == ParkingSpaceStatus.OCCUPIED
                    || to == ParkingSpaceStatus.AVAILABLE;
            case OCCUPIED -> to == ParkingSpaceStatus.AVAILABLE
                    || to == ParkingSpaceStatus.RESERVED;
            case MAINTENANCE -> to == ParkingSpaceStatus.AVAILABLE
                    || to == ParkingSpaceStatus.RESERVED;
        };
        if (!valid) {
            throw new BadRequestException("Invalid manual transition from " + from + " to " + to);
        }
    }

    private BigDecimal normalizeRotation(BigDecimal rotation) {
        BigDecimal normalized = rotation.remainder(BigDecimal.valueOf(360));
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            normalized = normalized.add(BigDecimal.valueOf(360));
        }
        return normalized.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private void requireParkingSpace(ParkingSpace parkingSpace) {
        requireField(parkingSpace, "parkingSpace");
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }

    private void requireStatus(ParkingSpace parkingSpace, ParkingSpaceStatus... expectedStatuses) {
        requireParkingSpace(parkingSpace);
        for (ParkingSpaceStatus expectedStatus : expectedStatuses) {
            if (parkingSpace.getStatus() == expectedStatus) {
                return;
            }
        }
        throw new BadRequestException("ParkingSpace is not in a valid status for this action");
    }
}