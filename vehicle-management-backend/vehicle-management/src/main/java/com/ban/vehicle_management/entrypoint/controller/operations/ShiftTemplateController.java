package com.ban.vehicle_management.entrypoint.controller.operations;

import com.ban.vehicle_management.application.operations.shifttemplate.mapper.ShiftTemplateApiMapper;
import com.ban.vehicle_management.application.operations.shifttemplate.port.in.ShiftTemplatePortIn;
import com.ban.vehicle_management.domain.operations.shifttemplate.model.ShiftTemplate;
import com.ban.vehicle_management.entrypoint.dto.operations.shifttemplate.request.CreateShiftTemplateRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.shifttemplate.request.ShiftTemplateFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.shifttemplate.request.UpdateShiftTemplateRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.shifttemplate.response.ShiftTemplateAdminResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operations/shift-templates")
public class ShiftTemplateController {

    private final ShiftTemplatePortIn shiftTemplatePortIn;
    private final ShiftTemplateApiMapper shiftTemplateApiMapper;

    public ShiftTemplateController(
            ShiftTemplatePortIn shiftTemplatePortIn,
            ShiftTemplateApiMapper shiftTemplateApiMapper
    ) {
        this.shiftTemplatePortIn = shiftTemplatePortIn;
        this.shiftTemplateApiMapper = shiftTemplateApiMapper;
    }

    @PostMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('SHIFT_CREATE_ALL')")
    public ResponseEntity<ApiResponse<ShiftTemplateAdminResponse>> create(
            @RequestBody CreateShiftTemplateRequest request
    ) {
        ShiftTemplate created = shiftTemplatePortIn.createShiftTemplate(
                shiftTemplateApiMapper.toDomain(request)
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.ok(
                        "Shift template created successfully",
                        shiftTemplateApiMapper.toAdminResponse(created)
                )
        );
    }

    @GetMapping("/{shiftTemplateId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('SHIFT_READ_ALL')")
    public ResponseEntity<ApiResponse<ShiftTemplateAdminResponse>> getById(
            @PathVariable UUID shiftTemplateId
    ) {
        ShiftTemplate result =
                shiftTemplatePortIn.getShiftTemplateById(shiftTemplateId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched shift template successfully",
                shiftTemplateApiMapper.toAdminResponse(result)
        ));
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('SHIFT_READ_ALL')")
    public ResponseEntity<ApiResponse<List<ShiftTemplateAdminResponse>>> getAll(
            @ModelAttribute ShiftTemplateFilterRequest request
    ) {
        List<ShiftTemplate> results =
                shiftTemplatePortIn.getShiftTemplates(
                        request.parkingLotId(),
                        request.shiftType(),
                        request.status(),
                        request.keyword()
                );

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched shift templates successfully",
                shiftTemplateApiMapper.toAdminResponses(results)
        ));
    }

    @PutMapping("/{shiftTemplateId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('SHIFT_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<ShiftTemplateAdminResponse>> update(
            @PathVariable UUID shiftTemplateId,
            @RequestBody UpdateShiftTemplateRequest request
    ) {
        ShiftTemplate updated = shiftTemplatePortIn.updateShiftTemplate(
                shiftTemplateId,
                shiftTemplateApiMapper.toDomain(request)
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Shift template updated successfully",
                shiftTemplateApiMapper.toAdminResponse(updated)
        ));
    }

    @PatchMapping("/{shiftTemplateId}/activate")
    @PreAuthorize("@permissionAuthorizer.hasPermission('SHIFT_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<ShiftTemplateAdminResponse>> activate(
            @PathVariable UUID shiftTemplateId
    ) {
        ShiftTemplate activated =
                shiftTemplatePortIn.activateShiftTemplate(shiftTemplateId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Shift template activated successfully",
                shiftTemplateApiMapper.toAdminResponse(activated)
        ));
    }

    @DeleteMapping("/{shiftTemplateId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('SHIFT_DELETE_ALL')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID shiftTemplateId
    ) {
        shiftTemplatePortIn.deleteShiftTemplate(shiftTemplateId);

        return ResponseEntity.ok(
                ApiResponse.ok("Shift template deactivated successfully")
        );
    }
}