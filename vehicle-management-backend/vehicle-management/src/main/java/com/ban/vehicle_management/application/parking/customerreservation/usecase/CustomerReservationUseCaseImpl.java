package com.ban.vehicle_management.application.parking.customerreservation.usecase;

import com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionQrImagePortOut;
import com.ban.vehicle_management.application.catalog.availability.port.out.ParkingLotCatalogAvailabilityPortOut;
import com.ban.vehicle_management.application.catalog.pricerule.port.out.PriceRulePortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.parking.availability.ParkingAvailabilityQuery;
import com.ban.vehicle_management.application.parking.customerreservation.model.ReservationOption;
import com.ban.vehicle_management.application.parking.customerreservation.model.ReservationView;
import com.ban.vehicle_management.application.parking.customerreservation.port.in.CustomerReservationAdmissionPortIn;
import com.ban.vehicle_management.application.parking.customerreservation.port.in.CustomerReservationExpiryPortIn;
import com.ban.vehicle_management.application.parking.customerreservation.port.in.CustomerReservationPaymentPortIn;
import com.ban.vehicle_management.application.parking.customerreservation.port.in.CustomerReservationPortIn;
import com.ban.vehicle_management.application.parking.customerreservation.port.out.CustomerReservationPortOut;
import com.ban.vehicle_management.application.parking.customerreservation.service.ReservationQrService;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkinglevel.port.out.ParkingLevelPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.application.parking.parkingspace.port.out.ParkingSpacePortOut;
import com.ban.vehicle_management.application.parking.parkingspaceallocation.port.out.ParkingSpaceAllocationPortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.application.people.customer.authorization.CustomerIdentityResolver;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.billing.invoice.policy.InvoicePolicy;
import com.ban.vehicle_management.domain.catalog.pricerule.model.PriceRule;
import com.ban.vehicle_management.domain.common.licenseplate.LicensePlatePolicy;
import com.ban.vehicle_management.domain.parking.customerreservation.model.CustomerReservation;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.parking.parkinglevel.model.ParkingLevel;
import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.domain.parking.parkingspace.policy.ParkingSpacePolicy;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.shared.enumeration.billing.InvoicePurpose;
import com.ban.vehicle_management.shared.enumeration.parking.CustomerReservationStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus;
import com.ban.vehicle_management.shared.enumeration.parking.TrackingMode;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerReservationUseCaseImpl implements CustomerReservationPortIn,
        CustomerReservationPaymentPortIn, CustomerReservationAdmissionPortIn,
        CustomerReservationExpiryPortIn {
    private static final Duration PAYMENT_HOLD = Duration.ofMinutes(10);
    private static final Duration CHECK_IN_WINDOW = Duration.ofHours(1);
    private static final String CUSTOMER_PAYMENT_PERMISSION = "PAYMENT_CREATE_OWN";

    private final CustomerReservationPortOut reservations;
    private final CustomerIdentityResolver customers;
    private final CurrentAccountPortIn accounts;
    private final ParkingLotPortOut lots;
    private final ZonePortOut zones;
    private final ParkingLevelPortOut levels;
    private final ParkingSpacePortOut spaces;
    private final ParkingSpaceAllocationPortOut allocations;
    private final ParkingSessionPortOut sessions;
    private final ParkingAvailabilityQuery availability;
    private final ParkingLotCatalogAvailabilityPortOut catalogAvailability;
    private final PriceRulePortOut priceRules;
    private final InvoicePortOut invoices;
    private final ReservationQrService qr;
    private final SubscriptionQrImagePortOut qrImages;
    private final InvoicePolicy invoicePolicy = new InvoicePolicy();
    private final ParkingSpacePolicy spacePolicy = new ParkingSpacePolicy();
    private final LicensePlatePolicy platePolicy = new LicensePlatePolicy();

    public CustomerReservationUseCaseImpl(CustomerReservationPortOut reservations,
            CustomerIdentityResolver customers, CurrentAccountPortIn accounts,
            ParkingLotPortOut lots, ZonePortOut zones, ParkingLevelPortOut levels,
            ParkingSpacePortOut spaces,
            ParkingSpaceAllocationPortOut allocations, ParkingSessionPortOut sessions,
            ParkingAvailabilityQuery availability,
            ParkingLotCatalogAvailabilityPortOut catalogAvailability,
            PriceRulePortOut priceRules, InvoicePortOut invoices, ReservationQrService qr,
            SubscriptionQrImagePortOut qrImages) {
        this.reservations = reservations;
        this.customers = customers;
        this.accounts = accounts;
        this.lots = lots;
        this.zones = zones;
        this.levels = levels;
        this.spaces = spaces;
        this.allocations = allocations;
        this.sessions = sessions;
        this.availability = availability;
        this.catalogAvailability = catalogAvailability;
        this.priceRules = priceRules;
        this.invoices = invoices;
        this.qr = qr;
        this.qrImages = qrImages;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationOption> findOptions(UUID parkingLotId, UUID vehicleTypeId) {
        requireActiveLotAndVehicle(parkingLotId, vehicleTypeId);
        Instant now = Instant.now();
        BigDecimal prepaid = quote(now, vehicleTypeId).prepaid();
        return zones.findAll(parkingLotId, vehicleTypeId, ZoneStatus.ACTIVE, null).stream()
                .filter(zone -> serves(zone, vehicleTypeId))
                .map(zone -> option(zone, vehicleTypeId, prepaid, now))
                .filter(option -> option.available() > 0)
                .toList();
    }

    @Override
    @Transactional
    public ReservationView create(UUID parkingLotId, UUID zoneId, UUID parkingSpaceId,
            UUID vehicleTypeId, String licensePlate, boolean policyAccepted) {
        accounts.requirePermission(CUSTOMER_PAYMENT_PERMISSION);
        UUID customerId = customers.resolveCurrentApprovedCustomerId();
        return createReservation(parkingLotId, zoneId, parkingSpaceId, vehicleTypeId,
                licensePlate, policyAccepted, customerId);
    }

    @Override
    @Transactional
    public ReservationView createGuest(UUID parkingLotId, UUID zoneId, UUID parkingSpaceId,
            UUID vehicleTypeId, String licensePlate, boolean policyAccepted) {
        return createReservation(parkingLotId, zoneId, parkingSpaceId, vehicleTypeId,
                licensePlate, policyAccepted, null);
    }

    private ReservationView createReservation(UUID parkingLotId, UUID zoneId, UUID parkingSpaceId,
            UUID vehicleTypeId, String licensePlate, boolean policyAccepted, UUID customerId) {
        if (!policyAccepted) {
            throw new BadRequestException("Reservation pricing and no-refund policy must be accepted");
        }
        requireActiveLotAndVehicle(parkingLotId, vehicleTypeId);
        String plate = platePolicy.normalizeRequired(licensePlate, "licensePlate");
        Zone zone = zones.findByIdForUpdate(zoneId)
                .orElseThrow(() -> new NotFoundException("Parking zone not found"));
        if (!parkingLotId.equals(zone.getParkingLotId()) || zone.getStatus() != ZoneStatus.ACTIVE
                || !serves(zone, vehicleTypeId)) {
            throw new ConflictException("Selected zone does not serve this vehicle type");
        }
        Instant now = Instant.now();
        if (reservations.existsActiveByPlateAndZone(zoneId, plate, now)) {
            throw new ConflictException("This license plate already has an active reservation in the zone");
        }
        if (sessions.findOpenByLicensePlateIn(plate).stream()
                .anyMatch(session -> parkingLotId.equals(session.getParkingLotId()))) {
            throw new ConflictException("This license plate already has an open parking session in the parking lot");
        }
        if (zone.getTrackingMode() == TrackingMode.SPACE) {
            validateAndReserveSpace(zone, parkingSpaceId, vehicleTypeId, now);
        } else {
            if (parkingSpaceId != null) {
                throw new BadRequestException("CAPACITY reservation cannot select a parking space");
            }
            long available = availability.get(zone, DateTimeUtils.toVietnamLocalDate(now)).visitorAvailable();
            if (available <= 0) {
                throw new ConflictException("No reservation capacity is available");
            }
        }
        PriceQuote price = quote(now, vehicleTypeId);
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(invoiceId);
        invoice.setInvoiceNo("RSV-" + invoiceId.toString().substring(0, 18).toUpperCase());
        invoice.setCustomerId(customerId);
        invoice.setParkingLotId(parkingLotId);
        invoice.setPurpose(InvoicePurpose.RESERVATION_PREPAYMENT);
        invoice.setAmount(price.prepaid());
        invoice.setDiscountAmount(BigDecimal.ZERO);
        invoicePolicy.initializeNewInvoice(invoice, invoice.getInvoiceNo(), now);
        invoices.save(invoice);

        CustomerReservation reservation = new CustomerReservation();
        reservation.setReservationId(UUID.randomUUID());
        reservation.setCustomerId(customerId);
        reservation.setParkingLotId(parkingLotId);
        reservation.setZoneId(zoneId);
        reservation.setParkingSpaceId(parkingSpaceId);
        reservation.setVehicleTypeId(vehicleTypeId);
        reservation.setLicensePlate(plate);
        reservation.setQrToken(qr.generateToken());
        reservation.setInvoiceId(invoiceId);
        reservation.setStatus(CustomerReservationStatus.PENDING_PAYMENT);
        reservation.setPaymentHoldExpiresAt(now.plus(PAYMENT_HOLD));
        reservation.setPrepaidAmount(price.prepaid());
        reservation.setDayPrice(price.day().getBasePrice());
        reservation.setNightPrice(price.night().getBasePrice());
        reservation.setDayStart(price.day().getTimeFrom());
        reservation.setDayEnd(price.day().getTimeTo());
        reservation.setPolicyVersion("1");
        reservation.setCreatedAt(now);
        return view(reservations.save(reservation));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationView> findMine() {
        accounts.requirePermission(CUSTOMER_PAYMENT_PERMISSION);
        return reservations.findByCustomerId(customers.resolveCurrentApprovedCustomerId())
                .stream().map(this::view).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationView getMine(UUID reservationId) {
        accounts.requirePermission(CUSTOMER_PAYMENT_PERMISSION);
        CustomerReservation reservation = reservations.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("Reservation not found"));
        if (!customers.resolveCurrentApprovedCustomerId().equals(reservation.getCustomerId())) {
            throw new AccessDeniedException("Reservation does not belong to current customer");
        }
        return view(reservation);
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationView getGuest(UUID reservationId, String accessToken) {
        CustomerReservation reservation = reservations.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("Reservation not found"));
        requireGuestAccess(reservation, accessToken);
        return view(reservation);
    }

    @Override
    @Transactional(readOnly = true)
    public UUID authorizeGuestPayment(UUID reservationId, String accessToken) {
        CustomerReservation reservation = reservations.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("Reservation not found"));
        requireGuestAccess(reservation, accessToken);
        if (reservation.getStatus() != CustomerReservationStatus.PENDING_PAYMENT
                || !reservation.getPaymentHoldExpiresAt().isAfter(Instant.now())) {
            throw new ConflictException("Reservation payment hold has expired");
        }
        return reservation.getInvoiceId();
    }

    @Override
    @Transactional
    public ReservationView cancel(UUID reservationId) {
        accounts.requirePermission(CUSTOMER_PAYMENT_PERMISSION);
        CustomerReservation reservation = reservations.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new NotFoundException("Reservation not found"));
        if (!customers.resolveCurrentApprovedCustomerId().equals(reservation.getCustomerId())) {
            throw new AccessDeniedException("Reservation does not belong to current customer");
        }
        return cancelReservation(reservation);
    }

    @Override
    @Transactional
    public ReservationView cancelGuest(UUID reservationId, String accessToken) {
        CustomerReservation reservation = reservations.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new NotFoundException("Reservation not found"));
        requireGuestAccess(reservation, accessToken);
        return cancelReservation(reservation);
    }

    private ReservationView cancelReservation(CustomerReservation reservation) {
        if (reservation.getStatus() != CustomerReservationStatus.HELD) {
            throw new ConflictException("Only a paid, unused reservation can be cancelled");
        }
        reservation.setStatus(CustomerReservationStatus.CANCELLED);
        reservation.setClosedAt(Instant.now());
        reservations.save(reservation);
        releaseSpace(reservation);
        return view(reservation);
    }

    private void requireGuestAccess(CustomerReservation reservation, String accessToken) {
        if (reservation.getCustomerId() != null || accessToken == null || accessToken.length() != 43
                || !MessageDigest.isEqual(
                        reservation.getQrToken().getBytes(StandardCharsets.US_ASCII),
                        accessToken.getBytes(StandardCharsets.US_ASCII))) {
            throw new NotFoundException("Reservation not found");
        }
    }

    @Override
    @Transactional
    public void onPaymentConfirmed(UUID invoiceId, Instant paidAt) {
        CustomerReservation reservation = reservations.findByInvoiceIdForUpdate(invoiceId)
                .orElseThrow(() -> new NotFoundException("Reservation for paid invoice not found"));
        if (reservation.getStatus() == CustomerReservationStatus.EXPIRED) {
            reservation.setPaidAt(paidAt);
            reservation.setStatus(CustomerReservationStatus.PAYMENT_REVIEW);
            reservations.save(reservation);
            return;
        }
        if (reservation.getStatus() != CustomerReservationStatus.PENDING_PAYMENT) {
            return;
        }
        Instant now = Instant.now();
        reservation.setPaidAt(paidAt);
        if (!reservation.getPaymentHoldExpiresAt().isAfter(now)
                || !paidAt.plus(CHECK_IN_WINDOW).isAfter(now)) {
            reservation.setStatus(CustomerReservationStatus.PAYMENT_REVIEW);
            reservation.setClosedAt(now);
            reservations.save(reservation);
            releaseSpace(reservation);
            return;
        }
        reservation.setStatus(CustomerReservationStatus.HELD);
        reservation.setCheckInDeadline(paidAt.plus(CHECK_IN_WINDOW));
        reservations.save(reservation);
    }

    @Override
    @Transactional
    public CustomerReservation resolveForCheckIn(UUID parkingLotId, UUID zoneId,
            UUID vehicleTypeId, String licensePlate, String qrPayload, Instant now) {
        CustomerReservation reservation;
        if (qrPayload != null && !qrPayload.isBlank()) {
            String token = qr.extractToken(qrPayload)
                    .orElseThrow(() -> new BadRequestException("Invalid reservation QR"));
            reservation = reservations.findByQrTokenForUpdate(token)
                    .orElseThrow(() -> new NotFoundException("Reservation QR not found"));
            if (!qr.toPayload(reservation).equals(qrPayload)) {
                throw new BadRequestException("Reservation QR data does not match the paid reservation");
            }
        } else {
            List<CustomerReservation> matches = reservations.findHeldByPlateForUpdate(
                    parkingLotId, platePolicy.normalizeRequired(licensePlate, "licensePlate"), now);
            if (matches.isEmpty()) {
                return null;
            }
            List<CustomerReservation> zoneMatches = matches.stream()
                    .filter(item -> zoneId.equals(item.getZoneId()))
                    .toList();
            if (zoneMatches.isEmpty()) {
                throw new ConflictException("Reservation belongs to another parking zone");
            }
            if (zoneMatches.size() != 1) {
                throw new ConflictException("Multiple active reservations match this license plate");
            }
            reservation = zoneMatches.getFirst();
        }
        if (reservation.getStatus() != CustomerReservationStatus.HELD
                || reservation.getCheckInDeadline() == null
                || !reservation.getCheckInDeadline().isAfter(now)) {
            throw new ConflictException("Reservation is no longer valid for check-in");
        }
        if (!reservation.getParkingLotId().equals(parkingLotId)
                || !reservation.getZoneId().equals(zoneId)
                || !reservation.getVehicleTypeId().equals(vehicleTypeId)
                || !platePolicy.matches(reservation.getLicensePlate(), licensePlate)) {
            throw new ConflictException("Reservation does not match this check-in");
        }
        return reservation;
    }

    @Override
    @Transactional
    public void markCheckedIn(CustomerReservation value, UUID parkingSessionId, Instant now) {
        CustomerReservation reservation = reservations.findByIdForUpdate(value.getReservationId())
                .orElseThrow(() -> new NotFoundException("Reservation not found"));
        if (reservation.getStatus() != CustomerReservationStatus.HELD
                || !reservation.getCheckInDeadline().isAfter(now)) {
            throw new ConflictException("Reservation expired during check-in");
        }
        reservation.setStatus(CustomerReservationStatus.CHECKED_IN);
        reservation.setParkingSessionId(parkingSessionId);
        reservation.setCheckedInAt(now);
        reservations.save(reservation);
    }

    @Override
    @Transactional
    public int expireDue() {
        Instant now = Instant.now();
        int expired = 0;
        for (CustomerReservation candidate : reservations.findExpired(now)) {
            CustomerReservation reservation = reservations.findByIdForUpdate(candidate.getReservationId())
                    .orElse(null);
            if (reservation == null) {
                continue;
            }
            boolean pendingDue = reservation.getStatus() == CustomerReservationStatus.PENDING_PAYMENT
                    && !reservation.getPaymentHoldExpiresAt().isAfter(now);
            boolean paidDue = reservation.getStatus() == CustomerReservationStatus.HELD
                    && !reservation.getCheckInDeadline().isAfter(now);
            if (pendingDue || paidDue) {
                reservation.setStatus(CustomerReservationStatus.EXPIRED);
                reservation.setClosedAt(now);
                reservations.save(reservation);
                releaseSpace(reservation);
                expired++;
            }
        }
        return expired;
    }

    private ReservationOption option(Zone zone, UUID vehicleTypeId, BigDecimal prepaid, Instant now) {
        if (zone.getTrackingMode() == TrackingMode.CAPACITY) {
            long available = availability.get(zone, DateTimeUtils.toVietnamLocalDate(now)).visitorAvailable();
            return new ReservationOption(zone.getZoneId(), zone.getName(), zone.getTrackingMode(),
                    Math.max(0, available), List.of(), prepaid);
        }
        LocalDate date = DateTimeUtils.toVietnamLocalDate(now);
        List<ReservationOption.SpaceOption> eligible = spaces.findByZoneIdAndLifecycleStatus(
                        zone.getZoneId(), "ACTIVE").stream()
                .filter(space -> space.getStatus() == ParkingSpaceStatus.AVAILABLE)
                .filter(space -> space.getVehicleTypeId() == null
                        || space.getVehicleTypeId().equals(vehicleTypeId))
                .filter(space -> !sessions.existsOpenByParkingSpaceId(space.getParkingSpaceId()))
                .filter(space -> allocations.findActiveAllocationsForSpaceOnDate(
                        space.getParkingSpaceId(), date).isEmpty())
                .filter(space -> !reservations.existsActiveBySpace(space.getParkingSpaceId(), now))
                .map(space -> new ReservationOption.SpaceOption(space.getParkingSpaceId(), space.getCode()))
                .toList();
        return new ReservationOption(zone.getZoneId(), zone.getName(), zone.getTrackingMode(),
                eligible.size(), eligible, prepaid);
    }

    private void validateAndReserveSpace(Zone zone, UUID parkingSpaceId, UUID vehicleTypeId, Instant now) {
        if (parkingSpaceId == null) {
            throw new BadRequestException("SPACE reservation requires parkingSpaceId");
        }
        ParkingSpace space = spaces.findByIdForUpdate(parkingSpaceId)
                .orElseThrow(() -> new NotFoundException("Parking space not found"));
        if (!zone.getZoneId().equals(space.getZoneId())
                || !"ACTIVE".equals(space.getLifecycleStatus())
                || space.getStatus() != ParkingSpaceStatus.AVAILABLE
                || space.getVehicleTypeId() != null && !space.getVehicleTypeId().equals(vehicleTypeId)
                || sessions.existsOpenByParkingSpaceId(parkingSpaceId)
                || !allocations.findActiveAllocationsForSpaceOnDate(
                        parkingSpaceId, DateTimeUtils.toVietnamLocalDate(now)).isEmpty()
                || reservations.existsActiveBySpace(parkingSpaceId, now)) {
            throw new ConflictException("Selected parking space is no longer available");
        }
        spacePolicy.setAutomaticStatus(space, ParkingSpaceStatus.RESERVED, null);
        spaces.save(space);
    }

    private void releaseSpace(CustomerReservation reservation) {
        if (reservation.getParkingSpaceId() == null) {
            return;
        }
        ParkingSpace space = spaces.findByIdForUpdate(reservation.getParkingSpaceId()).orElse(null);
        if (space == null || space.getStatus() != ParkingSpaceStatus.RESERVED) {
            return;
        }
        if (!allocations.findActiveAllocationsForSpaceOnDate(space.getParkingSpaceId(),
                DateTimeUtils.toVietnamLocalDate(Instant.now())).isEmpty()) {
            return;
        }
        spacePolicy.setAutomaticStatus(space, ParkingSpaceStatus.AVAILABLE, null);
        spaces.save(space);
    }

    private void requireActiveLotAndVehicle(UUID lotId, UUID vehicleTypeId) {
        if (lotId == null || vehicleTypeId == null) {
            throw new BadRequestException("Parking lot and vehicle type are required");
        }
        ParkingLot lot = lots.findById(lotId)
                .orElseThrow(() -> new NotFoundException("Parking lot not found"));
        if (lot.getStatus() != ParkingLotStatus.ACTIVE
                || !catalogAvailability.isVehicleTypeEnabled(lotId, vehicleTypeId)) {
            throw new ConflictException("Parking lot does not serve this vehicle type");
        }
    }

    private boolean serves(Zone zone, UUID vehicleTypeId) {
        return zone.getVehicleTypeIds() != null && zone.getVehicleTypeIds().contains(vehicleTypeId)
                || vehicleTypeId.equals(zone.getVehicleTypeId());
    }

    private PriceQuote quote(Instant now, UUID vehicleTypeId) {
        LocalDate date = DateTimeUtils.toVietnamLocalDate(now);
        PriceRule day = priceRules.findActiveVisitorRuleByTime(vehicleTypeId, date, LocalTime.NOON)
                .orElseThrow(() -> new NotFoundException("Day visitor price rule not found"));
        PriceRule night = priceRules.findActiveVisitorRuleByTime(vehicleTypeId, date, LocalTime.MIDNIGHT)
                .orElseThrow(() -> new NotFoundException("Night visitor price rule not found"));
        BigDecimal prepaid = day.getBasePrice();
        if (prepaid == null || prepaid.signum() <= 0) {
            throw new ConflictException("Reservation requires a positive first-turn price");
        }
        return new PriceQuote(day, night, prepaid);
    }

    private ReservationView view(CustomerReservation reservation) {
        String payload = reservation.getStatus() == CustomerReservationStatus.HELD
                ? qr.toPayload(reservation) : null;
        String lotName = reservation.getParkingLotId() == null ? null
                : lots.findById(reservation.getParkingLotId()).map(ParkingLot::getName).orElse(null);
        Zone zone = reservation.getZoneId() == null ? null
                : zones.findById(reservation.getZoneId()).orElse(null);
        String levelName = zone == null || zone.getParkingLevelId() == null ? null
                : levels.findById(zone.getParkingLevelId()).map(ParkingLevel::getName).orElse(null);
        String spaceCode = reservation.getParkingSpaceId() == null ? null
                : spaces.findById(reservation.getParkingSpaceId()).map(ParkingSpace::getCode).orElse(null);
        return new ReservationView(reservation.getReservationId(), reservation.getParkingLotId(),
                reservation.getZoneId(), reservation.getParkingSpaceId(), reservation.getVehicleTypeId(),
                reservation.getLicensePlate(), reservation.getStatus(), reservation.getInvoiceId(),
                reservation.getPrepaidAmount(), reservation.getPaymentHoldExpiresAt(),
                reservation.getPaidAt(), reservation.getCheckInDeadline(),
                payload, payload == null ? null : qrImages.encodePngDataUrl(payload),
                reservation.getQrToken(), levelName, zone == null ? null : zone.getName(),
                spaceCode, lotName);
    }

    private record PriceQuote(PriceRule day, PriceRule night, BigDecimal prepaid) { }
}
