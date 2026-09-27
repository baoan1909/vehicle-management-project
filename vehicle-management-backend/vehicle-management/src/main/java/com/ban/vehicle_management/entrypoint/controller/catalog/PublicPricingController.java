package com.ban.vehicle_management.entrypoint.controller.catalog;

import com.ban.vehicle_management.application.catalog.priceplan.mapper.PricePlanApiMapper;
import com.ban.vehicle_management.application.catalog.availability.port.in.PublicParkingLotCatalogPortIn;
import com.ban.vehicle_management.application.catalog.priceplan.port.out.PricePlanPortOut;
import com.ban.vehicle_management.application.catalog.pricerule.mapper.PriceRuleApiMapper;
import com.ban.vehicle_management.application.catalog.pricerule.port.out.PriceRulePortOut;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.catalog.tickettype.mapper.TicketTypeApiMapper;
import com.ban.vehicle_management.application.catalog.vehicletype.mapper.VehicleTypeApiMapper;
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
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import java.util.List;
import java.util.Set;
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

    private final PricePlanPortOut pricePlanPortOut;
    private final PricePlanApiMapper pricePlanApiMapper;
    private final PriceRulePortOut priceRulePortOut;
    private final OrganizationPortOut organizationPortOut;
    private final ParkingLotPortOut parkingLotPortOut;
    private final PriceRuleApiMapper priceRuleApiMapper;
    private final PublicParkingLotCatalogPortIn publicParkingLotCatalogPortIn;
    private final VehicleTypeApiMapper vehicleTypeApiMapper;
    private final TicketTypeApiMapper ticketTypeApiMapper;

    public PublicPricingController(
            PricePlanPortOut pricePlanPortOut,
            PricePlanApiMapper pricePlanApiMapper,
            PriceRulePortOut priceRulePortOut,
            PriceRuleApiMapper priceRuleApiMapper,
            PublicParkingLotCatalogPortIn publicParkingLotCatalogPortIn,
            VehicleTypeApiMapper vehicleTypeApiMapper,
            TicketTypeApiMapper ticketTypeApiMapper,
            OrganizationPortOut organizationPortOut,
            ParkingLotPortOut parkingLotPortOut
    ) {
        this.pricePlanPortOut = pricePlanPortOut;
        this.pricePlanApiMapper = pricePlanApiMapper;
        this.priceRulePortOut = priceRulePortOut;
        this.organizationPortOut = organizationPortOut;
        this.parkingLotPortOut = parkingLotPortOut;
        this.priceRuleApiMapper = priceRuleApiMapper;
        this.publicParkingLotCatalogPortIn = publicParkingLotCatalogPortIn;
        this.vehicleTypeApiMapper = vehicleTypeApiMapper;
        this.ticketTypeApiMapper = ticketTypeApiMapper;
    }

    @GetMapping("/price-plans")
    public ResponseEntity<ApiResponse<List<PricePlanAdminResponse>>> getPricePlans(
            @ModelAttribute PricePlanFilterRequest request,
            @RequestParam(required = false) UUID parkingLotId
    ) {
        List<PricePlan> pricePlans = pricePlanPortOut.findAll(
                true,
                request.appliesTo(),
                request.effectiveDate(),
                request.keyword(),
                Set.of(resolvePublicOrganizationId(parkingLotId))
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched public price plans successfully",
                pricePlanApiMapper.toAdminResponses(pricePlans)
        ));
    }

    @GetMapping("/price-rules")
    public ResponseEntity<ApiResponse<List<PriceRuleAdminResponse>>> getPriceRules(
            @ModelAttribute PriceRuleFilterRequest request,
            @RequestParam(required = false) UUID parkingLotId
    ) {
        List<PriceRule> priceRules = priceRulePortOut.findAll(
                request.pricePlanId(),
                request.vehicleTypeId(),
                request.ticketTypeId(),
                true,
                request.keyword(),
                Set.of(resolvePublicOrganizationId(parkingLotId))
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched public price rules successfully",
                priceRuleApiMapper.toAdminResponses(publicParkingLotCatalogPortIn.filterAvailablePriceRules(parkingLotId, priceRules))
        ));
    }

    @GetMapping("/vehicle-types")
    public ResponseEntity<ApiResponse<List<VehicleTypeAdminResponse>>> getVehicleTypes(
            @ModelAttribute VehicleTypeFilterRequest request,
            @RequestParam(required = false) UUID parkingLotId
    ) {
        List<VehicleType> vehicleTypes = publicParkingLotCatalogPortIn.getVehicleTypes(parkingLotId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched public vehicle types successfully",
                vehicleTypeApiMapper.toAdminResponses(vehicleTypes)
        ));
    }

    @GetMapping("/ticket-types")
    public ResponseEntity<ApiResponse<List<TicketTypeAdminResponse>>> getTicketTypes(
            @ModelAttribute TicketTypeFilterRequest request,
            @RequestParam(required = false) UUID parkingLotId
    ) {
        List<TicketType> ticketTypes = publicParkingLotCatalogPortIn.getTicketTypes(parkingLotId, request.keyword());

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched public ticket types successfully",
                ticketTypeApiMapper.toAdminResponses(ticketTypes)
        ));
    }

    private UUID resolvePublicOrganizationId(UUID parkingLotId) {
        if (parkingLotId == null) {
            return organizationPortOut.findByCode("COPARKING_INTERNAL")
                    .orElseThrow(() -> new NotFoundException("Default catalog not found"))
                    .getOrganizationId();
        }
        var parkingLot = parkingLotPortOut.findById(parkingLotId)
                .orElseThrow(() -> new NotFoundException("Parking lot not found"));
        if (parkingLot.getStatus() != ParkingLotStatus.ACTIVE) {
            throw new NotFoundException("Active parking lot not found");
        }
        return parkingLot.getOrganizationId();
    }
}
