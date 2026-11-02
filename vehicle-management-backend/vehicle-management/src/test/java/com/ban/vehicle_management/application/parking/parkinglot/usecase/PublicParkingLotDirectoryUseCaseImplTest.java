package com.ban.vehicle_management.application.parking.parkinglot.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.catalog.availability.port.in.PublicParkingLotCatalogPortIn;
import com.ban.vehicle_management.application.catalog.priceplan.port.out.PricePlanPortOut;
import com.ban.vehicle_management.application.catalog.pricerule.port.out.PriceRulePortOut;
import com.ban.vehicle_management.application.parking.availability.ParkingAvailabilityQuery;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.domain.catalog.priceplan.model.PricePlan;
import com.ban.vehicle_management.domain.catalog.pricerule.model.PriceRule;
import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import com.ban.vehicle_management.domain.parking.availability.model.ParkingAvailabilitySnapshot;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublicParkingLotDirectoryUseCaseImplTest {

    private static final UUID ACTIVE_LOT_ID = UUID.randomUUID();
    private static final UUID ORGANIZATION_ID = UUID.randomUUID();
    private static final UUID VEHICLE_TYPE_ID = UUID.randomUUID();

    @Mock private ParkingLotPortOut parkingLotPortOut;
    @Mock private ZonePortOut zonePortOut;
    @Mock private PublicParkingLotCatalogPortIn publicParkingLotCatalogPortIn;
    @Mock private ParkingAvailabilityQuery parkingAvailabilityQuery;
    @Mock private PricePlanPortOut pricePlanPortOut;
    @Mock private PriceRulePortOut priceRulePortOut;
    @Mock private ParkingSessionPortOut parkingSessionPortOut;
    @InjectMocks private PublicParkingLotDirectoryUseCaseImpl useCase;

    @Test
    void directoryIncludesOnlyActiveLotsAndReportsPublicSummary() {
        ParkingLot activeLot = lot(ACTIVE_LOT_ID, "Active lot", ParkingLotStatus.ACTIVE);
        Zone activeZone = new Zone();
        activeZone.setZoneId(UUID.randomUUID());
        activeZone.setVehicleTypeId(VEHICLE_TYPE_ID);

        when(parkingLotPortOut.findAll(ParkingLotStatus.ACTIVE, null, null, null))
                .thenReturn(List.of(activeLot));
        when(zonePortOut.findAll(ACTIVE_LOT_ID, null, ZoneStatus.ACTIVE, null))
                .thenReturn(List.of(activeZone));
        when(publicParkingLotCatalogPortIn.getVehicleTypes(ACTIVE_LOT_ID))
                .thenReturn(List.of(vehicleType()));
        PricePlan plan = new PricePlan();
        plan.setPricePlanId(UUID.randomUUID());
        PriceRule minimumRule = priceRule(plan.getPricePlanId(), "4000");
        PriceRule maximumRule = priceRule(plan.getPricePlanId(), "80000");
        when(pricePlanPortOut.findAll(any(), any(), any(), any(), any()))
                .thenReturn(List.of(plan));
        when(priceRulePortOut.findAll(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(minimumRule, maximumRule));
        when(publicParkingLotCatalogPortIn.filterAvailablePriceRules(
                ACTIVE_LOT_ID, List.of(minimumRule, maximumRule)))
                .thenReturn(List.of(minimumRule, maximumRule));
        when(parkingSessionPortOut.countByParkingLotIdAndStatus(
                ACTIVE_LOT_ID, ParkingSessionStatus.CLOSED)).thenReturn(125L);
        when(parkingAvailabilityQuery.get(any(Zone.class), any()))
                .thenReturn(new ParkingAvailabilitySnapshot(
                        activeZone.getZoneId(), 10, 0, 10, 2, 1, 2,
                        7, 5, 7, 2, 1, 0));

        var results = useCase.getDirectory();

        assertEquals(List.of(ACTIVE_LOT_ID),
                results.stream().map(item -> item.parkingLotId()).toList());
        assertEquals(10, results.getFirst().totalCapacity());
        assertEquals(5, results.getFirst().visitorAvailable());
        assertTrue(results.getFirst().hasAvailability());
        assertEquals(1, results.getFirst().vehicleTypes().size());
        assertEquals(125, results.getFirst().completedSessionCount());
        assertEquals(new BigDecimal("4000"), results.getFirst().minimumPrice());
        assertEquals(new BigDecimal("80000"), results.getFirst().maximumPrice());
        verify(parkingLotPortOut, never()).findAll(ParkingLotStatus.SETUP, null, null, null);
        verify(parkingLotPortOut, never()).findAll(ParkingLotStatus.MAINTENANCE, null, null, null);
        verify(parkingLotPortOut, never()).findAll(ParkingLotStatus.CLOSED, null, null, null);
    }

    private ParkingLot lot(UUID id, String name, ParkingLotStatus status) {
        ParkingLot lot = new ParkingLot();
        lot.setParkingLotId(id);
        lot.setCode("LOT-" + id.toString().substring(0, 6));
        lot.setName(name);
        lot.setStatus(status);
        lot.setOrganizationId(ORGANIZATION_ID);
        return lot;
    }

    private VehicleType vehicleType() {
        VehicleType type = new VehicleType();
        type.setVehicleTypeId(VEHICLE_TYPE_ID);
        type.setCode("MOTORBIKE");
        type.setName("Xe máy");
        return type;
    }

    private PriceRule priceRule(UUID pricePlanId, String basePrice) {
        PriceRule rule = new PriceRule();
        rule.setPriceRuleId(UUID.randomUUID());
        rule.setPricePlanId(pricePlanId);
        rule.setVehicleTypeId(VEHICLE_TYPE_ID);
        rule.setBasePrice(new BigDecimal(basePrice));
        rule.setIsActive(true);
        return rule;
    }
}
