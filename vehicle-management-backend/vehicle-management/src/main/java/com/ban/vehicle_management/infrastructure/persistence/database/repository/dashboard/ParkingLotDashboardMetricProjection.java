package com.ban.vehicle_management.infrastructure.persistence.database.repository.dashboard;

import java.util.UUID;

public interface ParkingLotDashboardMetricProjection {
    UUID getParkingLotId();

    long getCurrentParkingCount();

    long getOfflineDeviceCount();
}
