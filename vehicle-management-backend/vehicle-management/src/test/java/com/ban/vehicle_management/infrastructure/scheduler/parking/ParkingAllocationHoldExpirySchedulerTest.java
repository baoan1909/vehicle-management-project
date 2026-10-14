package com.ban.vehicle_management.infrastructure.scheduler.parking;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.parking.parkingspaceallocation.port.in.ParkingSpaceAllocationPortIn;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParkingAllocationHoldExpirySchedulerTest {

    @Mock private ParkingSpaceAllocationPortIn allocationPortIn;

    @InjectMocks private ParkingAllocationHoldExpiryScheduler scheduler;

    @Test
    void shouldDelegateExpiryToUseCase() {
        when(allocationPortIn.expireHeldAllocations()).thenReturn(3);

        scheduler.expireHeldAllocations();

        verify(allocationPortIn).expireHeldAllocations();
    }
}
