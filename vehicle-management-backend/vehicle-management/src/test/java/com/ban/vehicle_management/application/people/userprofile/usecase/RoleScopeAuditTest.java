package com.ban.vehicle_management.application.people.userprofile.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountProfilePortOut;
import com.ban.vehicle_management.application.billing.payment.authorization.PaymentAccessGuard;
import com.ban.vehicle_management.application.billing.payment.port.out.EmployeePaymentScopePortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkingsession.authorization.EmployeeParkingLotAccessGuard;
import com.ban.vehicle_management.application.parking.parkingsession.authorization.ParkingSessionAccessGuard;
import com.ban.vehicle_management.application.notification.broadcastannouncement.mapper.BroadcastAnnouncementDomainMapper;
import com.ban.vehicle_management.application.notification.broadcastannouncement.port.out.BroadcastAnnouncementPortOut;
import com.ban.vehicle_management.application.notification.broadcastannouncement.usecase.BroadcastAnnouncementUseCaseImpl;
import com.ban.vehicle_management.application.notification.notification.model.BroadcastNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.application.people.userprofile.port.in.UserProfileAvatarPortIn;
import com.ban.vehicle_management.application.people.userprofile.port.out.UserProfilePortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.notification.broadcastannouncement.model.BroadcastAnnouncement;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.people.userprofile.policy.UserProfilePolicy;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.notification.BroadcastAnnouncementAudienceType;
import com.ban.vehicle_management.shared.enumeration.notification.BroadcastAnnouncementStatus;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

/**
 * Regression coverage for the role/scope audit fixes.
 * Run with -Drole.scope.audit=true -Dtest=RoleScopeAuditTest. No real data or notifications.
 * Only the evidence exporter is opt-in; security assertions always run.
 */
@ExtendWith(MockitoExtension.class)
class RoleScopeAuditTest {
    @Mock CurrentAccountPortIn currentAccountPortIn;
    @Mock UserProfilePortOut profiles;
    @Mock UserProfileAvatarPortIn avatars;
    @Spy UserProfilePolicy profilePolicy = new UserProfilePolicy();
    @Mock BroadcastAnnouncementPortOut announcements;
    @Mock NotificationPortIn notifications;
    @Mock BroadcastAnnouncementDomainMapper announcementMapper;
    @InjectMocks UserProfileUseCaseImpl profileUseCase;
    @InjectMocks BroadcastAnnouncementUseCaseImpl announcementUseCase;
    @Mock OrganizationAccessGuard organizationAccessGuard;
    @Mock EmployeeParkingLotAccessGuard employeeParkingLotAccessGuard;
    @InjectMocks ParkingSessionAccessGuard parkingSessionAccessGuard;
    @Mock AccountProfilePortOut accountProfilePortOut;
    @Mock ParkingLotPortOut parkingLotPortOut;
    @Mock EmployeePaymentScopePortOut employeePaymentScopePortOut;
    @InjectMocks PaymentAccessGuard paymentAccessGuard;

    @Test
    @EnabledIfSystemProperty(named = "role.scope.audit", matches = "true")
    void exportEffectiveGrantsForFullyApprovedSyntheticActors() throws Exception {
        Path evidence = Path.of("docs/qa/role-permission-audit-2026-11-28");
        ObjectMapper mapper = new ObjectMapper();
        String json = Files.readString(evidence.resolve("role-permissions.json")).replace("\uFEFF", "");
        ArrayNode output = mapper.createArrayNode();
        for (var row : mapper.readTree(json)) {
            String role = row.get("code").asText();
            Set<String> grants = new java.util.HashSet<>();
            row.get("permissions").forEach(value -> grants.add(value.asText()));
            if (!"SYSTEM_ADMIN".equals(role)) grants.removeIf(p -> p.startsWith("BROADCAST_NOTIFICATION_"));
            if (Set.of("PARKING_MANAGER", "EMPLOYEE", "PARTNER_ADMIN").contains(role))
                grants.removeAll(Set.of("USER_PROFILE_CREATE_ALL", "USER_PROFILE_READ_ALL", "USER_PROFILE_UPDATE_ALL"));
            if (Set.of("SYSTEM_ADMIN", "PARTNER_ADMIN", "PARKING_MANAGER").contains(role))
                grants.addAll(Set.of("AVATAR_APPROVAL_READ_ALL", "AVATAR_APPROVAL_REVIEW_ALL"));
            boolean employee = Set.of("EMPLOYEE", "PARKING_MANAGER").contains(role);
            boolean customer = "CUSTOMER".equals(role);
            boolean partner = "PARTNER_ADMIN".equals(role);
            CurrentAccountAccess actor = new CurrentAccountAccess(UUID.randomUUID(), "audit", "audit", null,
                    UUID.randomUUID(), role, true, partner,
                    employee || partner ? Set.of(UUID.randomUUID()) : Set.of(),
                    employee ? Set.of(UUID.randomUUID()) : Set.of(), AccountStatus.ACTIVE,
                    employee ? EmployeeStatus.ACTIVE : null,
                    customer ? CustomerStatus.ACTIVE : null,
                    customer ? CustomerApprovalStatus.APPROVED : null, false, grants);
            var result = output.addObject();
            result.put("code", role);
            result.put("assignedCount", grants.size());
            result.set("permissions", mapper.valueToTree(actor.getEffectivePermissionCodes()));
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(evidence.resolve("effective-permissions-after-fix.json").toFile(), output);
        assertEquals(5, output.size());
    }

    @Test
    void assignedEmployeeCannotReadUnrelatedFullProfile() {
        authenticateAssignedActor("EMPLOYEE", Set.of("USER_PROFILE_READ_ALL"));
        assertThrows(AccessDeniedException.class, () -> profileUseCase.getUserProfileById(UUID.randomUUID()));
        verifyNoInteractions(profiles, avatars);
    }

    @Test
    void assignedEmployeeCannotListFullProfileDirectory() {
        authenticateAssignedActor("EMPLOYEE", Set.of("USER_PROFILE_READ_ALL"));
        assertThrows(AccessDeniedException.class, () -> profileUseCase.getUserProfiles(null, null));
        verifyNoInteractions(profiles, avatars);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"EMPLOYEE", "PARKING_MANAGER", "PARTNER_ADMIN", "CUSTOMER", "CUSTOM_ADMIN"})
    void onlySystemAdminCanUseGlobalBroadcastGrants(String role) {
        authenticateAssignedActor(role, Set.of("BROADCAST_NOTIFICATION_PUBLISH_ALL"));
        assertThrows(AccessDeniedException.class, () -> announcementUseCase.publishBroadcastAnnouncement(UUID.randomUUID()));
        verifyNoInteractions(announcements, notifications);
    }

