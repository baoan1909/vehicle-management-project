package com.ban.vehicle_management.application.billing.wallet.authorization;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountProfilePortOut;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.domain.billing.wallet.model.Wallet;
import com.ban.vehicle_management.domain.iam.account.model.AccountProfileState;
import com.ban.vehicle_management.shared.enumeration.billing.WalletOwnerType;
import com.ban.vehicle_management.shared.enumeration.billing.WalletPurpose;
import com.ban.vehicle_management.shared.enumeration.billing.WalletStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class WalletAccessGuardTest {

    @Mock CurrentAccountPortIn currentAccountPortIn;
    @Mock AccountProfilePortOut accountProfilePortOut;
    @Mock OrganizationPortOut organizationPortOut;
    @InjectMocks WalletAccessGuard guard;

    private Wallet customerWallet(UUID customerId) {
        Wallet wallet = new Wallet();
        wallet.setWalletId(UUID.randomUUID());
        wallet.setOwnerType(WalletOwnerType.CUSTOMER);
        wallet.setCustomerId(customerId);
        wallet.setWalletPurpose(WalletPurpose.PERSONAL);
        wallet.setCurrency("VND");
        wallet.setAvailableBalance(BigDecimal.ZERO);
        wallet.setPendingBalance(BigDecimal.ZERO);
        wallet.setHeldBalance(BigDecimal.ZERO);
        wallet.setStatus(WalletStatus.ACTIVE);
        return wallet;
    }

    private Wallet partnerWallet(UUID organizationId) {
        Wallet wallet = new Wallet();
        wallet.setWalletId(UUID.randomUUID());
        wallet.setOwnerType(WalletOwnerType.ORGANIZATION);
        wallet.setOrganizationId(organizationId);
        wallet.setWalletPurpose(WalletPurpose.ORGANIZATION_SETTLEMENT);
        wallet.setCurrency("VND");
        wallet.setAvailableBalance(BigDecimal.ZERO);
        wallet.setPendingBalance(BigDecimal.ZERO);
        wallet.setHeldBalance(BigDecimal.ZERO);
        wallet.setStatus(WalletStatus.ACTIVE);
        return wallet;
    }

    @Test
    void customerCannotReadAnotherCustomerWallet() {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        when(currentAccountPortIn.hasPermission(WalletAccessGuard.READ_ALL)).thenReturn(false);
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(accountProfilePortOut.findProfileStateByAccountId(accountId))
                .thenReturn(Optional.of(new AccountProfileState(
                        accountId, "user", "e@mail.com", null, "CUSTOMER", null, "Name", null,
                        null, null, null, null, null, null, null, null, null, null, null,
                        me, null, null, CustomerStatus.ACTIVE, CustomerApprovalStatus.APPROVED, null)));

        assertThrows(AccessDeniedException.class, () -> guard.ensureCanReadCustomerWallet(customerWallet(other)));
    }

    @Test
    void customerCanReadOwnWallet() {
        UUID me = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        when(currentAccountPortIn.hasPermission(WalletAccessGuard.READ_ALL)).thenReturn(false);
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(accountProfilePortOut.findProfileStateByAccountId(accountId))
                .thenReturn(Optional.of(new AccountProfileState(
                        accountId, "user", "e@mail.com", null, "CUSTOMER", null, "Name", null,
                        null, null, null, null, null, null, null, null, null, null, null,
                        me, null, null, CustomerStatus.ACTIVE, CustomerApprovalStatus.APPROVED, null)));

        assertDoesNotThrow(() -> guard.ensureCanReadCustomerWallet(customerWallet(me)));
    }

    @Test
    void partnerCannotReadOtherPartnerWallet() {
        UUID orgA = UUID.randomUUID();
        UUID orgB = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        when(currentAccountPortIn.hasPermission(WalletAccessGuard.READ_ALL)).thenReturn(false);
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(organizationPortOut.findActiveOrganizationIdsByAccountId(accountId)).thenReturn(Set.of(orgA));

        assertThrows(AccessDeniedException.class, () -> guard.ensureCanReadPartnerWallet(partnerWallet(orgB)));
    }

    @Test
    void platformReadAllBypassesScope() {
        when(currentAccountPortIn.hasPermission(WalletAccessGuard.READ_ALL)).thenReturn(true);

        assertDoesNotThrow(() -> guard.ensureCanReadPartnerWallet(partnerWallet(UUID.randomUUID())));
    }
}
