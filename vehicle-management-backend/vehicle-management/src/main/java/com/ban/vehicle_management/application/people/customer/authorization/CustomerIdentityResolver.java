package com.ban.vehicle_management.application.people.customer.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.people.customer.port.out.CustomerPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.exception.CustomerIdentityRequiredException;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/** Resolves server-owned customer identity without reloading authorization graphs. */
@Component
public class CustomerIdentityResolver {

    private final CurrentAccountPortIn currentAccountPortIn;
    private final CustomerPortOut customerPortOut;

    public CustomerIdentityResolver(CurrentAccountPortIn currentAccountPortIn, CustomerPortOut customerPortOut) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.customerPortOut = customerPortOut;
    }

    public UUID resolveCurrentCustomerIdForSupport(String permissionCode) {
        CurrentAccountAccess access = currentAccountPortIn.getCurrentAccountOrThrow();
        if (!access.canUseEffectivePermissions() || !access.permissionCodes().contains(permissionCode)) {
            throw new AccessDeniedException("Access is denied");
        }
        if (access.customerStatus() == null && access.customerApprovalStatus() == null) {
            throw new CustomerIdentityRequiredException(
                    "Tài khoản chưa có hồ sơ khách hàng để sử dụng hỗ trợ");
        }
        currentAccountPortIn.requirePermission(permissionCode);
        CustomerApprovalStatus approval = access.customerApprovalStatus();
        boolean supportLifecycle = CustomerApprovalStatus.PENDING.equals(approval)
                || CustomerApprovalStatus.REJECTED.equals(approval)
                || (CustomerApprovalStatus.APPROVED.equals(approval)
                && CustomerStatus.ACTIVE.equals(access.customerStatus()));
        if (!supportLifecycle) {
            throw new AccessDeniedException("Access is denied");
        }
        return findCustomerId(access.accountId());
    }

    public UUID resolveCurrentApprovedCustomerId() {
        CurrentAccountAccess access = currentAccountPortIn.getCurrentAccountOrThrow();
        if (!CustomerStatus.ACTIVE.equals(access.customerStatus())
                || !CustomerApprovalStatus.APPROVED.equals(access.customerApprovalStatus())) {
            throw new AccessDeniedException("Access is denied");
        }
        return findCustomerId(access.accountId());
    }

    private UUID findCustomerId(UUID accountId) {
        return customerPortOut.findCustomerIdByAccountId(accountId)
                .orElseThrow(() -> new CustomerIdentityRequiredException(
                        "Tài khoản chưa có hồ sơ khách hàng để sử dụng hỗ trợ"));
    }
}
