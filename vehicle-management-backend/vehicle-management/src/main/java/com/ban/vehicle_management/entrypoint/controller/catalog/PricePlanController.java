package com.ban.vehicle_management.entrypoint.controller.catalog;

import com.ban.vehicle_management.application.catalog.priceplan.mapper.PricePlanApiMapper;
import com.ban.vehicle_management.application.catalog.priceplan.port.in.PricePlanPortIn;
import com.ban.vehicle_management.domain.catalog.priceplan.model.PricePlan;
import com.ban.vehicle_management.entrypoint.dto.catalog.priceplan.request.CreatePricePlanRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.priceplan.request.PricePlanFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.priceplan.request.UpdatePricePlanRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.priceplan.response.PricePlanAdminResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog/price-plans")
public class PricePlanController {

    private final PricePlanPortIn pricePlanPortIn;
    private final PricePlanApiMapper pricePlanApiMapper;

    public PricePlanController(
            PricePlanPortIn pricePlanPortIn,
            PricePlanApiMapper pricePlanApiMapper
    ) {
        this.pricePlanPortIn = pricePlanPortIn;
        this.pricePlanApiMapper = pricePlanApiMapper;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PricePlanAdminResponse>> createPricePlan(
            @RequestBody CreatePricePlanRequest request
    ) {
        PricePlan createdPricePlan = pricePlanPortIn.createPricePlan(pricePlanApiMapper.toDomain(request));

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Price plan created successfully",
                pricePlanApiMapper.toAdminResponse(createdPricePlan)
        ));
    }

    @GetMapping("/{pricePlanId}")
    public ResponseEntity<ApiResponse<PricePlanAdminResponse>> getPricePlanById(
            @PathVariable UUID pricePlanId
    ) {
        PricePlan pricePlan = pricePlanPortIn.getPricePlanById(pricePlanId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched price plan successfully",
                pricePlanApiMapper.toAdminResponse(pricePlan)
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PricePlanAdminResponse>>> getPricePlans(
            @ModelAttribute PricePlanFilterRequest request
    ) {
        List<PricePlan> pricePlans = pricePlanPortIn.getPricePlans(
                request.isActive(),
                request.appliesTo(),
                request.effectiveDate(),
                request.keyword()
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched price plans successfully",
                pricePlanApiMapper.toAdminResponses(pricePlans)
        ));
    }

    @PutMapping("/{pricePlanId}")
    public ResponseEntity<ApiResponse<PricePlanAdminResponse>> updatePricePlan(
            @PathVariable UUID pricePlanId,
            @RequestBody UpdatePricePlanRequest request
    ) {
        PricePlan updatedPricePlan = pricePlanPortIn.updatePricePlan(
                pricePlanId,
                pricePlanApiMapper.toDomain(request)
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Price plan updated successfully",
                pricePlanApiMapper.toAdminResponse(updatedPricePlan)
        ));
    }

    @DeleteMapping("/{pricePlanId}")
    public ResponseEntity<ApiResponse<Void>> deletePricePlan(
            @PathVariable UUID pricePlanId
    ) {
        pricePlanPortIn.deletePricePlan(pricePlanId);

        return ResponseEntity.ok(ApiResponse.ok("Price plan deactivated successfully"));
    }

    @PatchMapping("/{pricePlanId}/activate")
    public ResponseEntity<ApiResponse<PricePlanAdminResponse>> activatePricePlan(
            @PathVariable UUID pricePlanId
    ) {
        PricePlan activatedPricePlan = pricePlanPortIn.activatePricePlan(pricePlanId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Price plan activated successfully",
                pricePlanApiMapper.toAdminResponse(activatedPricePlan)
        ));
    }
}