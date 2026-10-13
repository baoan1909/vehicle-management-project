package com.ban.vehicle_management.application.parking.parkingspaceallocation.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionPortOut;
import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglevel.port.out.ParkingLevelPortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkingspace.port.out.ParkingSpacePortOut;
import com.ban.vehicle_management.application.parking.parkingspaceallocation.port.out.ParkingSpaceAllocationPortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.application.people.customervehicle.port.out.CustomerVehiclePortOut;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.parking.parkinglevel.model.ParkingLevel;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.domain.parking.parkingspaceallocation.model.ParkingSpaceAllocation;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceAllocationStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AllocationActivateRecheckTest {

    @Mock private ParkingSpaceAllocationPortOut allocationPortOut;
    @Mock private ParkingSpacePortOut parkingSpacePortOut;
    @Mock private ZonePortOut zonePortOut;
    @Mock private ParkingLevelPortOut parkingLevelPortOut;
    @Mock private ParkingLotPortOut parkingLotPortOut;
    @Mock private SubscriptionPortOut subscriptionPortOut;
    @Mock private CustomerVehiclePortOut customerVehiclePortOut;
    @Mock private OrganizationAccessGuard organizationAccessGuard;
    @Mock private AuditLogPortOut auditLogPortOut;
    @Mock private CurrentAccountPortIn currentAccountPortIn;

    @InjectMocks private ParkingSpaceAllocationUseCaseImpl useCase;

    private final UUID lotId = UUID.randomUUID();
    private final UUID levelId = UUID.randomUUID();
    private final UUID zoneId = UUID.randomUUID();
    private final UUID spaceId = UUID.randomUUID();
    private final UUID subscriptionId = UUID.randomUUID();
    private final UUID vehicleTypeId = UUID.randomUUID();
    private final UUID allocationId = UUID.randomUUID();

    @BeforeEach
    void setUpTopology() {
        ParkingSpace space = new ParkingSpace();
        space.setParkingSpaceId(spaceId);
        space.setZoneId(zoneId);
        space.setVehicleTypeId(vehicleTypeId);
        space.setLifecycleStatus("ACTIVE");
        space.setStatus(ParkingSpaceStatus.AVAILABLE);
        Zone zone = new Zone();
        zone.setZoneId(zoneId);
        zone.setParkingLevelId(levelId);
        zone.setParkingLotId(lotId);
        ParkingLevel level = new ParkingLevel();
        level.setParkingLevelId(levelId);
        level.setParkingLotId(lotId);
        ParkingLot lot = new ParkingLot();
        lot.setParkingLotId(lotId);
        lot.setOrganizationId(UUID.randomUUID());
        when(parkingSpacePortOut.findById(spaceId)).thenReturn(Optional.of(space));
        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone));
        when(parkingLevelPortOut.findById(levelId)).thenReturn(Optional.of(level));
        when(parkingLotPortOut.findById(lotId)).thenReturn(Optional.of(lot));
    }

    @Test
    void shouldRejectActivateWhenOverlappingAllocationAppeared() {
        ParkingSpaceAllocation held = heldAllocation();
        when(allocationPortOut.findById(allocationId)).thenReturn(Optional.of(held));
        when(subscriptionPortOut.findById(subscriptionId))
                .thenReturn(Optional.of(subscription()));
        when(customerVehiclePortOut.findById(any()))
                .thenReturn(Optional.of(vehicle()));
        when(allocationPortOut.findByParkingSpaceId(spaceId)).thenReturn(List.of(held));
        when(allocationPortOut.existsOverlappingAllocation(
                eq(spaceId), eq(held.getEffectiveFrom()), eq(held.getEffectiveTo()), eq(allocationId)))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> useCase.activateAllocation(allocationId));
        verify(allocationPortOut, never()).save(any());
    }

    @Test
    void shouldActivateWhenRecheckPassesAndExcludeSelf() {
        ParkingSpaceAllocation held = heldAllocation();
        when(allocationPortOut.findById(allocationId)).thenReturn(Optional.of(held));
        when(subscriptionPortOut.findById(subscriptionId))
                .thenReturn(Optional.of(subscription()));
        when(customerVehiclePortOut.findById(any()))
                .thenReturn(Optional.of(vehicle()));
        when(allocationPortOut.findByParkingSpaceId(spaceId)).thenReturn(List.of(held));
        when(allocationPortOut.existsOverlappingAllocation(
                eq(spaceId), eq(held.getEffectiveFrom()), eq(held.getEffectiveTo()), eq(allocationId)))
                .thenReturn(false);
        when(allocationPortOut.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(UUID.randomUUID());

        ParkingSpaceAllocation saved = useCase.activateAllocation(allocationId);

        assertEquals(ParkingSpaceAllocationStatus.ACTIVE, saved.getAllocationStatus());
        verify(allocationPortOut).existsOverlappingAllocation(
                eq(spaceId), eq(held.getEffectiveFrom()), eq(held.getEffectiveTo()), eq(allocationId));
    }

    private ParkingSpaceAllocation heldAllocation() {
        ParkingSpaceAllocation allocation = new ParkingSpaceAllocation();
        allocation.setAllocationId(allocationId);
        allocation.setParkingSpaceId(spaceId);
        allocation.setSubscriptionId(subscriptionId);
        allocation.setEffectiveFrom(LocalDate.now().minusDays(1));
        allocation.setEffectiveTo(LocalDate.now().plusDays(30));
        allocation.setAllocationStatus(ParkingSpaceAllocationStatus.HELD);
        return allocation;
    }

    private Subscription subscription() {
        Subscription subscription = new Subscription();
        subscription.setSubscriptionId(subscriptionId);
        subscription.setParkingLotId(lotId);
        subscription.setCustomerVehicleId(UUID.randomUUID());
        return subscription;
    }

    private CustomerVehicle vehicle() {
        CustomerVehicle vehicle = new CustomerVehicle();
        vehicle.setCustomerVehicleId(UUID.randomUUID());
        vehicle.setVehicleTypeId(vehicleTypeId);
        return vehicle;
    }
}
