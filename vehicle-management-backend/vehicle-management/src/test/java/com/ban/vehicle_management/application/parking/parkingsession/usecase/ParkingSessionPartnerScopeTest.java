package com.ban.vehicle_management.application.parking.parkingsession.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import com.ban.vehicle_management.application.accesscontrol.subscription.authorization.SubscriptionAccessGuard;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.iam.organization.model.result.ParkingLotAccessScope;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.mapper.ParkingSessionManagementResultMapper;
import com.ban.vehicle_management.application.parking.parkingsession.authorization.EmployeeParkingLotAccessGuard;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.ParkingSessionManagementResult;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.application.people.customervehicle.port.out.CustomerVehiclePortOut;
import com.ban.vehicle_management.application.storage.port.out.FileAccessPort;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParkingSessionPartnerScopeTest {

    @Mock private ParkingCheckInUseCaseImpl parkingCheckInUseCase;
    @Mock private ParkingCheckOutUseCaseImpl parkingCheckOutUseCase;
    @Mock private ParkingSessionPortOut parkingSessionPortOut;
    @Mock private CustomerVehiclePortOut customerVehiclePortOut;
    @Mock private SubscriptionAccessGuard subscriptionAccessGuard;
    @Mock private FileAccessPort fileAccessPort;
    @Mock private ParkingSessionManagementResultMapper parkingSessionManagementResultMapper;
    @Mock private CurrentAccountPortIn currentAccountPortIn;
    @Mock private OrganizationAccessGuard organizationAccessGuard;
    @Mock private EmployeeParkingLotAccessGuard employeeParkingLotAccessGuard;
    @Mock private ParkingLotPortOut parkingLotPortOut;

    @InjectMocks private ParkingSessionUseCaseImpl useCase;

    @Test
    void partnerAdminOnlyReceivesSessionsFromItsOwnLots() {
        UUID organizationId = UUID.randomUUID();
        UUID firstLotId = UUID.randomUUID();
        UUID secondLotId = UUID.randomUUID();
        ParkingSessionManagementResult first = mock(ParkingSessionManagementResult.class);
        ParkingSessionManagementResult second = mock(ParkingSessionManagementResult.class);

        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(account("PARTNER_ADMIN"));
        when(organizationAccessGuard.resolveParkingLotAccessScope())
                .thenReturn(new ParkingLotAccessScope(false, Set.of(organizationId), Set.of()));
        when(parkingLotPortOut.findAll(null, null, Set.of(organizationId), null))
                .thenReturn(List.of(lot(firstLotId), lot(secondLotId)));
        when(parkingSessionPortOut.findManagementSessions(
                null, null, null, null, null, null, null, Set.of(firstLotId, secondLotId)))
                .thenReturn(List.of(first, second));
        when(parkingSessionManagementResultMapper.withResolvedEventImageUrls(anyList(), eq(fileAccessPort)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(List.of(first, second), useCase.getSessions(null, null, null, null, null, null));
        verify(parkingSessionPortOut).findManagementSessions(
                null, null, null, null, null, null, null, Set.of(firstLotId, secondLotId));
    }

    @Test
    void unassignedManagerQueriesNoLots() {
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(account("PARKING_MANAGER"));
        when(organizationAccessGuard.resolveParkingLotAccessScope())
                .thenReturn(new ParkingLotAccessScope(false, Set.of(), Set.of()));
        when(parkingSessionPortOut.findManagementSessions(null, null, null, null, null, null, null, Set.of()))
                .thenReturn(List.of());
        when(parkingSessionManagementResultMapper.withResolvedEventImageUrls(anyList(), eq(fileAccessPort)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(List.of(), useCase.getSessions(null, null, null, null, null, null));
    }

    private CurrentAccountAccess account(String roleCode) {
        return new CurrentAccountAccess(
                UUID.randomUUID(), "subject", "partner", "partner@example.com", UUID.randomUUID(),
                roleCode, AccountStatus.ACTIVE, null, Set.of()
        );
    }

    private ParkingLot lot(UUID lotId) {
        ParkingLot lot = new ParkingLot();
        lot.setParkingLotId(lotId);
        return lot;
    }
}
