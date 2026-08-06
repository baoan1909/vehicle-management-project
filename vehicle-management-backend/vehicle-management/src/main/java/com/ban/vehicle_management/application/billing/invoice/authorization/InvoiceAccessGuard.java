package com.ban.vehicle_management.application.billing.invoice.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountProfilePortOut;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.iam.account.model.AccountProfileState;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class InvoiceAccessGuard {

    private static final String CREATE_PERMISSION = "INVOICE_CREATE_ALL";
    private static final String READ_OWN_PERMISSION = "INVOICE_READ_OWN";
    private static final String READ_ALL_PERMISSION = "INVOICE_READ_ALL";
    private static final String CANCEL_PERMISSION = "INVOICE_CANCEL_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final AccountProfilePortOut accountProfilePortOut;

    public  InvoiceAccessGuard(
            CurrentAccountPortIn currentAccountPortIn,
            AccountProfilePortOut accountProfilePortOut
    ){
        this.currentAccountPortIn = currentAccountPortIn;
        this.accountProfilePortOut = accountProfilePortOut;
    }

    public  void  ensureCanCreate(){
        currentAccountPortIn.requirePermission(CREATE_PERMISSION);
    }

    public void ensureCanCancel(){
        currentAccountPortIn.requirePermission(CANCEL_PERMISSION);
    }

    public void ensureCanReadAll(){
        currentAccountPortIn.requirePermission(READ_ALL_PERMISSION);
    }

    public void ensureCanRead(Invoice invoice){
        if (currentAccountPortIn.hasPermission(READ_ALL_PERMISSION)){
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
