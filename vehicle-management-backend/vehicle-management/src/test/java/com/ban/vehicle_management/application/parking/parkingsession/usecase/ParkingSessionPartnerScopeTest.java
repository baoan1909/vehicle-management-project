package com.ban.vehicle_management.application.parking.parkingsession.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ban.vehicle_management.application.accesscontrol.subscription.authorization.SubscriptionAccessGuard;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.mapper.ParkingSessionManagementResultMapper;
import com.ban.vehicle_management.application.parking.parkingsession.authorization.EmployeeParkingLotAccessGuard;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.ParkingSessionManagementResult;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.application.people.customervehicle.port.out.CustomerVehiclePortOut;
import com.ban.vehicle_management.application.storage.port.out.FileAccessPort;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

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
        UUID firstLotId = UUID.randomUUID();
        UUID secondLotId = UUID.randomUUID();
        ParkingSessionManagementResult first = mock(ParkingSessionManagementResult.class);
        ParkingSessionManagementResult second = mock(ParkingSessionManagementResult.class);

        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(account("CUSTOM_ROLE", OrganizationAccessGuard.PARKING_SCOPE_PARTNER));
        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut))
                .thenReturn(Set.of(firstLotId, secondLotId));
        when(parkingSessionPortOut.findManagementSessions(
                null, null, null, null, null, null, null, Set.of(firstLotId, secondLotId)))
                .thenReturn(List.of(first, second));
        when(parkingSessionManagementResultMapper.withResolvedEventImageUrls(anyList(), eq(fileAccessPort)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(List.of(first, second), useCase.getSessions(null, null, null, null, null, null, null));
        verify(parkingSessionPortOut).findManagementSessions(
                null, null, null, null, null, null, null, Set.of(firstLotId, secondLotId));
    }

    @Test
    void unassignedManagerQueriesNoLots() {
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(account("PARKING_MANAGER", OrganizationAccessGuard.PARKING_SCOPE_ASSIGNED));
        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut))
                .thenReturn(Set.of());
        when(parkingSessionPortOut.findManagementSessions(null, null, null, null, null, null, null, Set.of()))
                .thenReturn(List.of());
        when(parkingSessionManagementResultMapper.withResolvedEventImageUrls(anyList(), eq(fileAccessPort)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(List.of(), useCase.getSessions(null, null, null, null, null, null, null));
    }

    @Test
    void selectedLotNarrowsSessionsWithinPartnerScope() {
        UUID firstLotId = UUID.randomUUID();
        UUID secondLotId = UUID.randomUUID();
        ParkingSessionManagementResult session = mock(ParkingSessionManagementResult.class);
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(account("PARTNER_ADMIN", OrganizationAccessGuard.PARKING_SCOPE_PARTNER));
        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut))
                .thenReturn(Set.of(firstLotId, secondLotId));
        when(parkingSessionPortOut.findManagementSessions(
                null, null, null, null, null, null, null, Set.of(firstLotId)))
                .thenReturn(List.of(session));
        when(parkingSessionManagementResultMapper.withResolvedEventImageUrls(anyList(), eq(fileAccessPort)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(List.of(session), useCase.getSessions(null, null, null, null, null, null, firstLotId));
        verify(parkingSessionPortOut).findManagementSessions(
                null, null, null, null, null, null, null, Set.of(firstLotId));
    }

    @Test
    void selectedLotOutsidePartnerScopeIsDeniedBeforeQuery() {
        UUID ownLotId = UUID.randomUUID();
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(account("PARTNER_ADMIN", OrganizationAccessGuard.PARKING_SCOPE_PARTNER));
        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut))
                .thenReturn(Set.of(ownLotId));

        assertThrows(AccessDeniedException.class,
                () -> useCase.getSessions(null, null, null, null, null, null, UUID.randomUUID()));
        verifyNoInteractions(parkingSessionPortOut);
    }

    private CurrentAccountAccess account(String roleCode, String scopePermission) {
        return new CurrentAccountAccess(
                UUID.randomUUID(), "subject", "partner", "partner@example.com", UUID.randomUUID(),
                roleCode, AccountStatus.ACTIVE,
                "PARKING_MANAGER".equals(roleCode) ? EmployeeStatus.ACTIVE : null,
                Set.of(scopePermission)
        );
    }
}
