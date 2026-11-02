package com.ban.vehicle_management.application.iam.organization.usecase;

import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.AccountAccessInvalidationPortIn;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.iam.organization.authorization.ParkingLotScopeResolver;
import com.ban.vehicle_management.application.iam.organization.port.in.ParkingLotAssignmentPortIn;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.application.iam.organization.port.out.ParkingLotAssignmentPortOut;
import com.ban.vehicle_management.application.people.employee.port.out.EmployeePortOut;
import com.ban.vehicle_management.domain.audit.auditlog.model.AuditLog;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.iam.organization.model.ParkingLotManagementAssignment;
import com.ban.vehicle_management.domain.people.employee.model.EmployeeParkingLotAssignment;
import com.ban.vehicle_management.shared.enumeration.iam.ManagementAssignmentRole;
import com.ban.vehicle_management.shared.enumeration.iam.ManagementAssignmentStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotManagementState;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeParkingLotAssignmentSource;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeParkingLotAssignmentStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeParkingLotAssignmentType;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.exception.ScopedAccessDeniedException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParkingLotAssignmentUseCaseImpl implements ParkingLotAssignmentPortIn {
    private static final String MANAGE_ORGANIZATION = "ORGANIZATION_MEMBERSHIP_MANAGE_ALL";
    private static final String MANAGE_EMPLOYEE = "EMPLOYEE_UPDATE_ALL";
    private static final String ASSIGNED_SCOPE = "PARKING_SCOPE_ASSIGNED";
    private static final Instant FAR_FUTURE = Instant.parse("9999-12-31T23:59:59Z");

    private final CurrentAccountPortIn currentAccountPortIn;
    private final OrganizationAccessGuard organizationAccessGuard;
    private final OrganizationPortOut organizationPortOut;
    private final ParkingLotAssignmentPortOut assignmentPortOut;
    private final ParkingLotScopeResolver scopeResolver;
    private final EmployeePortOut employeePortOut;
    private final AccountAccessInvalidationPortIn invalidationPortIn;
    private final AuditLogPortOut auditLogPortOut;

    public ParkingLotAssignmentUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            OrganizationAccessGuard organizationAccessGuard,
            OrganizationPortOut organizationPortOut,
            ParkingLotAssignmentPortOut assignmentPortOut,
            ParkingLotScopeResolver scopeResolver,
            EmployeePortOut employeePortOut,
            AccountAccessInvalidationPortIn invalidationPortIn,
            AuditLogPortOut auditLogPortOut
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.organizationAccessGuard = organizationAccessGuard;
        this.organizationPortOut = organizationPortOut;
        this.assignmentPortOut = assignmentPortOut;
        this.scopeResolver = scopeResolver;
        this.employeePortOut = employeePortOut;
        this.invalidationPortIn = invalidationPortIn;
        this.auditLogPortOut = auditLogPortOut;
    }

    @Override
    @Transactional
    public ParkingLotManagementAssignment assignManager(
            UUID organizationId, UUID parkingLotId, UUID managerAccountId,
            ManagementAssignmentRole role, Instant effectiveAt, boolean acceptanceRequired, String handoverNote) {
        requirePartnerManagement(organizationId, parkingLotId);
        validateManager(organizationId, managerAccountId);
        Instant startsAt = effectiveAt == null ? Instant.now() : effectiveAt;
        ManagementAssignmentRole resolvedRole = role == null ? ManagementAssignmentRole.PRIMARY : role;
        assignmentPortOut.lockParkingLot(parkingLotId);
        List<ParkingLotManagementAssignment> overlapping =
                assignmentPortOut.findManagementAssignmentsForUpdate(parkingLotId, startsAt, FAR_FUTURE);
        boolean conflicts = overlapping.stream().anyMatch(existing ->
                Objects.equals(existing.getManagerAccountId(), managerAccountId)
                        || (ManagementAssignmentRole.PRIMARY.equals(resolvedRole)
                        && ManagementAssignmentRole.PRIMARY.equals(existing.getManagementRole())));
        if (conflicts) {
            throw new ConflictException("Management assignment overlaps an existing assignment");
        }
        ParkingLotManagementAssignment assignment = new ParkingLotManagementAssignment();
        assignment.setAssignmentId(UUID.randomUUID());
        assignment.setOrganizationId(organizationId);
        assignment.setParkingLotId(parkingLotId);
        assignment.setManagerAccountId(managerAccountId);
        assignment.setManagementRole(resolvedRole);
        assignment.setStatus(acceptanceRequired
                ? ManagementAssignmentStatus.PENDING_ACCEPTANCE : ManagementAssignmentStatus.ACTIVE);
        assignment.setEffectiveFrom(startsAt);
        assignment.setAcceptedAt(acceptanceRequired ? null : Instant.now());
        assignment.setAssignedBy(currentAccountPortIn.getCurrentAccountIdOrThrow());
        assignment.setHandoverNote(normalizeNote(handoverNote));
        ParkingLotManagementAssignment saved = assignmentPortOut.saveManagementAssignment(assignment);
        assignmentPortOut.updateParkingLotManagementState(parkingLotId,
                acceptanceRequired || startsAt.isAfter(Instant.now())
                        ? ParkingLotManagementState.HANDOVER : ParkingLotManagementState.MANAGED);
        invalidate(managerAccountId, parkingLotId, "Phạm vi quản lý bãi xe đã thay đổi.");
        audit("PARKING_MANAGER_ASSIGNED", saved.getAssignmentId(), Map.of(
                "parkingLotId", parkingLotId, "managerAccountId", managerAccountId,
                "status", saved.getStatus().name()));
        return saved;
    }

    @Override
    @Transactional
    public ParkingLotManagementAssignment replaceManager(
            UUID organizationId, UUID parkingLotId, UUID outgoingManagerId,
            UUID incomingManagerId, Instant effectiveAt, boolean acceptanceRequired, String handoverNote) {
        requirePartnerManagement(organizationId, parkingLotId);
        if (Objects.equals(outgoingManagerId, incomingManagerId)) {
            throw new BadRequestException("Incoming Manager must be different from outgoing Manager");
        }
        validateManager(organizationId, incomingManagerId);
        Instant at = effectiveAt == null ? Instant.now() : effectiveAt;
        assignmentPortOut.lockParkingLot(parkingLotId);
        List<ParkingLotManagementAssignment> current = assignmentPortOut
                .findManagementAssignmentsForUpdate(parkingLotId, at, FAR_FUTURE);
        ParkingLotManagementAssignment outgoing = current.stream()
                .filter(item -> Objects.equals(item.getManagerAccountId(), outgoingManagerId))
                .findFirst().orElseThrow(() -> new NotFoundException("Outgoing Manager assignment not found"));
        outgoing.setStatus(at.isAfter(Instant.now())
                ? ManagementAssignmentStatus.ACTIVE : ManagementAssignmentStatus.ENDED);
        outgoing.setEffectiveTo(at);
        outgoing.setEndedBy(currentAccountPortIn.getCurrentAccountIdOrThrow());
        outgoing.setEndReason("REPLACED");
        assignmentPortOut.saveManagementAssignment(outgoing);

        ParkingLotManagementAssignment incoming = new ParkingLotManagementAssignment();
        incoming.setAssignmentId(UUID.randomUUID());
        incoming.setOrganizationId(organizationId);
        incoming.setParkingLotId(parkingLotId);
        incoming.setManagerAccountId(incomingManagerId);
        incoming.setManagementRole(ManagementAssignmentRole.PRIMARY);
        incoming.setStatus(acceptanceRequired
                ? ManagementAssignmentStatus.PENDING_ACCEPTANCE : ManagementAssignmentStatus.ACTIVE);
        incoming.setEffectiveFrom(at);
        incoming.setAcceptedAt(acceptanceRequired ? null : Instant.now());
        incoming.setAssignedBy(currentAccountPortIn.getCurrentAccountIdOrThrow());
        incoming.setHandoverNote(normalizeNote(handoverNote));
        ParkingLotManagementAssignment saved = assignmentPortOut.saveManagementAssignment(incoming);
        assignmentPortOut.updateParkingLotManagementState(parkingLotId,
                acceptanceRequired || at.isAfter(Instant.now())
                        ? ParkingLotManagementState.HANDOVER : ParkingLotManagementState.MANAGED);
        invalidate(outgoingManagerId, parkingLotId, "Quyền quản lý bãi xe đã được thu hồi.");
        invalidate(incomingManagerId, parkingLotId, "Bạn được phân công tiếp nhận bãi xe.");
        audit("PARKING_MANAGER_REPLACED", saved.getAssignmentId(), Map.of(
                "parkingLotId", parkingLotId,
                "outgoingManagerId", outgoingManagerId,
                "incomingManagerId", incomingManagerId));
        return saved;
    }

    @Override
    @Transactional
    public ParkingLotManagementAssignment acceptManagementAssignment(UUID assignmentId) {
        ParkingLotManagementAssignment assignment = assignmentPortOut.findManagementAssignmentForUpdate(assignmentId)
                .orElseThrow(() -> new NotFoundException("Management assignment not found"));
        if (!Objects.equals(assignment.getManagerAccountId(), currentAccountPortIn.getCurrentAccountIdOrThrow())) {
            throw new ScopedAccessDeniedException("MANAGER_ACCEPTANCE_REQUIRED", "Only the assigned Manager may accept handover");
        }
        if (!ManagementAssignmentStatus.PENDING_ACCEPTANCE.equals(assignment.getStatus())) {
            throw new ConflictException("Management assignment is not pending acceptance");
        }
        assignment.setStatus(ManagementAssignmentStatus.ACTIVE);
        assignment.setAcceptedAt(Instant.now());
        ParkingLotManagementAssignment saved = assignmentPortOut.saveManagementAssignment(assignment);
        assignmentPortOut.updateParkingLotManagementState(
                assignment.getParkingLotId(), assignment.getEffectiveFrom().isAfter(Instant.now())
                        ? ParkingLotManagementState.HANDOVER : ParkingLotManagementState.MANAGED);
        invalidate(assignment.getManagerAccountId(), assignment.getParkingLotId(), "Bàn giao bãi xe đã được tiếp nhận.");
        audit("PARKING_MANAGER_ACCEPTED", assignmentId, Map.of("parkingLotId", assignment.getParkingLotId()));
        return saved;
    }

    @Override
    @Transactional
    public void revokeManagerImmediately(UUID managerAccountId, String reason) {
        Instant now = Instant.now();
        List<ParkingLotManagementAssignment> assignments =
                assignmentPortOut.findManagerAssignmentsForUpdate(managerAccountId, now);
        for (ParkingLotManagementAssignment assignment : assignments) {
            assignment.setStatus(ManagementAssignmentStatus.ENDED);
            assignment.setEffectiveTo(now);
            assignment.setEndedBy(currentAccountPortIn.getCurrentAccountIdOrThrow());
            assignment.setEndReason(TextValidationUtils.normalizeNullableText(reason, "reason", 500));
        }
        assignmentPortOut.saveManagementAssignments(assignments);
        assignments.forEach(assignment -> {
            boolean anotherActiveManager = assignmentPortOut.hasEffectiveActiveManager(
                    assignment.getParkingLotId(), now);
            assignmentPortOut.updateParkingLotManagementState(
                    assignment.getParkingLotId(), anotherActiveManager
                            ? ParkingLotManagementState.MANAGED
                            : ParkingLotManagementState.PARTNER_SUPERVISED);
            audit("PARKING_LOT_PARTNER_SUPERVISED", assignment.getParkingLotId(),
                    Map.of("revokedManagerId", managerAccountId));
        });
        if (!assignments.isEmpty()) {
            invalidate(managerAccountId, assignments.get(0).getParkingLotId(), "Quyền quản lý bãi xe đã bị thu hồi.");
        }
    }

    @Override
    @Transactional
    public void endManagerAssignment(
            UUID organizationId, UUID parkingLotId, UUID managerAccountId, String reason) {
        requirePartnerManagement(organizationId, parkingLotId);
        Instant now = Instant.now();
        assignmentPortOut.lockParkingLot(parkingLotId);
        List<ParkingLotManagementAssignment> assignments = assignmentPortOut
                .findManagementAssignmentsForUpdate(parkingLotId, now, FAR_FUTURE);
        ParkingLotManagementAssignment target = assignments.stream()
                .filter(item -> Objects.equals(item.getManagerAccountId(), managerAccountId))
                .findFirst().orElseThrow(() -> new NotFoundException("Manager assignment not found"));
        target.setStatus(ManagementAssignmentStatus.ENDED);
        target.setEffectiveTo(now);
        target.setEndedBy(currentAccountPortIn.getCurrentAccountIdOrThrow());
        target.setEndReason(TextValidationUtils.normalizeRequiredText(reason, "reason", 500));
        assignmentPortOut.saveManagementAssignment(target);
        boolean anotherActiveManager = assignments.stream()
                .anyMatch(item -> !Objects.equals(item.getAssignmentId(), target.getAssignmentId())
                        && ManagementAssignmentStatus.ACTIVE.equals(item.getStatus()));
        assignmentPortOut.updateParkingLotManagementState(parkingLotId,
                anotherActiveManager ? ParkingLotManagementState.MANAGED : ParkingLotManagementState.PARTNER_SUPERVISED);
        invalidate(managerAccountId, parkingLotId, "Quyền quản lý bãi xe đã bị thu hồi.");
        audit("PARKING_MANAGER_REVOKED", target.getAssignmentId(), Map.of(
                "parkingLotId", parkingLotId, "managerAccountId", managerAccountId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingLotManagementAssignment> getManagementHistory(UUID organizationId, UUID parkingLotId) {
        requirePartnerManagement(organizationId, parkingLotId);
        return assignmentPortOut.findManagementHistory(parkingLotId);
    }

    @Override
    @Transactional
    public EmployeeParkingLotAssignment assignEmployee(
            UUID organizationId, UUID parkingLotId, UUID employeeId,
            EmployeeParkingLotAssignmentType type, Instant effectiveFrom, Instant effectiveTo) {
        currentAccountPortIn.requirePermission(MANAGE_EMPLOYEE);
        ensureCanAssignEmployeeAtLot(organizationId, parkingLotId);
        validateEmployee(organizationId, employeeId);
        Instant from = effectiveFrom == null ? Instant.now() : effectiveFrom;
        if (effectiveTo != null && !effectiveTo.isAfter(from)) {
            throw new BadRequestException("effectiveTo must be after effectiveFrom");
        }
        if (!assignmentPortOut.findEmployeeAssignmentsForUpdate(employeeId, parkingLotId, from).isEmpty()) {
            throw new ConflictException("Employee already has an effective parking-lot assignment");
        }
        UUID actor = currentAccountPortIn.getCurrentAccountIdOrThrow();
        EmployeeParkingLotAssignment assignment = new EmployeeParkingLotAssignment();
        assignment.setAssignmentId(UUID.randomUUID());
        assignment.setOrganizationId(organizationId);
        assignment.setParkingLotId(parkingLotId);
        assignment.setEmployeeId(employeeId);
        assignment.setAssignmentType(type == null ? EmployeeParkingLotAssignmentType.PRIMARY : type);
        assignment.setStatus(from.isAfter(Instant.now())
                ? EmployeeParkingLotAssignmentStatus.SCHEDULED : EmployeeParkingLotAssignmentStatus.ACTIVE);
        assignment.setEffectiveFrom(from);
        assignment.setEffectiveTo(effectiveTo);
        assignment.setAssignedByAccountId(actor);
        assignment.setAssignmentSource(isPartner()
                ? EmployeeParkingLotAssignmentSource.PARTNER : EmployeeParkingLotAssignmentSource.MANAGER);
        EmployeeParkingLotAssignment saved = assignmentPortOut.saveEmployeeAssignment(assignment);
        employeePortOut.findAccountIdByEmployeeId(employeeId)
                .ifPresent(accountId -> invalidate(accountId, parkingLotId, "Phạm vi bãi xe làm việc đã thay đổi."));
        audit("EMPLOYEE_PARKING_LOT_ASSIGNED", saved.getAssignmentId(), Map.of(
                "parkingLotId", parkingLotId, "employeeId", employeeId, "source", saved.getAssignmentSource().name()));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeParkingLotAssignment> getEmployeeAssignments(UUID organizationId) {
        currentAccountPortIn.requirePermission("EMPLOYEE_READ_ALL");
        organizationAccessGuard.ensureCanAccessOrganization(organizationId);
        return assignmentPortOut.findEmployeeAssignmentsByOrganizationId(organizationId);
    }

    @Override
    @Transactional
    public EmployeeParkingLotAssignment transferEmployee(
            UUID organizationId, UUID employeeId, UUID sourceParkingLotId,
            UUID targetParkingLotId, Instant effectiveAt, String reason) {
        currentAccountPortIn.requirePermission(MANAGE_ORGANIZATION);
        organizationAccessGuard.ensureCanManageOrganization(organizationId);
        if (Objects.equals(sourceParkingLotId, targetParkingLotId)) {
            throw new BadRequestException("Source and target parking lots must be different");
        }
        requireLotInOrganization(organizationId, sourceParkingLotId);
        requireLotInOrganization(organizationId, targetParkingLotId);
        Instant at = effectiveAt == null ? Instant.now() : effectiveAt;
        List<EmployeeParkingLotAssignment> sourceAssignments =
                assignmentPortOut.findEmployeeAssignmentsForUpdate(employeeId, sourceParkingLotId, at);
        if (sourceAssignments.isEmpty()) {
            throw new NotFoundException("Active Employee parking-lot assignment not found");
        }
        UUID actor = currentAccountPortIn.getCurrentAccountIdOrThrow();
        String normalizedReason = TextValidationUtils.normalizeRequiredText(reason, "reason", 500);
        sourceAssignments.forEach(source -> {
            source.setStatus(at.isAfter(Instant.now())
                    ? EmployeeParkingLotAssignmentStatus.ACTIVE
                    : EmployeeParkingLotAssignmentStatus.ENDED);
            source.setEffectiveTo(at);
            source.setEndedByAccountId(actor);
            source.setEndReason(normalizedReason);
            assignmentPortOut.saveEmployeeAssignment(source);
        });
        EmployeeParkingLotAssignment target = assignEmployee(
                organizationId, targetParkingLotId, employeeId,
                EmployeeParkingLotAssignmentType.PRIMARY, at, null);
        audit("EMPLOYEE_PARKING_LOT_TRANSFERRED", target.getAssignmentId(), Map.of(
                "employeeId", employeeId, "sourceParkingLotId", sourceParkingLotId,
                "targetParkingLotId", targetParkingLotId));
        return target;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> getMyReadableParkingLots() {
        currentAccountPortIn.requirePermission(ASSIGNED_SCOPE);
        return scopeResolver.resolveCurrentEmployeeReadableLots();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> getMyOperableParkingLots() {
        currentAccountPortIn.requirePermission(ASSIGNED_SCOPE);
        return scopeResolver.resolveCurrentEmployeeOperableLots();
    }

    @Override
    @Transactional
    public void suspendParkingLotOperations(UUID organizationId, UUID parkingLotId, String reason) {
        requirePartnerManagement(organizationId, parkingLotId);
        String normalizedReason = TextValidationUtils.normalizeRequiredText(reason, "reason", 500);
        assignmentPortOut.lockParkingLot(parkingLotId);
        assignmentPortOut.updateParkingLotManagementState(
                parkingLotId, ParkingLotManagementState.OPERATION_SUSPENDED);
        invalidateOperationalAccounts(parkingLotId, "Hoạt động tại bãi xe đã bị đình chỉ.");
        audit("PARKING_LOT_OPERATION_SUSPENDED", parkingLotId, Map.of("reason", normalizedReason));
    }

    @Override
    @Transactional
    public void resumeParkingLotOperations(UUID organizationId, UUID parkingLotId) {
        requirePartnerManagement(organizationId, parkingLotId);
        Instant now = Instant.now();
        assignmentPortOut.lockParkingLot(parkingLotId);
        boolean managed = assignmentPortOut.hasEffectiveActiveManager(parkingLotId, now);
        assignmentPortOut.updateParkingLotManagementState(parkingLotId,
                managed ? ParkingLotManagementState.MANAGED : ParkingLotManagementState.PARTNER_SUPERVISED);
        invalidateOperationalAccounts(parkingLotId, "Hoạt động tại bãi xe đã được khôi phục.");
        audit("PARKING_LOT_OPERATION_RESUMED", parkingLotId, Map.of("managed", managed));
    }

    private void requirePartnerManagement(UUID organizationId, UUID parkingLotId) {
        currentAccountPortIn.requirePermission(MANAGE_ORGANIZATION);
        organizationAccessGuard.ensureCanManageOrganization(organizationId);
        requireLotInOrganization(organizationId, parkingLotId);
    }

    private void requireLotInOrganization(UUID organizationId, UUID parkingLotId) {
        if (!organizationPortOut.allParkingLotsBelongToOrganization(organizationId, Set.of(parkingLotId))) {
            throw new NotFoundException("Parking lot not found in organization");
        }
    }

    private void validateManager(UUID organizationId, UUID managerAccountId) {
        if (!organizationPortOut.isActiveAccountWithRole(managerAccountId, OrganizationAccessGuard.PARKING_MANAGER)
                || organizationPortOut.findActiveMembershipId(organizationId, managerAccountId).isEmpty()) {
            throw new ScopedAccessDeniedException(
                    "CROSS_ORGANIZATION_ACCESS_DENIED", "Active Manager must belong to the parking-lot organization");
        }
    }

    private void validateEmployee(UUID organizationId, UUID employeeId) {
        var employee = employeePortOut.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));
        UUID accountId = employeePortOut.findAccountIdByEmployeeId(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee account not found"));
        if (!EmployeeStatus.ACTIVE.equals(employee.getStatus())
                || !employeePortOut.hasAccountRole(employeeId, "EMPLOYEE")
                || organizationPortOut.findActiveMembershipId(organizationId, accountId).isEmpty()) {
            throw new ScopedAccessDeniedException(
                    "CROSS_ORGANIZATION_ACCESS_DENIED", "Active Employee must belong to the parking-lot organization");
        }
    }

    private void ensureCanAssignEmployeeAtLot(UUID organizationId, UUID parkingLotId) {
        requireLotInOrganization(organizationId, parkingLotId);
        if (isPartner()) {
            organizationAccessGuard.ensureCanManageOrganization(organizationId);
            return;
        }
        UUID actor = currentAccountPortIn.getCurrentAccountIdOrThrow();
        if (!scopeResolver.resolveManagerManagedLots(actor).contains(parkingLotId)) {
            throw new ScopedAccessDeniedException(
                    "MANAGER_NOT_ASSIGNED_TO_PARKING_LOT", "Manager is not assigned to this parking lot");
        }
    }

    private boolean isPartner() {
        return currentAccountPortIn.getCurrentAccountOrThrow().getEffectivePermissionCodes()
                .contains(OrganizationAccessGuard.PARKING_SCOPE_PARTNER);
    }

    private String normalizeNote(String note) {
        return TextValidationUtils.normalizeNullableText(note, "handoverNote", 1000);
    }

    private void invalidate(UUID accountId, UUID lotId, String reason) {
        invalidationPortIn.invalidateAccount(accountId, reason, "parking", "parking_lots", lotId);
    }

    private void invalidateOperationalAccounts(UUID parkingLotId, String reason) {
        organizationPortOut.findActiveOperationalAccountIdsByParkingLotId(parkingLotId)
                .forEach(accountId -> invalidate(accountId, parkingLotId, reason));
    }

    private void audit(String action, UUID targetId, Map<String, Object> data) {
        AuditLog log = new AuditLog();
        log.setAuditLogId(UUID.randomUUID());
        log.setActorAccountId(currentAccountPortIn.getCurrentAccountIdOrThrow());
        log.setAction(action);
        log.setTargetSchema("iam");
        log.setTargetTable("parking_lot_management_assignments");
        log.setTargetId(targetId);
        log.setNewData(data);
        auditLogPortOut.save(log);
    }
}
