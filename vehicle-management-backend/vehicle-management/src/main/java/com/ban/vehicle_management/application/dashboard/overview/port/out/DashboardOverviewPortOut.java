package com.ban.vehicle_management.application.dashboard.overview.port.out;

import com.ban.vehicle_management.entrypoint.dto.dashboard.response.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface DashboardOverviewPortOut {
    BigDecimal sumPaidRevenue(Instant fromInstant, Instant toInstantExclusive);

    long countCheckIns(Instant fromInstant, Instant toInstantExclusive);

    long countCheckOuts(Instant fromInstant, Instant toInstantExclusive);

    long countParkingAt(Instant toInstantExclusive);

    long sumActiveZoneCapacity();

    List<RevenueTrendPointResponse> getRevenueTrend(
            LocalDate fromDate,
            LocalDate toDate,
            Instant fromInstant,
            Instant toInstantExclusive
    );

    VehicleTypeRatioResponse getVehicleTypeRatio(Instant fromInstant, Instant toInstantExclusive);

    CardStatusOverviewResponse getCardStatusOverview();

    long countNewAccounts(Instant fromInstant, Instant toInstantExclusive);

    long countNewCustomers(Instant fromInstant, Instant toInstantExclusive);

    long countNewCustomerVehicles(Instant fromInstant, Instant toInstantExclusive);

    List<DeviceStatusItemResponse> getDeviceStatusOverview();
}