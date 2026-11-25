package com.ban.vehicle_management.application.parking.parkingsession.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.ban.vehicle_management.application.accesscontrol.subscription.authorization.SubscriptionAccessGuard;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.mapper.ParkingSessionManagementResultMapper;
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
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ParkingSessionOwnSummaryTest {
    @Mock ParkingCheckInUseCaseImpl checkIn;
    @Mock ParkingCheckOutUseCaseImpl checkOut;
    @Mock ParkingSessionPortOut sessions;
    @Mock CustomerVehiclePortOut vehicles;
    @Mock SubscriptionAccessGuard guard;
    @Mock FileAccessPort files;
    @Mock ParkingSessionManagementResultMapper mapper;
    @Mock CurrentAccountPortIn account;
    @Mock OrganizationAccessGuard organizations;
    @Mock ParkingLotPortOut lots;
    @Mock com.ban.vehicle_management.application.parking.parkingsession.authorization.EmployeeParkingLotAccessGuard employeeLots;
    @InjectMocks ParkingSessionUseCaseImpl service;

    @Test
    void summaryIsOwnBoundedAndDoesNotSignAnyEvidenceUrl() {
        UUID customer = UUID.randomUUID(); CustomerVehicle vehicle = new CustomerVehicle();
        vehicle.setCustomerVehicleId(UUID.randomUUID()); vehicle.setCustomerId(customer);
        when(guard.resolveCurrentApprovedCustomerId()).thenReturn(customer);
        when(vehicles.findAll(customer,null,null,null,null)).thenReturn(List.of(vehicle));
        when(sessions.findOwnManagementSessions(isNull(),isNull(),isNull(),isNull(),isNull(),isNull(),eq(List.of(vehicle.getCustomerVehicleId())),isNull(),eq(20)))
                .thenReturn(List.of());
        assertEquals(List.of(), service.getOwnSessionSummaries(null,20));
        verify(account).requirePermission("PARKING_SESSION_READ_OWN");
        verifyNoInteractions(files,mapper);
    }

    @Test
    void revokedPermissionStopsBeforeQueryingOwnedVehicles() {
        doThrow(new AccessDeniedException("revoked")).when(account).requirePermission("PARKING_SESSION_READ_OWN");
        assertThrows(AccessDeniedException.class, () -> service.getOwnSessionSummaries(null,20));
        verifyNoInteractions(guard,vehicles,sessions,files);
    }
}
