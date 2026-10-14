package com.ban.vehicle_management.application.parking.parkinglevel.usecase;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglevel.port.out.ParkingLevelPortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.domain.parking.parkinglevel.model.ParkingLevel;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLevelStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ParkingLevelUseCaseScopeTest {

    @Mock
    private ParkingLevelPortOut parkingLevelPortOut;
    @Mock
    private ParkingLotPortOut parkingLotPortOut;
    @Mock
    private OrganizationAccessGuard organizationAccessGuard;

    @InjectMocks
    private ParkingLevelUseCaseImpl useCase;

    @Test
    void shouldDenyReadWhenLotScopeForbidsIt() {
        UUID levelId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        when(parkingLevelPortOut.findById(levelId)).thenReturn(Optional.of(level(levelId, lotId)));
        when(parkingLotPortOut.findById(lotId)).thenReturn(Optional.of(lot(lotId)));
        doThrow(new AccessDeniedException("denied"))
                .when(organizationAccessGuard).ensureCanAccessParkingLot(any(ParkingLot.class));

        assertThrows(AccessDeniedException.class, () -> useCase.getParkingLevelById(levelId));
    }

    @Test
    void shouldRejectMoveToAnotherParkingLot() {
        UUID levelId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        when(parkingLevelPortOut.findById(levelId)).thenReturn(Optional.of(level(levelId, lotId)));
        when(parkingLotPortOut.findById(lotId)).thenReturn(Optional.of(lot(lotId)));

        ParkingLevel request = level(null, UUID.randomUUID());

        assertThrows(BadRequestException.class, () -> useCase.updateParkingLevel(levelId, request));
        verify(parkingLevelPortOut, never()).save(any(ParkingLevel.class));
    }

    @Test
    void shouldRefuseDeleteWhenZonesExist() {
        UUID levelId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        when(parkingLevelPortOut.findById(levelId)).thenReturn(Optional.of(level(levelId, lotId)));
        when(parkingLotPortOut.findById(lotId)).thenReturn(Optional.of(lot(lotId)));
        when(parkingLevelPortOut.countZonesByParkingLevelId(levelId)).thenReturn(2L);

        assertThrows(ConflictException.class, () -> useCase.deleteParkingLevel(levelId));
        verify(parkingLevelPortOut, never()).delete(levelId);
    }

    private ParkingLevel level(UUID levelId, UUID lotId) {
        ParkingLevel level = new ParkingLevel();
        level.setParkingLevelId(levelId);
        level.setParkingLotId(lotId);
        level.setCode("L1");
        level.setName("Level 1");
        level.setDisplayOrder(1);
        level.setCanvasWidth(BigDecimal.valueOf(100));
        level.setCanvasHeight(BigDecimal.valueOf(100));
        level.setStatus(ParkingLevelStatus.ACTIVE);
        return level;
    }

    private ParkingLot lot(UUID lotId) {
        ParkingLot lot = new ParkingLot();
        lot.setParkingLotId(lotId);
        lot.setOrganizationId(UUID.randomUUID());
        return lot;
    }
}
