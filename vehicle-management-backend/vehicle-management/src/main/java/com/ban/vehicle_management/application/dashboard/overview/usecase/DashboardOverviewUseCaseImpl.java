package com.ban.vehicle_management.application.dashboard.overview.usecase;

import com.ban.vehicle_management.application.dashboard.overview.model.DashboardPeriod;
import com.ban.vehicle_management.application.dashboard.overview.port.in.DashboardOverviewPortIn;
import com.ban.vehicle_management.application.dashboard.overview.port.out.DashboardOverviewPortOut;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.*;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;

@Service
public class DashboardOverviewUseCaseImpl implements DashboardOverviewPortIn {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final long MAX_RANGE_DAYS = 366;

    private final DashboardOverviewPortOut dashboardOverviewPortOut;

    public DashboardOverviewUseCaseImpl(DashboardOverviewPortOut dashboardOverviewPortOut) {
        this.dashboardOverviewPortOut = dashboardOverviewPortOut;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardOverviewResponse getOverview(LocalDate fromDate, LocalDate toDate) {
        DashboardPeriod period = resolvePeriod(fromDate, toDate);

        BigDecimal currentRevenue = dashboardOverviewPortOut.sumPaidRevenue(
                period.fromInstant(),
                period.toInstantExclusive()
        );
        BigDecimal previousRevenue = dashboardOverviewPortOut.sumPaidRevenue(
                period.previousFromInstant(),
                period.previousToInstantExclusive()
        );

        long currentCheckIn = dashboardOverviewPortOut.countCheckIns(
                period.fromInstant(),
                period.toInstantExclusive()
        );
        long previousCheckIn = dashboardOverviewPortOut.countCheckIns(
                period.previousFromInstant(),
                period.previousToInstantExclusive()
        );

        long currentCheckOut = dashboardOverviewPortOut.countCheckOuts(
                period.fromInstant(),
                period.toInstantExclusive()
        );
        long previousCheckOut = dashboardOverviewPortOut.countCheckOuts(
                period.previousFromInstant(),
                period.previousToInstantExclusive()
        );

        long currentParking = dashboardOverviewPortOut.countParkingAt(period.toInstantExclusive());
        long previousParking = dashboardOverviewPortOut.countParkingAt(period.previousToInstantExclusive());

        long capacity = dashboardOverviewPortOut.sumActiveZoneCapacity();
        BigDecimal currentOccupancy = calculateOccupancyRate(currentParking, capacity);
        BigDecimal previousOccupancy = calculateOccupancyRate(previousParking, capacity);

        long currentAccounts = dashboardOverviewPortOut.countNewAccounts(
                period.fromInstant(),
                period.toInstantExclusive()
        );
        long previousAccounts = dashboardOverviewPortOut.countNewAccounts(
                period.previousFromInstant(),
                period.previousToInstantExclusive()
        );

        long currentCustomers = dashboardOverviewPortOut.countNewCustomers(
                period.fromInstant(),
                period.toInstantExclusive()
        );
        long previousCustomers = dashboardOverviewPortOut.countNewCustomers(
                period.previousFromInstant(),
                period.previousToInstantExclusive()
        );

        long currentVehicles = dashboardOverviewPortOut.countNewCustomerVehicles(
                period.fromInstant(),
                period.toInstantExclusive()
        );
        long previousVehicles = dashboardOverviewPortOut.countNewCustomerVehicles(
                period.previousFromInstant(),
                period.previousToInstantExclusive()
        );

        return new DashboardOverviewResponse(
                period.fromDate(),
                period.toDate(),
                toKpi(currentRevenue, previousRevenue),
                toKpi(BigDecimal.valueOf(currentCheckIn), BigDecimal.valueOf(previousCheckIn)),
                toKpi(BigDecimal.valueOf(currentCheckOut), BigDecimal.valueOf(previousCheckOut)),
                toKpi(BigDecimal.valueOf(currentParking), BigDecimal.valueOf(previousParking)),
                toKpi(currentOccupancy, previousOccupancy),
                dashboardOverviewPortOut.getRevenueTrend(
                        period.fromDate(),
                        period.toDate(),
                        period.fromInstant(),
                        period.toInstantExclusive()
                ),
                dashboardOverviewPortOut.getVehicleTypeRatio(
                        period.fromInstant(),
                        period.toInstantExclusive()
                ),
                dashboardOverviewPortOut.getCardStatusOverview(),
                new UserGrowthOverviewResponse(
                        toKpi(BigDecimal.valueOf(currentAccounts), BigDecimal.valueOf(previousAccounts)),
                        toKpi(BigDecimal.valueOf(currentCustomers), BigDecimal.valueOf(previousCustomers)),
                        toKpi(BigDecimal.valueOf(currentVehicles), BigDecimal.valueOf(previousVehicles))
                ),
                dashboardOverviewPortOut.getDeviceStatusOverview()
        );
    }

    private DashboardPeriod resolvePeriod(LocalDate fromDate, LocalDate toDate) {
        LocalDate resolvedToDate = toDate == null ? LocalDate.now(VIETNAM_ZONE) : toDate;
        LocalDate resolvedFromDate = fromDate == null ? resolvedToDate.minusDays(6) : fromDate;

        if (resolvedFromDate.isAfter(resolvedToDate)) {
            throw new BadRequestException("fromDate must not be after toDate");
        }

        long periodDays = ChronoUnit.DAYS.between(resolvedFromDate, resolvedToDate) + 1;
        if (periodDays > MAX_RANGE_DAYS) {
            throw new BadRequestException("Date range must not exceed 1 year");
        }

        LocalDate previousToDate = resolvedFromDate.minusDays(1);
        LocalDate previousFromDate = previousToDate.minusDays(periodDays - 1);

        return new DashboardPeriod(
                resolvedFromDate,
                resolvedToDate,
                toStartOfDay(resolvedFromDate),
                toStartOfNextDay(resolvedToDate),
                previousFromDate,
                previousToDate,
                toStartOfDay(previousFromDate),
                toStartOfNextDay(previousToDate)
        );
    }

    private Instant toStartOfDay(LocalDate date) {
        return date.atStartOfDay(VIETNAM_ZONE).toInstant();
    }

    private Instant toStartOfNextDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay(VIETNAM_ZONE).toInstant();
    }

    private BigDecimal calculateOccupancyRate(long parkingCount, long capacity) {
        if (capacity <= 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(parkingCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(capacity), 2, RoundingMode.HALF_UP);
    }

    private DashboardKpiResponse toKpi(BigDecimal current, BigDecimal previous) {
        BigDecimal changePercent = calculateChangePercent(current, previous);
        return new DashboardKpiResponse(
                current,
                previous,
                changePercent,
                resolveDirection(changePercent)
        );
    }

    private BigDecimal calculateChangePercent(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(100);
        }

        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, RoundingMode.HALF_UP);
    }

    private String resolveDirection(BigDecimal changePercent) {
        if (changePercent.compareTo(BigDecimal.ZERO) > 0) {
            return "UP";
        }
        if (changePercent.compareTo(BigDecimal.ZERO) < 0) {
            return "DOWN";
        }
        return "NONE";
    }
}