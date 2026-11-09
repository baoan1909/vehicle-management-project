package com.ban.vehicle_management.application.parking.customerreservation.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionQrImagePortOut;
import com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut;
import com.ban.vehicle_management.application.catalog.availability.port.out.ParkingLotCatalogAvailabilityPortOut;
import com.ban.vehicle_management.application.catalog.pricerule.port.out.PriceRulePortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.parking.availability.ParkingAvailabilityQuery;
import com.ban.vehicle_management.application.parking.customerreservation.port.out.CustomerReservationPortOut;
import com.ban.vehicle_management.application.parking.customerreservation.service.ReservationQrService;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkinglevel.port.out.ParkingLevelPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.application.parking.parkingspace.port.out.ParkingSpacePortOut;
import com.ban.vehicle_management.application.parking.parkingspaceallocation.port.out.ParkingSpaceAllocationPortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.application.people.customer.authorization.CustomerIdentityResolver;
import com.ban.vehicle_management.domain.parking.customerreservation.model.CustomerReservation;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.parking.parkinglevel.model.ParkingLevel;
import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.shared.enumeration.parking.CustomerReservationStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomerReservationUseCaseImplTest {
    private CustomerReservationPortOut reservations;
    private SubscriptionQrImagePortOut qrImages;
    private ParkingLotPortOut lots;
    private ZonePortOut zones;
    private ParkingLevelPortOut levels;
    private ParkingSpacePortOut spaces;
    private ParkingSessionPortOut sessions;
    private ParkingLotCatalogAvailabilityPortOut catalogAvailability;
    private CustomerReservationUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        reservations = mock(CustomerReservationPortOut.class);
        qrImages = mock(SubscriptionQrImagePortOut.class);
        lots = mock(ParkingLotPortOut.class);
        zones = mock(ZonePortOut.class);
        levels = mock(ParkingLevelPortOut.class);
        spaces = mock(ParkingSpacePortOut.class);
        sessions = mock(ParkingSessionPortOut.class);
        catalogAvailability = mock(ParkingLotCatalogAvailabilityPortOut.class);
        useCase = new CustomerReservationUseCaseImpl(reservations,
                mock(CustomerIdentityResolver.class), mock(CurrentAccountPortIn.class),
                lots, zones, levels,
                spaces, mock(ParkingSpaceAllocationPortOut.class),
                sessions, mock(ParkingAvailabilityQuery.class),
                catalogAvailability, mock(PriceRulePortOut.class),
                mock(InvoicePortOut.class), new ReservationQrService(),
                qrImages);
    }

    @Test
    void paidReservationGetsOneHourFromConfirmedPayment() {
        CustomerReservation reservation = pending();
        Instant paidAt = Instant.now();
        reservation.setPaymentHoldExpiresAt(paidAt.plusSeconds(300));
        when(reservations.findByInvoiceIdForUpdate(reservation.getInvoiceId()))
                .thenReturn(Optional.of(reservation));

        useCase.onPaymentConfirmed(reservation.getInvoiceId(), paidAt);

        assertEquals(CustomerReservationStatus.HELD, reservation.getStatus());
        assertEquals(paidAt.plusSeconds(3600), reservation.getCheckInDeadline());
        verify(reservations).save(reservation);
    }

    @Test
    void latePaymentAfterSchedulerExpirationNeedsManualReview() {
        CustomerReservation reservation = pending();
        reservation.setStatus(CustomerReservationStatus.EXPIRED);
        Instant paidAt = Instant.now();
        when(reservations.findByInvoiceIdForUpdate(reservation.getInvoiceId()))
                .thenReturn(Optional.of(reservation));

        useCase.onPaymentConfirmed(reservation.getInvoiceId(), paidAt);

        assertEquals(CustomerReservationStatus.PAYMENT_REVIEW, reservation.getStatus());
        assertEquals(paidAt, reservation.getPaidAt());
        assertNull(reservation.getCheckInDeadline());
        verify(reservations).save(reservation);
    }

    @Test
    void expiredPaidReservationCannotCheckInByPlate() {
        CustomerReservation reservation = pending();
        reservation.setStatus(CustomerReservationStatus.HELD);
        reservation.setParkingLotId(UUID.randomUUID());
        reservation.setZoneId(UUID.randomUUID());
        reservation.setVehicleTypeId(UUID.randomUUID());
        reservation.setLicensePlate("59A12345");
        reservation.setCheckInDeadline(Instant.now().minusSeconds(1));
        Instant now = Instant.now();
        when(reservations.findHeldByPlateForUpdate(reservation.getParkingLotId(),
                reservation.getLicensePlate(), now)).thenReturn(java.util.List.of(reservation));

        assertThrows(ConflictException.class, () -> useCase.resolveForCheckIn(
                reservation.getParkingLotId(), reservation.getZoneId(),
                reservation.getVehicleTypeId(), reservation.getLicensePlate(), null, now));
    }

    @Test
    void guestCanPayOnlyWithReservationSecret() {
        CustomerReservation reservation = pending();
        reservation.setQrToken(new ReservationQrService().generateToken());
        when(reservations.findById(reservation.getReservationId()))
                .thenReturn(Optional.of(reservation));

        assertEquals(reservation.getInvoiceId(), useCase.authorizeGuestPayment(
                reservation.getReservationId(), reservation.getQrToken()));
        assertThrows(NotFoundException.class, () -> useCase.authorizeGuestPayment(
                reservation.getReservationId(), new ReservationQrService().generateToken()));
    }

    @Test
    void guestCannotCreatePaymentAfterTemporaryHoldExpires() {
        CustomerReservation reservation = pending();
        reservation.setQrToken(new ReservationQrService().generateToken());
        reservation.setPaymentHoldExpiresAt(Instant.now().minusSeconds(1));
        when(reservations.findById(reservation.getReservationId()))
                .thenReturn(Optional.of(reservation));

        assertThrows(ConflictException.class, () -> useCase.authorizeGuestPayment(
                reservation.getReservationId(), reservation.getQrToken()));
    }

    @Test
    void guestReservationDetailIncludesLotLevelZoneAndSpace() {
        CustomerReservation reservation = pending();
        reservation.setQrToken(new ReservationQrService().generateToken());
        reservation.setParkingLotId(UUID.randomUUID());
        reservation.setZoneId(UUID.randomUUID());
        reservation.setParkingSpaceId(UUID.randomUUID());
        ParkingLot lot = new ParkingLot();
        lot.setName("Bãi xe Nông Lâm");
        Zone zone = new Zone();
        zone.setName("Khu A");
        zone.setParkingLevelId(UUID.randomUUID());
        ParkingLevel level = new ParkingLevel();
        level.setName("Tầng 1");
        ParkingSpace space = new ParkingSpace();
        space.setCode("A-06");
        when(reservations.findById(reservation.getReservationId())).thenReturn(Optional.of(reservation));
        when(lots.findById(reservation.getParkingLotId())).thenReturn(Optional.of(lot));
        when(zones.findById(reservation.getZoneId())).thenReturn(Optional.of(zone));
        when(levels.findById(zone.getParkingLevelId())).thenReturn(Optional.of(level));
        when(spaces.findById(reservation.getParkingSpaceId())).thenReturn(Optional.of(space));

        var result = useCase.getGuest(reservation.getReservationId(), reservation.getQrToken());

        assertEquals("Bãi xe Nông Lâm", result.parkingLotName());
        assertEquals("Tầng 1", result.parkingLevelName());
        assertEquals("Khu A", result.zoneName());
        assertEquals("A-06", result.parkingSpaceCode());
    }

    @Test
    void cannotReserveAtLotWhileSamePlateHasOpenSessionThere() {
        UUID lotId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        UUID vehicleTypeId = UUID.randomUUID();
        ParkingLot lot = new ParkingLot();
        lot.setStatus(ParkingLotStatus.ACTIVE);
        Zone zone = new Zone();
        zone.setParkingLotId(lotId);
        zone.setStatus(ZoneStatus.ACTIVE);
        zone.setVehicleTypeId(vehicleTypeId);
        ParkingSession session = new ParkingSession();
        session.setParkingLotId(lotId);
        when(lots.findById(lotId)).thenReturn(Optional.of(lot));
        when(catalogAvailability.isVehicleTypeEnabled(lotId, vehicleTypeId)).thenReturn(true);
        when(zones.findByIdForUpdate(zoneId)).thenReturn(Optional.of(zone));
        when(sessions.findOpenByLicensePlateIn("60A99999")).thenReturn(java.util.List.of(session));

        ConflictException error = assertThrows(ConflictException.class, () -> useCase.createGuest(
                lotId, zoneId, null, vehicleTypeId, "60A-999.99", true));

        assertEquals("This license plate already has an open parking session in the parking lot",
                error.getMessage());
        verify(sessions).findOpenByLicensePlateIn("60A99999");
        verify(reservations, never()).save(any());
    }

    @Test
    void paidGuestReservationIssuesQrForCheckIn() {
        CustomerReservation reservation = pending();
        reservation.setQrToken(new ReservationQrService().generateToken());
        reservation.setParkingLotId(UUID.randomUUID());
        reservation.setZoneId(UUID.randomUUID());
        reservation.setParkingSpaceId(UUID.randomUUID());
        reservation.setVehicleTypeId(UUID.randomUUID());
        reservation.setLicensePlate("60H545645");
        reservation.setStatus(CustomerReservationStatus.HELD);
        reservation.setPaidAt(Instant.now());
        reservation.setCheckInDeadline(Instant.now().plusSeconds(3600));
        when(reservations.findById(reservation.getReservationId()))
                .thenReturn(Optional.of(reservation));
        when(qrImages.encodePngDataUrl(anyString())).thenReturn("data:image/png;base64,test");

        var result = useCase.getGuest(reservation.getReservationId(), reservation.getQrToken());

        assertNotNull(result.qrPayload());
        assertEquals("data:image/png;base64,test", result.qrCodeDataUrl());
        assertEquals(reservation.getQrToken(), result.accessToken());
        when(reservations.findByQrTokenForUpdate(reservation.getQrToken()))
                .thenReturn(Optional.of(reservation));
        assertEquals(reservation, useCase.resolveForCheckIn(reservation.getParkingLotId(),
                reservation.getZoneId(), reservation.getVehicleTypeId(),
                reservation.getLicensePlate(), result.qrPayload(), Instant.now()));
    }

    @Test
    void accountReservationQrCannotAuthorizeGuestCancellation() {
        CustomerReservation reservation = pending();
        reservation.setCustomerId(UUID.randomUUID());
        reservation.setQrToken(new ReservationQrService().generateToken());
        when(reservations.findByIdForUpdate(reservation.getReservationId()))
                .thenReturn(Optional.of(reservation));

        assertThrows(NotFoundException.class, () -> useCase.cancelGuest(
                reservation.getReservationId(), reservation.getQrToken()));
    }

    private CustomerReservation pending() {
        CustomerReservation reservation = new CustomerReservation();
        reservation.setReservationId(UUID.randomUUID());
        reservation.setInvoiceId(UUID.randomUUID());
        reservation.setStatus(CustomerReservationStatus.PENDING_PAYMENT);
        reservation.setPaymentHoldExpiresAt(Instant.now().plusSeconds(300));
        return reservation;
    }
}
