package com.ban.vehicle_management.application.parking.parkingsession.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionQrImagePortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.authorization.ParkingSessionAccessGuard;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ParkingQrImageUseCaseImplTest {
    @Mock private ParkingSessionAccessGuard accessGuard;
    @Mock private ParkingLotPortOut parkingLots;
    @Mock private SubscriptionQrImagePortOut images;
    @Mock private ParkingLot lot;
    @InjectMocks private ParkingQrImageUseCaseImpl useCase;

    @Test
    void decodesReservationQrWithinAuthorizedLot() {
        UUID lotId = UUID.randomUUID();
        byte[] image = {1, 2, 3};
        String payload = "COPARKING:RESERVATION:1:" + "x".repeat(200);
        when(parkingLots.findById(lotId)).thenReturn(Optional.of(lot));
        when(images.decode(image)).thenReturn(Optional.of(payload));

        assertEquals(payload, useCase.decodeForOperation(image, lotId));
        verify(accessGuard).ensureCanUseOcr();
        verify(accessGuard).ensureCanOperateParkingLot(lot);
    }

    @Test
    void rejectsQrOutsideAuthorizedLotBeforeDecoding() {
        UUID lotId = UUID.randomUUID();
        when(parkingLots.findById(lotId)).thenReturn(Optional.of(lot));
        org.mockito.Mockito.doThrow(new AccessDeniedException("denied"))
                .when(accessGuard).ensureCanOperateParkingLot(lot);

        assertThrows(AccessDeniedException.class,
                () -> useCase.decodeForOperation(new byte[] {1}, lotId));
        verifyNoInteractions(images);
    }

    @Test
    void rejectsUnrecognizedQr() {
        UUID lotId = UUID.randomUUID();
        when(parkingLots.findById(lotId)).thenReturn(Optional.of(lot));
        when(images.decode(any())).thenReturn(Optional.of("other-qr"));

        assertThrows(BadRequestException.class,
                () -> useCase.decodeForOperation(new byte[] {1}, lotId));
    }
}