    @Test
    void systemAdminRetainsExplicitBroadcastPermission() {
        CurrentAccountAccess actor = assignedActor("SYSTEM_ADMIN", Set.of("BROADCAST_NOTIFICATION_PUBLISH_ALL"));
        assertTrue(actor.getEffectivePermissionCodes().contains("BROADCAST_NOTIFICATION_PUBLISH_ALL"));
        assertFalse(assignedActor("SYSTEM_ADMIN", Set.of()).getEffectivePermissionCodes().contains("BROADCAST_NOTIFICATION_PUBLISH_ALL"));
    }

    @Test
    void customEmployeeRoleMustUseEmployeeShiftGuard() {
        CurrentAccountAccess actor = assignedActor("EMPLOYEE_CUSTOM",
                Set.of("PARKING_SCOPE_SHIFT", "PARKING_SESSION_CHECK_IN_ALL"));
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(actor);
        ParkingLot lot = new ParkingLot();
        lot.setParkingLotId(actor.assignedParkingLotIds().iterator().next());
        parkingSessionAccessGuard.ensureCanOperateParkingLot(lot);
        verify(employeeParkingLotAccessGuard).ensureCanOperate(lot.getParkingLotId());
        verifyNoInteractions(organizationAccessGuard);
    }

    @Test
    void assignedAndShiftScopesRequireOpenShiftForPayment() {
        CurrentAccountAccess actor = assignedActor("EMPLOYEE", Set.of("PARKING_SCOPE_SHIFT", "PAYMENT_CREATE_ALL"));
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(actor);
        when(employeePaymentScopePortOut.findOpenShiftParkingLotIds(actor.accountId())).thenReturn(Set.of());
        Invoice invoice = new Invoice();
        invoice.setParkingLotId(actor.assignedParkingLotIds().iterator().next());
        assertThrows(AccessDeniedException.class, () -> paymentAccessGuard.ensureCanRecordPayment(invoice));
        verifyNoInteractions(organizationAccessGuard);
    }

    @Test
    void openShiftCannotExtendAssignedParkingLotScope() {
        CurrentAccountAccess actor = assignedActor("EMPLOYEE", Set.of("PARKING_SCOPE_SHIFT", "PAYMENT_CREATE_ALL"));
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(actor);
        UUID otherLot = UUID.randomUUID();
        when(employeePaymentScopePortOut.findOpenShiftParkingLotIds(actor.accountId())).thenReturn(Set.of(otherLot));
        Invoice invoice = new Invoice();
        invoice.setParkingLotId(otherLot);
        assertThrows(AccessDeniedException.class, () -> paymentAccessGuard.ensureCanRecordPayment(invoice));
    }

    @Test
    void assignedEmployeeCanCollectDuringOwnOpenShift() {
        CurrentAccountAccess actor = assignedActor("EMPLOYEE", Set.of("PARKING_SCOPE_SHIFT", "PAYMENT_CREATE_ALL"));
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(actor);
        when(employeePaymentScopePortOut.findOpenShiftParkingLotIds(actor.accountId())).thenReturn(actor.assignedParkingLotIds());
        Invoice invoice = new Invoice();
        invoice.setParkingLotId(actor.assignedParkingLotIds().iterator().next());
        assertDoesNotThrow(() -> paymentAccessGuard.ensureCanRecordPayment(invoice));
    }

    private CurrentAccountAccess assignedActor(String role, Set<String> grants) {
        java.util.HashSet<String> permissions = new java.util.HashSet<>(grants);
        permissions.add("PARKING_SCOPE_ASSIGNED");
        CurrentAccountAccess actor = new CurrentAccountAccess(UUID.randomUUID(), "audit", "audit", null,
                UUID.randomUUID(), role, true, false, Set.of(UUID.randomUUID()), Set.of(UUID.randomUUID()),
                AccountStatus.ACTIVE, EmployeeStatus.ACTIVE, null, null, false, permissions);
        return actor;
    }

    private void authenticateAssignedActor(String role, Set<String> grants) {
        CurrentAccountAccess actor = assignedActor(role, grants);
        doAnswer(invocation -> {
            if (!actor.getEffectivePermissionCodes().contains(invocation.getArgument(0, String.class))) {
                throw new AccessDeniedException("Missing effective permission");
            }
            return null;
        }).when(currentAccountPortIn).requirePermission(anyString());
    }
}
