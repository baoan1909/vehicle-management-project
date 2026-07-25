package com.ban.vehicle_management.application.parking.parkingsession.usecase;

import com.ban.vehicle_management.application.accesscontrol.card.port.out.CardPortOut;
import com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut;
import com.ban.vehicle_management.application.catalog.pricerule.port.out.PriceRulePortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.parking.gate.port.out.GatePortOut;
import com.ban.vehicle_management.application.parking.lane.port.out.LanePortOut;
import com.ban.vehicle_management.application.parking.parkingevent.port.out.ParkingEventPortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.authorization.ParkingSessionAccessGuard;
import com.ban.vehicle_management.application.parking.parkingsession.mapper.ParkingCheckOutMapper;
import com.ban.vehicle_management.application.parking.parkingsession.model.command.CheckOutCommand;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.CheckOutPreviewResult;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.CheckOutResult;
import com.ban.vehicle_management.application.parking.parkingsession.port.in.ParkingCheckoutCompletionPortIn;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.application.storage.model.StoreFileCommand;
import com.ban.vehicle_management.application.storage.model.StoredFile;
import com.ban.vehicle_management.application.storage.port.out.FileAccessPort;
import com.ban.vehicle_management.application.storage.port.out.FileStoragePort;
import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.domain.accesscontrol.card.policy.CardPolicy;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.billing.invoice.policy.InvoicePolicy;
import com.ban.vehicle_management.domain.catalog.pricerule.model.PriceRule;
import com.ban.vehicle_management.domain.parking.gate.model.Gate;
import com.ban.vehicle_management.domain.parking.lane.model.Lane;
import com.ban.vehicle_management.domain.parking.parkingevent.model.ParkingEvent;
import com.ban.vehicle_management.domain.parking.parkingevent.policy.ParkingEventPolicy;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import com.ban.vehicle_management.domain.parking.parkingsession.policy.ParkingCheckOutPolicy;
import com.ban.vehicle_management.domain.parking.parkingsession.policy.ParkingCheckoutPricePolicy;
import com.ban.vehicle_management.domain.parking.parkingsession.policy.ParkingLicensePlatePolicy;
import com.ban.vehicle_management.domain.parking.parkingsession.policy.ParkingSessionPolicy;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingEventType;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;
import com.ban.vehicle_management.shared.enumeration.storage.StorageBucket;
import com.ban.vehicle_management.shared.enumeration.storage.StorageFolder;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ParkingCheckOutUseCaseImpl implements ParkingCheckoutCompletionPortIn {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParkingCheckOutUseCaseImpl.class);
    private static final String PARKING_EVENT_RESOURCE_TYPE = "parking.parking_events";
    private static final String CUSTOMER_TYPE_VISITOR = "VISITOR";
    private static final String CUSTOMER_TYPE_SUBSCRIPTION = "SUBSCRIPTION";
    private static final String BARRIER_ACTION_OPEN = "OPEN";
    private static final String BARRIER_ACTION_WAIT_PAYMENT = "WAIT_PAYMENT";
    private static final int CHECK_IN_IMAGE_READ_URL_EXPIRE_SECONDS = 15 * 60;
    private static final LocalTime DAY_REFERENCE_TIME = LocalTime.NOON;
    private static final LocalTime NIGHT_REFERENCE_TIME = LocalTime.MIDNIGHT;
    private static final DateTimeFormatter INVOICE_NO_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyyMMddHHmmss")
            .withZone(DateTimeUtils.VIETNAM_ZONE);
    private static final List<InvoiceStatus> ACTIVE_INVOICE_STATUSES = List.of(
            InvoiceStatus.UNPAID,
            InvoiceStatus.PAID
    );

    private final ParkingSessionAccessGuard parkingSessionAccessGuard;
    private final CurrentAccountPortIn currentAccountPortIn;
    private final CardPortOut cardPortOut;
    private final LanePortOut lanePortOut;
    private final GatePortOut gatePortOut;
    private final ZonePortOut zonePortOut;
    private final ParkingLotPortOut parkingLotPortOut;
    private final ParkingSessionPortOut parkingSessionPortOut;
    private final ParkingEventPortOut parkingEventPortOut;
    private final PriceRulePortOut priceRulePortOut;
    private final InvoicePortOut invoicePortOut;
    private final ParkingCheckOutMapper parkingCheckOutMapper;
    private final FileStoragePort fileStoragePort;
    private final FileAccessPort fileAccessPort;
    private final CardPolicy cardPolicy = new CardPolicy();
    private final ParkingCheckOutPolicy parkingCheckOutPolicy = new ParkingCheckOutPolicy();
    private final ParkingCheckoutPricePolicy parkingCheckoutPricePolicy = new ParkingCheckoutPricePolicy();
    private final ParkingLicensePlatePolicy licensePlatePolicy = new ParkingLicensePlatePolicy();
    private final ParkingSessionPolicy parkingSessionPolicy = new ParkingSessionPolicy();
    private final ParkingEventPolicy parkingEventPolicy = new ParkingEventPolicy();
    private final InvoicePolicy invoicePolicy = new InvoicePolicy();

    public ParkingCheckOutUseCaseImpl(
            ParkingSessionAccessGuard parkingSessionAccessGuard,
            CurrentAccountPortIn currentAccountPortIn,
            CardPortOut cardPortOut,
            LanePortOut lanePortOut,
            GatePortOut gatePortOut,
            ZonePortOut zonePortOut,
            ParkingLotPortOut parkingLotPortOut,
            ParkingSessionPortOut parkingSessionPortOut,
            ParkingEventPortOut parkingEventPortOut,
            PriceRulePortOut priceRulePortOut,
            InvoicePortOut invoicePortOut,
            ParkingCheckOutMapper parkingCheckOutMapper,
            FileStoragePort fileStoragePort,
            FileAccessPort fileAccessPort
    ) {
        this.parkingSessionAccessGuard = parkingSessionAccessGuard;
        this.currentAccountPortIn = currentAccountPortIn;
        this.cardPortOut = cardPortOut;
        this.lanePortOut = lanePortOut;
        this.gatePortOut = gatePortOut;
        this.zonePortOut = zonePortOut;
        this.parkingLotPortOut = parkingLotPortOut;
        this.parkingSessionPortOut = parkingSessionPortOut;
        this.parkingEventPortOut = parkingEventPortOut;
        this.priceRulePortOut = priceRulePortOut;
        this.invoicePortOut = invoicePortOut;
        this.parkingCheckOutMapper = parkingCheckOutMapper;
        this.fileStoragePort = fileStoragePort;
        this.fileAccessPort = fileAccessPort;
    }

    @Transactional
    public CheckOutResult checkOut(CheckOutCommand command) {
        parkingSessionAccessGuard.ensureCanCheckOut();
        requireCommand(command);
        requireLicensePlateImage(command.licensePlateImage());
        requirePersonImage(command.personImage());

        Instant now = Instant.now();
        String cardUid = TextValidationUtils.normalizeRequiredText(command.cardUid(), "cardUid", 100);
        String licensePlate = licensePlatePolicy.normalizeRequired(command.licensePlate(), "licensePlate");
        String note = TextValidationUtils.normalizeNullableText(command.note(), "note", 0);

        Lane lane = findLane(command.laneId());
        parkingCheckOutPolicy.validateLaneForCheckOut(lane);
        Gate gate = findGate(lane.getGateId());
        Zone zone = findZone(gate.getZoneId());
        ParkingLot parkingLot = findParkingLot(zone.getParkingLotId());
        parkingCheckOutPolicy.validateOperationalTopology(lane, gate, zone, parkingLot);

        Card card = cardPortOut.findByUidForUpdate(cardUid)
                .orElseThrow(() -> new NotFoundException("Card not found"));
        parkingCheckOutPolicy.validateCardCanExit(card);

        ParkingSession parkingSession = parkingSessionPortOut.findOpenByCardId(card.getCardId())
                .orElseThrow(() -> new ConflictException("Open parking session not found for card"));
        if (!licensePlatePolicy.matches(parkingSession.getLicensePlateIn(), licensePlate)) {
            throw new ConflictException("Detected license plate does not match check-in license plate");
        }

        boolean subscriptionSession = isSubscriptionSession(parkingSession);
        if (!subscriptionSession) {
            throw new ConflictException("Visitor checkout must be prepared and paid before completion");
        }
        BigDecimal totalPrice = subscriptionSession
                ? BigDecimal.ZERO
                : calculateVisitorPrice(parkingSession, now);
        String customerType = subscriptionSession ? CUSTOMER_TYPE_SUBSCRIPTION : CUSTOMER_TYPE_VISITOR;
        String barrierAction = subscriptionSession ? BARRIER_ACTION_OPEN : BARRIER_ACTION_WAIT_PAYMENT;

        UUID actorAccountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        UUID parkingEventId = UUID.randomUUID();
        StoredFile storedLicensePlateImage = null;
        StoredFile storedPersonImage = null;

        try {
            parkingSessionPolicy.checkOut(parkingSession, now, licensePlate, totalPrice);
            ParkingSession savedParkingSession = parkingSessionPortOut.save(parkingSession);

            if (subscriptionSession) {
                cardPolicy.markAssignedFromInUse(card);
            } else {
                cardPolicy.release(card);
            }
            cardPortOut.save(card);

            storedLicensePlateImage = storeCheckOutLicensePlateImage(
                    command.licensePlateImage(),
                    parkingEventId,
                    savedParkingSession.getParkingSessionId(),
                    actorAccountId,
                    licensePlate
            );
            storedPersonImage = storeCheckOutPersonImage(
                    command.personImage(),
                    parkingEventId,
                    savedParkingSession.getParkingSessionId(),
                    actorAccountId,
                    licensePlate
            );

            ParkingEvent parkingEvent = parkingCheckOutMapper.toCheckOutEvent(
                    parkingEventId,
                    savedParkingSession.getParkingSessionId(),
                    lane.getLaneId(),
                    licensePlate,
                    storedLicensePlateImage.objectKey(),
                    storedPersonImage.objectKey(),
                    actorAccountId,
                    note,
                    now
            );
            parkingEventPolicy.initialize(parkingEvent);
            ParkingEvent savedParkingEvent = parkingEventPortOut.save(parkingEvent);

            Invoice invoice = subscriptionSession ? null : createParkingSessionInvoice(savedParkingSession, totalPrice, now);

            return new CheckOutResult(
                    savedParkingSession,
                    savedParkingEvent,
                    invoice,
                    customerType,
                    barrierAction
            );
        } catch (RuntimeException exception) {
            deleteStoredFileQuietly(storedLicensePlateImage);
            deleteStoredFileQuietly(storedPersonImage);
            throw exception;
        }
    }

    @Transactional
    public CheckOutResult prepareVisitorCheckOut(CheckOutCommand command) {
        parkingSessionAccessGuard.ensureCanCheckOut();
        requireCommand(command);
        requireLicensePlateImage(command.licensePlateImage());
        requirePersonImage(command.personImage());

        Instant now = Instant.now();
        String cardUid = TextValidationUtils.normalizeRequiredText(command.cardUid(), "cardUid", 100);
        String licensePlate = licensePlatePolicy.normalizeRequired(command.licensePlate(), "licensePlate");
        String note = TextValidationUtils.normalizeNullableText(command.note(), "note", 0);

        Lane lane = findLane(command.laneId());
        parkingCheckOutPolicy.validateLaneForCheckOut(lane);
        Gate gate = findGate(lane.getGateId());
        Zone zone = findZone(gate.getZoneId());
        ParkingLot parkingLot = findParkingLot(zone.getParkingLotId());
        parkingCheckOutPolicy.validateOperationalTopology(lane, gate, zone, parkingLot);

        Card card = cardPortOut.findByUidForUpdate(cardUid)
                .orElseThrow(() -> new NotFoundException("Card not found"));
        parkingCheckOutPolicy.validateCardCanExit(card);

        ParkingSession parkingSession = parkingSessionPortOut.findOpenByCardId(card.getCardId())
                .orElseThrow(() -> new ConflictException("Open parking session not found for card"));
        if (!licensePlatePolicy.matches(parkingSession.getLicensePlateIn(), licensePlate)) {
            throw new ConflictException("Detected license plate does not match check-in license plate");
        }
        if (isSubscriptionSession(parkingSession)) {
            throw new ConflictException("Subscription checkout does not require payment preparation");
        }

        Invoice existingInvoice = invoicePortOut.findFirstByParkingSessionIdAndStatusIn(
                        parkingSession.getParkingSessionId(),
                        ACTIVE_INVOICE_STATUSES
                )
                .orElse(null);
        if (existingInvoice != null) {
            ParkingEvent pendingEvent = findPendingCheckOutEvent(parkingSession.getParkingSessionId());
            resolveParkingEventImageUrls(pendingEvent);
            return new CheckOutResult(
                    parkingSession,
                    pendingEvent,
                    existingInvoice,
                    CUSTOMER_TYPE_VISITOR,
                    BARRIER_ACTION_WAIT_PAYMENT
            );
        }

        BigDecimal totalPrice = calculateVisitorPrice(parkingSession, now);
        UUID actorAccountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        UUID parkingEventId = UUID.randomUUID();
        StoredFile storedLicensePlateImage = null;
        StoredFile storedPersonImage = null;

        try {
            storedLicensePlateImage = storeCheckOutLicensePlateImage(
                    command.licensePlateImage(),
                    parkingEventId,
                    parkingSession.getParkingSessionId(),
                    actorAccountId,
                    licensePlate
            );
            storedPersonImage = storeCheckOutPersonImage(
                    command.personImage(),
                    parkingEventId,
                    parkingSession.getParkingSessionId(),
                    actorAccountId,
                    licensePlate
            );

            ParkingEvent pendingEvent = parkingCheckOutMapper.toCheckOutEvent(
                    parkingEventId,
                    parkingSession.getParkingSessionId(),
                    lane.getLaneId(),
                    licensePlate,
                    storedLicensePlateImage.objectKey(),
                    storedPersonImage.objectKey(),
                    actorAccountId,
                    note,
                    now
            );
            pendingEvent.setEventType(ParkingEventType.CHECK_OUT_PENDING);
            parkingEventPolicy.initialize(pendingEvent);
            ParkingEvent savedPendingEvent = parkingEventPortOut.save(pendingEvent);
            Invoice invoice = createParkingSessionInvoice(parkingSession, totalPrice, now);
            resolveParkingEventImageUrls(savedPendingEvent);

            return new CheckOutResult(
                    parkingSession,
                    savedPendingEvent,
                    invoice,
                    CUSTOMER_TYPE_VISITOR,
                    BARRIER_ACTION_WAIT_PAYMENT
            );
        } catch (RuntimeException exception) {
            deleteStoredFileQuietly(storedLicensePlateImage);
            deleteStoredFileQuietly(storedPersonImage);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public CheckOutResult getCheckOutByInvoice(UUID invoiceId) {
        parkingSessionAccessGuard.ensureCanCheckOut();
        Invoice invoice = invoicePortOut.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found"));
        if (invoice.getParkingSessionId() == null) {
            throw new ConflictException("Invoice is not linked to a parking session");
        }

        ParkingSession parkingSession = parkingSessionPortOut.findById(invoice.getParkingSessionId())
                .orElseThrow(() -> new NotFoundException("Parking session not found"));
        ParkingEvent parkingEvent = findCheckOutEvent(parkingSession);
        resolveParkingEventImageUrls(parkingEvent);

        return new CheckOutResult(
                parkingSession,
                parkingEvent,
                invoice,
                CUSTOMER_TYPE_VISITOR,
                ParkingSessionStatus.CLOSED.equals(parkingSession.getStatus())
                        ? BARRIER_ACTION_OPEN
                        : BARRIER_ACTION_WAIT_PAYMENT
        );
    }

    @Override
    @Transactional
    public void completePaidCheckout(UUID invoiceId) {
        Invoice invoice = invoicePortOut.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found"));
        if (invoice.getParkingSessionId() == null) {
            return;
        }
        if (!InvoiceStatus.PAID.equals(invoice.getStatus())) {
            throw new ConflictException("Parking checkout invoice must be paid before completion");
        }

        ParkingSession parkingSession = parkingSessionPortOut.findByIdForUpdate(invoice.getParkingSessionId())
                .orElseThrow(() -> new NotFoundException("Parking session not found"));
        if (ParkingSessionStatus.CLOSED.equals(parkingSession.getStatus())) {
            return;
        }
        if (!ParkingSessionStatus.OPEN.equals(parkingSession.getStatus())) {
            throw new ConflictException("Parking session is not open for checkout");
        }

        ParkingEvent pendingEvent = findPendingCheckOutEvent(parkingSession.getParkingSessionId());
        Instant checkOutTime = invoice.getPaidAt() == null ? Instant.now() : invoice.getPaidAt();
        parkingSessionPolicy.checkOut(
                parkingSession,
                checkOutTime,
                pendingEvent.getLicensePlateDetected(),
                invoice.getFinalAmount()
        );
        parkingSessionPortOut.save(parkingSession);

        Card card = cardPortOut.findByIdForUpdate(parkingSession.getCardId())
                .orElseThrow(() -> new NotFoundException("Card not found"));
        cardPolicy.release(card);
        cardPortOut.save(card);

        pendingEvent.setEventType(ParkingEventType.CHECK_OUT);
        pendingEvent.setEventTime(checkOutTime);
        parkingEventPolicy.validateState(pendingEvent);
        parkingEventPortOut.save(pendingEvent);
    }

    @Transactional(readOnly = true)
    public CheckOutPreviewResult previewCheckOutByCardUid(String rawCardUid) {
        parkingSessionAccessGuard.ensureCanCheckOut();
        String cardUid = TextValidationUtils.normalizeRequiredText(rawCardUid, "cardUid", 100);
        Card card = cardPortOut.findByUid(cardUid)
                .orElseThrow(() -> new NotFoundException("Card not found"));
        parkingCheckOutPolicy.validateCardCanExit(card);

        ParkingSession parkingSession = parkingSessionPortOut.findOpenByCardId(card.getCardId())
                .orElseThrow(() -> new ConflictException("Open parking session not found for card"));
        ParkingEvent checkInEvent = parkingEventPortOut
                .findLatestBySessionIdAndEventType(parkingSession.getParkingSessionId(), ParkingEventType.CHECK_IN)
                .orElse(null);
        resolveCheckInEventImageUrls(checkInEvent);
        Instant now = Instant.now();
        boolean subscriptionSession = isSubscriptionSession(parkingSession);
        BigDecimal estimatedTotalPrice = BigDecimal.ZERO;
        String pricingMessage = null;
        if (!subscriptionSession) {
            try {
                estimatedTotalPrice = calculateVisitorPrice(parkingSession, now);
            } catch (NotFoundException exception) {
                estimatedTotalPrice = null;
                pricingMessage = exception.getMessage();
            }
        }
        String customerType = subscriptionSession ? CUSTOMER_TYPE_SUBSCRIPTION : CUSTOMER_TYPE_VISITOR;

        return new CheckOutPreviewResult(parkingSession, checkInEvent, estimatedTotalPrice, now, customerType, pricingMessage);
    }

    private void resolveCheckInEventImageUrls(ParkingEvent checkInEvent) {
        if (checkInEvent == null) {
            return;
        }
        checkInEvent.setLicensePlateImagePath(resolvePrivateReadUrl(checkInEvent.getLicensePlateImagePath()));
        checkInEvent.setPersonImagePath(resolvePrivateReadUrl(checkInEvent.getPersonImagePath()));
    }

    private void resolveParkingEventImageUrls(ParkingEvent parkingEvent) {
        if (parkingEvent == null) {
            return;
        }
        parkingEvent.setLicensePlateImagePath(resolvePrivateReadUrl(parkingEvent.getLicensePlateImagePath()));
        parkingEvent.setPersonImagePath(resolvePrivateReadUrl(parkingEvent.getPersonImagePath()));
    }

    private ParkingEvent findPendingCheckOutEvent(UUID parkingSessionId) {
        return parkingEventPortOut
                .findLatestBySessionIdAndEventType(parkingSessionId, ParkingEventType.CHECK_OUT_PENDING)
                .orElseThrow(() -> new ConflictException("Pending checkout evidence not found"));
    }

    private ParkingEvent findCheckOutEvent(ParkingSession parkingSession) {
        ParkingEventType eventType = ParkingSessionStatus.CLOSED.equals(parkingSession.getStatus())
                ? ParkingEventType.CHECK_OUT
                : ParkingEventType.CHECK_OUT_PENDING;
        return parkingEventPortOut
                .findLatestBySessionIdAndEventType(parkingSession.getParkingSessionId(), eventType)
                .orElseThrow(() -> new ConflictException("Checkout evidence not found"));
    }

    private String resolvePrivateReadUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank() || isBrowserReachableUrl(objectKey)) {
            return objectKey;
        }
        return fileAccessPort.createReadUrl(objectKey, CHECK_IN_IMAGE_READ_URL_EXPIRE_SECONDS);
    }

    private boolean isBrowserReachableUrl(String value) {
        String normalized = value.toLowerCase();
        return normalized.startsWith("http://") || normalized.startsWith("https://") || normalized.startsWith("/");
    }

    private BigDecimal calculateVisitorPrice(ParkingSession parkingSession, Instant checkOutTime) {
        LocalDate effectiveDate = DateTimeUtils.toVietnamLocalDate(checkOutTime);
        PriceRule dayRule = priceRulePortOut.findActiveVisitorRuleByTime(
                        parkingSession.getVehicleTypeId(),
                        effectiveDate,
                        DAY_REFERENCE_TIME
                )
                .orElseThrow(() -> new NotFoundException("Active day parking price rule not found"));
        PriceRule nightRule = priceRulePortOut.findActiveVisitorRuleByTime(
                        parkingSession.getVehicleTypeId(),
                        effectiveDate,
                        NIGHT_REFERENCE_TIME
                )
                .orElseThrow(() -> new NotFoundException("Active night parking price rule not found"));

        LocalDateTime checkInLocalTime = LocalDateTime.ofInstant(
                parkingSession.getCheckInTime(),
                DateTimeUtils.VIETNAM_ZONE
        );
        LocalDateTime checkOutLocalTime = LocalDateTime.ofInstant(checkOutTime, DateTimeUtils.VIETNAM_ZONE);

        return parkingCheckoutPricePolicy.calculateVisitorPrice(
                checkInLocalTime,
                checkOutLocalTime,
                dayRule.getBasePrice(),
                nightRule.getBasePrice(),
                dayRule.getTimeFrom(),
                dayRule.getTimeTo()
        );
    }

    private Invoice createParkingSessionInvoice(ParkingSession parkingSession, BigDecimal totalPrice, Instant issuedAt) {
        if (totalPrice.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        if (invoicePortOut.existsByParkingSessionIdAndStatusIn(
                parkingSession.getParkingSessionId(),
                ACTIVE_INVOICE_STATUSES
        )) {
            throw new ConflictException("Active invoice already exists for parking session");
        }

        Invoice invoice = new Invoice();
        invoice.setInvoiceId(UUID.randomUUID());
        invoice.setCustomerId(parkingSession.getCustomerId());
        invoice.setParkingSessionId(parkingSession.getParkingSessionId());
        invoice.setAmount(totalPrice);
        invoice.setDiscountAmount(BigDecimal.ZERO);

        invoicePolicy.initializeNewInvoice(invoice, generateInvoiceNo(invoice.getInvoiceId(), issuedAt), issuedAt);
        return invoicePortOut.save(invoice);
    }

    private StoredFile storeCheckOutLicensePlateImage(
            MultipartFile licensePlateImage,
            UUID parkingEventId,
            UUID parkingSessionId,
            UUID actorAccountId,
            String licensePlate
    ) {
        if (licensePlateImage == null || licensePlateImage.isEmpty()) {
            throw new BadRequestException("licensePlateImage must not be empty");
        }
        return fileStoragePort.store(new StoreFileCommand(
                licensePlateImage,
                StorageBucket.PRIVATE,
                StorageFolder.PARKING_EVENT,
                PARKING_EVENT_RESOURCE_TYPE,
                parkingEventId,
                actorAccountId,
                Map.of(
                        "event_type", "CHECK_OUT",
                        "image_type", "LICENSE_PLATE",
                        "license_plate", licensePlate,
                        "parking_session_id", parkingSessionId.toString()
                )
        ));
    }

    private StoredFile storeCheckOutPersonImage(
            MultipartFile personImage,
            UUID parkingEventId,
            UUID parkingSessionId,
            UUID actorAccountId,
            String licensePlate
    ) {
        if (personImage == null || personImage.isEmpty()) {
            throw new BadRequestException("personImage must not be empty");
        }
        return fileStoragePort.store(new StoreFileCommand(
                personImage,
                StorageBucket.PRIVATE,
                StorageFolder.PARKING_EVENT,
                PARKING_EVENT_RESOURCE_TYPE,
                parkingEventId,
                actorAccountId,
                Map.of(
                        "event_type", "CHECK_OUT",
                        "image_type", "PERSON",
                        "license_plate", licensePlate,
                        "parking_session_id", parkingSessionId.toString()
                )
        ));
    }

    private void deleteStoredFileQuietly(StoredFile storedFile) {
        if (storedFile == null || storedFile.objectKey() == null || storedFile.objectKey().isBlank()) {
            return;
        }
        try {
            fileStoragePort.delete(storedFile.objectKey());
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to delete parking check-out image after transaction failure", exception);
        }
    }

    private Lane findLane(UUID laneId) {
        requireField(laneId, "laneId");
        return lanePortOut.findById(laneId)
                .orElseThrow(() -> new NotFoundException("Lane not found"));
    }

    private Gate findGate(UUID gateId) {
        if (gateId == null) {
            throw new ConflictException("Lane is not linked to a gate");
        }
        return gatePortOut.findById(gateId)
                .orElseThrow(() -> new NotFoundException("Gate not found"));
    }

    private Zone findZone(UUID zoneId) {
        if (zoneId == null) {
            throw new ConflictException("Gate is not linked to a zone");
        }
        return zonePortOut.findById(zoneId)
                .orElseThrow(() -> new NotFoundException("Zone not found"));
    }

    private ParkingLot findParkingLot(UUID parkingLotId) {
        if (parkingLotId == null) {
            throw new ConflictException("Zone is not linked to a parking lot");
        }
        return parkingLotPortOut.findById(parkingLotId)
                .orElseThrow(() -> new NotFoundException("Parking lot not found"));
    }

    private String generateInvoiceNo(UUID invoiceId, Instant now) {
        String suffix = invoiceId.toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();
        return "INV-" + INVOICE_NO_TIME_FORMATTER.format(now) + "-" + suffix;
    }

    private boolean isSubscriptionSession(ParkingSession parkingSession) {
        return parkingSession.getCustomerId() != null || parkingSession.getCustomerVehicleId() != null;
    }

    private void requireCommand(CheckOutCommand command) {
        requireField(command, "checkOutCommand");
    }

    private void requireLicensePlateImage(MultipartFile licensePlateImage) {
        if (licensePlateImage == null || licensePlateImage.isEmpty()) {
            throw new BadRequestException("licensePlateImage must not be empty");
        }
    }

    private void requirePersonImage(MultipartFile personImage) {
        if (personImage == null || personImage.isEmpty()) {
            throw new BadRequestException("personImage must not be empty");
        }
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }
}
