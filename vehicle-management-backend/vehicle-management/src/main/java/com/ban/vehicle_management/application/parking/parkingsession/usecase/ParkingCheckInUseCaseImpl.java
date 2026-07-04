package com.ban.vehicle_management.application.parking.parkingsession.usecase;

import com.ban.vehicle_management.application.accesscontrol.card.port.out.CardPortOut;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionPortOut;
import com.ban.vehicle_management.application.catalog.cardtype.port.out.CardTypePortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.parking.gate.port.out.GatePortOut;
import com.ban.vehicle_management.application.parking.lane.port.out.LanePortOut;
import com.ban.vehicle_management.application.parking.parkingevent.port.out.ParkingEventPortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.authorization.ParkingSessionAccessGuard;
import com.ban.vehicle_management.application.parking.parkingsession.mapper.ParkingCheckInMapper;
import com.ban.vehicle_management.application.parking.parkingsession.model.command.CheckInCommand;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.CheckInResult;
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
import com.ban.vehicle_management.domain.catalog.cardtype.model.CardType;
import com.ban.vehicle_management.domain.parking.gate.model.Gate;
import com.ban.vehicle_management.domain.parking.lane.model.Lane;
import com.ban.vehicle_management.domain.parking.parkingevent.model.ParkingEvent;
import com.ban.vehicle_management.domain.parking.parkingevent.policy.ParkingEventPolicy;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
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
import com.ban.vehicle_management.shared.enumeration.storage.StorageBucket;
import com.ban.vehicle_management.shared.enumeration.storage.StorageFolder;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final ParkingSessionPortOut parkingSessionPortOut;
    private final ParkingEventPortOut parkingEventPortOut;
    private final ParkingCheckInMapper parkingCheckInMapper;
    private final FileStoragePort fileStoragePort;
    private final CardPolicy cardPolicy = new CardPolicy();
    private final ParkingCheckInPolicy parkingCheckInPolicy = new ParkingCheckInPolicy();
    private final ParkingLicensePlatePolicy licensePlatePolicy = new ParkingLicensePlatePolicy();
    private final ParkingSessionPolicy parkingSessionPolicy = new ParkingSessionPolicy();
    private final ParkingEventPolicy parkingEventPolicy = new ParkingEventPolicy();

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
            ParkingSessionPortOut parkingSessionPortOut,
            ParkingEventPortOut parkingEventPortOut,
            ParkingCheckInMapper parkingCheckInMapper,
            FileStoragePort fileStoragePort
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
        this.parkingSessionPortOut = parkingSessionPortOut;
        this.parkingEventPortOut = parkingEventPortOut;
        this.parkingCheckInMapper = parkingCheckInMapper;
        this.fileStoragePort = fileStoragePort;
    }

    @Transactional
    public CheckInResult checkIn(CheckInCommand command) {
        parkingSessionAccessGuard.ensureCanCheckIn();
        requireCommand(command);
        requireLicensePlateImage(command.licensePlateImage());
        requirePersonImage(command.personImage());

        Instant now = Instant.now();
        String cardUid = TextValidationUtils.normalizeRequiredText(command.cardUid(), "cardUid", 100);
        String licensePlate = licensePlatePolicy.normalizeRequired(command.licensePlate(), "licensePlate");
        String note = TextValidationUtils.normalizeNullableText(command.note(), "note", 0);

        Lane lane = findLane(command.laneId());
        parkingCheckInPolicy.validateLaneForCheckIn(lane);
        Gate gate = findGate(lane.getGateId());
        Zone zone = findZone(gate.getZoneId());
        ParkingLot parkingLot = findParkingLot(zone.getParkingLotId());
        parkingCheckInPolicy.validateOperationalTopology(lane, gate, zone, parkingLot);

        Card card = cardPortOut.findByUidForUpdate(cardUid)
                .orElseThrow(() -> new NotFoundException("Card not found"));
        CardType cardType = cardTypePortOut.findById(card.getCardTypeId())
                .orElseThrow(() -> new NotFoundException("Card type not found"));
        parkingCheckInPolicy.validateCardCanEnter(card, cardType);
        parkingCheckInPolicy.validateVehicleTypeAccepted(card.getVehicleTypeId(), zone);
        parkingCheckInPolicy.ensureCardHasNoOpenSession(parkingSessionPortOut.existsOpenByCardId(card.getCardId()));
        parkingCheckInPolicy.validateZoneCapacity(zone, parkingSessionPortOut.countOpenByZoneId(zone.getZoneId()));

        CheckInCustomerContext customerContext = resolveCustomerContext(card, cardType, licensePlate, now);
        UUID actorAccountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        UUID parkingEventId = UUID.randomUUID();
        StoredFile storedLicensePlateImage = null;
        StoredFile storedPersonImage = null;

        try {
            cardPortOut.save(customerContext.card());

            ParkingSession parkingSession = parkingCheckInMapper.toOpenSession(
                    UUID.randomUUID(),
                    customerContext.card(),
                    customerContext.subscription(),
                    customerContext.customerVehicle(),
                    zone,
                    licensePlate,
                    now
            );
            parkingSessionPolicy.initialize(parkingSession);
            ParkingSession savedParkingSession = parkingSessionPortOut.save(parkingSession);

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

            UUID subscriptionId = customerContext.subscription() == null
                    ? null
                    : customerContext.subscription().getSubscriptionId();

            return new CheckInResult(
                    savedParkingSession,
                    savedParkingEvent,
                    subscriptionId,
                    customerContext.customerType(),
                    BARRIER_ACTION_OPEN
            );
        } catch (RuntimeException exception) {
            deleteStoredFileQuietly(storedLicensePlateImage);
            deleteStoredFileQuietly(storedPersonImage);
            throw exception;
        }
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

    private CheckInCustomerContext resolveCustomerContext(Card card, CardType cardType, String licensePlate, Instant now) {
        if (parkingCheckInPolicy.isVisitorCard(cardType)) {
            cardPolicy.assign(card, now);
            cardPolicy.markInUse(card);
            return new CheckInCustomerContext(card, null, null, CUSTOMER_TYPE_VISITOR);
        }

        if (parkingCheckInPolicy.isSubscriptionCard(cardType)) {
            LocalDate businessDate = DateTimeUtils.toVietnamLocalDate(now);
            Subscription subscription = subscriptionPortOut.findActiveByCardId(card.getCardId(), businessDate)
                    .orElseThrow(() -> new ConflictException("Active subscription not found for card"));

            Customer customer = customerPortOut.findById(subscription.getCustomerId())
                    .orElseThrow(() -> new NotFoundException("Customer not found"));
            CustomerVehicle customerVehicle = customerVehiclePortOut.findById(subscription.getCustomerVehicleId())
                    .orElseThrow(() -> new NotFoundException("Customer vehicle not found"));

            parkingCheckInPolicy.validateSubscriptionContext(card, subscription, customer, customerVehicle, licensePlate);
            cardPolicy.markInUse(card);
            return new CheckInCustomerContext(card, subscription, customerVehicle, CUSTOMER_TYPE_SUBSCRIPTION);
        }

        throw new ConflictException("Card is not eligible for parking check-in");
    }

    private Lane findLane(UUID laneId) {
        requireField(laneId, "laneId");
        return lanePortOut.findById(laneId)
                .orElseThrow(() -> new NotFoundException("Lane not found"));
    }

    private Gate findGate(UUID gateId) {
        return gatePortOut.findById(gateId)
                .orElseThrow(() -> new NotFoundException("Gate not found"));
    }

    private Zone findZone(UUID zoneId) {
        return zonePortOut.findById(zoneId)
                .orElseThrow(() -> new NotFoundException("Zone not found"));
    }

    private ParkingLot findParkingLot(UUID parkingLotId) {
        return parkingLotPortOut.findById(parkingLotId)
                .orElseThrow(() -> new NotFoundException("Parking lot not found"));
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
