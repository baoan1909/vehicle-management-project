package com.ban.vehicle_management.infrastructure.scheduler.parking;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.parking.parkingspaceallocation.port.in.ParkingSpaceAllocationPortIn;
import com.ban.vehicle_management.application.parking.allocation.SubscriptionParkingAllocationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParkingAllocationHoldExpirySchedulerTest {

    @Mock private ParkingSpaceAllocationPortIn allocationPortIn;
    @Mock private SubscriptionParkingAllocationService subscriptionAllocationService;

    @InjectMocks private ParkingAllocationHoldExpiryScheduler scheduler;

    @Test
    void shouldDelegateExpiryToUseCase() {
        when(allocationPortIn.expireHeldAllocations()).thenReturn(3);
        when(subscriptionAllocationService.expireCapacityHolds(org.mockito.ArgumentMatchers.any()))
                .thenReturn(2);

        scheduler.expireHeldAllocations();

        verify(allocationPortIn).expireHeldAllocations();
        verify(subscriptionAllocationService).expireCapacityHolds(org.mockito.ArgumentMatchers.any());
    }
}
