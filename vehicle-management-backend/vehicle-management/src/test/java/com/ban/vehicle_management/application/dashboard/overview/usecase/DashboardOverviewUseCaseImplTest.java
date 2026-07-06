package com.ban.vehicle_management.application.dashboard.overview.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.dashboard.overview.port.out.DashboardOverviewPortOut;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.CardStatusOverviewResponse;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.DashboardOverviewResponse;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.DeviceStatusItemResponse;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.RevenueTrendPointResponse;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.VehicleTypeRatioResponse;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardOverviewUseCaseImplTest {

    @Mock
    private DashboardOverviewPortOut dashboardOverviewPortOut;

    @Test
    void shouldBuildOverviewUsingCurrentAndPreviousPeriod() {
        DashboardOverviewUseCaseImpl useCase = new DashboardOverviewUseCaseImpl(dashboardOverviewPortOut);
        LocalDate fromDate = LocalDate.of(2026, 7, 1);
        LocalDate toDate = LocalDate.of(2026, 7, 7);

        List<RevenueTrendPointResponse> revenueTrend = List.of(
                new RevenueTrendPointResponse(fromDate, BigDecimal.valueOf(50000))
        );
        VehicleTypeRatioResponse vehicleTypeRatio = new VehicleTypeRatioResponse(0, List.of());
        CardStatusOverviewResponse cardStatus = new CardStatusOverviewResponse(5, 4, 1);
        List<DeviceStatusItemResponse> deviceStatus = List.of(
                new DeviceStatusItemResponse("CAMERA", "Camera", 2, 1, 0)
        );

        when(dashboardOverviewPortOut.sumPaidRevenue(any(), any()))
                .thenReturn(BigDecimal.valueOf(50000), BigDecimal.valueOf(100000));
        when(dashboardOverviewPortOut.countCheckIns(any(), any()))
                .thenReturn(10L, 5L);
        when(dashboardOverviewPortOut.countCheckOuts(any(), any()))
                .thenReturn(4L, 8L);
        when(dashboardOverviewPortOut.countParkingAt(any()))
                .thenReturn(20L, 10L);
        when(dashboardOverviewPortOut.sumActiveZoneCapacity())
                .thenReturn(100L);
        when(dashboardOverviewPortOut.countNewAccounts(any(), any()))
                .thenReturn(3L, 0L);
        when(dashboardOverviewPortOut.countNewCustomers(any(), any()))
                .thenReturn(0L, 0L);
        when(dashboardOverviewPortOut.countNewCustomerVehicles(any(), any()))
                .thenReturn(2L, 4L);
        when(dashboardOverviewPortOut.getRevenueTrend(any(), any(), any(), any()))
                .thenReturn(revenueTrend);
        when(dashboardOverviewPortOut.getVehicleTypeRatio(any(), any()))
                .thenReturn(vehicleTypeRatio);
        when(dashboardOverviewPortOut.getCardStatusOverview())
                .thenReturn(cardStatus);
        when(dashboardOverviewPortOut.getDeviceStatusOverview())
                .thenReturn(deviceStatus);

        DashboardOverviewResponse result = useCase.getOverview(fromDate, toDate);

        assertEquals(fromDate, result.fromDate());
        assertEquals(toDate, result.toDate());
        assertEquals(BigDecimal.valueOf(50000), result.totalRevenue().value());
        assertEquals(BigDecimal.valueOf(100000), result.totalRevenue().previousValue());
        assertEquals(BigDecimal.valueOf(-50).setScale(2), result.totalRevenue().changePercent());
        assertEquals("DOWN", result.totalRevenue().changeDirection());
        assertEquals(BigDecimal.valueOf(10), result.checkInCount().value());
        assertEquals(BigDecimal.valueOf(100).setScale(2), result.checkInCount().changePercent());
        assertEquals("UP", result.checkInCount().changeDirection());
        assertEquals(BigDecimal.valueOf(4), result.checkOutCount().value());
        assertEquals(BigDecimal.valueOf(-50).setScale(2), result.checkOutCount().changePercent());
        assertEquals(BigDecimal.valueOf(20), result.currentParkingCount().value());
        assertEquals(BigDecimal.valueOf(100).setScale(2), result.currentParkingCount().changePercent());
        assertEquals(BigDecimal.valueOf(20).setScale(2), result.occupancyRate().value());
        assertEquals(BigDecimal.valueOf(10).setScale(2), result.occupancyRate().previousValue());
        assertSame(revenueTrend, result.revenueTrend());
        assertSame(vehicleTypeRatio, result.vehicleTypeRatio());
        assertSame(cardStatus, result.cardStatus());
        assertSame(deviceStatus, result.deviceStatus());
        assertEquals(BigDecimal.valueOf(3), result.userGrowth().newAccountCount().value());
        assertEquals(BigDecimal.valueOf(100), result.userGrowth().newAccountCount().changePercent());
        assertEquals("NONE", result.userGrowth().newCustomerCount().changeDirection());
        assertEquals(BigDecimal.valueOf(-50).setScale(2), result.userGrowth().newCustomerVehicleCount().changePercent());

        ArgumentCaptor<Instant> fromInstantCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> toInstantCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(dashboardOverviewPortOut).getRevenueTrend(
                org.mockito.Mockito.eq(fromDate),
                org.mockito.Mockito.eq(toDate),
                fromInstantCaptor.capture(),
                toInstantCaptor.capture()
        );
        assertEquals(Instant.parse("2026-06-30T17:00:00Z"), fromInstantCaptor.getValue());
        assertEquals(Instant.parse("2026-07-07T17:00:00Z"), toInstantCaptor.getValue());
    }

    @Test
    void shouldDefaultFromDateToSixDaysBeforeToDate() {
        DashboardOverviewUseCaseImpl useCase = new DashboardOverviewUseCaseImpl(dashboardOverviewPortOut);
        LocalDate toDate = LocalDate.of(2026, 7, 6);

        stubEmptyOverview();

        DashboardOverviewResponse result = useCase.getOverview(null, toDate);

        assertEquals(LocalDate.of(2026, 6, 30), result.fromDate());
        assertEquals(toDate, result.toDate());
    }

    @Test
    void shouldRejectWhenFromDateAfterToDate() {
        DashboardOverviewUseCaseImpl useCase = new DashboardOverviewUseCaseImpl(dashboardOverviewPortOut);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> useCase.getOverview(LocalDate.of(2026, 7, 7), LocalDate.of(2026, 7, 1))
        );

        assertEquals("fromDate must not be after toDate", exception.getMessage());
    }

    @Test
    void shouldRejectWhenDateRangeExceedsOneYear() {
        DashboardOverviewUseCaseImpl useCase = new DashboardOverviewUseCaseImpl(dashboardOverviewPortOut);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> useCase.getOverview(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 2))
        );

        assertEquals("Date range must not exceed 1 year", exception.getMessage());
    }

    @Test
    void shouldReturnZeroOccupancyWhenCapacityIsZero() {
        DashboardOverviewUseCaseImpl useCase = new DashboardOverviewUseCaseImpl(dashboardOverviewPortOut);

        when(dashboardOverviewPortOut.sumPaidRevenue(any(), any()))
                .thenReturn(BigDecimal.ZERO, BigDecimal.ZERO);
        when(dashboardOverviewPortOut.countCheckIns(any(), any()))
                .thenReturn(0L, 0L);
        when(dashboardOverviewPortOut.countCheckOuts(any(), any()))
                .thenReturn(0L, 0L);
        when(dashboardOverviewPortOut.countParkingAt(any()))
                .thenReturn(5L, 3L);
        when(dashboardOverviewPortOut.sumActiveZoneCapacity())
                .thenReturn(0L);
        when(dashboardOverviewPortOut.countNewAccounts(any(), any()))
                .thenReturn(0L, 0L);
        when(dashboardOverviewPortOut.countNewCustomers(any(), any()))
                .thenReturn(0L, 0L);
        when(dashboardOverviewPortOut.countNewCustomerVehicles(any(), any()))
                .thenReturn(0L, 0L);
        when(dashboardOverviewPortOut.getRevenueTrend(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(dashboardOverviewPortOut.getVehicleTypeRatio(any(), any()))
                .thenReturn(new VehicleTypeRatioResponse(0, List.of()));
        when(dashboardOverviewPortOut.getCardStatusOverview())
                .thenReturn(new CardStatusOverviewResponse(0, 0, 0));
        when(dashboardOverviewPortOut.getDeviceStatusOverview())
                .thenReturn(List.of());

        DashboardOverviewResponse result = useCase.getOverview(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 1)
        );

        assertEquals(BigDecimal.ZERO, result.occupancyRate().value());
        assertEquals(BigDecimal.ZERO, result.occupancyRate().previousValue());
        assertEquals("NONE", result.occupancyRate().changeDirection());
    }

    private void stubEmptyOverview() {
        when(dashboardOverviewPortOut.sumPaidRevenue(any(), any()))
                .thenReturn(BigDecimal.ZERO, BigDecimal.ZERO);
        when(dashboardOverviewPortOut.countCheckIns(any(), any()))
                .thenReturn(0L, 0L);
        when(dashboardOverviewPortOut.countCheckOuts(any(), any()))
                .thenReturn(0L, 0L);
        when(dashboardOverviewPortOut.countParkingAt(any()))
                .thenReturn(0L, 0L);
        when(dashboardOverviewPortOut.sumActiveZoneCapacity())
                .thenReturn(0L);
        when(dashboardOverviewPortOut.countNewAccounts(any(), any()))
                .thenReturn(0L, 0L);
        when(dashboardOverviewPortOut.countNewCustomers(any(), any()))
                .thenReturn(0L, 0L);
        when(dashboardOverviewPortOut.countNewCustomerVehicles(any(), any()))
                .thenReturn(0L, 0L);
        when(dashboardOverviewPortOut.getRevenueTrend(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(dashboardOverviewPortOut.getVehicleTypeRatio(any(), any()))
                .thenReturn(new VehicleTypeRatioResponse(0, List.of()));
        when(dashboardOverviewPortOut.getCardStatusOverview())
                .thenReturn(new CardStatusOverviewResponse(0, 0, 0));
        when(dashboardOverviewPortOut.getDeviceStatusOverview())
                .thenReturn(List.of());
    }
}
