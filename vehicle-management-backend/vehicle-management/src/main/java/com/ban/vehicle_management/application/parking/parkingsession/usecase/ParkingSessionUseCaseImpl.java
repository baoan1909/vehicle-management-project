package com.ban.vehicle_management.application.parking.parkingsession.usecase;

import com.ban.vehicle_management.application.accesscontrol.subscription.authorization.SubscriptionAccessGuard;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.model.command.CheckInCommand;
import com.ban.vehicle_management.application.parking.parkingsession.authorization.EmployeeParkingLotAccessGuard;
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
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
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
    private final CurrentAccountPortIn currentAccountPortIn;
    private final OrganizationAccessGuard organizationAccessGuard;
    private final EmployeeParkingLotAccessGuard employeeParkingLotAccessGuard;
    private final ParkingLotPortOut parkingLotPortOut;

    public ParkingSessionUseCaseImpl(
            ParkingCheckInUseCaseImpl parkingCheckInUseCase,
            ParkingCheckOutUseCaseImpl parkingCheckOutUseCase,
            ParkingSessionPortOut parkingSessionPortOut,
            CustomerVehiclePortOut customerVehiclePortOut,
            SubscriptionAccessGuard subscriptionAccessGuard,
            FileAccessPort fileAccessPort,
            ParkingSessionManagementResultMapper parkingSessionManagementResultMapper,
            CurrentAccountPortIn currentAccountPortIn,
            OrganizationAccessGuard organizationAccessGuard,
            EmployeeParkingLotAccessGuard employeeParkingLotAccessGuard,
            ParkingLotPortOut parkingLotPortOut
    ) {
        this.parkingCheckInUseCase = parkingCheckInUseCase;
        this.parkingCheckOutUseCase = parkingCheckOutUseCase;
        this.parkingSessionPortOut = parkingSessionPortOut;
        this.customerVehiclePortOut = customerVehiclePortOut;
        this.subscriptionAccessGuard = subscriptionAccessGuard;
        this.fileAccessPort = fileAccessPort;
        this.parkingSessionManagementResultMapper = parkingSessionManagementResultMapper;
        this.currentAccountPortIn = currentAccountPortIn;
        this.organizationAccessGuard = organizationAccessGuard;
        this.employeeParkingLotAccessGuard = employeeParkingLotAccessGuard;
        this.parkingLotPortOut = parkingLotPortOut;
    }

    @Override
    public List<ParkingSessionManagementResult> getSessions(
            ParkingSessionStatus status,
            UUID vehicleTypeId,
            UUID zoneId,
            LocalDate fromDate,
            LocalDate toDate,
            String keyword,
            UUID parkingLotId
    ) {
        Instant checkInFrom = DateTimeUtils.startOfDayInVietnam(fromDate);
        Instant checkInTo = DateTimeUtils.startOfNextDayInVietnam(toDate);
        String normalizedKeyword = TextValidationUtils.normalizeNullableText(keyword, "keyword", 100);
        Set<UUID> allowedLotIds = narrowToRequestedLot(resolveOperationalLotIds(), parkingLotId);
        List<ParkingSessionManagementResult> sessions = parkingSessionPortOut.findManagementSessions(
                status,
                vehicleTypeId,
                zoneId,
                checkInFrom,
                checkInTo,
                normalizedKeyword,
                null,
                allowedLotIds
        );
        return parkingSessionManagementResultMapper.withResolvedEventImageUrls(
                sessions, fileAccessPort
        );
    }

    @Override
    public List<ParkingSessionManagementResult> getOwnSessions(
            ParkingSessionStatus status,
            UUID vehicleTypeId,
            UUID zoneId,
            LocalDate fromDate,
            LocalDate toDate,
            String keyword,
            UUID parkingLotId
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
                customerVehicleIds,
                parkingLotId == null ? null : Set.of(parkingLotId)
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

    private Set<UUID> resolveOperationalLotIds() {
        Set<String> permissions = currentAccountPortIn.getCurrentAccountOrThrow().getEffectivePermissionCodes();
        if (permissions.contains(OrganizationAccessGuard.PARKING_SCOPE_PLATFORM)
                || permissions.contains(OrganizationAccessGuard.PARKING_SCOPE_PARTNER)
                || permissions.contains(OrganizationAccessGuard.PARKING_SCOPE_ASSIGNED)) {
            return organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut);
        }
        if (permissions.contains("PARKING_SESSION_CHECK_IN_ALL")
                || permissions.contains("PARKING_SESSION_CHECK_OUT_ALL")) {
            return employeeParkingLotAccessGuard.activeParkingLotIds();
        }
        throw new AccessDeniedException("Current account has no parking lot monitoring scope");
    }

    private Set<UUID> narrowToRequestedLot(Set<UUID> allowedLotIds, UUID requestedLotId) {
        if (requestedLotId == null) {
            return allowedLotIds;
        }
        if (allowedLotIds != null && !allowedLotIds.contains(requestedLotId)) {
            throw new AccessDeniedException("Không có quyền xem phiên gửi xe của bãi này");
        }
        return Set.of(requestedLotId);
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
