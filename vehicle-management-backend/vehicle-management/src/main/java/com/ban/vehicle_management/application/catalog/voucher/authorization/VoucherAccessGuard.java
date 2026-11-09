package com.ban.vehicle_management.application.catalog.voucher.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.domain.catalog.voucher.model.Voucher;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.shared.enumeration.catalog.VoucherScopeType;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class VoucherAccessGuard {
    private static final String PLATFORM_SCOPE = "PARKING_SCOPE_PLATFORM";
    private static final String PARTNER_SCOPE = "PARKING_SCOPE_PARTNER";
    private static final String ASSIGNED_SCOPE = "PARKING_SCOPE_ASSIGNED";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final ParkingLotPortOut parkingLotPortOut;

    public VoucherAccessGuard(CurrentAccountPortIn currentAccountPortIn, ParkingLotPortOut parkingLotPortOut) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.parkingLotPortOut = parkingLotPortOut;
    }

    public void applyCreationScope(Voucher voucher) {
        CurrentAccountAccess account = currentAccountPortIn.getCurrentAccountOrThrow();
        Set<String> permissions = account.getEffectivePermissionCodes();
        if (permissions.contains(PLATFORM_SCOPE)) {
            if (voucher.getScopeType() != null && voucher.getScopeType() != VoucherScopeType.PLATFORM) {
                throw new AccessDeniedException("System Admin can only create platform vouchers");
            }
            requireNoOwnerInput(voucher);
            setScope(voucher, VoucherScopeType.PLATFORM, null, null);
            return;
        }
        if (permissions.contains(PARTNER_SCOPE)) {
            applyPartnerCreationScope(voucher, account);
            return;
        }
        if (permissions.contains(ASSIGNED_SCOPE)) {
            applyManagerCreationScope(voucher, account);
            return;
        }
        throw new AccessDeniedException("Current account cannot create vouchers");
    }

    public void ensureCanRead(Voucher voucher) {
        if (!canAccess(voucher, currentAccountPortIn.getCurrentAccountOrThrow())) {
            throw new AccessDeniedException("Current account cannot access this voucher");
        }
    }

    public void ensureCanUpdate(Voucher voucher) {
        ensureCanRead(voucher);
    }

    public void ensureCanChangeStatus(Voucher voucher) {
        CurrentAccountAccess account = currentAccountPortIn.getCurrentAccountOrThrow();
        if (account.getEffectivePermissionCodes().contains(ASSIGNED_SCOPE)
                && !account.getEffectivePermissionCodes().contains(PARTNER_SCOPE)
                && !account.getEffectivePermissionCodes().contains(PLATFORM_SCOPE)) {
            throw new AccessDeniedException("Parking Manager cannot activate or pause vouchers");
        }
        if (!canAccess(voucher, account)) {
            throw new AccessDeniedException("Current account cannot change this voucher status");
        }
    }

    public List<Voucher> filterVisible(List<Voucher> vouchers) {
        CurrentAccountAccess account = currentAccountPortIn.getCurrentAccountOrThrow();
        return vouchers.stream().filter(voucher -> canAccess(voucher, account)).toList();
    }

    private void applyPartnerCreationScope(Voucher voucher, CurrentAccountAccess account) {
        if (voucher.getOrganizationId() != null) {
            throw new BadRequestException("organizationId is derived from the authenticated Partner");
        }
        VoucherScopeType requested = voucher.getScopeType() == null ? VoucherScopeType.PARTNER : voucher.getScopeType();
        if (requested == VoucherScopeType.PLATFORM) {
            throw new AccessDeniedException("Partner Admin cannot create platform vouchers");
        }
        if (requested == VoucherScopeType.PARTNER) {
            UUID organizationId = requireSingleOrganization(account.activeOrganizationIds());
            if (voucher.getParkingLotId() != null) {
                throw new BadRequestException("parkingLotId must be empty for Partner vouchers");
            }
            setScope(voucher, requested, organizationId, null);
            return;
        }
        ParkingLot parkingLot = requireParkingLot(voucher.getParkingLotId());
        if (!account.activeOrganizationIds().contains(parkingLot.getOrganizationId())) {
            throw new AccessDeniedException("Parking lot does not belong to the current Partner");
        }
        setScope(voucher, VoucherScopeType.PARKING_LOT, parkingLot.getOrganizationId(), parkingLot.getParkingLotId());
    }

    private void applyManagerCreationScope(Voucher voucher, CurrentAccountAccess account) {
        if (voucher.getOrganizationId() != null) {
            throw new BadRequestException("organizationId is derived from the assigned parking lot");
        }
        if (voucher.getScopeType() != null && voucher.getScopeType() != VoucherScopeType.PARKING_LOT) {
            throw new AccessDeniedException("Parking Manager can only create parking-lot vouchers");
        }
        UUID parkingLotId = resolveManagerParkingLot(voucher.getParkingLotId(), account.assignedParkingLotIds());
        ParkingLot parkingLot = requireParkingLot(parkingLotId);
        setScope(voucher, VoucherScopeType.PARKING_LOT, parkingLot.getOrganizationId(), parkingLotId);
    }

    private boolean canAccess(Voucher voucher, CurrentAccountAccess account) {
        Set<String> permissions = account.getEffectivePermissionCodes();
        if (permissions.contains(PLATFORM_SCOPE)) {
            return voucher.getScopeType() == VoucherScopeType.PLATFORM;
        }
        if (permissions.contains(PARTNER_SCOPE)) {
            return voucher.getScopeType() != VoucherScopeType.PLATFORM
                    && voucher.getOrganizationId() != null
                    && account.activeOrganizationIds().contains(voucher.getOrganizationId());
        }
        if (permissions.contains(ASSIGNED_SCOPE)) {
            return voucher.getScopeType() == VoucherScopeType.PARKING_LOT
                    && voucher.getParkingLotId() != null
                    && account.assignedParkingLotIds().contains(voucher.getParkingLotId());
        }
        return false;
    }

    private UUID resolveManagerParkingLot(UUID requested, Set<UUID> assigned) {
        if (requested != null && assigned.contains(requested)) return requested;
        if (requested == null && assigned.size() == 1) return assigned.iterator().next();
        if (requested == null && assigned.size() > 1) {
            throw new BadRequestException("parkingLotId is required when the Manager is assigned to multiple parking lots");
        }
        throw new AccessDeniedException("Parking Manager is not assigned to the requested parking lot");
    }

    private ParkingLot requireParkingLot(UUID parkingLotId) {
        if (parkingLotId == null) throw new BadRequestException("parkingLotId must not be null");
        return parkingLotPortOut.findById(parkingLotId)
                .orElseThrow(() -> new NotFoundException("Parking lot not found"));
    }

    private UUID requireSingleOrganization(Set<UUID> organizationIds) {
        if (organizationIds.size() != 1) {
            throw new BadRequestException("Current Partner account must have exactly one active organization");
        }
        return organizationIds.iterator().next();
    }

    private void requireNoOwnerInput(Voucher voucher) {
        if (voucher.getOrganizationId() != null || voucher.getParkingLotId() != null) {
            throw new BadRequestException("Platform vouchers must not specify organizationId or parkingLotId");
        }
    }

    private void setScope(Voucher voucher, VoucherScopeType scopeType, UUID organizationId, UUID parkingLotId) {
        voucher.setScopeType(scopeType);
        voucher.setOrganizationId(organizationId);
        voucher.setParkingLotId(parkingLotId);
    }
}
