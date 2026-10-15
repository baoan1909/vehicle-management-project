package com.ban.vehicle_management.application.people.customervehicle.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.people.customervehicle.authorization.CustomerVehicleAccessGuard;
import com.ban.vehicle_management.application.people.customervehicle.port.out.CustomerVehiclePortOut;
import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerVehicleOwnListLimitTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000601");

    @Mock
    private CustomerVehiclePortOut customerVehiclePortOut;

    @Mock
    private CustomerVehicleAccessGuard customerVehicleAccessGuard;

    @InjectMocks
    private CustomerVehicleUseCaseImpl useCase;

    @Test
    void shouldDefaultOwnVehicleLimitTo100() {
        when(customerVehicleAccessGuard.resolveCustomerIdForRead(null)).thenReturn(CUSTOMER_ID);
        when(customerVehiclePortOut.findOwnVehicles(eq(CUSTOMER_ID), eq(100))).thenReturn(List.of());

        assertEquals(List.of(), useCase.getMyCustomerVehicles(null));
    }

    @Test
    void shouldClampOwnVehicleLimit() {
        when(customerVehicleAccessGuard.resolveCustomerIdForRead(null)).thenReturn(CUSTOMER_ID);
        when(customerVehiclePortOut.findOwnVehicles(eq(CUSTOMER_ID), eq(200)))
                .thenReturn(List.of(new CustomerVehicle()));

        assertEquals(1, useCase.getMyCustomerVehicles(Integer.MAX_VALUE).size());
        verify(customerVehiclePortOut).findOwnVehicles(eq(CUSTOMER_ID), eq(200));
    }
}
