package com.ban.vehicle_management.application.catalog.voucher.authorization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.domain.catalog.voucher.model.Voucher;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.shared.enumeration.catalog.VoucherScopeType;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;

class VoucherAccessGuardTest {
    private static final UUID ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000811");
    private static final UUID PARKING_LOT_ID = UUID.fromString("00000000-0000-0000-0000-000000000812");

    private CurrentAccountAccess currentAccount;
    private ParkingLot storedParkingLot;
    private VoucherAccessGuard guard;

    @BeforeEach
    void setUp() {
        storedParkingLot = parkingLot();
        guard = new VoucherAccessGuard(currentAccountPort(), parkingLotPort());
    }

    @Test
    void managerAutomaticallyUsesSingleAssignedParkingLot() {
        currentAccount = account(
                "PARKING_MANAGER", Set.of(ORGANIZATION_ID), Set.of(PARKING_LOT_ID), Set.of("PARKING_SCOPE_ASSIGNED"));
        Voucher voucher = new Voucher();

        guard.applyCreationScope(voucher);

        assertEquals(VoucherScopeType.PARKING_LOT, voucher.getScopeType());
        assertEquals(ORGANIZATION_ID, voucher.getOrganizationId());
        assertEquals(PARKING_LOT_ID, voucher.getParkingLotId());
    }

    @Test
    void managerCannotActivateVoucher() {
        currentAccount = account(
                "PARKING_MANAGER", Set.of(ORGANIZATION_ID), Set.of(PARKING_LOT_ID), Set.of("PARKING_SCOPE_ASSIGNED"));
        Voucher voucher = scopedVoucher(VoucherScopeType.PARKING_LOT, ORGANIZATION_ID, PARKING_LOT_ID);

        assertThrows(AccessDeniedException.class, () -> guard.ensureCanChangeStatus(voucher));
    }

    @Test
    void partnerOnlySeesVouchersOwnedByItsOrganization() {
        UUID otherOrganization = UUID.fromString("00000000-0000-0000-0000-000000000899");
        currentAccount = account(
                "PARTNER_ADMIN", Set.of(ORGANIZATION_ID), Set.of(), Set.of("PARKING_SCOPE_PARTNER"));

        List<Voucher> visible = guard.filterVisible(List.of(
                scopedVoucher(VoucherScopeType.PLATFORM, null, null),
                scopedVoucher(VoucherScopeType.PARTNER, ORGANIZATION_ID, null),
                scopedVoucher(VoucherScopeType.PARKING_LOT, ORGANIZATION_ID, PARKING_LOT_ID),
                scopedVoucher(VoucherScopeType.PARTNER, otherOrganization, null)
        ));

        assertEquals(2, visible.size());
    }

    @Test
    void systemAdminCannotCreatePartnerVoucher() {
        currentAccount = account(
                "SYSTEM_ADMIN", Set.of(), Set.of(), Set.of("PARKING_SCOPE_PLATFORM"));
        Voucher voucher = new Voucher();
        voucher.setScopeType(VoucherScopeType.PARTNER);

        assertThrows(AccessDeniedException.class, () -> guard.applyCreationScope(voucher));
    }

    private ParkingLot parkingLot() {
        ParkingLot parkingLot = new ParkingLot();
        parkingLot.setParkingLotId(PARKING_LOT_ID);
        parkingLot.setOrganizationId(ORGANIZATION_ID);
        return parkingLot;
    }

    private Voucher scopedVoucher(VoucherScopeType scope, UUID organizationId, UUID parkingLotId) {
        Voucher voucher = new Voucher();
        voucher.setScopeType(scope);
        voucher.setOrganizationId(organizationId);
        voucher.setParkingLotId(parkingLotId);
        return voucher;
    }

    private CurrentAccountAccess account(
            String roleCode,
            Set<UUID> organizationIds,
            Set<UUID> assignedParkingLotIds,
            Set<String> permissionCodes
    ) {
        return new CurrentAccountAccess(
                UUID.randomUUID(), "subject", "username", "user@example.com", UUID.randomUUID(), roleCode,
                true, true, organizationIds, assignedParkingLotIds, AccountStatus.ACTIVE,
                EmployeeStatus.ACTIVE, null, null, false, null, permissionCodes
        );
    }

    private CurrentAccountPortIn currentAccountPort() {
        return new CurrentAccountPortIn() {
            @Override public Optional<CurrentAccountAccess> getCurrentAccount() { return Optional.ofNullable(currentAccount); }
            @Override public CurrentAccountAccess getCurrentAccountOrThrow() { return currentAccount; }
            @Override public Optional<UUID> getCurrentAccountId() { return Optional.ofNullable(currentAccount).map(CurrentAccountAccess::accountId); }
            @Override public UUID getCurrentAccountIdOrThrow() { return currentAccount.accountId(); }
            @Override public boolean hasPermission(String code) { return currentAccount.getEffectivePermissionCodes().contains(code); }
            @Override public void requirePermission(String code) {
                if (!hasPermission(code)) throw new AccessDeniedException("Access denied");
            }
        };
    }

    private ParkingLotPortOut parkingLotPort() {
        return new ParkingLotPortOut() {
            @Override public ParkingLot save(ParkingLot parkingLot) { return parkingLot; }
            @Override public Optional<ParkingLot> findById(UUID parkingLotId) {
                return storedParkingLot != null && storedParkingLot.getParkingLotId().equals(parkingLotId)
                        ? Optional.of(storedParkingLot) : Optional.empty();
            }
            @Override public List<ParkingLot> findAll(ParkingLotStatus status, String keyword, Set<UUID> organizationIds, Set<UUID> parkingLotIds) { return List.of(storedParkingLot); }
            @Override public boolean existsByOrganizationIdAndCode(UUID organizationId, String code) { return false; }
            @Override public boolean existsByOrganizationIdAndCodeAndParkingLotIdNot(UUID organizationId, String code, UUID parkingLotId) { return false; }
            @Override public boolean hasActiveZones(UUID parkingLotId) { return false; }
            @Override public boolean isReadyForActivation(UUID parkingLotId) { return false; }
        };
    }
}
