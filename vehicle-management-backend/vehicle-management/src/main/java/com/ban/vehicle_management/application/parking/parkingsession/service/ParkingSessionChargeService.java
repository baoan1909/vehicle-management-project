package com.ban.vehicle_management.application.parking.parkingsession.service;

import com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut;
import com.ban.vehicle_management.application.catalog.pricerule.port.out.PriceRulePortOut;
import com.ban.vehicle_management.application.parking.customerreservation.port.out.CustomerReservationPortOut;
import com.ban.vehicle_management.domain.catalog.pricerule.model.PriceRule;
import com.ban.vehicle_management.domain.parking.customerreservation.model.CustomerReservation;
import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import com.ban.vehicle_management.domain.parking.parkingsession.policy.ParkingCheckoutPricePolicy;
import com.ban.vehicle_management.shared.enumeration.billing.InvoicePurpose;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.stereotype.Service;

@Service
public class ParkingSessionChargeService {
    private static final LocalTime DAY_REFERENCE_TIME = LocalTime.NOON;
    private static final LocalTime NIGHT_REFERENCE_TIME = LocalTime.MIDNIGHT;

    private final PriceRulePortOut priceRules;
    private final CustomerReservationPortOut reservations;
    private final InvoicePortOut invoices;
    private final ParkingCheckoutPricePolicy pricePolicy = new ParkingCheckoutPricePolicy();

    public ParkingSessionChargeService(
            PriceRulePortOut priceRules,
            CustomerReservationPortOut reservations,
            InvoicePortOut invoices
    ) {
        this.priceRules = priceRules;
        this.reservations = reservations;
        this.invoices = invoices;
    }

    public BigDecimal grossAmount(ParkingSession session, Instant checkOutTime) {
        CustomerReservation reservation = reservationFor(session);
        if (reservation != null) {
            if (reservation.getPaidAt() == null) {
                throw new ConflictException("Reservation payment time is missing");
            }
            return pricePolicy.calculateVisitorPrice(
                    LocalDateTime.ofInstant(reservation.getPaidAt(), DateTimeUtils.VIETNAM_ZONE),
                    LocalDateTime.ofInstant(checkOutTime, DateTimeUtils.VIETNAM_ZONE),
                    reservation.getDayPrice(), reservation.getNightPrice(),
                    reservation.getDayStart(), reservation.getDayEnd());
        }

        LocalDate effectiveDate = DateTimeUtils.toVietnamLocalDate(checkOutTime);
        PriceRule dayRule = priceRules.findActiveVisitorRuleByTime(
                        session.getParkingLotId(), session.getVehicleTypeId(), effectiveDate,
                        DAY_REFERENCE_TIME)
                .orElseThrow(() -> new NotFoundException("Active day parking price rule not found"));
        PriceRule nightRule = priceRules.findActiveVisitorRuleByTime(
                        session.getParkingLotId(), session.getVehicleTypeId(), effectiveDate,
                        NIGHT_REFERENCE_TIME)
                .orElseThrow(() -> new NotFoundException("Active night parking price rule not found"));

        return pricePolicy.calculateVisitorPrice(
                LocalDateTime.ofInstant(session.getCheckInTime(), DateTimeUtils.VIETNAM_ZONE),
                LocalDateTime.ofInstant(checkOutTime, DateTimeUtils.VIETNAM_ZONE),
                dayRule.getBasePrice(), nightRule.getBasePrice(),
                dayRule.getTimeFrom(), dayRule.getTimeTo());
    }

    public BigDecimal successfulPrepaidAmount(ParkingSession session) {
        CustomerReservation reservation = reservationFor(session);
        BigDecimal reservationAmount = reservation == null
                ? BigDecimal.ZERO
                : reservation.getPrepaidAmount();
        BigDecimal sessionPrepayments = invoices.sumFinalAmountByParkingSessionIdAndPurposeAndStatus(
                session.getParkingSessionId(),
                InvoicePurpose.PARKING_SESSION_PREPAYMENT,
                InvoiceStatus.PAID);
        return reservationAmount.add(sessionPrepayments == null ? BigDecimal.ZERO : sessionPrepayments);
    }

    public BigDecimal amountDue(ParkingSession session, Instant checkOutTime) {
        return grossAmount(session, checkOutTime)
                .subtract(successfulPrepaidAmount(session))
                .max(BigDecimal.ZERO);
    }

    private CustomerReservation reservationFor(ParkingSession session) {
        return reservations.findBySessionId(session.getParkingSessionId()).orElse(null);
    }
}
