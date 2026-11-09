package com.ban.vehicle_management.infrastructure.persistence.database.repository.dashboard;

public interface DeviceStatusCountProjection {
    String getDeviceType();

    String getStatus();

    long getTotal();
}
