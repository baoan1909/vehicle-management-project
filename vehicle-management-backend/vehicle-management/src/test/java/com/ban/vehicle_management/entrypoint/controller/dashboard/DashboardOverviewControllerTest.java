package com.ban.vehicle_management.entrypoint.controller.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.dashboard.overview.port.in.DashboardOverviewPortIn;
import com.ban.vehicle_management.entrypoint.dto.dashboard.request.DashboardOverviewFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.CardStatusOverviewResponse;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.DashboardKpiResponse;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.DashboardOverviewResponse;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.UserGrowthOverviewResponse;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.VehicleTypeRatioResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class DashboardOverviewControllerTest {

    @Mock
    private DashboardOverviewPortIn dashboardOverviewPortIn;

    @Test
    void shouldReturnDashboardOverviewResponse() {
        DashboardOverviewController controller = new DashboardOverviewController(dashboardOverviewPortIn);
        LocalDate fromDate = LocalDate.of(2026, 7, 1);
        LocalDate toDate = LocalDate.of(2026, 7, 7);
        DashboardOverviewResponse overview = overview(fromDate, toDate);

        when(dashboardOverviewPortIn.getOverview(fromDate, toDate))
                .thenReturn(overview);

        ResponseEntity<ApiResponse<DashboardOverviewResponse>> response =
                controller.getOverview(new DashboardOverviewFilterRequest(fromDate, toDate));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Fetched dashboard overview successfully", response.getBody().getMessage());
        assertSame(overview, response.getBody().getData());
        verify(dashboardOverviewPortIn).getOverview(fromDate, toDate);
    }

    @Test
    void shouldForwardNullFilterValuesToUseCase() {
        DashboardOverviewController controller = new DashboardOverviewController(dashboardOverviewPortIn);
        DashboardOverviewResponse overview = overview(LocalDate.of(2026, 6, 30), LocalDate.of(2026, 7, 6));

        when(dashboardOverviewPortIn.getOverview(null, null))
                .thenReturn(overview);

        ResponseEntity<ApiResponse<DashboardOverviewResponse>> response =
                controller.getOverview(new DashboardOverviewFilterRequest(null, null));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(overview, response.getBody().getData());
        verify(dashboardOverviewPortIn).getOverview(null, null);
    }

    private DashboardOverviewResponse overview(LocalDate fromDate, LocalDate toDate) {
        DashboardKpiResponse zeroKpi = new DashboardKpiResponse(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "NONE"
        );

        return new DashboardOverviewResponse(
                fromDate,
                toDate,
                zeroKpi,
                zeroKpi,
                zeroKpi,
                zeroKpi,
                zeroKpi,
                List.of(),
                new VehicleTypeRatioResponse(0, List.of()),
                new CardStatusOverviewResponse(0, 0, 0),
                new UserGrowthOverviewResponse(zeroKpi, zeroKpi, zeroKpi),
                List.of()
        );
    }
}
