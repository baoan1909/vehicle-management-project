package com.ban.vehicle_management.application.notification.userguide.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.iam.organization.model.result.ParkingLotAccessScope;
import com.ban.vehicle_management.application.notification.userguide.model.result.PostOnboardingReadinessResult;
import com.ban.vehicle_management.application.notification.userguide.model.result.ResolvedUserGuide;
import com.ban.vehicle_management.application.notification.userguide.port.in.UserGuideRuntimePortIn;
import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuidePortOut;
import com.ban.vehicle_management.application.parking.parkinglot.model.result.ParkingLotActivationReadiness;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.authorization.ParkingSessionAccessGuard;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuide;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideJourneyStepConfig;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideStep;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideVersion;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.parking.GeocodingStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotManagementState;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideDisplayType;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class PostOnboardingReadinessUseCaseImplTest {
    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID ORGANIZATION_ID = UUID.randomUUID();
    private static final UUID PARKING_LOT_ID = UUID.randomUUID();

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;
    @Mock
    private OrganizationAccessGuard organizationAccessGuard;
    @Mock
    private ParkingLotPortOut parkingLotPortOut;
    @Mock
    private ParkingSessionAccessGuard parkingSessionAccessGuard;
    @Mock
    private UserGuideRuntimePortIn userGuideRuntimePortIn;
    @Mock
    private UserGuidePortOut userGuidePortOut;

    private PostOnboardingReadinessUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new PostOnboardingReadinessUseCaseImpl(
                currentAccountPortIn,
                organizationAccessGuard,
                parkingLotPortOut,
                parkingSessionAccessGuard,
                userGuideRuntimePortIn,
                userGuidePortOut,
                new JourneyStepReadinessEvaluatorRegistry()
        );
        when(userGuideRuntimePortIn.resolveManualGuides(
                anyString(), anyString(), eq(UserGuideDisplayType.JOURNEY)))
                .thenReturn(List.of(journey()));
    }

    @Test
    void offersParkingLotCreationWhenPartnerScopeHasNoLot() {
        Set<String> permissions = Set.of(
                "PARKING_SCOPE_PARTNER",
                "PARKING_LOT_READ_ALL",
                "PARKING_LOT_CREATE_ALL",
                "PRICE_PLAN_CREATE_ALL",
                "PARKING_SESSION_CHECK_IN_ALL"
        );
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(account(permissions));
        when(organizationAccessGuard.resolveParkingLotAccessScope()).thenReturn(
                new ParkingLotAccessScope(false, Set.of(ORGANIZATION_ID), Set.of())
        );
        when(parkingLotPortOut.findAll(isNull(), isNull(), any(), isNull())).thenReturn(List.of());

        PostOnboardingReadinessResult result = useCase.getReadiness(null);

        assertEquals("POST_ONBOARDING_TEST_JOURNEY", result.guideCode());
        assertTrue(result.parkingLots().isEmpty());
        assertEquals("ACTIONABLE", step(result, "PARKING_SCOPE").state());
        assertEquals("BLOCKED", step(result, "CHECK_IN").state());
    }

    @Test
    void reportsEmployeeShiftBlockWithoutRemovingGrantedPermissions() {
        Set<String> permissions = Set.of(
                "PARKING_SCOPE_ASSIGNED",
                "PARKING_LOT_READ_ALL",
                "PARKING_SESSION_CHECK_IN_ALL",
                "PARKING_SESSION_CHECK_OUT_ALL"
        );
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(account(permissions));
        when(organizationAccessGuard.resolveParkingLotAccessScope()).thenReturn(
                new ParkingLotAccessScope(false, Set.of(), Set.of(PARKING_LOT_ID))
        );
        ParkingLot lot = activeLot();
        when(parkingLotPortOut.findAll(isNull(), isNull(), isNull(), any())).thenReturn(List.of(lot));
        when(parkingLotPortOut.getActivationReadiness(PARKING_LOT_ID)).thenReturn(ready());
        doThrow(new AccessDeniedException("Active shift required"))
                .when(parkingSessionAccessGuard).ensureCanOperateParkingLot(lot);

        PostOnboardingReadinessResult result = useCase.getReadiness(PARKING_LOT_ID);

        assertFalse(result.parkingLots().getFirst().operableNow());
        assertEquals("BLOCKED", step(result, "CHECK_IN").state());
        assertEquals("BLOCKED", step(result, "COMPLETE_CHECK_OUT").state());
        assertTrue(step(result, "CHECK_IN").description().contains("Phân công"));
    }

    @Test
    void separatesCheckOutAndPaymentCapabilitiesForOperableLot() {
        Set<String> permissions = Set.of(
                "PARKING_SCOPE_ASSIGNED",
                "PARKING_LOT_READ_ALL",
                "PARKING_SESSION_CHECK_IN_ALL",
                "PARKING_SESSION_CHECK_OUT_ALL"
        );
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(account(permissions));
        when(organizationAccessGuard.resolveParkingLotAccessScope()).thenReturn(
                new ParkingLotAccessScope(false, Set.of(), Set.of(PARKING_LOT_ID))
        );
        ParkingLot lot = activeLot();
        when(parkingLotPortOut.findAll(isNull(), isNull(), isNull(), any())).thenReturn(List.of(lot));
        when(parkingLotPortOut.getActivationReadiness(PARKING_LOT_ID)).thenReturn(ready());

        PostOnboardingReadinessResult result = useCase.getReadiness(PARKING_LOT_ID);

        assertEquals("ACTIONABLE", step(result, "CHECK_IN").state());
        assertEquals("ACTIONABLE", step(result, "PREPARE_CHECK_OUT").state());
        assertEquals("WAITING_FOR_PERMISSION", step(result, "COLLECT_PAYMENT").state());
        verify(parkingLotPortOut, times(1)).getActivationReadiness(PARKING_LOT_ID);
    }

    @Test
    void keepsUnassignedManagerInWaitingStateInsteadOfReturningForbidden() {
        Set<String> assignedRolePermissions = Set.of(
                "PARKING_SCOPE_ASSIGNED",
                "PARKING_LOT_READ_ALL",
                "PARKING_SPACE_CONFIGURE_ALL"
        );
        CurrentAccountAccess managerWithoutLot = new CurrentAccountAccess(
                ACCOUNT_ID,
                "subject",
                "manager",
                "manager@example.com",
                UUID.randomUUID(),
                "PARKING_MANAGER",
                true,
                false,
                Set.of(),
                Set.of(),
                AccountStatus.ACTIVE,
                EmployeeStatus.ACTIVE,
                null,
                null,
                false,
                assignedRolePermissions
        );
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(managerWithoutLot);
        when(organizationAccessGuard.resolveParkingLotAccessScope())
                .thenThrow(new AccessDeniedException("No effective parking scope"));

        PostOnboardingReadinessResult result = useCase.getReadiness(null);

        assertTrue(result.parkingLots().isEmpty());
        assertEquals("WAITING_FOR_PERMISSION", step(result, "PARKING_SCOPE").state());
        assertTrue(step(result, "PARKING_SCOPE").description().contains("phân công bãi"));
    }

    private PostOnboardingReadinessResult.JourneyStep step(
            PostOnboardingReadinessResult result,
            String code
    ) {
        return result.steps().stream()
                .filter(item -> code.equals(item.stepKey()))
                .findFirst()
                .orElseThrow();
    }

    private ResolvedUserGuide journey() {
        List<String> keys = List.of(
                "ACCOUNT_ACCESS", "PARKING_SCOPE", "CATALOG_PRICING", "PARKING_TOPOLOGY",
                "PARKING_ACTIVATION", "WORKFORCE_SHIFT", "CHECK_IN", "FIND_SESSION",
                "PREPARE_CHECK_OUT", "COLLECT_PAYMENT", "COMPLETE_CHECK_OUT", "EXCEPTIONS");
        UUID guideId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UserGuide guide = new UserGuide();
        guide.setGuideId(guideId);
        guide.setCode("POST_ONBOARDING_TEST_JOURNEY");
        guide.setStatus(UserGuideStatus.PUBLISHED);
        guide.setDisplayType(UserGuideDisplayType.JOURNEY);
        UserGuideVersion version = new UserGuideVersion();
        version.setGuideVersionId(versionId);
        version.setGuideId(guideId);
        version.setVersionNumber(1);
        version.setTitle("Lộ trình kiểm thử");
        version.setSummary("Nội dung được cấu hình từ User Guide");
        version.setSteps(keys.stream().map(key -> {
            int order = keys.indexOf(key);
            UserGuideJourneyStepConfig config = new UserGuideJourneyStepConfig();
            config.setStepKey(key);
            config.setReadinessKey(key);
            config.setStateContent(Map.of());
            String body = switch (key) {
                case "CHECK_IN" -> "Phân công, scope hoặc ca OPEN chưa đáp ứng.";
                case "PARKING_SCOPE" -> "Cần phân công bãi trước khi vận hành.";
                default -> "Trạng thái " + key;
            };
            return new UserGuideStep(UUID.randomUUID(), versionId, order, key, body,
                    null, null, null, 0, config);
        }).toList());
        return new ResolvedUserGuide(guide, version, null, Map.of());
    }

    private CurrentAccountAccess account(Set<String> permissions) {
        boolean partner = permissions.contains("PARKING_SCOPE_PARTNER");
        return new CurrentAccountAccess(
                ACCOUNT_ID,
                "subject",
                "operator",
                "operator@example.com",
                UUID.randomUUID(),
                partner ? "PARTNER_ADMIN" : "EMPLOYEE",
                true,
                partner,
                partner ? Set.of(ORGANIZATION_ID) : Set.of(),
                partner ? Set.of() : Set.of(PARKING_LOT_ID),
                AccountStatus.ACTIVE,
                partner ? null : EmployeeStatus.ACTIVE,
                null,
                null,
                false,
                permissions
        );
    }

    private ParkingLot activeLot() {
        ParkingLot lot = new ParkingLot();
        lot.setParkingLotId(PARKING_LOT_ID);
        lot.setOrganizationId(ORGANIZATION_ID);
        lot.setName("Bãi trung tâm");
        lot.setStatus(ParkingLotStatus.ACTIVE);
        lot.setManagementState(ParkingLotManagementState.MANAGED);
        lot.setAddressDisplay("Quận 1");
        lot.setCurrentWardCode("26734");
        lot.setLatitude(BigDecimal.valueOf(10.77));
        lot.setLongitude(BigDecimal.valueOf(106.69));
        lot.setGeocodingStatus(GeocodingStatus.RESOLVED);
        return lot;
    }

    private ParkingLotActivationReadiness ready() {
        return new ParkingLotActivationReadiness(true, true, true, true, true, true);
    }
}
