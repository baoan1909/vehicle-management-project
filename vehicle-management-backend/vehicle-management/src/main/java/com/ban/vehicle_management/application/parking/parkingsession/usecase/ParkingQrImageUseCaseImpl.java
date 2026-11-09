package com.ban.vehicle_management.application.parking.parkingsession.usecase;

import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionQrImagePortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.authorization.ParkingSessionAccessGuard;
import com.ban.vehicle_management.application.parking.parkingsession.port.in.ParkingQrImagePortIn;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ParkingQrImageUseCaseImpl implements ParkingQrImagePortIn {
    private static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    private static final String RESERVATION_PREFIX = "COPARKING:RESERVATION:1:";
    private static final String SUBSCRIPTION_PREFIX = "COPARKING:SUBSCRIPTION:1:";

    private final ParkingSessionAccessGuard accessGuard;
    private final ParkingLotPortOut parkingLots;
    private final SubscriptionQrImagePortOut images;

    public ParkingQrImageUseCaseImpl(ParkingSessionAccessGuard accessGuard,
            ParkingLotPortOut parkingLots, SubscriptionQrImagePortOut images) {
        this.accessGuard = accessGuard;
        this.parkingLots = parkingLots;
        this.images = images;
    }

    @Override
    public String decodeForOperation(byte[] imageBytes, UUID parkingLotId) {
        accessGuard.ensureCanUseOcr();
        if (parkingLotId == null) {
            throw new BadRequestException("parkingLotId must not be null");
        }
        var lot = parkingLots.findById(parkingLotId)
                .orElseThrow(() -> new NotFoundException("Parking lot not found"));
        accessGuard.ensureCanOperateParkingLot(lot);
        if (imageBytes == null || imageBytes.length == 0 || imageBytes.length > MAX_IMAGE_BYTES) {
            throw new BadRequestException("QR image must be between 1 byte and 5 MB");
        }
        String payload = images.decode(imageBytes)
                .orElseThrow(() -> new BadRequestException("Unable to read QR image"));
        if (payload.length() > 512 || !(payload.startsWith(RESERVATION_PREFIX)
                || payload.startsWith(SUBSCRIPTION_PREFIX))) {
            throw new BadRequestException("Unsupported check-in QR");
        }
        return payload;
    }
}
