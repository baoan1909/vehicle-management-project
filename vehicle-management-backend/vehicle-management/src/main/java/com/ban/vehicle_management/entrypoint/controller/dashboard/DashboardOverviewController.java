package com.ban.vehicle_management.entrypoint.controller.dashboard;

import com.ban.vehicle_management.application.dashboard.overview.port.in.DashboardOverviewPortIn;
import com.ban.vehicle_management.entrypoint.dto.dashboard.request.DashboardOverviewFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.DashboardOverviewResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardOverviewController {

    private final DashboardOverviewPortIn dashboardOverviewPortIn;

    public DashboardOverviewController(DashboardOverviewPortIn dashboardOverviewPortIn) {
        this.dashboardOverviewPortIn = dashboardOverviewPortIn;
    }

    @GetMapping("/overview")
    @PreAuthorize("@permissionAuthorizer.hasPermission('DASHBOARD_READ_ALL')")
    public ResponseEntity<ApiResponse<DashboardOverviewResponse>> getOverview(
            @ModelAttribute DashboardOverviewFilterRequest request
    ) {
        DashboardOverviewResponse response = dashboardOverviewPortIn.getOverview(
                request.fromDate(),
                request.toDate()
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched dashboard overview successfully",
                response
        ));
    }
}