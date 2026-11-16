package com.ban.vehicle_management.entrypoint.controller.catalog;

import com.ban.vehicle_management.application.catalog.priceplan.mapper.PricePlanApiMapper;
import com.ban.vehicle_management.application.catalog.availability.port.in.PublicParkingLotCatalogPortIn;
import com.ban.vehicle_management.application.catalog.pricerule.mapper.PriceRuleApiMapper;
import com.ban.vehicle_management.application.catalog.tickettype.mapper.TicketTypeApiMapper;
import com.ban.vehicle_management.application.catalog.vehicletype.mapper.VehicleTypeApiMapper;
import com.ban.vehicle_management.domain.catalog.priceplan.model.PricePlan;
import com.ban.vehicle_management.domain.catalog.pricerule.model.PriceRule;
import com.ban.vehicle_management.domain.catalog.tickettype.model.TicketType;
import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import com.ban.vehicle_management.entrypoint.dto.catalog.priceplan.request.PricePlanFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.priceplan.response.PricePlanUserResponse;
import com.ban.vehicle_management.entrypoint.dto.catalog.pricerule.request.PriceRuleFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.pricerule.response.PriceRuleUserResponse;
import com.ban.vehicle_management.entrypoint.dto.catalog.tickettype.request.TicketTypeFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.tickettype.response.TicketTypeUserResponse;
import com.ban.vehicle_management.entrypoint.dto.catalog.vehicletype.request.VehicleTypeFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.vehicletype.response.VehicleTypeUserResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/pricing")
public class PublicPricingController {

    private final PricePlanApiMapper pricePlanApiMapper;
    private final PriceRuleApiMapper priceRuleApiMapper;
    private final PublicParkingLotCatalogPortIn publicParkingLotCatalogPortIn;
    private final VehicleTypeApiMapper vehicleTypeApiMapper;
    private final TicketTypeApiMapper ticketTypeApiMapper;

    public PublicPricingController(
            PricePlanApiMapper pricePlanApiMapper,
            PriceRuleApiMapper priceRuleApiMapper,
            PublicParkingLotCatalogPortIn publicParkingLotCatalogPortIn,
            VehicleTypeApiMapper vehicleTypeApiMapper,
            TicketTypeApiMapper ticketTypeApiMapper
    ) {
        this.pricePlanApiMapper = pricePlanApiMapper;
        this.priceRuleApiMapper = priceRuleApiMapper;
        this.publicParkingLotCatalogPortIn = publicParkingLotCatalogPortIn;
        this.vehicleTypeApiMapper = vehicleTypeApiMapper;
        this.ticketTypeApiMapper = ticketTypeApiMapper;
    }

    @GetMapping("/price-plans")
    public ResponseEntity<ApiResponse<List<PricePlanUserResponse>>> getPricePlans(
            @ModelAttribute PricePlanFilterRequest request,
            @RequestParam(required = false) UUID parkingLotId
    ) {
        List<PricePlan> pricePlans = publicParkingLotCatalogPortIn.getPricePlans(
                parkingLotId,
                request.appliesTo(),
                request.effectiveDate(),
                request.keyword()
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched public price plans successfully",
                pricePlanApiMapper.toUserResponses(pricePlans)
        ));
    }

    @GetMapping("/price-rules")
    public ResponseEntity<ApiResponse<List<PriceRuleUserResponse>>> getPriceRules(
            @ModelAttribute PriceRuleFilterRequest request,
            @RequestParam(required = false) UUID parkingLotId
    ) {
        List<PriceRule> priceRules = publicParkingLotCatalogPortIn.getPriceRules(
                parkingLotId,
                request.pricePlanId(),
                request.vehicleTypeId(),
                request.ticketTypeId(),
                request.keyword()
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched public price rules successfully",
                priceRuleApiMapper.toUserResponses(priceRules)
        ));
    }

    @GetMapping("/vehicle-types")
    public ResponseEntity<ApiResponse<List<VehicleTypeUserResponse>>> getVehicleTypes(
            @ModelAttribute VehicleTypeFilterRequest request,
            @RequestParam(required = false) UUID parkingLotId
    ) {
        List<VehicleType> vehicleTypes = publicParkingLotCatalogPortIn.getVehicleTypes(parkingLotId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched public vehicle types successfully",
                vehicleTypeApiMapper.toUserResponses(vehicleTypes)
        ));
    }

    @GetMapping("/ticket-types")
    public ResponseEntity<ApiResponse<List<TicketTypeUserResponse>>> getTicketTypes(
            @ModelAttribute TicketTypeFilterRequest request,
            @RequestParam(required = false) UUID parkingLotId
    ) {
        List<TicketType> ticketTypes = publicParkingLotCatalogPortIn.getTicketTypes(parkingLotId, request.keyword());

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched public ticket types successfully",
                ticketTypeApiMapper.toUserResponses(ticketTypes)
        ));
    }
}
