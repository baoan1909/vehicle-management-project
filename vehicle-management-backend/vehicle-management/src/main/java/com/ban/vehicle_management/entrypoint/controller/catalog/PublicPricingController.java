package com.ban.vehicle_management.entrypoint.controller.catalog;

import com.ban.vehicle_management.application.catalog.priceplan.mapper.PricePlanApiMapper;
import com.ban.vehicle_management.application.catalog.priceplan.port.in.PricePlanPortIn;
import com.ban.vehicle_management.application.catalog.pricerule.mapper.PriceRuleApiMapper;
import com.ban.vehicle_management.application.catalog.pricerule.port.in.PriceRulePortIn;
import com.ban.vehicle_management.application.catalog.tickettype.mapper.TicketTypeApiMapper;
import com.ban.vehicle_management.application.catalog.tickettype.port.out.TicketTypePortOut;
import com.ban.vehicle_management.application.catalog.vehicletype.mapper.VehicleTypeApiMapper;
import com.ban.vehicle_management.application.catalog.vehicletype.port.out.VehicleTypePortOut;
import com.ban.vehicle_management.domain.catalog.priceplan.model.PricePlan;
import com.ban.vehicle_management.domain.catalog.pricerule.model.PriceRule;
import com.ban.vehicle_management.domain.catalog.tickettype.model.TicketType;
import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import com.ban.vehicle_management.entrypoint.dto.catalog.priceplan.request.PricePlanFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.priceplan.response.PricePlanAdminResponse;
import com.ban.vehicle_management.entrypoint.dto.catalog.pricerule.request.PriceRuleFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.pricerule.response.PriceRuleAdminResponse;
import com.ban.vehicle_management.entrypoint.dto.catalog.tickettype.request.TicketTypeFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.tickettype.response.TicketTypeAdminResponse;
import com.ban.vehicle_management.entrypoint.dto.catalog.vehicletype.request.VehicleTypeFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.vehicletype.response.VehicleTypeAdminResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/pricing")
public class PublicPricingController {

    private final PricePlanPortIn pricePlanPortIn;
    private final PricePlanApiMapper pricePlanApiMapper;
    private final PriceRulePortIn priceRulePortIn;
    private final PriceRuleApiMapper priceRuleApiMapper;
    private final VehicleTypePortOut vehicleTypePortOut;
    private final VehicleTypeApiMapper vehicleTypeApiMapper;
    private final TicketTypePortOut ticketTypePortOut;
    private final TicketTypeApiMapper ticketTypeApiMapper;

    public PublicPricingController(
            PricePlanPortIn pricePlanPortIn,
            PricePlanApiMapper pricePlanApiMapper,
            PriceRulePortIn priceRulePortIn,
            PriceRuleApiMapper priceRuleApiMapper,
            VehicleTypePortOut vehicleTypePortOut,
            VehicleTypeApiMapper vehicleTypeApiMapper,
            TicketTypePortOut ticketTypePortOut,
            TicketTypeApiMapper ticketTypeApiMapper
    ) {
        this.pricePlanPortIn = pricePlanPortIn;
        this.pricePlanApiMapper = pricePlanApiMapper;
        this.priceRulePortIn = priceRulePortIn;
        this.priceRuleApiMapper = priceRuleApiMapper;
        this.vehicleTypePortOut = vehicleTypePortOut;
        this.vehicleTypeApiMapper = vehicleTypeApiMapper;
        this.ticketTypePortOut = ticketTypePortOut;
        this.ticketTypeApiMapper = ticketTypeApiMapper;
    }

    @GetMapping("/price-plans")
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
                "Fetched public price plans successfully",
                pricePlanApiMapper.toAdminResponses(pricePlans)
        ));
    }

    @GetMapping("/price-rules")
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
                "Fetched public price rules successfully",
                priceRuleApiMapper.toAdminResponses(priceRules)
        ));
    }

    @GetMapping("/vehicle-types")
    public ResponseEntity<ApiResponse<List<VehicleTypeAdminResponse>>> getVehicleTypes(
            @ModelAttribute VehicleTypeFilterRequest request
    ) {
        List<VehicleType> vehicleTypes = vehicleTypePortOut.findAll(request.isActive());

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched public vehicle types successfully",
                vehicleTypeApiMapper.toAdminResponses(vehicleTypes)
        ));
    }

    @GetMapping("/ticket-types")
    public ResponseEntity<ApiResponse<List<TicketTypeAdminResponse>>> getTicketTypes(
            @ModelAttribute TicketTypeFilterRequest request
    ) {
        List<TicketType> ticketTypes = ticketTypePortOut.findAll(request.status(), normalizeKeyword(request.keyword()));

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched public ticket types successfully",
                ticketTypeApiMapper.toAdminResponses(ticketTypes)
        ));
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }
}
