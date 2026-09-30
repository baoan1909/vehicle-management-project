package com.ban.vehicle_management.entrypoint.controller.reference;

import com.ban.vehicle_management.application.reference.administrativedivision.mapper.AdministrativeDivisionApiMapper;
import com.ban.vehicle_management.application.reference.administrativedivision.port.in.AdministrativeDivisionPortIn;
import com.ban.vehicle_management.entrypoint.dto.reference.response.AdministrativeDivisionResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/public/administrative-divisions")
public class AdministrativeDivisionController {

    private final AdministrativeDivisionPortIn administrativeDivisionPortIn;
    private final AdministrativeDivisionApiMapper mapper;

    public AdministrativeDivisionController(
            AdministrativeDivisionPortIn administrativeDivisionPortIn,
            AdministrativeDivisionApiMapper mapper
    ) {
        this.administrativeDivisionPortIn = administrativeDivisionPortIn;
        this.mapper = mapper;
    }

    @GetMapping("/current/provinces")
    public ResponseEntity<ApiResponse<List<AdministrativeDivisionResponse>>> getCurrentProvinces() {
        return ok(mapper.toResponses(administrativeDivisionPortIn.getCurrentProvinces()));
    }

    @GetMapping("/current/provinces/{code}/wards")
    public ResponseEntity<ApiResponse<List<AdministrativeDivisionResponse>>> getCurrentWards(
            @PathVariable @Pattern(regexp = "\\d{2}") String code
    ) {
        return ok(mapper.toResponses(administrativeDivisionPortIn.getCurrentWards(code)));
    }

    @GetMapping("/legacy/provinces")
    public ResponseEntity<ApiResponse<List<AdministrativeDivisionResponse>>> getLegacyProvinces() {
        return ok(mapper.toResponses(administrativeDivisionPortIn.getLegacyProvinces()));
    }

    @GetMapping("/legacy/provinces/{code}/districts")
    public ResponseEntity<ApiResponse<List<AdministrativeDivisionResponse>>> getLegacyDistricts(
            @PathVariable @Pattern(regexp = "\\d{2}") String code
    ) {
        return ok(mapper.toResponses(administrativeDivisionPortIn.getLegacyDistricts(code)));
    }

    @GetMapping("/legacy/districts/{code}/wards")
    public ResponseEntity<ApiResponse<List<AdministrativeDivisionResponse>>> getLegacyWards(
            @PathVariable @Pattern(regexp = "\\d{3}") String code
    ) {
        return ok(mapper.toResponses(administrativeDivisionPortIn.getLegacyWards(code)));
    }

    private ResponseEntity<ApiResponse<List<AdministrativeDivisionResponse>>> ok(
            List<AdministrativeDivisionResponse> data
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Fetched administrative divisions successfully", data));
    }
}
