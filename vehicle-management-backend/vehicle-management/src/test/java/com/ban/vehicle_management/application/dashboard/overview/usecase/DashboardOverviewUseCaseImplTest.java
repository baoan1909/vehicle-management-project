package com.ban.vehicle_management.application.dashboard.overview.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.dashboard.overview.port.out.DashboardOverviewPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.iam.organization.model.result.ParkingLotAccessScope;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.domain.iam.organization.model.Organization;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.CardStatusOverviewResponse;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.DashboardOverviewResponse;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.VehicleTypeRatioResponse;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class DashboardOverviewUseCaseImplTest {

    private static final UUID ORGANIZATION_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID PARKING_LOT_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Mock
    private DashboardOverviewPortOut dashboardOverviewPortOut;
    @Mock
    private CurrentAccountPortIn currentAccountPortIn;
    @Mock
    private OrganizationAccessGuard organizationAccessGuard;
    @Mock
    private OrganizationPortOut organizationPortOut;
    @Mock
    private ParkingLotPortOut parkingLotPortOut;

    private DashboardOverviewUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        DateTimeUtils.configureAppZone(ZoneId.of("Asia/Ho_Chi_Minh"));
        useCase = new DashboardOverviewUseCaseImpl(
                dashboardOverviewPortOut,
                currentAccountPortIn,
                organizationAccessGuard,
                organizationPortOut,
                parkingLotPortOut);
    }

    @AfterEach
    void resetApplicationZone() {
        DateTimeUtils.configureAppZone(DateTimeUtils.UTC_ZONE);
    }

    @Test
    void shouldBuildPlatformOverviewUsingOnlyResolvedParkingLots() {
        ParkingLot parkingLot = parkingLot(PARKING_LOT_ID, ORGANIZATION_ID, "Bãi A");
        stubPlatformScope(List.of(parkingLot));
        stubOverviewMetrics();
        when(dashboardOverviewPortOut.sumPaidRevenue(any(), any(), anySet()))
                .thenReturn(BigDecimal.valueOf(50000), BigDecimal.valueOf(100000));
        when(dashboardOverviewPortOut.countCheckIns(any(), any(), anySet()))
                .thenReturn(10L, 5L);
        when(dashboardOverviewPortOut.countCheckOuts(any(), any(), anySet()))
                .thenReturn(4L, 8L);
        when(dashboardOverviewPortOut.countParkingAt(any(), anySet()))
                .thenReturn(20L, 10L);
        when(dashboardOverviewPortOut.sumActiveZoneCapacity(anySet())).thenReturn(100L);
        when(dashboardOverviewPortOut.countNewAccounts(any(), any())).thenReturn(3L, 0L);
        when(dashboardOverviewPortOut.countNewCustomers(any(), any())).thenReturn(0L, 0L);
        when(dashboardOverviewPortOut.countNewCustomerVehicles(any(), any())).thenReturn(2L, 4L);

        LocalDate fromDate = LocalDate.of(2026, 7, 1);
        LocalDate toDate = LocalDate.of(2026, 7, 7);
        DashboardOverviewResponse result = useCase.getOverview(fromDate, toDate, null, null);

        assertEquals("PLATFORM", result.scope().level());
        assertEquals("Toàn sàn", result.scope().title());
        assertEquals(1, result.networkSummary().organizationCount());
        assertEquals(1, result.networkSummary().parkingLotCount());
        assertEquals(BigDecimal.valueOf(50000), result.totalRevenue().value());
        assertEquals(BigDecimal.valueOf(-50).setScale(2), result.totalRevenue().changePercent());
        assertEquals(BigDecimal.valueOf(20).setScale(2), result.occupancyRate().value());
        assertEquals(BigDecimal.valueOf(3), result.userGrowth().newAccountCount().value());
        verify(currentAccountPortIn).requirePermission("DASHBOARD_READ_ALL");

        ArgumentCaptor<Set<UUID>> scopeCaptor = ArgumentCaptor.forClass(Set.class);
        verify(dashboardOverviewPortOut).sumActiveZoneCapacity(scopeCaptor.capture());
        assertEquals(Set.of(PARKING_LOT_ID), scopeCaptor.getValue());

        ArgumentCaptor<Instant> fromInstantCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> toInstantCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(dashboardOverviewPortOut).getRevenueTrend(
                org.mockito.Mockito.eq(fromDate),
                org.mockito.Mockito.eq(toDate),
                fromInstantCaptor.capture(),
                toInstantCaptor.capture(),
                org.mockito.Mockito.eq(Set.of(PARKING_LOT_ID)));
        assertEquals(Instant.parse("2026-06-30T17:00:00Z"), fromInstantCaptor.getValue());
        assertEquals(Instant.parse("2026-07-07T17:00:00Z"), toInstantCaptor.getValue());
    }

    @Test
    void shouldKeepPartnerDashboardInsideActiveOrganization() {
        ParkingLot parkingLot = parkingLot(PARKING_LOT_ID, ORGANIZATION_ID, "Bãi Partner");
        when(organizationAccessGuard.resolveParkingLotAccessScope())
                .thenReturn(new ParkingLotAccessScope(false, Set.of(ORGANIZATION_ID), Set.of()));
        when(parkingLotPortOut.findAll(isNull(), isNull(), org.mockito.Mockito.eq(Set.of(ORGANIZATION_ID)), isNull()))
                .thenReturn(List.of(parkingLot));
        when(organizationPortOut.findAllByIds(Set.of(ORGANIZATION_ID)))
                .thenReturn(List.of(organization(ORGANIZATION_ID, "Partner A")));
        stubOverviewMetrics();
        when(dashboardOverviewPortOut.countNewRegisteredCustomers(any(), any(), anySet()))
                .thenReturn(6L, 2L);

        DashboardOverviewResponse result = useCase.getOverview(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 7), null, null);

        assertEquals("PARTNER", result.scope().level());
        assertEquals("Partner A", result.scope().title());
        assertEquals(ORGANIZATION_ID, result.scope().organizationId());
        assertEquals(Set.of(PARKING_LOT_ID), result.parkingLots().stream()
                .map(item -> item.parkingLotId()).collect(java.util.stream.Collectors.toSet()));
        assertEquals(BigDecimal.valueOf(6), result.userGrowth().newCustomerCount().value());
        assertEquals(BigDecimal.valueOf(200).setScale(2),
                result.userGrowth().newCustomerCount().changePercent());
        verify(dashboardOverviewPortOut, never()).countNewAccounts(any(), any());
    }

    @Test
    void shouldDefaultManagerWithOneAssignmentToThatParkingLot() {
        ParkingLot parkingLot = parkingLot(PARKING_LOT_ID, ORGANIZATION_ID, "Bãi được giao");
        when(organizationAccessGuard.resolveParkingLotAccessScope())
                .thenReturn(new ParkingLotAccessScope(false, Set.of(), Set.of(PARKING_LOT_ID)));
        when(parkingLotPortOut.findAll(isNull(), isNull(), isNull(), org.mockito.Mockito.eq(Set.of(PARKING_LOT_ID))))
                .thenReturn(List.of(parkingLot));
        when(organizationPortOut.findAllByIds(Set.of(ORGANIZATION_ID)))
                .thenReturn(List.of(organization(ORGANIZATION_ID, "Partner A")));
        stubOverviewMetrics();

        DashboardOverviewResponse result = useCase.getOverview(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 7), null, null);

        assertEquals("PARKING_LOT", result.scope().level());
        assertEquals(PARKING_LOT_ID, result.scope().parkingLotId());
        assertEquals("Bãi được giao", result.scope().title());
    }

    @Test
    void shouldRejectParkingLotOutsideResolvedScope() {
        ParkingLot parkingLot = parkingLot(PARKING_LOT_ID, ORGANIZATION_ID, "Bãi được giao");
        when(organizationAccessGuard.resolveParkingLotAccessScope())
                .thenReturn(new ParkingLotAccessScope(false, Set.of(), Set.of(PARKING_LOT_ID)));
        when(parkingLotPortOut.findAll(isNull(), isNull(), isNull(), org.mockito.Mockito.eq(Set.of(PARKING_LOT_ID))))
                .thenReturn(List.of(parkingLot));
        when(organizationPortOut.findAllByIds(Set.of(ORGANIZATION_ID)))
                .thenReturn(List.of(organization(ORGANIZATION_ID, "Partner A")));

        assertThrows(AccessDeniedException.class, () -> useCase.getOverview(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 7), null, UUID.randomUUID()));
    }

    @Test
    void shouldRejectWhenFromDateAfterToDate() {
        assertThrows(BadRequestException.class, () -> useCase.getOverview(
                LocalDate.of(2026, 7, 7), LocalDate.of(2026, 7, 1), null, null));
    }

    @Test
    void shouldRejectWhenDateRangeExceedsOneYear() {
        assertThrows(BadRequestException.class, () -> useCase.getOverview(
                LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 2), null, null));
    }

    private void stubPlatformScope(List<ParkingLot> parkingLots) {
        when(organizationAccessGuard.resolveParkingLotAccessScope())
                .thenReturn(ParkingLotAccessScope.unrestrictedScope());
        when(parkingLotPortOut.findAll(isNull(), isNull(), isNull(), isNull())).thenReturn(parkingLots);
        when(organizationPortOut.findAll()).thenReturn(List.of(organization(ORGANIZATION_ID, "Partner A")));
    }

    private void stubOverviewMetrics() {
        when(dashboardOverviewPortOut.sumPaidRevenue(any(), any(), anySet()))
                .thenReturn(BigDecimal.ZERO, BigDecimal.ZERO);
        when(dashboardOverviewPortOut.sumAppliedPlatformFee(any(), any(), anySet()))
                .thenReturn(BigDecimal.ZERO, BigDecimal.ZERO);
        when(dashboardOverviewPortOut.countCheckIns(any(), any(), anySet())).thenReturn(0L, 0L);
        when(dashboardOverviewPortOut.countCheckOuts(any(), any(), anySet())).thenReturn(0L, 0L);
        when(dashboardOverviewPortOut.countParkingAt(any(), anySet())).thenReturn(0L, 0L);
        when(dashboardOverviewPortOut.sumActiveZoneCapacity(anySet())).thenReturn(0L);
        when(dashboardOverviewPortOut.getRevenueTrend(any(), any(), any(), any(), anySet()))
                .thenReturn(List.of());
        when(dashboardOverviewPortOut.getVehicleTypeRatio(any(), any(), anySet()))
                .thenReturn(new VehicleTypeRatioResponse(0, List.of()));
        when(dashboardOverviewPortOut.getCardStatusOverview(anySet()))
                .thenReturn(new CardStatusOverviewResponse(0, 0, 0));
        when(dashboardOverviewPortOut.getDeviceStatusOverview(anySet())).thenReturn(List.of());
        when(dashboardOverviewPortOut.getParkingLotMetrics(anySet(), any())).thenReturn(List.of());
    }

    private ParkingLot parkingLot(UUID parkingLotId, UUID organizationId, String name) {
        ParkingLot parkingLot = new ParkingLot();
        parkingLot.setParkingLotId(parkingLotId);
        parkingLot.setOrganizationId(organizationId);
        parkingLot.setName(name);
        parkingLot.setStatus(ParkingLotStatus.ACTIVE);
        parkingLot.setTotalCapacity(100);
        return parkingLot;
    }

    private Organization organization(UUID organizationId, String name) {
        Organization organization = new Organization();
        organization.setOrganizationId(organizationId);
        organization.setName(name);
        return organization;
    }
}
