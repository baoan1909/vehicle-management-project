package com.ban.vehicle_management.application.parking.parkingsession.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.accesscontrol.subscription.authorization.SubscriptionAccessGuard;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkingsession.authorization.EmployeeParkingLotAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.mapper.ParkingSessionManagementResultMapper;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.ParkingSessionManagementResult;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.application.people.customervehicle.port.out.CustomerVehiclePortOut;
import com.ban.vehicle_management.application.storage.port.out.FileAccessPort;
import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParkingSessionOwnListLimitTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000701");
    private static final UUID VEHICLE_ID = UUID.fromString("00000000-0000-0000-0000-000000000702");

    @Mock
    private ParkingCheckInUseCaseImpl parkingCheckInUseCase;

    @Mock
    private ParkingCheckOutUseCaseImpl parkingCheckOutUseCase;

    @Mock
    private ParkingSessionPortOut parkingSessionPortOut;

    @Mock
    private CustomerVehiclePortOut customerVehiclePortOut;

    @Mock
    private SubscriptionAccessGuard subscriptionAccessGuard;

    @Mock
    private FileAccessPort fileAccessPort;

    @Mock
    private ParkingSessionManagementResultMapper parkingSessionManagementResultMapper;

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private OrganizationAccessGuard organizationAccessGuard;

    @Mock
    private EmployeeParkingLotAccessGuard employeeParkingLotAccessGuard;

    @Mock
    private ParkingLotPortOut parkingLotPortOut;

    @InjectMocks
    private ParkingSessionUseCaseImpl useCase;

    @Test
    void shouldBoundOwnSessionQueryWithDefaultLimit() {
        CustomerVehicle vehicle = new CustomerVehicle();
        vehicle.setCustomerVehicleId(VEHICLE_ID);
        when(subscriptionAccessGuard.resolveCurrentApprovedCustomerId()).thenReturn(CUSTOMER_ID);
        when(customerVehiclePortOut.findAll(eq(CUSTOMER_ID), eq(null), eq(null), eq(null), eq(null)))
                .thenReturn(List.of(vehicle));
        when(parkingSessionPortOut.findOwnManagementSessions(eq(null), eq(null), eq(null), eq(null),
                eq(null), eq(null), eq(List.of(VEHICLE_ID)), eq(null), eq(100)))
                .thenReturn(List.of());
        when(parkingSessionManagementResultMapper.withResolvedEventImageUrls(eq(List.of()), any()))
                .thenReturn(List.of());

        assertEquals(List.of(), useCase.getOwnSessions(null, null, null, null, null, null, null, null));
    }

    @Test
    void shouldClampOwnSessionLimit() {
        CustomerVehicle vehicle = new CustomerVehicle();
        vehicle.setCustomerVehicleId(VEHICLE_ID);
        when(subscriptionAccessGuard.resolveCurrentApprovedCustomerId()).thenReturn(CUSTOMER_ID);
        when(customerVehiclePortOut.findAll(eq(CUSTOMER_ID), eq(null), eq(null), eq(null), eq(null)))
                .thenReturn(List.of(vehicle));
        when(parkingSessionPortOut.findOwnManagementSessions(eq(null), eq(null), eq(null), eq(null),
                eq(null), eq(null), eq(List.of(VEHICLE_ID)), eq(null), eq(200)))
                .thenReturn(List.of());
        when(parkingSessionManagementResultMapper.withResolvedEventImageUrls(eq(List.of()), any()))
                .thenReturn(List.of());

        useCase.getOwnSessions(null, null, null, null, null, null, null, 10000);
        verify(parkingSessionPortOut).findOwnManagementSessions(eq(null), eq(null), eq(null), eq(null),
                eq(null), eq(null), eq(List.of(VEHICLE_ID)), eq(null), eq(200));
    }

    @Test
    void shouldReturnEmptyWhenCustomerHasNoVehicles() {
        when(subscriptionAccessGuard.resolveCurrentApprovedCustomerId()).thenReturn(CUSTOMER_ID);
        when(customerVehiclePortOut.findAll(eq(CUSTOMER_ID), eq(null), eq(null), eq(null), eq(null)))
                .thenReturn(List.of());

        assertEquals(List.of(), useCase.getOwnSessions(null, null, null, null, null, null, null, null));
    }
}
