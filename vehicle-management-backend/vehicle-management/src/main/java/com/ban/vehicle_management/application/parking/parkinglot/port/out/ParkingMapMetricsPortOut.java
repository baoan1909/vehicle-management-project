package com.ban.vehicle_management.application.parking.parkinglot.port.out;

public interface ParkingMapMetricsPortOut {
    void recordNearbySearch(long durationNanos, int resultCount, boolean success);

    void recordGeolocationOutcome(String outcome);
}
