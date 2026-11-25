package com.ban.vehicle_management.application.dashboard.overview.usecase;

import com.ban.vehicle_management.application.dashboard.overview.model.DashboardParkingLotMetric;
import com.ban.vehicle_management.application.dashboard.overview.model.DashboardPeriod;
import com.ban.vehicle_management.application.dashboard.overview.port.in.DashboardOverviewPortIn;
import com.ban.vehicle_management.application.dashboard.overview.port.out.DashboardOverviewPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.iam.organization.model.result.ParkingLotAccessScope;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.domain.iam.organization.model.Organization;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.DashboardKpiResponse;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.DashboardNetworkSummaryResponse;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.DashboardOrganizationOptionResponse;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.DashboardOverviewResponse;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.DashboardParkingLotOptionResponse;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.DashboardParkingLotSummaryResponse;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.DashboardScopeResponse;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.UserGrowthOverviewResponse;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardOverviewUseCaseImpl implements DashboardOverviewPortIn {

    private static final String DASHBOARD_READ_ALL = "DASHBOARD_READ_ALL";
    private static final long MAX_RANGE_DAYS = 366;

    private final DashboardOverviewPortOut dashboardOverviewPortOut;
    private final CurrentAccountPortIn currentAccountPortIn;
    private final OrganizationAccessGuard organizationAccessGuard;
    private final OrganizationPortOut organizationPortOut;
    private final ParkingLotPortOut parkingLotPortOut;

    public DashboardOverviewUseCaseImpl(
            DashboardOverviewPortOut dashboardOverviewPortOut,
            CurrentAccountPortIn currentAccountPortIn,
            OrganizationAccessGuard organizationAccessGuard,
            OrganizationPortOut organizationPortOut,
            ParkingLotPortOut parkingLotPortOut
    ) {
        this.dashboardOverviewPortOut = dashboardOverviewPortOut;
        this.currentAccountPortIn = currentAccountPortIn;
        this.organizationAccessGuard = organizationAccessGuard;
        this.organizationPortOut = organizationPortOut;
        this.parkingLotPortOut = parkingLotPortOut;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardOverviewResponse getOverview(
            LocalDate fromDate,
            LocalDate toDate,
            UUID requestedOrganizationId,
            UUID requestedParkingLotId
    ) {
        currentAccountPortIn.requirePermission(DASHBOARD_READ_ALL);
        DashboardPeriod period = resolvePeriod(fromDate, toDate);
        DashboardScopeSelection selection = resolveScope(requestedOrganizationId, requestedParkingLotId);
        Set<UUID> parkingLotIds = selection.parkingLotIds();

        BigDecimal currentRevenue = dashboardOverviewPortOut.sumPaidRevenue(
                period.fromInstant(), period.toInstantExclusive(), parkingLotIds);
        BigDecimal previousRevenue = dashboardOverviewPortOut.sumPaidRevenue(
                period.previousFromInstant(), period.previousToInstantExclusive(), parkingLotIds);
        BigDecimal currentPlatformFee = dashboardOverviewPortOut.sumAppliedPlatformFee(
                period.fromInstant(), period.toInstantExclusive(), parkingLotIds);
        BigDecimal previousPlatformFee = dashboardOverviewPortOut.sumAppliedPlatformFee(
                period.previousFromInstant(), period.previousToInstantExclusive(), parkingLotIds);

        long currentCheckIn = dashboardOverviewPortOut.countCheckIns(
                period.fromInstant(), period.toInstantExclusive(), parkingLotIds);
        long previousCheckIn = dashboardOverviewPortOut.countCheckIns(
                period.previousFromInstant(), period.previousToInstantExclusive(), parkingLotIds);
        long currentCheckOut = dashboardOverviewPortOut.countCheckOuts(
                period.fromInstant(), period.toInstantExclusive(), parkingLotIds);
        long previousCheckOut = dashboardOverviewPortOut.countCheckOuts(
                period.previousFromInstant(), period.previousToInstantExclusive(), parkingLotIds);

        Instant snapshotAt = Instant.now();
        long currentParking = dashboardOverviewPortOut.countParkingAt(snapshotAt, parkingLotIds);
        long previousParking = dashboardOverviewPortOut.countParkingAt(
                period.previousToInstantExclusive(), parkingLotIds);
        long capacity = dashboardOverviewPortOut.sumActiveZoneCapacity(parkingLotIds);
        BigDecimal currentOccupancy = calculateOccupancyRate(currentParking, capacity);
        BigDecimal previousOccupancy = calculateOccupancyRate(previousParking, capacity);

        UserGrowthOverviewResponse userGrowth = selection.level().equals("PLATFORM")
                ? loadPlatformGrowth(period)
                : loadScopedRegistrationGrowth(period, parkingLotIds);
        List<DashboardParkingLotSummaryResponse> parkingLotSummaries = buildParkingLotSummaries(
                selection.selectedParkingLots(), parkingLotIds, snapshotAt);

        return new DashboardOverviewResponse(
                period.fromDate(),
                period.toDate(),
                toScopeResponse(selection),
                toNetworkSummary(selection, parkingLotSummaries),
                toKpi(currentRevenue, previousRevenue),
                toKpi(currentPlatformFee, previousPlatformFee),
                toKpi(BigDecimal.valueOf(currentCheckIn), BigDecimal.valueOf(previousCheckIn)),
                toKpi(BigDecimal.valueOf(currentCheckOut), BigDecimal.valueOf(previousCheckOut)),
                toKpi(BigDecimal.valueOf(currentParking), BigDecimal.valueOf(previousParking)),
                toKpi(currentOccupancy, previousOccupancy),
                dashboardOverviewPortOut.getRevenueTrend(
                        period.fromDate(), period.toDate(), period.fromInstant(),
                        period.toInstantExclusive(), parkingLotIds),
                dashboardOverviewPortOut.getVehicleTypeRatio(
                        period.fromInstant(), period.toInstantExclusive(), parkingLotIds),
                dashboardOverviewPortOut.getCardStatusOverview(parkingLotIds),
                userGrowth,
                dashboardOverviewPortOut.getDeviceStatusOverview(parkingLotIds),
                parkingLotSummaries
        );
    }

    private DashboardScopeSelection resolveScope(UUID requestedOrganizationId, UUID requestedParkingLotId) {
        ParkingLotAccessScope accessScope = organizationAccessGuard.resolveParkingLotAccessScope();
        List<ParkingLot> accessibleParkingLots = parkingLotPortOut.findAll(
                null,
                null,
                accessScope.unrestricted() || accessScope.organizationIds().isEmpty()
                        ? null : accessScope.organizationIds(),
                accessScope.unrestricted() || !accessScope.organizationIds().isEmpty()
                        ? null : accessScope.parkingLotIds()
        ).stream().sorted(Comparator.comparing(ParkingLot::getName, String.CASE_INSENSITIVE_ORDER)).toList();

        Set<UUID> accessibleOrganizationIds = accessScope.unrestricted()
                ? Set.of()
                : !accessScope.organizationIds().isEmpty()
                        ? accessScope.organizationIds()
                        : accessibleParkingLots.stream()
                                .map(ParkingLot::getOrganizationId)
                                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<Organization> accessibleOrganizations = (accessScope.unrestricted()
                ? organizationPortOut.findAll()
                : organizationPortOut.findAllByIds(accessibleOrganizationIds)).stream()
                .sorted(Comparator.comparing(Organization::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        if (requestedParkingLotId != null) {
            ParkingLot selectedParkingLot = accessibleParkingLots.stream()
                    .filter(parkingLot -> requestedParkingLotId.equals(parkingLot.getParkingLotId()))
                    .findFirst()
                    .orElseThrow(() -> new AccessDeniedException(
                            "Current account cannot access the requested parking lot dashboard"));
            if (requestedOrganizationId != null
                    && !requestedOrganizationId.equals(selectedParkingLot.getOrganizationId())) {
                throw new BadRequestException("parkingLotId does not belong to organizationId");
            }
            return selectionForParkingLot(
                    selectedParkingLot, accessibleOrganizations, accessibleParkingLots);
        }

        if (requestedOrganizationId != null) {
            Organization selectedOrganization = accessibleOrganizations.stream()
                    .filter(organization -> requestedOrganizationId.equals(organization.getOrganizationId()))
                    .findFirst()
                    .orElseThrow(() -> new AccessDeniedException(
                            "Current account cannot access the requested partner dashboard"));
            List<ParkingLot> partnerParkingLots = accessibleParkingLots.stream()
                    .filter(parkingLot -> requestedOrganizationId.equals(parkingLot.getOrganizationId()))
                    .toList();
            return new DashboardScopeSelection(
                    "PARTNER", selectedOrganization.getName(), requestedOrganizationId, null,
                    parkingLotIds(partnerParkingLots), partnerParkingLots,
                    accessibleOrganizations, accessibleParkingLots);
        }

        if (accessScope.unrestricted()) {
            return new DashboardScopeSelection(
                    "PLATFORM", "Toàn sàn", null, null,
                    parkingLotIds(accessibleParkingLots), accessibleParkingLots,
                    accessibleOrganizations, accessibleParkingLots);
        }

        if (!accessScope.organizationIds().isEmpty()) {
            Organization onlyOrganization = accessibleOrganizations.size() == 1
                    ? accessibleOrganizations.getFirst() : null;
            return new DashboardScopeSelection(
                    "PARTNER",
                    onlyOrganization == null ? "Các Partner được phân quyền" : onlyOrganization.getName(),
                    onlyOrganization == null ? null : onlyOrganization.getOrganizationId(),
                    null,
                    parkingLotIds(accessibleParkingLots), accessibleParkingLots,
                    accessibleOrganizations, accessibleParkingLots);
        }

        if (accessibleParkingLots.size() == 1) {
            return selectionForParkingLot(
                    accessibleParkingLots.getFirst(), accessibleOrganizations, accessibleParkingLots);
        }

        return new DashboardScopeSelection(
                "ASSIGNED_LOTS",
                accessibleParkingLots.isEmpty() ? "Chưa được phân công bãi" : "Các bãi được phân công",
                null,
                null,
                parkingLotIds(accessibleParkingLots), accessibleParkingLots,
                accessibleOrganizations, accessibleParkingLots);
    }

    private DashboardScopeSelection selectionForParkingLot(
            ParkingLot parkingLot,
            List<Organization> accessibleOrganizations,
            List<ParkingLot> accessibleParkingLots
    ) {
        return new DashboardScopeSelection(
                "PARKING_LOT", parkingLot.getName(), parkingLot.getOrganizationId(),
                parkingLot.getParkingLotId(), Set.of(parkingLot.getParkingLotId()),
                List.of(parkingLot), accessibleOrganizations, accessibleParkingLots);
    }

    private Set<UUID> parkingLotIds(List<ParkingLot> parkingLots) {
        return parkingLots.stream()
                .map(ParkingLot::getParkingLotId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private DashboardScopeResponse toScopeResponse(DashboardScopeSelection selection) {
        return new DashboardScopeResponse(
                selection.level(), selection.title(), selection.organizationId(), selection.parkingLotId(),
                selection.accessibleOrganizations().stream()
                        .map(organization -> new DashboardOrganizationOptionResponse(
                                organization.getOrganizationId(), organization.getName()))
                        .toList(),
                selection.accessibleParkingLots().stream()
                        .map(parkingLot -> new DashboardParkingLotOptionResponse(
                                parkingLot.getParkingLotId(), parkingLot.getOrganizationId(), parkingLot.getName()))
                        .toList()
        );
    }

    private DashboardNetworkSummaryResponse toNetworkSummary(
            DashboardScopeSelection selection,
            List<DashboardParkingLotSummaryResponse> parkingLotSummaries
    ) {
        long activeParkingLots = selection.selectedParkingLots().stream()
                .filter(parkingLot -> ParkingLotStatus.ACTIVE.equals(parkingLot.getStatus()))
                .count();
        long totalCapacity = selection.selectedParkingLots().stream()
                .map(ParkingLot::getTotalCapacity)
                .filter(java.util.Objects::nonNull)
                .mapToLong(Integer::longValue)
                .sum();
        long offlineDevices = parkingLotSummaries.stream()
                .mapToLong(DashboardParkingLotSummaryResponse::offlineDeviceCount)
                .sum();
        long organizationCount = selection.selectedParkingLots().stream()
                .map(ParkingLot::getOrganizationId)
                .distinct()
                .count();
        if (selection.level().equals("PLATFORM")) {
            organizationCount = selection.accessibleOrganizations().size();
        } else if (selection.level().equals("PARTNER") && selection.organizationId() != null) {
            organizationCount = 1;
        }
        return new DashboardNetworkSummaryResponse(
                organizationCount, selection.selectedParkingLots().size(), activeParkingLots,
                totalCapacity, offlineDevices);
    }

    private List<DashboardParkingLotSummaryResponse> buildParkingLotSummaries(
            List<ParkingLot> parkingLots,
            Set<UUID> parkingLotIds,
            Instant snapshotAt
    ) {
        Map<UUID, DashboardParkingLotMetric> metrics = dashboardOverviewPortOut
                .getParkingLotMetrics(parkingLotIds, snapshotAt).stream()
                .collect(Collectors.toMap(
                        DashboardParkingLotMetric::parkingLotId,
                        metric -> metric,
                        (left, right) -> left,
                        LinkedHashMap::new));
        return parkingLots.stream().map(parkingLot -> {
            DashboardParkingLotMetric metric = metrics.getOrDefault(
                    parkingLot.getParkingLotId(),
                    new DashboardParkingLotMetric(parkingLot.getParkingLotId(), 0, 0));
            long capacity = parkingLot.getTotalCapacity() == null ? 0 : parkingLot.getTotalCapacity();
            return new DashboardParkingLotSummaryResponse(
                    parkingLot.getParkingLotId(), parkingLot.getOrganizationId(), parkingLot.getName(),
                    parkingLot.getStatus().name(), capacity, metric.currentParkingCount(),
                    calculateOccupancyRate(metric.currentParkingCount(), capacity), metric.offlineDeviceCount());
        }).toList();
    }

    private UserGrowthOverviewResponse loadPlatformGrowth(DashboardPeriod period) {
        long currentAccounts = dashboardOverviewPortOut.countNewAccounts(
                period.fromInstant(), period.toInstantExclusive());
        long previousAccounts = dashboardOverviewPortOut.countNewAccounts(
                period.previousFromInstant(), period.previousToInstantExclusive());
        long currentCustomers = dashboardOverviewPortOut.countNewCustomers(
                period.fromInstant(), period.toInstantExclusive());
        long previousCustomers = dashboardOverviewPortOut.countNewCustomers(
                period.previousFromInstant(), period.previousToInstantExclusive());
        long currentVehicles = dashboardOverviewPortOut.countNewCustomerVehicles(
                period.fromInstant(), period.toInstantExclusive());
        long previousVehicles = dashboardOverviewPortOut.countNewCustomerVehicles(
                period.previousFromInstant(), period.previousToInstantExclusive());
        return new UserGrowthOverviewResponse(
                toKpi(BigDecimal.valueOf(currentAccounts), BigDecimal.valueOf(previousAccounts)),
                toKpi(BigDecimal.valueOf(currentCustomers), BigDecimal.valueOf(previousCustomers)),
                toKpi(BigDecimal.valueOf(currentVehicles), BigDecimal.valueOf(previousVehicles)));
    }

    private UserGrowthOverviewResponse loadScopedRegistrationGrowth(
            DashboardPeriod period,
            Set<UUID> parkingLotIds
    ) {
        long currentCustomers = dashboardOverviewPortOut.countNewRegisteredCustomers(
                period.fromInstant(), period.toInstantExclusive(), parkingLotIds);
        long previousCustomers = dashboardOverviewPortOut.countNewRegisteredCustomers(
                period.previousFromInstant(), period.previousToInstantExclusive(), parkingLotIds);
        DashboardKpiResponse empty = toKpi(BigDecimal.ZERO, BigDecimal.ZERO);
        return new UserGrowthOverviewResponse(
                empty,
                toKpi(BigDecimal.valueOf(currentCustomers), BigDecimal.valueOf(previousCustomers)),
                empty);
    }

    private DashboardPeriod resolvePeriod(LocalDate fromDate, LocalDate toDate) {
        LocalDate resolvedToDate = toDate == null ? LocalDate.now(DateTimeUtils.getAppZone()) : toDate;
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
                resolvedFromDate, resolvedToDate,
                DateTimeUtils.startOfDayInAppZone(resolvedFromDate),
                DateTimeUtils.startOfNextDayInAppZone(resolvedToDate),
                previousFromDate, previousToDate,
                DateTimeUtils.startOfDayInAppZone(previousFromDate),
                DateTimeUtils.startOfNextDayInAppZone(previousToDate));
    }

    private BigDecimal calculateOccupancyRate(long parkingCount, long capacity) {
        if (capacity <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(parkingCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(capacity), 2, RoundingMode.HALF_UP);
    }

    private DashboardKpiResponse toKpi(BigDecimal current, BigDecimal previous) {
        BigDecimal changePercent = calculateChangePercent(current, previous);
        return new DashboardKpiResponse(
                current, previous, changePercent, resolveDirection(changePercent));
    }

    private BigDecimal calculateChangePercent(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(100);
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, RoundingMode.HALF_UP);
    }

    private String resolveDirection(BigDecimal changePercent) {
        if (changePercent.compareTo(BigDecimal.ZERO) > 0) return "UP";
        if (changePercent.compareTo(BigDecimal.ZERO) < 0) return "DOWN";
        return "NONE";
    }

    private record DashboardScopeSelection(
            String level,
            String title,
            UUID organizationId,
            UUID parkingLotId,
            Set<UUID> parkingLotIds,
            List<ParkingLot> selectedParkingLots,
            List<Organization> accessibleOrganizations,
            List<ParkingLot> accessibleParkingLots
    ) {
    }
}
