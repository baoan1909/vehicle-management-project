package com.ban.vehicle_management.application.parking.parkingspaceallocation.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ParkingSpaceAllocationUseCaseTest {

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
    private final UUID customerVehicleId = UUID.randomUUID();

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
    void shouldCreateHeldAllocationAndAudit() {
        Subscription subscription = subscription(lotId, vehicleTypeId);
        when(subscriptionPortOut.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(customerVehiclePortOut.findById(customerVehicleId))
                .thenReturn(Optional.of(vehicle(vehicleTypeId)));
        when(allocationPortOut.findByParkingSpaceId(spaceId)).thenReturn(List.of());
        when(allocationPortOut.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(UUID.randomUUID());
        ParkingSpaceAllocation allocation = allocation();

        ParkingSpaceAllocation saved = useCase.createAllocation(allocation);

        assertEquals(ParkingSpaceAllocationStatus.HELD, saved.getAllocationStatus());
        verify(organizationAccessGuard).ensureCanOperateParkingLot(any(ParkingLot.class));
        verify(auditLogPortOut).save(any());
    }

    @Test
    void shouldRejectOperationalTransitionWhenGuardDenies() {
        ParkingSpaceAllocation allocation = allocation();
        allocation.setAllocationId(UUID.randomUUID());
        allocation.setAllocationStatus(ParkingSpaceAllocationStatus.HELD);
        when(allocationPortOut.findById(allocation.getAllocationId())).thenReturn(Optional.of(allocation));
        doThrow(new AccessDeniedException("read only"))
                .when(organizationAccessGuard).ensureCanOperateParkingLot(any(ParkingLot.class));

        assertThrows(AccessDeniedException.class,
                () -> useCase.activateAllocation(allocation.getAllocationId()));
        verify(allocationPortOut, never()).save(any());
    }

    @Test
    void shouldRejectVehicleTypeMismatch() {
        Subscription subscription = subscription(lotId, UUID.randomUUID());
        when(subscriptionPortOut.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(customerVehiclePortOut.findById(customerVehicleId))
                .thenReturn(Optional.of(vehicle(UUID.randomUUID())));

        assertThrows(ConflictException.class, () -> useCase.createAllocation(allocation()));
        verify(allocationPortOut, never()).save(any());
    }

    private ParkingSpaceAllocation allocation() {
        ParkingSpaceAllocation allocation = new ParkingSpaceAllocation();
        allocation.setParkingSpaceId(spaceId);
        allocation.setSubscriptionId(subscriptionId);
        allocation.setEffectiveFrom(LocalDate.now());
        allocation.setEffectiveTo(LocalDate.now().plusMonths(1));
        allocation.setHoldExpiresAt(java.time.Instant.now().plusSeconds(3600));
        return allocation;
    }

    private Subscription subscription(UUID parkingLotId, UUID ignoredVehicleTypeId) {
        Subscription subscription = new Subscription();
        subscription.setSubscriptionId(subscriptionId);
        subscription.setParkingLotId(parkingLotId);
        subscription.setCustomerVehicleId(customerVehicleId);
        return subscription;
    }

    private CustomerVehicle vehicle(UUID typeId) {
        CustomerVehicle vehicle = new CustomerVehicle();
        vehicle.setCustomerVehicleId(customerVehicleId);
        vehicle.setVehicleTypeId(typeId);
        return vehicle;
    }
}
