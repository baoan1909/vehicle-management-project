package com.ban.vehicle_management.application.dashboard.overview.port.out;

import com.ban.vehicle_management.application.dashboard.overview.model.DashboardParkingLotMetric;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface DashboardOverviewPortOut {
    BigDecimal sumPaidRevenue(Instant fromInstant, Instant toInstantExclusive, Set<UUID> parkingLotIds);

    BigDecimal sumAppliedPlatformFee(Instant fromInstant, Instant toInstantExclusive, Set<UUID> parkingLotIds);

    long countCheckIns(Instant fromInstant, Instant toInstantExclusive, Set<UUID> parkingLotIds);

    long countCheckOuts(Instant fromInstant, Instant toInstantExclusive, Set<UUID> parkingLotIds);

    long countParkingAt(Instant toInstantExclusive, Set<UUID> parkingLotIds);

    long sumActiveZoneCapacity(Set<UUID> parkingLotIds);

    List<RevenueTrendPointResponse> getRevenueTrend(
            LocalDate fromDate,
            LocalDate toDate,
            Instant fromInstant,
            Instant toInstantExclusive,
            Set<UUID> parkingLotIds
    );

    VehicleTypeRatioResponse getVehicleTypeRatio(
            Instant fromInstant,
            Instant toInstantExclusive,
            Set<UUID> parkingLotIds
    );

    CardStatusOverviewResponse getCardStatusOverview(Set<UUID> parkingLotIds);

    long countNewAccounts(Instant fromInstant, Instant toInstantExclusive);

    long countNewCustomers(Instant fromInstant, Instant toInstantExclusive);

    long countNewCustomerVehicles(Instant fromInstant, Instant toInstantExclusive);

    List<DeviceStatusItemResponse> getDeviceStatusOverview(Set<UUID> parkingLotIds);

    List<DashboardParkingLotMetric> getParkingLotMetrics(Set<UUID> parkingLotIds, Instant at);
}
