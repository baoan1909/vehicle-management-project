package com.ban.vehicle_management.application.parking.customerreservation.service;

import com.ban.vehicle_management.domain.parking.customerreservation.model.CustomerReservation;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ReservationQrService {
    private static final String PREFIX = "COPARKING:RESERVATION:1:";
    private final SecureRandom random = new SecureRandom();

    public String generateToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String toPayload(CustomerReservation reservation) {
        if (reservation.getPaidAt() == null) {
            throw new BadRequestException("Reservation is not paid");
        }
        String details = reservation.getLicensePlate() + "|" + reservation.getPaidAt() + "|"
                + reservation.getZoneId() + "|"
                + (reservation.getParkingSpaceId() == null ? "CAPACITY" : reservation.getParkingSpaceId());
        return PREFIX + reservation.getQrToken() + ":"
                + Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(details.getBytes(StandardCharsets.UTF_8));
    }

    public Optional<String> extractToken(String payload) {
        if (payload == null || !payload.startsWith(PREFIX)) {
            return Optional.empty();
        }
        String remainder = payload.substring(PREFIX.length());
        int separator = remainder.indexOf(':');
        if (separator != 43 || remainder.length() > 512) {
            return Optional.empty();
        }
        String token = remainder.substring(0, separator);
        return token.matches("[A-Za-z0-9_-]{43}") ? Optional.of(token) : Optional.empty();
    }
}
