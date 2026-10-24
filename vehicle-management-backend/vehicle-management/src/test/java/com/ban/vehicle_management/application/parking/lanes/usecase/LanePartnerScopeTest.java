package com.ban.vehicle_management.application.parking.lanes.usecase;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.application.parking.gate.port.out.GatePortOut;
import com.ban.vehicle_management.application.parking.lane.port.out.LanePortOut;
import com.ban.vehicle_management.application.parking.lane.usecase.LaneUsecaseImpl;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;
import com.ban.vehicle_management.shared.enumeration.parking.LaneStatus;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class LanePartnerScopeTest {

    @Mock private LanePortOut lanePortOut;
    @Mock private NotificationPortIn notificationPortIn;
    @Mock private GatePortOut gatePortOut;
    @Mock private ZonePortOut zonePortOut;
    @Mock private ParkingLotPortOut parkingLotPortOut;
    @Mock private OrganizationAccessGuard organizationAccessGuard;

    private LaneUsecaseImpl laneUseCase;

    @BeforeEach
    void setUp() {
        laneUseCase = new LaneUsecaseImpl(
                lanePortOut,
                notificationPortIn,
                gatePortOut,
                zonePortOut,
                parkingLotPortOut,
                organizationAccessGuard
        );
    }

    @Test
    void partnerLaneListIsRestrictedToAccessibleParkingLots() {
        Set<UUID> ownParkingLotIds = Set.of(UUID.randomUUID(), UUID.randomUUID());
        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut))
                .thenReturn(ownParkingLotIds);

        laneUseCase.getLanes(null, null, LaneDirection.IN, LaneStatus.ACTIVE, null);

        verify(lanePortOut).findAll(
                null,
                LaneDirection.IN,
                LaneStatus.ACTIVE,
                null,
                ownParkingLotIds
        );
    }

    @Test
    void partnerCannotRequestLanesFromAnotherOrganizationParkingLot() {
        UUID otherParkingLotId = UUID.randomUUID();
        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut))
                .thenReturn(Set.of(UUID.randomUUID()));

        assertThrows(
                AccessDeniedException.class,
                () -> laneUseCase.getLanes(
                        null,
                        otherParkingLotId,
                        LaneDirection.IN,
                        LaneStatus.ACTIVE,
                        null
                )
        );
    }
}
