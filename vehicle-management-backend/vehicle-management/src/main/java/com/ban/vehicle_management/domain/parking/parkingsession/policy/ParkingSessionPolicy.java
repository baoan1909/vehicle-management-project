package com.ban.vehicle_management.domain.parking.parkingsession.policy;

import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import com.ban.vehicle_management.shared.enumeration.ParkingSessionStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.math.BigDecimal;
import java.time.Instant;

public class ParkingSessionPolicy {

    public void initialize(ParkingSession parkingSession) {
        requireParkingSession(parkingSession);
        requireField(parkingSession.getCardId(), "cardId");
        requireField(parkingSession.getVehicleTypeId(), "vehicleTypeId");
        requireField(parkingSession.getCheckInTime(), "checkInTime");
        parkingSession.setLicensePlateIn(TextValidationUtils.normalizeRequiredText(parkingSession.getLicensePlateIn(), "licensePlateIn", 20));
        parkingSession.setLicensePlateOut(TextValidationUtils.normalizeNullableText(parkingSession.getLicensePlateOut(), "licensePlateOut", 20));
        if (parkingSession.getStatus() == null) {
            parkingSession.setStatus(ParkingSessionStatus.OPEN);
        }
        validateState(parkingSession);
    }

    public void checkOut(ParkingSession parkingSession, Instant checkOutTime, String licensePlateOut,
                         BigDecimal totalPrice) {
        requireStatus(parkingSession, ParkingSessionStatus.OPEN);
        requireField(checkOutTime, "checkOutTime");
        requirePrice(totalPrice, "totalPrice");

        parkingSession.setCheckOutTime(checkOutTime);
        parkingSession.setLicensePlateOut(TextValidationUtils.normalizeRequiredText(licensePlateOut, "licensePlateOut", 20));
        parkingSession.setTotalPrice(totalPrice);
        parkingSession.setStatus(ParkingSessionStatus.CLOSED);
        validateState(parkingSession);
    }

    public void markLostCard(ParkingSession parkingSession, Instant checkOutTime, BigDecimal totalPrice) {
        requireStatus(parkingSession, ParkingSessionStatus.OPEN);
        requireField(checkOutTime, "checkOutTime");

        parkingSession.setCheckOutTime(checkOutTime);
        parkingSession.setTotalPrice(normalizePrice(totalPrice));
        parkingSession.setStatus(ParkingSessionStatus.LOST_CARD);
        validateState(parkingSession);
    }

    public void cancel(ParkingSession parkingSession) {
        requireStatus(parkingSession, ParkingSessionStatus.OPEN);
        parkingSession.setCheckOutTime(null);
        parkingSession.setLicensePlateOut(null);
        parkingSession.setTotalPrice(null);
        parkingSession.setStatus(ParkingSessionStatus.CANCELLED);
        validateState(parkingSession);
    }

    public void validateState(ParkingSession parkingSession) {
        requireParkingSession(parkingSession);
        requireField(parkingSession.getCardId(), "cardId");
        requireField(parkingSession.getVehicleTypeId(), "vehicleTypeId");
        requireField(parkingSession.getCheckInTime(), "checkInTime");
        requireField(parkingSession.getStatus(), "status");
        parkingSession.setLicensePlateIn(TextValidationUtils.normalizeRequiredText(parkingSession.getLicensePlateIn(), "licensePlateIn", 20));
        parkingSession.setLicensePlateOut(TextValidationUtils.normalizeNullableText(parkingSession.getLicensePlateOut(), "licensePlateOut", 20));
        parkingSession.setTotalPrice(normalizePrice(parkingSession.getTotalPrice()));

        if (parkingSession.getCheckOutTime() != null
                && parkingSession.getCheckOutTime().isBefore(parkingSession.getCheckInTime())) {
            throw new BadRequestException("checkOutTime must not be before checkInTime");
        }

        switch (parkingSession.getStatus()) {
            case OPEN -> {
                if (parkingSession.getCheckOutTime() != null
                        || parkingSession.getLicensePlateOut() != null
                        || parkingSession.getTotalPrice() != null) {
                    throw new BadRequestException(
                            "Open parkingSession must not have checkOutTime, licensePlateOut, or totalPrice");
                }
            }
            case CLOSED -> {
                requireField(parkingSession.getCheckOutTime(), "checkOutTime");
                parkingSession.setLicensePlateOut(
                        TextValidationUtils.normalizeRequiredText(parkingSession.getLicensePlateOut(), "licensePlateOut", 20));
                requirePrice(parkingSession.getTotalPrice(), "totalPrice");
            }
            case LOST_CARD -> requireField(parkingSession.getCheckOutTime(), "checkOutTime");
            case CANCELLED -> {
                if (parkingSession.getCheckOutTime() != null
                        || parkingSession.getLicensePlateOut() != null
                        || parkingSession.getTotalPrice() != null) {
                    throw new BadRequestException(
                            "Cancelled parkingSession must not have checkOutTime, licensePlateOut, or totalPrice");
                }
            }
        }
    }

    private void requireStatus(ParkingSession parkingSession, ParkingSessionStatus expectedStatus) {
        requireParkingSession(parkingSession);
        if (parkingSession.getStatus() != expectedStatus) {
            throw new BadRequestException("ParkingSession must be in " + expectedStatus + " status");
        }
    }

    private BigDecimal normalizePrice(BigDecimal price) {
        if (price == null) {
            return null;
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("totalPrice must not be negative");
        }
        return price;
    }

    private void requirePrice(BigDecimal price, String fieldName) {
        requireField(price, fieldName);
        normalizePrice(price);
    }

    private void requireParkingSession(ParkingSession parkingSession) {
        requireField(parkingSession, "parkingSession");
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }

}

