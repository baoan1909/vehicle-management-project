package com.ban.vehicle_management.application.notification.userguide.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.iam.organization.model.result.ParkingLotAccessScope;
import com.ban.vehicle_management.application.notification.userguide.model.result.JourneyReadinessContext;
import com.ban.vehicle_management.application.notification.userguide.model.result.JourneyStepEvaluation;
import com.ban.vehicle_management.application.notification.userguide.model.result.PostOnboardingReadinessResult;
import com.ban.vehicle_management.application.notification.userguide.model.result.PostOnboardingReadinessResult.JourneyStep;
import com.ban.vehicle_management.application.notification.userguide.model.result.PostOnboardingReadinessResult.ParkingLotReadiness;
import com.ban.vehicle_management.application.notification.userguide.model.result.ResolvedUserGuide;
import com.ban.vehicle_management.application.notification.userguide.port.in.PostOnboardingReadinessPortIn;
import com.ban.vehicle_management.application.notification.userguide.port.in.UserGuideRuntimePortIn;
import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuidePortOut;
import com.ban.vehicle_management.application.parking.parkinglot.model.result.ParkingLotActivationReadiness;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.authorization.ParkingSessionAccessGuard;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuide;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideJourneyStateContent;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideJourneyStepConfig;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideStep;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideDisplayType;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideErrorCode;
import com.ban.vehicle_management.shared.enumeration.parking.GeocodingStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotManagementState;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.exception.UserGuideException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostOnboardingReadinessUseCaseImpl implements PostOnboardingReadinessPortIn {
    private static final String FEATURE_CODE = "OPERATIONS_ONBOARDING";
    private static final String CONTEXT_KEY = "/ADMIN/HEADER";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final OrganizationAccessGuard organizationAccessGuard;
    private final ParkingLotPortOut parkingLotPortOut;
    private final ParkingSessionAccessGuard parkingSessionAccessGuard;
    private final UserGuideRuntimePortIn userGuideRuntimePortIn;
    private final UserGuidePortOut userGuidePortOut;
    private final JourneyStepReadinessEvaluatorRegistry evaluatorRegistry;

    public PostOnboardingReadinessUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            OrganizationAccessGuard organizationAccessGuard,
            ParkingLotPortOut parkingLotPortOut,
            ParkingSessionAccessGuard parkingSessionAccessGuard,
            UserGuideRuntimePortIn userGuideRuntimePortIn,
            UserGuidePortOut userGuidePortOut,
            JourneyStepReadinessEvaluatorRegistry evaluatorRegistry
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.organizationAccessGuard = organizationAccessGuard;
        this.parkingLotPortOut = parkingLotPortOut;
        this.parkingSessionAccessGuard = parkingSessionAccessGuard;
        this.userGuideRuntimePortIn = userGuideRuntimePortIn;
        this.userGuidePortOut = userGuidePortOut;
        this.evaluatorRegistry = evaluatorRegistry;
    }

    @Override
    @Transactional(readOnly = true)
    public PostOnboardingReadinessResult resolveJourney(
            String featureCode, String contextKey, UUID requestedParkingLotId) {
        CurrentAccountAccess account = currentAccountPortIn.getCurrentAccountOrThrow();
        ResolvedUserGuide journey = userGuideRuntimePortIn
                .resolveManualGuides(
                        featureCode == null || featureCode.isBlank() ? FEATURE_CODE : featureCode,
                        contextKey == null || contextKey.isBlank() ? CONTEXT_KEY : contextKey,
                        UserGuideDisplayType.JOURNEY)
                .stream().findFirst()
                .orElseThrow(() -> new UserGuideException(UserGuideErrorCode.USER_GUIDE_NOT_FOUND,
                        "No published journey is available for the current account"));
        Set<String> permissions = account.getEffectivePermissionCodes();
        List<ParkingLot> lots = findAccessibleLots(resolveScope());
        ParkingLot selectedLot = selectParkingLot(lots, requestedParkingLotId);
        List<ParkingLotReadiness> lotReadiness = lots.stream()
                .map(lot -> toReadiness(lot, permissions,
                        selectedLot != null && lot.getParkingLotId().equals(selectedLot.getParkingLotId())))
                .sorted(Comparator.comparing(ParkingLotReadiness::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
        ParkingLotReadiness selectedReadiness = selectedLot == null ? null : lotReadiness.stream()
                .filter(item -> item.parkingLotId().equals(selectedLot.getParkingLotId()))
                .findFirst().orElse(null);
        JourneyReadinessContext context = new JourneyReadinessContext(
                permissions, selectedLot, selectedReadiness, operationBlockReason(selectedLot, selectedReadiness));
        List<JourneyStep> steps = journey.version().getSteps().stream()
                .sorted(Comparator.comparing(UserGuideStep::getDisplayOrder))
                .map(step -> resolveStep(step, context, selectedLot))
                .toList();
        return new PostOnboardingReadinessResult(
                account.accessRevision(), journey.guide().getCode(), journey.version().getVersionNumber(),
                journey.version().getTitle(), journey.version().getSummary(),
                selectedLot == null ? null : selectedLot.getParkingLotId(), lotReadiness, steps);
    }

    private JourneyStep resolveStep(UserGuideStep step, JourneyReadinessContext context, ParkingLot lot) {
        UserGuideJourneyStepConfig config = step.getJourneyConfig();
        if (config == null) throw new UserGuideException(
                UserGuideErrorCode.USER_GUIDE_INVALID_STEP, "Published JOURNEY step has no configuration");
        JourneyStepEvaluation evaluation = evaluatorRegistry.evaluate(config.getReadinessKey(), context);
        UserGuideJourneyStateContent content = config.getStateContent().get(evaluation.state());
        String description = content != null && content.getDescription() != null
                ? content.getDescription() : step.getBody();
        String ownerHint = content == null ? null : content.getOwnerHint();
        String actionLabel = evaluation.actionAllowed()
                ? content != null && content.getActionLabel() != null ? content.getActionLabel() : config.getActionLabel()
                : null;
        String route = evaluation.actionAllowed() ? resolveRoute(config.getRouteTemplate(), lot) : null;
        return new JourneyStep(step.getDisplayOrder(), config.getStepKey(), step.getTitle(),
                evaluation.state().name(), description, ownerHint, route, actionLabel,
                resolveLinkedGuideCode(config.getLinkedGuideId()));
    }

    private String resolveLinkedGuideCode(UUID linkedGuideId) {
        if (linkedGuideId == null) return null;
        UserGuide guide = userGuidePortOut.findById(linkedGuideId).orElse(null);
        if (guide == null || guide.getDisplayType() != UserGuideDisplayType.COACH_MARK) return null;
        try {
            userGuideRuntimePortIn.getPublishedGuide(guide.getCode());
            return guide.getCode();
        } catch (UserGuideException unavailable) {
            return null;
        }
    }

    private String resolveRoute(String routeTemplate, ParkingLot lot) {
        if (routeTemplate == null) return null;
        String parkingLotId = lot == null ? null : encode(lot.getParkingLotId());
        String organizationId = lot == null ? null : encode(lot.getOrganizationId());
        if (routeTemplate.contains("{PARKING_LOT_ID}") && parkingLotId == null) return null;
        if (routeTemplate.contains("{ORGANIZATION_ID}") && organizationId == null) return null;
        return routeTemplate.replace("{PARKING_LOT_ID}", parkingLotId == null ? "" : parkingLotId)
                .replace("{ORGANIZATION_ID}", organizationId == null ? "" : organizationId);
    }

    private String encode(UUID value) {
        return value == null ? null : URLEncoder.encode(value.toString(), StandardCharsets.UTF_8);
    }

    private ParkingLotAccessScope resolveScope() {
        try {
            return organizationAccessGuard.resolveParkingLotAccessScope();
        } catch (AccessDeniedException exception) {
            return new ParkingLotAccessScope(false, Set.of(), Set.of());
        }
    }

    private List<ParkingLot> findAccessibleLots(ParkingLotAccessScope scope) {
        if (!scope.unrestricted() && scope.organizationIds().isEmpty() && scope.parkingLotIds().isEmpty()) {
            return List.of();
        }
        return parkingLotPortOut.findAll(null, null,
                scope.unrestricted() || scope.organizationIds().isEmpty() ? null : scope.organizationIds(),
                scope.unrestricted() || !scope.organizationIds().isEmpty() ? null : scope.parkingLotIds());
    }

    private ParkingLot selectParkingLot(List<ParkingLot> lots, UUID requestedParkingLotId) {
        if (requestedParkingLotId != null) {
            return lots.stream().filter(lot -> requestedParkingLotId.equals(lot.getParkingLotId()))
                    .findFirst().orElseThrow(() -> new NotFoundException(
                            "Parking lot is not available in the current scope"));
        }
        return lots.stream().sorted(Comparator.comparingInt(this::parkingLotPriority)
                        .thenComparing(ParkingLot::getName, String.CASE_INSENSITIVE_ORDER))
                .findFirst().orElse(null);
    }

    private int parkingLotPriority(ParkingLot lot) {
        if (ParkingLotStatus.SETUP.equals(lot.getStatus())) return 0;
        if (ParkingLotStatus.ACTIVE.equals(lot.getStatus())) return 1;
        return 2;
    }

    private ParkingLotReadiness toReadiness(ParkingLot lot, Set<String> permissions, boolean evaluated) {
        if (!evaluated) {
            return new ParkingLotReadiness(lot.getParkingLotId(), lot.getName(),
                    lot.getStatus() == null ? null : lot.getStatus().name(),
                    lot.getManagementState() == null ? null : lot.getManagementState().name(),
                    false, false, false, false, false, false,
                    false, false, false, false, false, false);
        }
        ParkingLotActivationReadiness activation = parkingLotPortOut.getActivationReadiness(lot.getParkingLotId());
        boolean locationReady = lot.getAddressDisplay() != null && !lot.getAddressDisplay().isBlank()
                && lot.getCurrentWardCode() != null && lot.getLatitude() != null && lot.getLongitude() != null
                && (GeocodingStatus.RESOLVED.equals(lot.getGeocodingStatus())
                    || GeocodingStatus.MANUAL_CONFIRMED.equals(lot.getGeocodingStatus()));
        boolean active = ParkingLotStatus.ACTIVE.equals(lot.getStatus());
        boolean operable = active && canOperate(lot);
        boolean checkIn = operable && permissions.contains(ParkingSessionAccessGuard.PARKING_SESSION_CHECK_IN_ALL);
        boolean checkOut = operable && permissions.contains(ParkingSessionAccessGuard.PARKING_SESSION_CHECK_OUT_ALL);
        boolean exceptionCapability = permissions.contains(ParkingSessionAccessGuard.PARKING_SESSION_PLATE_REVIEW_ALL)
                || permissions.contains(ParkingSessionAccessGuard.PARKING_SESSION_PLATE_OVERRIDE_ALL)
                || permissions.contains("LOST_CARD_REPORT_CREATE_ALL")
                || permissions.contains("LOST_CARD_REPORT_UPDATE_ALL");
        return new ParkingLotReadiness(lot.getParkingLotId(), lot.getName(),
                lot.getStatus() == null ? null : lot.getStatus().name(),
                lot.getManagementState() == null ? null : lot.getManagementState().name(), true,
                locationReady, activation.catalogAndPricingReady(),
                activation.activeZoneReady() && activation.activeGateReady()
                        && activation.activeInLaneReady() && activation.activeOutLaneReady()
                        && activation.publishedSpaceLayoutsReady(),
                locationReady && activation.ready(), operable, checkIn,
                operable && hasAny(permissions, "CARD_READ_ALL", "PARKING_SESSION_CHECK_IN_ALL", "PARKING_SESSION_CHECK_OUT_ALL"),
                checkOut, operable && permissions.contains("PAYMENT_CREATE_ALL"), checkOut,
                operable && exceptionCapability);
    }

    private boolean canOperate(ParkingLot lot) {
        try {
            parkingSessionAccessGuard.ensureCanOperateParkingLot(lot);
            return !ParkingLotManagementState.OPERATION_SUSPENDED.equals(lot.getManagementState());
        } catch (AccessDeniedException exception) {
            return false;
        }
    }

    private String operationBlockReason(ParkingLot lot, ParkingLotReadiness readiness) {
        if (lot == null) return "Chưa có bãi trong phạm vi hiện tại.";
        if (!ParkingLotStatus.ACTIVE.equals(lot.getStatus())) return "Bãi chưa được kích hoạt.";
        if (ParkingLotManagementState.OPERATION_SUSPENDED.equals(lot.getManagementState())) {
            return "Hoạt động của bãi đang bị đình chỉ.";
        }
        if (readiness == null || !readiness.operableNow()) {
            return "Phân công, scope hoặc ca OPEN hiện tại chưa đáp ứng điều kiện vận hành bãi.";
        }
        return null;
    }

    private boolean hasAny(Set<String> permissions, String... required) {
        for (String permission : required) if (permissions.contains(permission)) return true;
        return false;
    }
}
