package com.ban.vehicle_management.application.parking.allocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.parking.capacityallocation.port.out.SubscriptionCapacityAllocationPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.application.parking.parkingspace.port.out.ParkingSpacePortOut;
import com.ban.vehicle_management.application.parking.parkingspaceallocation.port.out.ParkingSpaceAllocationPortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.parking.capacityallocation.model.SubscriptionCapacityAllocation;
import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.domain.parking.parkingspaceallocation.model.ParkingSpaceAllocation;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceAllocationStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus;
import com.ban.vehicle_management.shared.enumeration.parking.SubscriptionCapacityAllocationStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubscriptionParkingAllocationServiceTest {

    private ZonePortOut zonePortOut;
    private ParkingSpacePortOut spacePortOut;
    private ParkingSpaceAllocationPortOut spaceAllocationPortOut;
    private SubscriptionCapacityAllocationPortOut capacityAllocationPortOut;
    private ParkingSessionPortOut sessionPortOut;
    private SubscriptionParkingAllocationService service;

    @BeforeEach
    void setUp() {
        zonePortOut = mock(ZonePortOut.class);
        spacePortOut = mock(ParkingSpacePortOut.class);
        spaceAllocationPortOut = mock(ParkingSpaceAllocationPortOut.class);
        capacityAllocationPortOut = mock(SubscriptionCapacityAllocationPortOut.class);
        sessionPortOut = mock(ParkingSessionPortOut.class);
        service = new SubscriptionParkingAllocationService(zonePortOut, spacePortOut,
                spaceAllocationPortOut, capacityAllocationPortOut, sessionPortOut);
    }

    @Test
    void activationRequiresAnEntitlement() {
        Subscription subscription = subscription();
        when(capacityAllocationPortOut.findLiveBySubscriptionId(subscription.getSubscriptionId()))
                .thenReturn(Optional.empty());
        when(spaceAllocationPortOut.findActiveAllocationsBySubscription(subscription.getSubscriptionId()))
                .thenReturn(List.of());

        assertThrows(ConflictException.class, () -> service.activate(subscription));
    }

    @Test
    void capacityActivationMovesHoldToActualSubscriptionPeriod() {
        Subscription subscription = subscription();
        UUID zoneId = UUID.randomUUID();
        SubscriptionCapacityAllocation allocation = new SubscriptionCapacityAllocation();
        allocation.setAllocationId(UUID.randomUUID());
        allocation.setSubscriptionId(subscription.getSubscriptionId());
        allocation.setZoneId(zoneId);
        allocation.setEffectiveFrom(subscription.getEffectiveFrom().minusDays(2));
        allocation.setEffectiveTo(subscription.getEffectiveTo().minusDays(2));
        allocation.setAllocationStatus(SubscriptionCapacityAllocationStatus.HELD);
        Zone zone = new Zone();
        zone.setZoneId(zoneId);
        zone.setCapacity(2);
        zone.setUnavailableCapacity(0);
        when(capacityAllocationPortOut.findLiveBySubscriptionId(subscription.getSubscriptionId()))
                .thenReturn(Optional.of(allocation));
        when(spaceAllocationPortOut.findActiveAllocationsBySubscription(subscription.getSubscriptionId()))
                .thenReturn(List.of());
        when(zonePortOut.findByIdForUpdate(zoneId)).thenReturn(Optional.of(zone));
        when(capacityAllocationPortOut.maxConcurrentReservations(zoneId,
                subscription.getEffectiveFrom(), subscription.getEffectiveTo(), allocation.getAllocationId()))
                .thenReturn(1L);

        service.activate(subscription);

        assertEquals(SubscriptionCapacityAllocationStatus.ACTIVE, allocation.getAllocationStatus());
        assertEquals(subscription.getEffectiveFrom(), allocation.getEffectiveFrom());
        assertEquals(subscription.getEffectiveTo(), allocation.getEffectiveTo());
        verify(capacityAllocationPortOut).save(allocation);
    }

    @Test
    void releasingAllocationDoesNotMakeOccupiedSpaceAvailable() {
        Subscription subscription = subscription();
        UUID spaceId = UUID.randomUUID();
        ParkingSpaceAllocation allocation = new ParkingSpaceAllocation();
        allocation.setAllocationId(UUID.randomUUID());
        allocation.setSubscriptionId(subscription.getSubscriptionId());
        allocation.setParkingSpaceId(spaceId);
        allocation.setAllocationStatus(ParkingSpaceAllocationStatus.ACTIVE);
        ParkingSpace space = new ParkingSpace();
        space.setParkingSpaceId(spaceId);
        space.setCode("A-01");
        space.setZoneId(UUID.randomUUID());
        space.setStatus(ParkingSpaceStatus.OCCUPIED);
        space.setLifecycleStatus("ACTIVE");
        space.setStatusSource(com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatusSource.AUTOMATIC);
        space.setVersion(0L);
        ParkingSession session = new ParkingSession();
        session.setSubscriptionId(subscription.getSubscriptionId());
        when(capacityAllocationPortOut.findLiveBySubscriptionId(subscription.getSubscriptionId()))
                .thenReturn(Optional.empty());
        when(spaceAllocationPortOut.findActiveAllocationsBySubscription(subscription.getSubscriptionId()))
                .thenReturn(List.of(allocation));
        when(spacePortOut.findByIdForUpdate(spaceId)).thenReturn(Optional.of(space));
        when(sessionPortOut.findOpenByParkingSpaceId(spaceId)).thenReturn(Optional.of(session));

        service.release(subscription.getSubscriptionId(), false);

        assertEquals(ParkingSpaceAllocationStatus.RELEASED, allocation.getAllocationStatus());
        assertEquals(ParkingSpaceStatus.OCCUPIED, space.getStatus());
        verify(spacePortOut).save(space);
    }

    @Test
    void expiredCapacityHoldIsCancelled() {
        Instant now = Instant.parse("2026-10-27T00:00:00Z");
        SubscriptionCapacityAllocation allocation = new SubscriptionCapacityAllocation();
        allocation.setAllocationId(UUID.randomUUID());
        allocation.setAllocationStatus(SubscriptionCapacityAllocationStatus.HELD);
        allocation.setHoldExpiresAt(now.minusSeconds(1));
        when(capacityAllocationPortOut.findExpiredHolds(now)).thenReturn(List.of(allocation));

        assertEquals(1, service.expireCapacityHolds(now));

        assertEquals(SubscriptionCapacityAllocationStatus.CANCELLED, allocation.getAllocationStatus());
        assertNull(allocation.getHoldExpiresAt());
        verify(capacityAllocationPortOut).save(allocation);
    }

    private Subscription subscription() {
        Subscription subscription = new Subscription();
        subscription.setSubscriptionId(UUID.randomUUID());
        subscription.setEffectiveFrom(LocalDate.of(2026, 11, 1));
        subscription.setEffectiveTo(LocalDate.of(2026, 11, 30));
        return subscription;
    }
}
