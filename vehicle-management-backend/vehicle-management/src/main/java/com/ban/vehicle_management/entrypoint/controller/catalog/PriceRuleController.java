package com.ban.vehicle_management.entrypoint.controller.catalog;

import com.ban.vehicle_management.application.catalog.pricerule.mapper.PriceRuleApiMapper;
import com.ban.vehicle_management.application.catalog.pricerule.port.in.PriceRulePortIn;
import com.ban.vehicle_management.domain.catalog.pricerule.model.PriceRule;
import com.ban.vehicle_management.entrypoint.dto.catalog.pricerule.request.CreatePriceRuleRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.pricerule.request.PriceRuleFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.pricerule.request.UpdatePriceRuleRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.pricerule.response.PriceRuleAdminResponse;
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
@RequestMapping("/api/catalog/price-rules")
public class PriceRuleController {

    private final PriceRulePortIn priceRulePortIn;
    private final PriceRuleApiMapper priceRuleApiMapper;

    public PriceRuleController(
            PriceRulePortIn priceRulePortIn,
            PriceRuleApiMapper priceRuleApiMapper
    ) {
        this.priceRulePortIn = priceRulePortIn;
        this.priceRuleApiMapper = priceRuleApiMapper;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PriceRuleAdminResponse>> createPriceRule(
            @RequestBody CreatePriceRuleRequest request
    ) {
        PriceRule createdPriceRule = priceRulePortIn.createPriceRule(
                priceRuleApiMapper.toDomain(request)
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Price rule created successfully",
                priceRuleApiMapper.toAdminResponse(createdPriceRule)
        ));
    }

    @GetMapping("/{priceRuleId}")
    public ResponseEntity<ApiResponse<PriceRuleAdminResponse>> getPriceRuleById(
            @PathVariable UUID priceRuleId
    ) {
        PriceRule priceRule = priceRulePortIn.getPriceRuleById(priceRuleId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched price rule successfully",
                priceRuleApiMapper.toAdminResponse(priceRule)
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PriceRuleAdminResponse>>> getPriceRules(
            @ModelAttribute PriceRuleFilterRequest request
    ) {
        List<PriceRule> priceRules = priceRulePortIn.getPriceRules(
                request.pricePlanId(),
                request.vehicleTypeId(),
                request.ticketTypeId(),
                request.isActive(),
                request.keyword()
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched price rules successfully",
                priceRuleApiMapper.toAdminResponses(priceRules)
        ));
    }

    @PutMapping("/{priceRuleId}")
    public ResponseEntity<ApiResponse<PriceRuleAdminResponse>> updatePriceRule(
            @PathVariable UUID priceRuleId,
            @RequestBody UpdatePriceRuleRequest request
    ) {
        PriceRule updatedPriceRule = priceRulePortIn.updatePriceRule(
                priceRuleId,
                priceRuleApiMapper.toDomain(request)
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Price rule updated successfully",
                priceRuleApiMapper.toAdminResponse(updatedPriceRule)
        ));
    }

    @DeleteMapping("/{priceRuleId}")
    public ResponseEntity<ApiResponse<Void>> deletePriceRule(
            @PathVariable UUID priceRuleId
    ) {
        priceRulePortIn.deletePriceRule(priceRuleId);

        return ResponseEntity.ok(ApiResponse.ok("Price rule deactivated successfully"));
    }

    @PatchMapping("/{priceRuleId}/activate")
    public ResponseEntity<ApiResponse<PriceRuleAdminResponse>> activatePriceRule(
            @PathVariable UUID priceRuleId
    ) {
        PriceRule activatedPriceRule = priceRulePortIn.activatePriceRule(priceRuleId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Price rule activated successfully",
                priceRuleApiMapper.toAdminResponse(activatedPriceRule)
        ));
    }
}