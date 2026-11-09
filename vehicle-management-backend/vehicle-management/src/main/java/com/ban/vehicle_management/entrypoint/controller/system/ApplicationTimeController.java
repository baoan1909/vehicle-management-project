package com.ban.vehicle_management.entrypoint.controller.system;

import com.ban.vehicle_management.application.system.time.port.in.ApplicationTimeQueryPortIn;
import com.ban.vehicle_management.application.system.time.mapper.ApplicationTimeApiMapper;
import com.ban.vehicle_management.entrypoint.dto.system.time.response.ApplicationTimeResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/application-time")
public class ApplicationTimeController {

    private final ApplicationTimeQueryPortIn applicationTimeQueryPortIn;
    private final ApplicationTimeApiMapper apiMapper;

    public ApplicationTimeController(
            ApplicationTimeQueryPortIn applicationTimeQueryPortIn,
            ApplicationTimeApiMapper apiMapper) {
        this.applicationTimeQueryPortIn = applicationTimeQueryPortIn;
        this.apiMapper = apiMapper;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ApplicationTimeResponse>> getApplicationTime() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Application timezone retrieved successfully",
                apiMapper.toResponse(applicationTimeQueryPortIn.getTimeZone())
        ));
    }
}
