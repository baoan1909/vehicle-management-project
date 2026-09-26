package com.ban.vehicle_management.application.billing.invoice.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountProfilePortOut;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.iam.account.model.AccountProfileState;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class InvoiceAccessGuard {

    private static final String CREATE_PERMISSION = "INVOICE_CREATE_ALL";
    private static final String READ_OWN_PERMISSION = "INVOICE_READ_OWN";
    private static final String READ_ALL_PERMISSION = "INVOICE_READ_ALL";
    private static final String CANCEL_PERMISSION = "INVOICE_CANCEL_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final AccountProfilePortOut accountProfilePortOut;
    private final OrganizationAccessGuard organizationAccessGuard;
    private final ParkingLotPortOut parkingLotPortOut;

    public  InvoiceAccessGuard(
            CurrentAccountPortIn currentAccountPortIn,
            AccountProfilePortOut accountProfilePortOut,
            OrganizationAccessGuard organizationAccessGuard,
            ParkingLotPortOut parkingLotPortOut
    ){
        this.currentAccountPortIn = currentAccountPortIn;
        this.accountProfilePortOut = accountProfilePortOut;
        this.organizationAccessGuard = organizationAccessGuard;
        this.parkingLotPortOut = parkingLotPortOut;
    }

    public  void  ensureCanCreate(){
        currentAccountPortIn.requirePermission(CREATE_PERMISSION);
    }

    public void ensureCanCreateAtParkingLot(UUID parkingLotId) {
        ensureLotOperable(parkingLotId);
    }

    public void ensureCanCancel(Invoice invoice){
        currentAccountPortIn.requirePermission(CANCEL_PERMISSION);
        ensureLotOperable(invoice.getParkingLotId());
    }

    public void ensureCanReadAll(){
        currentAccountPortIn.requirePermission(READ_ALL_PERMISSION);
    }

    public void ensureCanRead(Invoice invoice){
        if (currentAccountPortIn.hasPermission(READ_ALL_PERMISSION)){
            ensureLotVisible(invoice.getParkingLotId());
            return;
        }

        currentAccountPortIn.requirePermission(READ_OWN_PERMISSION);
        UUID customerId = resolveCurrentApprovedCustomerId();

        if (invoice.getCustomerId() == null || !customerId.equals(invoice.getCustomerId())){
            throw new AccessDeniedException("Access is denied");
        }
    }

    public  UUID resolveCustomerIdForList(UUID requestedCusromerId){
        if (currentAccountPortIn.hasPermission(READ_ALL_PERMISSION)){
            return requestedCusromerId;
        }

        currentAccountPortIn.requirePermission(READ_OWN_PERMISSION);
        UUID currentCustomerId = resolveCurrentApprovedCustomerId();
        if (requestedCusromerId != null && !currentCustomerId.equals(requestedCusromerId)){
            throw new AccessDeniedException("Access is denied");
        }

        return currentCustomerId;
    }

    /** Null means a platform-wide read or an own-customer read. */
    public Set<UUID> visibleParkingLotIdsForList() {
        if (!currentAccountPortIn.hasPermission(READ_ALL_PERMISSION)) {
            return null;
        }
        return organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut);
    }

    public Set<UUID> visibleParkingLotIdsForManagement(UUID organizationId, UUID parkingLotId) {
        ensureCanReadAll();
        Set<UUID> allowed = organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut);
        if (organizationId == null && parkingLotId == null) {
            return allowed;
        }

        if (parkingLotId != null) {
            ParkingLot lot = parkingLotPortOut.findById(parkingLotId)
                    .orElseThrow(() -> new AccessDeniedException("Parking lot is not accessible"));
            if (organizationId != null && !organizationId.equals(lot.getOrganizationId())) {
                throw new AccessDeniedException("Parking lot does not belong to the selected partner");
            }
            if (allowed != null && !allowed.contains(parkingLotId)) {
                throw new AccessDeniedException("Parking lot is not accessible");
            }
            return Set.of(parkingLotId);
        }

        Set<UUID> partnerLots = parkingLotPortOut.findAll(null, null, Set.of(organizationId), null).stream()
                .map(ParkingLot::getParkingLotId)
                .collect(Collectors.toSet());
        if (allowed != null) {
            partnerLots.retainAll(allowed);
        }
        return partnerLots;
    }

    public void ensureCanReadManagementInvoice(Invoice invoice) {
        ensureCanReadAll();
        ensureLotVisible(invoice.getParkingLotId());
    }

    private void ensureLotVisible(UUID parkingLotId) {
        Set<UUID> allowed = organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut);
        if (allowed != null && (parkingLotId == null || !allowed.contains(parkingLotId))) {
            throw new AccessDeniedException("Current account cannot access this parking lot invoice");
        }
    }

    private void ensureLotOperable(UUID parkingLotId) {
        Set<String> permissions = currentAccountPortIn.getCurrentAccountOrThrow().getEffectivePermissionCodes();
        if (!permissions.contains(OrganizationAccessGuard.PARKING_SCOPE_PARTNER)
                && !permissions.contains(OrganizationAccessGuard.PARKING_SCOPE_ASSIGNED)) {
            throw new AccessDeniedException("Current account cannot operate parking lot invoices");
        }
        if (parkingLotId == null) {
            throw new AccessDeniedException("Invoice has no parking lot scope");
        }
        Set<UUID> allowed = organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut);
        if (allowed == null || !allowed.contains(parkingLotId)) {
            throw new AccessDeniedException("Current account cannot operate this parking lot invoice");
        }
    }

    private  UUID resolveCurrentApprovedCustomerId(){
        UUID accountId = currentAccountPortIn.getCurrentAccountIdOrThrow();

        AccountProfileState profileState = accountProfilePortOut.findProfileStateByAccountId(accountId)
                .orElseThrow(()-> new AccessDeniedException("Access is denied"));

        if(profileState.customerId() == null
            || !CustomerStatus.ACTIVE.equals(profileState.customerStatus())
            || !CustomerApprovalStatus.APPROVED.equals(profileState.customerApprovalStatus()) ){
            throw new AccessDeniedException("Access is denied");
        }

        return profileState.customerId();
    }
}
