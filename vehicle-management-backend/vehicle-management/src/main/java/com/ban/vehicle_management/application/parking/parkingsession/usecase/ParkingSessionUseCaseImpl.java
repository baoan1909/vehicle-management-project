package com.ban.vehicle_management.application.parking.parkingsession.usecase;

import com.ban.vehicle_management.application.accesscontrol.subscription.authorization.SubscriptionAccessGuard;
import com.ban.vehicle_management.application.parking.parkingsession.model.command.CheckInCommand;
import com.ban.vehicle_management.application.parking.parkingsession.model.command.CheckOutCommand;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.CheckInResult;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.CheckOutPreviewResult;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.CheckOutResult;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.ParkingSessionManagementResult;
import com.ban.vehicle_management.application.parking.parkingsession.mapper.ParkingSessionManagementResultMapper;
import com.ban.vehicle_management.application.parking.parkingsession.port.in.ParkingSessionPortIn;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.application.people.customervehicle.port.out.CustomerVehiclePortOut;
import com.ban.vehicle_management.application.storage.port.out.FileAccessPort;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ParkingSessionUseCaseImpl implements ParkingSessionPortIn {

    private final ParkingCheckInUseCaseImpl parkingCheckInUseCase;
    private final ParkingCheckOutUseCaseImpl parkingCheckOutUseCase;
    private final ParkingSessionPortOut parkingSessionPortOut;
    private final CustomerVehiclePortOut customerVehiclePortOut;
    private final SubscriptionAccessGuard subscriptionAccessGuard;
    private final FileAccessPort fileAccessPort;
    private final ParkingSessionManagementResultMapper parkingSessionManagementResultMapper;

    public ParkingSessionUseCaseImpl(
            ParkingCheckInUseCaseImpl parkingCheckInUseCase,
            ParkingCheckOutUseCaseImpl parkingCheckOutUseCase,
            ParkingSessionPortOut parkingSessionPortOut,
            CustomerVehiclePortOut customerVehiclePortOut,
            SubscriptionAccessGuard subscriptionAccessGuard,
            FileAccessPort fileAccessPort,
            ParkingSessionManagementResultMapper parkingSessionManagementResultMapper
    ) {
        this.parkingCheckInUseCase = parkingCheckInUseCase;
        this.parkingCheckOutUseCase = parkingCheckOutUseCase;
        this.parkingSessionPortOut = parkingSessionPortOut;
        this.customerVehiclePortOut = customerVehiclePortOut;
        this.subscriptionAccessGuard = subscriptionAccessGuard;
        this.fileAccessPort = fileAccessPort;
        this.parkingSessionManagementResultMapper = parkingSessionManagementResultMapper;
    }

    @Override
    public List<ParkingSessionManagementResult> getSessions(
            ParkingSessionStatus status,
            UUID vehicleTypeId,
            UUID zoneId,
            LocalDate fromDate,
            LocalDate toDate,
            String keyword
    ) {
        Instant checkInFrom = DateTimeUtils.startOfDayInVietnam(fromDate);
        Instant checkInTo = DateTimeUtils.startOfNextDayInVietnam(toDate);
        String normalizedKeyword = TextValidationUtils.normalizeNullableText(keyword, "keyword", 100);
        List<ParkingSessionManagementResult> sessions = parkingSessionPortOut.findManagementSessions(
                status,
                vehicleTypeId,
                zoneId,
                checkInFrom,
                checkInTo,
                normalizedKeyword,
                null
        );
        return parkingSessionManagementResultMapper.withResolvedEventImageUrls(sessions, fileAccessPort);
    }

    @Override
    public List<ParkingSessionManagementResult> getOwnSessions(
            ParkingSessionStatus status,
            UUID vehicleTypeId,
            UUID zoneId,
            LocalDate fromDate,
            LocalDate toDate,
            String keyword
    ) {
        UUID customerId = subscriptionAccessGuard.resolveCurrentApprovedCustomerId();
        List<UUID> customerVehicleIds = customerVehiclePortOut.findAll(customerId, null, null, null, null)
                .stream()
                .map(customerVehicle -> customerVehicle.getCustomerVehicleId())
                .toList();

        if (customerVehicleIds.isEmpty()) {
            return List.of();
        }

        Instant checkInFrom = DateTimeUtils.startOfDayInVietnam(fromDate);
        Instant checkInTo = DateTimeUtils.startOfNextDayInVietnam(toDate);
        String normalizedKeyword = TextValidationUtils.normalizeNullableText(keyword, "keyword", 100);
        List<ParkingSessionManagementResult> sessions = parkingSessionPortOut.findManagementSessions(
                status,
                vehicleTypeId,
                zoneId,
                checkInFrom,
                checkInTo,
                normalizedKeyword,
                customerVehicleIds
        );
        return parkingSessionManagementResultMapper.withResolvedEventImageUrls(sessions, fileAccessPort);
    }

    @Override
    public CheckInResult checkIn(CheckInCommand command) {
        return parkingCheckInUseCase.checkIn(command);
    }

    @Override
    public CheckOutResult checkOut(CheckOutCommand command) {
        return parkingCheckOutUseCase.checkOut(command);
    }

    @Override
    public CheckOutResult prepareVisitorCheckOut(CheckOutCommand command) {
        return parkingCheckOutUseCase.prepareVisitorCheckOut(command);
    }

    @Override
    public CheckOutResult getCheckOutByInvoice(UUID invoiceId) {
        return parkingCheckOutUseCase.getCheckOutByInvoice(invoiceId);
    }

    @Override
    public CheckOutPreviewResult previewCheckOutByCardUid(String cardUid) {
        return parkingCheckOutUseCase.previewCheckOutByCardUid(cardUid);
    }
}
