package com.ban.vehicle_management.application.parking.parkingsession.usecase;

import com.ban.vehicle_management.application.accesscontrol.card.port.out.CardPortOut;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionPortOut;
import com.ban.vehicle_management.application.accesscontrol.subscription.service.SubscriptionQrTokenService;
import com.ban.vehicle_management.application.catalog.cardtype.port.out.CardTypePortOut;
import com.ban.vehicle_management.application.catalog.availability.port.out.ParkingLotCatalogAvailabilityPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.parking.gate.port.out.GatePortOut;
import com.ban.vehicle_management.application.parking.availability.ParkingAvailabilityQuery;
import com.ban.vehicle_management.application.parking.capacityallocation.port.out.SubscriptionCapacityAllocationPortOut;
import com.ban.vehicle_management.application.parking.lane.port.out.LanePortOut;
import com.ban.vehicle_management.application.parking.parkingevent.port.out.ParkingEventPortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkingspace.port.out.ParkingSpacePortOut;
import com.ban.vehicle_management.application.parking.parkingspaceallocation.port.out.ParkingSpaceAllocationPortOut;
import com.ban.vehicle_management.application.parking.realtime.ParkingRealtimeAfterCommit;
import com.ban.vehicle_management.application.parking.realtime.model.ParkingRealtimeEvent;
import com.ban.vehicle_management.application.parking.realtime.port.out.ParkingRealtimePublisherPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.authorization.ParkingSessionAccessGuard;
import com.ban.vehicle_management.application.parking.parkingsession.mapper.ParkingCheckInMapper;
import com.ban.vehicle_management.application.parking.parkingsession.model.command.CheckInCommand;
import com.ban.vehicle_management.application.parking.parkingsession.model.command.CheckInPreviewCommand;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.CheckInResult;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.CheckInPreviewResult;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.application.people.customer.port.out.CustomerPortOut;
import com.ban.vehicle_management.application.people.customervehicle.port.out.CustomerVehiclePortOut;
import com.ban.vehicle_management.application.storage.model.StoreFileCommand;
import com.ban.vehicle_management.application.storage.model.StoredFile;
import com.ban.vehicle_management.application.storage.port.out.FileStoragePort;
import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.domain.accesscontrol.card.policy.CardPolicy;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.accesscontrol.subscription.policy.SubscriptionQrPolicy;
import com.ban.vehicle_management.domain.common.licenseplate.LicensePlateResolution;
import com.ban.vehicle_management.domain.catalog.cardtype.model.CardType;
import com.ban.vehicle_management.domain.parking.gate.model.Gate;
import com.ban.vehicle_management.domain.parking.lane.model.Lane;
import com.ban.vehicle_management.domain.parking.parkingevent.model.ParkingEvent;
import com.ban.vehicle_management.domain.parking.parkingevent.policy.ParkingEventPolicy;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.domain.parking.parkingspace.policy.ParkingSpacePolicy;
import com.ban.vehicle_management.domain.parking.parkingsession.policy.ParkingCheckInPolicy;
import com.ban.vehicle_management.domain.parking.parkingsession.policy.ParkingLicensePlatePolicy;
import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import com.ban.vehicle_management.domain.parking.parkingsession.policy.ParkingSessionPolicy;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.enumeration.parking.ManualOccupancyType;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceAllocationStatus;
import com.ban.vehicle_management.shared.enumeration.parking.TrackingMode;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionQrVerificationStatus;
import com.ban.vehicle_management.shared.enumeration.storage.StorageBucket;
import com.ban.vehicle_management.shared.enumeration.storage.StorageFolder;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import io.micrometer.core.instrument.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ParkingCheckInUseCaseImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParkingCheckInUseCaseImpl.class);
    private static final String PARKING_EVENT_RESOURCE_TYPE = "parking.parking_events";
    private static final String CUSTOMER_TYPE_VISITOR = "VISITOR";
    private static final String CUSTOMER_TYPE_SUBSCRIPTION = "SUBSCRIPTION";
    private static final String BARRIER_ACTION_OPEN = "OPEN";
    private static final int MAX_QR_PAYLOAD_LENGTH = 128;

    private final ParkingSessionAccessGuard parkingSessionAccessGuard;
    private final CurrentAccountPortIn currentAccountPortIn;
    private final CardPortOut cardPortOut;
    private final CardTypePortOut cardTypePortOut;
    private final SubscriptionPortOut subscriptionPortOut;
    private final CustomerPortOut customerPortOut;
    private final CustomerVehiclePortOut customerVehiclePortOut;
    private final LanePortOut lanePortOut;
    private final GatePortOut gatePortOut;
    private final ZonePortOut zonePortOut;
    private final ParkingLotPortOut parkingLotPortOut;
    private final ParkingLotCatalogAvailabilityPortOut catalogAvailabilityPortOut;
    private final ParkingSessionPortOut parkingSessionPortOut;
    private final ParkingEventPortOut parkingEventPortOut;
    private final ParkingCheckInMapper parkingCheckInMapper;
    private final FileStoragePort fileStoragePort;
    private final ParkingSpacePortOut parkingSpacePortOut;
    private final ParkingSpaceAllocationPortOut allocationPortOut;
    private final ParkingRealtimePublisherPortOut parkingRealtimePublisher;
    private final ParkingAvailabilityQuery availabilityQuery;
    private final SubscriptionCapacityAllocationPortOut capacityAllocationPortOut;
    private final SubscriptionQrTokenService subscriptionQrTokenService;
    private final ParkingSpacePolicy parkingSpacePolicy = new ParkingSpacePolicy();
    private final CardPolicy cardPolicy = new CardPolicy();
    private final ParkingCheckInPolicy parkingCheckInPolicy = new ParkingCheckInPolicy();
    private final ParkingLicensePlatePolicy licensePlatePolicy = new ParkingLicensePlatePolicy();
    private final ParkingSessionPolicy parkingSessionPolicy = new ParkingSessionPolicy();
    private final ParkingEventPolicy parkingEventPolicy = new ParkingEventPolicy();
    private final SubscriptionQrPolicy subscriptionQrPolicy = new SubscriptionQrPolicy();

    public ParkingCheckInUseCaseImpl(
            ParkingSessionAccessGuard parkingSessionAccessGuard,
            CurrentAccountPortIn currentAccountPortIn,
            CardPortOut cardPortOut,
            CardTypePortOut cardTypePortOut,
            SubscriptionPortOut subscriptionPortOut,
            CustomerPortOut customerPortOut,
            CustomerVehiclePortOut customerVehiclePortOut,
            LanePortOut lanePortOut,
            GatePortOut gatePortOut,
            ZonePortOut zonePortOut,
            ParkingLotPortOut parkingLotPortOut,
            ParkingLotCatalogAvailabilityPortOut catalogAvailabilityPortOut,
            ParkingSessionPortOut parkingSessionPortOut,
            ParkingEventPortOut parkingEventPortOut,
            ParkingCheckInMapper parkingCheckInMapper,
            FileStoragePort fileStoragePort
    ) {
        this(parkingSessionAccessGuard, currentAccountPortIn, cardPortOut, cardTypePortOut,
                subscriptionPortOut, customerPortOut, customerVehiclePortOut, lanePortOut, gatePortOut,
                zonePortOut, parkingLotPortOut, catalogAvailabilityPortOut, parkingSessionPortOut,
                parkingEventPortOut, parkingCheckInMapper, fileStoragePort, null, null, null,
                null, null, new SubscriptionQrTokenService());
    }

    @Autowired
    public ParkingCheckInUseCaseImpl(
            ParkingSessionAccessGuard parkingSessionAccessGuard,
            CurrentAccountPortIn currentAccountPortIn,
            CardPortOut cardPortOut,
            CardTypePortOut cardTypePortOut,
            SubscriptionPortOut subscriptionPortOut,
            CustomerPortOut customerPortOut,
            CustomerVehiclePortOut customerVehiclePortOut,
            LanePortOut lanePortOut,
            GatePortOut gatePortOut,
            ZonePortOut zonePortOut,
            ParkingLotPortOut parkingLotPortOut,
            ParkingLotCatalogAvailabilityPortOut catalogAvailabilityPortOut,
            ParkingSessionPortOut parkingSessionPortOut,
            ParkingEventPortOut parkingEventPortOut,
            ParkingCheckInMapper parkingCheckInMapper,
            FileStoragePort fileStoragePort,
            ParkingSpacePortOut parkingSpacePortOut,
            ParkingSpaceAllocationPortOut allocationPortOut,
            ParkingRealtimePublisherPortOut parkingRealtimePublisher,
            ParkingAvailabilityQuery availabilityQuery,
            SubscriptionCapacityAllocationPortOut capacityAllocationPortOut,
            SubscriptionQrTokenService subscriptionQrTokenService
    ) {
        this.parkingSessionAccessGuard = parkingSessionAccessGuard;
        this.currentAccountPortIn = currentAccountPortIn;
        this.cardPortOut = cardPortOut;
        this.cardTypePortOut = cardTypePortOut;
        this.subscriptionPortOut = subscriptionPortOut;
        this.customerPortOut = customerPortOut;
        this.customerVehiclePortOut = customerVehiclePortOut;
        this.lanePortOut = lanePortOut;
        this.gatePortOut = gatePortOut;
        this.zonePortOut = zonePortOut;
        this.parkingLotPortOut = parkingLotPortOut;
        this.catalogAvailabilityPortOut = catalogAvailabilityPortOut;
        this.parkingSessionPortOut = parkingSessionPortOut;
        this.parkingEventPortOut = parkingEventPortOut;
        this.parkingCheckInMapper = parkingCheckInMapper;
        this.fileStoragePort = fileStoragePort;
        this.parkingSpacePortOut = parkingSpacePortOut;
        this.allocationPortOut = allocationPortOut;
        this.parkingRealtimePublisher = parkingRealtimePublisher;
        this.availabilityQuery = availabilityQuery;
        this.capacityAllocationPortOut = capacityAllocationPortOut;
        this.subscriptionQrTokenService = subscriptionQrTokenService;
    }

    public ParkingCheckInUseCaseImpl(
            ParkingSessionAccessGuard parkingSessionAccessGuard,
            CurrentAccountPortIn currentAccountPortIn,
            CardPortOut cardPortOut,
            CardTypePortOut cardTypePortOut,
            SubscriptionPortOut subscriptionPortOut,
            CustomerPortOut customerPortOut,
            CustomerVehiclePortOut customerVehiclePortOut,
            LanePortOut lanePortOut,
            GatePortOut gatePortOut,
            ZonePortOut zonePortOut,
            ParkingLotPortOut parkingLotPortOut,
            ParkingLotCatalogAvailabilityPortOut catalogAvailabilityPortOut,
            ParkingSessionPortOut parkingSessionPortOut,
            ParkingEventPortOut parkingEventPortOut,
            ParkingCheckInMapper parkingCheckInMapper,
            FileStoragePort fileStoragePort,
            ParkingSpacePortOut parkingSpacePortOut,
            ParkingSpaceAllocationPortOut allocationPortOut,
            ParkingRealtimePublisherPortOut parkingRealtimePublisher
    ) {
        this(parkingSessionAccessGuard, currentAccountPortIn, cardPortOut, cardTypePortOut,
                subscriptionPortOut, customerPortOut, customerVehiclePortOut, lanePortOut, gatePortOut,
                zonePortOut, parkingLotPortOut, catalogAvailabilityPortOut, parkingSessionPortOut,
                parkingEventPortOut, parkingCheckInMapper, fileStoragePort, parkingSpacePortOut,
                allocationPortOut, parkingRealtimePublisher, null, null,
                new SubscriptionQrTokenService());
    }

    @Transactional
    public CheckInResult checkIn(CheckInCommand command) {
        parkingSessionAccessGuard.ensureCanCheckIn();
        requireCommand(command);
        requireLicensePlateImage(command.licensePlateImage());
        requirePersonImage(command.personImage());

        Instant now = Instant.now();
        String qrPayload = TextValidationUtils.normalizeNullableText(
                command.subscriptionQrPayload(), "subscriptionQrPayload", MAX_QR_PAYLOAD_LENGTH);
        boolean qrCheckIn = qrPayload != null;
        if (qrCheckIn && command.cardUid() != null && !command.cardUid().isBlank()) {
            throw new BadRequestException("Chỉ được sử dụng QR hoặc thẻ vật lý cho một lần check-in");
        }
        String cardUid = qrCheckIn ? null
                : TextValidationUtils.normalizeRequiredText(command.cardUid(), "cardUid", 100);
        UUID requestedVehicleTypeId = command.vehicleTypeId();
        String licensePlate = licensePlatePolicy.normalizeRequired(command.licensePlate(), "licensePlate");
        LicensePlateResolution plateResolution = licensePlatePolicy.resolve(licensePlate, null);
        boolean formatConfirmed = Boolean.TRUE.equals(command.plateFormatConfirmed());
        boolean identityOverride = Boolean.TRUE.equals(command.plateIdentityOverride());
        if (plateResolution.needsReview() && !formatConfirmed) {
            throw new BadRequestException("Unknown license plate format must be confirmed before check-in");
        }
        if (formatConfirmed) {
            parkingSessionAccessGuard.ensureCanReviewUnknownPlate();
        }
        if (identityOverride) {
            parkingSessionAccessGuard.ensureCanOverridePlateIdentity();
        }
        String note = TextValidationUtils.normalizeNullableText(command.note(), "note", 0);

        Lane lane = findLane(command.laneId());
        parkingCheckInPolicy.validateLaneForCheckIn(lane);
        Gate gate = findGate(lane.getGateId());
        Zone zone = findZone(gate.getZoneId());
        ParkingLot parkingLot = findParkingLot(zone.getParkingLotId());
        parkingCheckInPolicy.validateOperationalTopology(lane, gate, zone, parkingLot);

        CheckInCustomerContext customerContext;
        if (qrCheckIn) {
            customerContext = resolveQrCustomerContext(
                    qrPayload, parkingLot, licensePlate, now, identityOverride, true);
        } else {
            Card card = cardPortOut.findByUidForUpdate(cardUid)
                    .orElseThrow(() -> new NotFoundException("Card not found"));
            parkingSessionAccessGuard.ensureCanUseCardInParkingLot(card, parkingLot);
            CardType cardType = cardTypePortOut.findById(card.getCardTypeId())
                    .orElseThrow(() -> new NotFoundException("Card type not found"));
            parkingCheckInPolicy.validateCardCanEnter(card, cardType);
            parkingCheckInPolicy.ensureCardHasNoOpenSession(
                    parkingSessionPortOut.existsOpenByCardId(card.getCardId()));
            customerContext = resolveCustomerContext(
                    card, cardType, licensePlate, now, identityOverride);
        }
        UUID resolvedVehicleTypeId = resolveVehicleTypeId(customerContext, requestedVehicleTypeId);
        UUID lockedZoneId = zone.getZoneId();
        zone = zonePortOut.findByIdForUpdate(lockedZoneId)
                .or(() -> zonePortOut.findById(lockedZoneId))
                .orElseThrow(() -> new NotFoundException("Zone not found"));
        // Honor already-active subscriptions; disabling a type stops new admissions, not existing contracts.
        if (customerContext.subscription() == null
                && !catalogAvailabilityPortOut.isVehicleTypeEnabled(parkingLot.getParkingLotId(), resolvedVehicleTypeId)) {
            throw new ConflictException("Vehicle type is not accepted at this parking lot");
        }
        parkingCheckInPolicy.validateVehicleTypeAccepted(resolvedVehicleTypeId, zone);
        validateAdmissionAvailability(zone, customerContext.subscription(), now);
        ParkingSpace assignedSpace = resolveCheckInSpace(
                zone, command.parkingSpaceId(), customerContext.subscription(), resolvedVehicleTypeId);
        UUID actorAccountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        UUID parkingEventId = UUID.randomUUID();
        StoredFile storedLicensePlateImage = null;
        StoredFile storedPersonImage = null;

        try {
            if (customerContext.card() != null) {
                cardPortOut.save(customerContext.card());
            }

            ParkingSession parkingSession = parkingCheckInMapper.toOpenSession(
                    UUID.randomUUID(),
                    customerContext.card(),
                    customerContext.subscription(),
                    customerContext.customerVehicle(),
                    zone,
                    resolvedVehicleTypeId,
                    licensePlate,
                    now
            );
            parkingSessionPolicy.initialize(parkingSession);
            if (assignedSpace != null) {
                parkingSession.setParkingSpaceId(assignedSpace.getParkingSpaceId());
            }
            ParkingSession savedParkingSession;
            try {
                savedParkingSession = parkingSessionPortOut.save(parkingSession);
            } catch (DataIntegrityViolationException spaceRace) {
                Metrics.counter("parking.space.checkin.assignment.conflict").increment();
                LOGGER.warn("Check-in space race for space {}",
                        assignedSpace == null ? command.parkingSpaceId() : assignedSpace.getParkingSpaceId());
                throw new ConflictException("A concurrent check-in already consumed this admission slot");
            }
            if (assignedSpace != null) {
                occupySpaceForCheckIn(zone, parkingLot, assignedSpace,
                        customerContext.subscription() == null
                                ? ManualOccupancyType.VISITOR
                                : ManualOccupancyType.REGISTERED);
            }

            storedLicensePlateImage = storeLicensePlateImage(
                    command.licensePlateImage(),
                    parkingEventId,
                    actorAccountId,
                    licensePlate
            );
            storedPersonImage = storePersonImage(
                    command.personImage(),
                    parkingEventId,
                    actorAccountId,
                    licensePlate
            );

            ParkingEvent parkingEvent = parkingCheckInMapper.toCheckInEvent(
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
            publishAvailabilityChanged(zone);

            UUID subscriptionId = customerContext.subscription() == null
                    ? null
                    : customerContext.subscription().getSubscriptionId();

            return new CheckInResult(
                    savedParkingSession,
                    savedParkingEvent,
                    subscriptionId,
                    customerContext.customerType(),
                    BARRIER_ACTION_OPEN,
                    assignedSpace == null ? null : assignedSpace.getCode()
            );
        } catch (RuntimeException exception) {
            deleteStoredFileQuietly(storedLicensePlateImage);
            deleteStoredFileQuietly(storedPersonImage);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public CheckInPreviewResult preview(CheckInPreviewCommand command) {
        parkingSessionAccessGuard.ensureCanCheckIn();
        requireField(command, "checkInPreviewCommand");
        String qrPayload = TextValidationUtils.normalizeNullableText(
                command.subscriptionQrPayload(), "subscriptionQrPayload", MAX_QR_PAYLOAD_LENGTH);
        boolean qrCheckIn = qrPayload != null;
        if (qrCheckIn && command.cardUid() != null && !command.cardUid().isBlank()) {
            throw new BadRequestException("Chỉ được sử dụng QR hoặc thẻ vật lý cho một lần check-in");
        }
        String cardUid = qrCheckIn ? null
                : TextValidationUtils.normalizeRequiredText(command.cardUid(), "cardUid", 100);
        String plate = licensePlatePolicy.normalizeRequired(command.licensePlate(), "licensePlate");
        Lane lane = findLane(command.laneId());
        parkingCheckInPolicy.validateLaneForCheckIn(lane);
        Gate gate = findGate(lane.getGateId());
        Zone zone = findZone(gate.getZoneId());
        ParkingLot lot = findParkingLot(zone.getParkingLotId());
        parkingCheckInPolicy.validateOperationalTopology(lane, gate, zone, lot);

        CheckInCustomerContext customerContext;
        UUID requestedVehicleTypeId = command.vehicleTypeId();
        LocalDate businessDate = DateTimeUtils.toVietnamLocalDate(Instant.now());
        if (qrCheckIn) {
            customerContext = resolveQrCustomerContext(
                    qrPayload, lot, plate, Instant.now(), false, false);
        } else {
            Card card = cardPortOut.findByUid(cardUid)
                    .orElseThrow(() -> new NotFoundException("Card not found"));
            parkingSessionAccessGuard.ensureCanUseCardInParkingLot(card, lot);
            CardType cardType = cardTypePortOut.findById(card.getCardTypeId())
                    .orElseThrow(() -> new NotFoundException("Card type not found"));
            parkingCheckInPolicy.validateCardCanEnter(card, cardType);
            if (parkingCheckInPolicy.isSubscriptionCard(cardType)) {
                Subscription subscription = subscriptionPortOut.findActiveByCardId(card.getCardId(), businessDate)
                        .orElseThrow(() -> new ConflictException("Active subscription not found for card"));
                Customer customer = customerPortOut.findById(subscription.getCustomerId())
                        .orElseThrow(() -> new NotFoundException("Customer not found"));
                CustomerVehicle vehicle = customerVehiclePortOut.findById(subscription.getCustomerVehicleId())
                        .orElseThrow(() -> new NotFoundException("Customer vehicle not found"));
                parkingCheckInPolicy.validateSubscriptionContext(card, subscription, customer, vehicle, plate);
                parkingCheckInPolicy.ensureSubscriptionHasNoOpenSession(
                        parkingSessionPortOut.existsOpenBySubscriptionId(subscription.getSubscriptionId()));
                customerContext = new CheckInCustomerContext(
                        card, subscription, vehicle, CUSTOMER_TYPE_SUBSCRIPTION);
            } else {
                requireField(requestedVehicleTypeId, "vehicleTypeId");
                customerContext = new CheckInCustomerContext(card, null, null, CUSTOMER_TYPE_VISITOR);
            }
        }
        UUID vehicleTypeId = resolveVehicleTypeId(customerContext, requestedVehicleTypeId);
        parkingCheckInPolicy.validateVehicleTypeAccepted(vehicleTypeId, zone);
        var availability = availabilityQuery == null ? null : availabilityQuery.get(zone, businessDate);
        long physicalAvailable = availability == null
                ? Math.max(0, value(zone.getCapacity()) - parkingSessionPortOut.countOpenByZoneId(zone.getZoneId()))
                : availability.physicalAvailable();
        long visitorAvailable = availability == null ? physicalAvailable : availability.visitorAvailable();

        List<CheckInPreviewResult.EligibleSpace> eligible = List.of();
        UUID recommended = null;
        Subscription subscription = customerContext.subscription();
        String customerType = customerContext.customerType();
        if (zone.getTrackingMode() != TrackingMode.SPACE) {
            if (subscription != null && (capacityAllocationPortOut == null
                    || capacityAllocationPortOut.findEffectiveBySubscriptionId(
                            subscription.getSubscriptionId(), businessDate)
                    .filter(item -> zone.getZoneId().equals(item.getZoneId())).isEmpty())) {
                throw new ConflictException("Subscription has no capacity entitlement for this zone");
            }
            if (subscription == null && visitorAvailable <= 0 || subscription != null && physicalAvailable <= 0) {
                throw new ConflictException("No check-in capacity is available");
            }
        } else if (subscription != null) {
            UUID subscriptionId = subscription.getSubscriptionId();
            var allocation = allocationPortOut.findActiveAllocationsBySubscription(subscriptionId).stream()
                    .filter(item -> !businessDate.isBefore(item.getEffectiveFrom())
                            && !businessDate.isAfter(item.getEffectiveTo()))
                    .findFirst().orElseThrow(() -> new ConflictException(
                            "Active subscription has no effective parking-space allocation"));
            ParkingSpace space = parkingSpacePortOut.findById(allocation.getParkingSpaceId())
                    .orElseThrow(() -> new NotFoundException("Allocated parking space not found"));
            if (!zone.getZoneId().equals(space.getZoneId())
                    || space.getStatus() != ParkingSpaceStatus.RESERVED
                    || parkingSessionPortOut.existsOpenByParkingSpaceId(space.getParkingSpaceId())) {
                throw new ConflictException("Allocated parking space is not ready for check-in");
            }
            recommended = space.getParkingSpaceId();
            eligible = List.of(new CheckInPreviewResult.EligibleSpace(
                    space.getParkingSpaceId(), space.getCode(), space.getStatus()));
        } else {
            UUID resolvedType = vehicleTypeId;
            eligible = parkingSpacePortOut.findByZoneIdAndLifecycleStatus(zone.getZoneId(), "ACTIVE")
                    .stream().filter(space -> visitorSpaceEligible(space, resolvedType)).map(space ->
                            new CheckInPreviewResult.EligibleSpace(
                                    space.getParkingSpaceId(), space.getCode(), space.getStatus()))
                    .toList();
            if (eligible.isEmpty()) {
                throw new ConflictException("No eligible visitor parking space is available");
            }
            if (eligible.size() == 1) {
                recommended = eligible.get(0).parkingSpaceId();
            }
        }
        return new CheckInPreviewResult(lot.getParkingLotId(), zone.getZoneId(), zone.getTrackingMode(),
                customerType, physicalAvailable, visitorAvailable, eligible.size() > 1,
                recommended, eligible);
    }

    private boolean visitorSpaceEligible(ParkingSpace space, UUID vehicleTypeId) {
        if (space.getStatus() != ParkingSpaceStatus.AVAILABLE
                || parkingSessionPortOut.existsOpenByParkingSpaceId(space.getParkingSpaceId())
                || space.getVehicleTypeId() != null && !space.getVehicleTypeId().equals(vehicleTypeId)) {
            return false;
        }
        return allocationPortOut.findByParkingSpaceId(space.getParkingSpaceId()).stream()
                .noneMatch(allocation -> allocation.getAllocationStatus() == ParkingSpaceAllocationStatus.ACTIVE
                        || allocation.getAllocationStatus() == ParkingSpaceAllocationStatus.HELD
                        && allocation.getHoldExpiresAt() != null
                        && allocation.getHoldExpiresAt().isAfter(Instant.now()));
    }

    private int value(Integer number) {
        return number == null ? 0 : number;
    }

    private StoredFile storeLicensePlateImage(
            MultipartFile licensePlateImage,
            UUID parkingEventId,
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
                        "event_type", "CHECK_IN",
                        "image_type", "LICENSE_PLATE",
                        "license_plate", licensePlate
                )
        ));
    }

    private StoredFile storePersonImage(
            MultipartFile personImage,
            UUID parkingEventId,
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
                        "event_type", "CHECK_IN",
                        "image_type", "PERSON",
                        "license_plate", licensePlate
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
            LOGGER.warn("Failed to delete parking check-in image after transaction failure", exception);
        }
    }

    private CheckInCustomerContext resolveCustomerContext(
            Card card,
            CardType cardType,
            String licensePlate,
            Instant now,
            boolean identityOverride
    ) {
        if (parkingCheckInPolicy.isVisitorCard(cardType)) {
            cardPolicy.assign(card, now);
            cardPolicy.markInUse(card);
            return new CheckInCustomerContext(card, null, null, CUSTOMER_TYPE_VISITOR);
        }

        if (parkingCheckInPolicy.isSubscriptionCard(cardType)) {
            LocalDate businessDate = DateTimeUtils.toVietnamLocalDate(now);
            Subscription subscription = subscriptionPortOut.findActiveByCardIdForUpdate(card.getCardId(), businessDate)
                    .or(() -> subscriptionPortOut.findActiveByCardId(card.getCardId(), businessDate))
                    .orElseThrow(() -> new ConflictException("Active subscription not found for card"));

            Customer customer = customerPortOut.findById(subscription.getCustomerId())
                    .orElseThrow(() -> new NotFoundException("Customer not found"));
            CustomerVehicle customerVehicle = customerVehiclePortOut.findById(subscription.getCustomerVehicleId())
                    .orElseThrow(() -> new NotFoundException("Customer vehicle not found"));

            parkingCheckInPolicy.validateSubscriptionContext(
                    card,
                    subscription,
                    customer,
                    customerVehicle,
                    licensePlate,
                    identityOverride
            );
            parkingCheckInPolicy.ensureSubscriptionHasNoOpenSession(
                    parkingSessionPortOut.existsOpenBySubscriptionId(subscription.getSubscriptionId()));
            cardPolicy.markInUse(card);
            return new CheckInCustomerContext(card, subscription, customerVehicle, CUSTOMER_TYPE_SUBSCRIPTION);
        }

        throw new ConflictException("Card is not eligible for parking check-in");
    }

    private CheckInCustomerContext resolveQrCustomerContext(
            String qrPayload,
            ParkingLot parkingLot,
            String detectedLicensePlate,
            Instant now,
            boolean identityOverride,
            boolean forUpdate
    ) {
        String qrToken = subscriptionQrTokenService.extractToken(qrPayload)
                .orElseThrow(() -> new ConflictException("Mã QR vé đăng ký không hợp lệ"));
        Subscription candidate = subscriptionPortOut.findByQrAccessToken(qrToken)
                .orElseThrow(() -> new ConflictException("Mã QR vé đăng ký không hợp lệ"));
        Subscription subscription = candidate;
        if (forUpdate) {
            subscription = subscriptionPortOut.findByIdForUpdate(candidate.getSubscriptionId())
                    .orElseThrow(() -> new NotFoundException("Subscription not found"));
            if (!qrToken.equals(subscription.getQrAccessToken())) {
                throw new ConflictException("Mã QR vé đăng ký đã được làm mới hoặc thu hồi");
            }
        }

        Customer customer = customerPortOut.findById(subscription.getCustomerId())
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        CustomerVehicle customerVehicle = customerVehiclePortOut.findById(subscription.getCustomerVehicleId())
                .orElseThrow(() -> new NotFoundException("Customer vehicle not found"));
        SubscriptionQrVerificationStatus qrStatus = subscriptionQrPolicy.verify(
                subscription,
                DateTimeUtils.toVietnamLocalDate(now),
                parkingLot.getParkingLotId(),
                customerVehicle.getLicensePlate(),
                detectedLicensePlate
        );
        ensureQrCanCheckIn(qrStatus);
        parkingCheckInPolicy.validateSubscriptionContext(
                subscription,
                customer,
                customerVehicle,
                detectedLicensePlate,
                identityOverride
        );
        parkingCheckInPolicy.ensureSubscriptionHasNoOpenSession(
                parkingSessionPortOut.existsOpenBySubscriptionId(subscription.getSubscriptionId()));

        Card physicalCard = resolveOptionalQrPhysicalCard(subscription, parkingLot, forUpdate);
        return new CheckInCustomerContext(
                physicalCard,
                subscription,
                customerVehicle,
                CUSTOMER_TYPE_SUBSCRIPTION
        );
    }

    private Card resolveOptionalQrPhysicalCard(
            Subscription subscription,
            ParkingLot parkingLot,
            boolean forUpdate
    ) {
        if (subscription.getCardId() == null) {
            return null;
        }
        Card card = (forUpdate
                ? cardPortOut.findByIdForUpdate(subscription.getCardId())
                : cardPortOut.findById(subscription.getCardId()))
                .orElseThrow(() -> new NotFoundException("Subscription card not found"));
        if (card.getStatus() != CardStatus.ASSIGNED) {
            return null;
        }
        parkingSessionAccessGuard.ensureCanUseCardInParkingLot(card, parkingLot);
        CardType cardType = cardTypePortOut.findById(card.getCardTypeId())
                .orElseThrow(() -> new NotFoundException("Card type not found"));
        if (!parkingCheckInPolicy.isSubscriptionCard(cardType)) {
            throw new ConflictException("Thẻ vật lý của vé đăng ký không đúng loại");
        }
        parkingCheckInPolicy.validateCardCanEnter(card, cardType);
        parkingCheckInPolicy.ensureCardHasNoOpenSession(
                parkingSessionPortOut.existsOpenByCardId(card.getCardId()));
        if (forUpdate) {
            cardPolicy.markInUse(card);
        }
        return card;
    }

    private void ensureQrCanCheckIn(SubscriptionQrVerificationStatus status) {
        if (status == SubscriptionQrVerificationStatus.VALID) {
            return;
        }
        String message = switch (status) {
            case INVALID_QR -> "Mã QR vé đăng ký không hợp lệ";
            case NOT_YET_VALID -> "Vé đăng ký chưa đến ngày hiệu lực";
            case EXPIRED -> "Vé đăng ký đã hết hạn";
            case REVOKED -> "Mã QR vé đăng ký đã bị thu hồi";
            case BLOCKED -> "Vé đăng ký đang bị tạm khóa";
            case INACTIVE -> "Vé đăng ký chưa hoạt động";
            case WRONG_PARKING_LOT -> "Vé đăng ký không áp dụng tại bãi xe này";
            case VEHICLE_MISMATCH -> "Biển số nhận diện không khớp với phương tiện đã đăng ký";
            case VALID -> throw new IllegalStateException("Unexpected valid QR status");
        };
        throw new ConflictException(message);
    }

    private UUID resolveVehicleTypeId(CheckInCustomerContext customerContext, UUID requestedVehicleTypeId) {
        CustomerVehicle customerVehicle = customerContext.customerVehicle();
        Subscription subscription = customerContext.subscription();
        if (customerVehicle != null && subscription != null) {
            UUID canonicalVehicleTypeId = customerVehicle.getVehicleTypeId();
            UUID operationalVehicleTypeId = subscriptionPortOut
                    .findOperationalVehicleTypeIdBySubscriptionId(subscription.getSubscriptionId())
                    .orElseThrow(() -> new ConflictException(
                            "Vé đăng ký chưa có loại xe vận hành phù hợp với đối tác"));
            if (requestedVehicleTypeId != null
                    && !requestedVehicleTypeId.equals(canonicalVehicleTypeId)
                    && !requestedVehicleTypeId.equals(operationalVehicleTypeId)) {
                throw new ConflictException("Selected vehicle type does not match subscription vehicle");
            }
            return operationalVehicleTypeId;
        }

        requireField(requestedVehicleTypeId, "vehicleTypeId");
        return requestedVehicleTypeId;
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
        ParkingLot parkingLot = parkingLotPortOut.findById(parkingLotId)
                .orElseThrow(() -> new NotFoundException("Parking lot not found"));
        parkingSessionAccessGuard.ensureCanOperateParkingLot(parkingLot);
        return parkingLot;
    }

    private ParkingSpace resolveCheckInSpace(
            Zone zone,
            UUID parkingSpaceId,
            Subscription subscription,
            UUID resolvedVehicleTypeId
    ) {
        if (zone.getTrackingMode() != TrackingMode.SPACE) {
            if (parkingSpaceId != null) {
                throw new BadRequestException("parkingSpaceId must be null for CAPACITY zones");
            }
            return null;
        }
        if (parkingSpacePortOut == null) {
            throw new BadRequestException("Parking space assignment is not available");
        }
        UUID resolvedSpaceId = parkingSpaceId;
        ParkingSpace autoAssignedSpace = null;
        Instant now = Instant.now();
        LocalDate today = DateTimeUtils.toAppLocalDate(now);
        if (subscription != null) {
            var ownAllocation = allocationPortOut.findActiveAllocationsBySubscription(
                            subscription.getSubscriptionId()).stream()
                    .filter(allocation -> !today.isBefore(allocation.getEffectiveFrom())
                            && !today.isAfter(allocation.getEffectiveTo()))
                    .findFirst()
                    .orElseThrow(() -> new ConflictException(
                            "Active subscription has no effective parking-space allocation"));
            resolvedSpaceId = ownAllocation.getParkingSpaceId();
            if (parkingSpaceId != null && !parkingSpaceId.equals(resolvedSpaceId)) {
                throw new BadRequestException("parkingSpaceId does not match the subscription allocation");
            }
        } else if (resolvedSpaceId == null) {
            autoAssignedSpace = parkingSpacePortOut.findFirstVisitorAvailableForUpdate(
                            zone.getZoneId(), resolvedVehicleTypeId, now)
                    .orElseThrow(() -> new ConflictException(
                            "No compatible parking space is available for visitor check-in"));
            resolvedSpaceId = autoAssignedSpace.getParkingSpaceId();
        }
        UUID selectedSpaceId = resolvedSpaceId;
        ParkingSpace space = autoAssignedSpace != null
                ? autoAssignedSpace
                : parkingSpacePortOut.findByIdForUpdate(selectedSpaceId)
                        .or(() -> parkingSpacePortOut.findById(selectedSpaceId))
                        .orElseThrow(() -> new NotFoundException("Parking space not found"));
        if (!zone.getZoneId().equals(space.getZoneId())) {
            throw new BadRequestException("Parking space does not belong to the check-in zone");
        }
        if (!"ACTIVE".equals(space.getLifecycleStatus())) {
            throw new ConflictException("Only an active parking space can be assigned");
        }
        ParkingSpaceStatus requiredStatus = subscription == null
                ? ParkingSpaceStatus.AVAILABLE : ParkingSpaceStatus.RESERVED;
        if (space.getStatus() != requiredStatus) {
            throw new ConflictException("Parking space is not available for check-in");
        }
        if (space.getVehicleTypeId() != null && !space.getVehicleTypeId().equals(resolvedVehicleTypeId)) {
            throw new ConflictException("Parking space does not support this vehicle type");
        }
        if (parkingSessionPortOut.existsOpenByParkingSpaceId(resolvedSpaceId)) {
            throw new ConflictException("Parking space is already occupied");
        }
        if (allocationPortOut != null) {
            var effective = allocationPortOut.findActiveAllocationsForSpaceOnDate(resolvedSpaceId, today);
            if (subscription == null) {
                boolean committed = !effective.isEmpty()
                        || allocationPortOut.findByParkingSpaceId(resolvedSpaceId).stream()
                        .anyMatch(allocation -> allocation.getAllocationStatus() == ParkingSpaceAllocationStatus.ACTIVE
                                || allocation.getAllocationStatus() == ParkingSpaceAllocationStatus.HELD
                                && allocation.getHoldExpiresAt() != null
                                && allocation.getHoldExpiresAt().isAfter(now));
                if (committed) {
                    throw new ConflictException("Parking space is reserved for a subscription");
                }
            } else {
                boolean ownAllocation = effective.stream()
                        .anyMatch(allocation -> subscription.getSubscriptionId()
                                .equals(allocation.getSubscriptionId()));
                if (!effective.isEmpty() && !ownAllocation) {
                    throw new ConflictException("Parking space is allocated to another subscription");
                }
            }
        }
        return space;
    }

    private void validateAdmissionAvailability(Zone zone, Subscription subscription, Instant now) {
        if (availabilityQuery == null) {
            parkingCheckInPolicy.validateZoneCapacity(zone,
                    parkingSessionPortOut.countOpenByZoneId(zone.getZoneId()));
            return;
        }
        LocalDate businessDate = DateTimeUtils.toVietnamLocalDate(now);
        var availability = availabilityQuery.get(zone, businessDate);
        if (subscription == null) {
            if (availability.visitorAvailable() <= 0) {
                throw new ConflictException("No visitor capacity is available in this zone");
            }
            return;
        }
        if (zone.getTrackingMode() != TrackingMode.SPACE) {
            if (capacityAllocationPortOut == null
                    || capacityAllocationPortOut.findEffectiveBySubscriptionId(
                            subscription.getSubscriptionId(), businessDate)
                    .filter(allocation -> zone.getZoneId().equals(allocation.getZoneId()))
                    .isEmpty()) {
                throw new ConflictException("Subscription has no capacity entitlement for this zone");
            }
        }
        if (availability.physicalAvailable() <= 0) {
            throw new ConflictException("No physical capacity is available in this zone");
        }
    }

    private void occupySpaceForCheckIn(
            Zone zone,
            ParkingLot parkingLot,
            ParkingSpace space,
            ManualOccupancyType occupancyType
    ) {
        parkingSpacePolicy.setAutomaticStatus(space, ParkingSpaceStatus.OCCUPIED, occupancyType);
        parkingSpacePortOut.save(space);
        Metrics.counter("parking.space.checkin.assigned").increment();
        LOGGER.info("check-in assigned space zone={} space={} occupancy={}",
                zone.getZoneId(), space.getParkingSpaceId(), occupancyType);
        ParkingRealtimeAfterCommit.publishAfterCommit(parkingRealtimePublisher,
                new ParkingRealtimeEvent(
                        "PARKING_SPACE_STATUS_CHANGED",
                        parkingLot.getParkingLotId(),
                        zone.getParkingLevelId(),
                        zone.getZoneId(),
                        space.getParkingSpaceId(),
                        ParkingSpaceStatus.OCCUPIED.name(),
                        zone.getLayoutVersion(),
                        Map.of("occupancyType", occupancyType.name())));
    }

    private void publishAvailabilityChanged(Zone zone) {
        if (availabilityQuery == null || parkingRealtimePublisher == null) {
            return;
        }
        var value = availabilityQuery.get(zone, DateTimeUtils.toVietnamLocalDate(Instant.now()));
        ParkingRealtimeAfterCommit.publishAfterCommit(parkingRealtimePublisher,
                new ParkingRealtimeEvent("PARKING_AVAILABILITY_CHANGED", zone.getParkingLotId(),
                        zone.getParkingLevelId(), zone.getZoneId(), null, null,
                        zone.getLayoutVersion(), Map.of(
                                "physicalAvailable", String.valueOf(value.physicalAvailable()),
                                "visitorAvailable", String.valueOf(value.visitorAvailable()),
                                "occupiedRegistered", String.valueOf(value.occupiedRegistered()),
                                "occupiedVisitor", String.valueOf(value.occupiedVisitor()),
                                "subscriptionReserved", String.valueOf(value.subscriptionReserved()),
                                "effectiveCapacity", String.valueOf(value.effectiveCapacity()))));
    }

    private void requireCommand(CheckInCommand command) {
        requireField(command, "checkInCommand");
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

    private record CheckInCustomerContext(
            Card card,
            Subscription subscription,
            CustomerVehicle customerVehicle,
            String customerType
    ) {
    }
}
