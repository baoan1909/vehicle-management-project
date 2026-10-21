package com.ban.vehicle_management.application.billing.wallet.usecase;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.billing.wallet.authorization.WalletAccessGuard;
import com.ban.vehicle_management.application.billing.wallet.port.out.LedgerPortOut;
import com.ban.vehicle_management.application.billing.wallet.port.out.WalletPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.application.people.customer.port.out.CustomerPortOut;
import com.ban.vehicle_management.domain.billing.wallet.model.Wallet;
import com.ban.vehicle_management.domain.iam.organization.model.Organization;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.shared.enumeration.billing.WalletPurpose;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WalletUseCaseImplTest {

    @Mock WalletPortOut walletPortOut;
    @Mock LedgerPortOut ledgerPortOut;
    @Mock WalletAccessGuard walletAccessGuard;
    @Mock CurrentAccountPortIn currentAccountPortIn;
    @Mock CustomerPortOut customerPortOut;
    @Mock OrganizationPortOut organizationPortOut;
    @InjectMocks WalletUseCaseImpl useCase;

    @Test
    void provisioningSameCustomerTwiceCreatesWalletOnlyOnce() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setCustomerId(customerId);
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setApprovalStatus(CustomerApprovalStatus.APPROVED);
        Wallet persisted = new Wallet();

        when(customerPortOut.findById(customerId)).thenReturn(Optional.of(customer));
        when(walletPortOut.findCustomerWallet(customerId, "VND", WalletPurpose.PERSONAL))
                .thenReturn(Optional.empty(), Optional.of(persisted));
        when(walletPortOut.createIfAbsent(org.mockito.ArgumentMatchers.any(Wallet.class)))
                .thenReturn(persisted);

        assertSame(persisted, useCase.provisionCustomerWallet(customerId));
        assertSame(persisted, useCase.provisionCustomerWallet(customerId));
        verify(walletPortOut, times(1)).createIfAbsent(org.mockito.ArgumentMatchers.any(Wallet.class));
    }

    @Test
    void provisioningSamePartnerTwiceCreatesWalletOnlyOnce() {
        UUID organizationId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setOrganizationId(organizationId);
        organization.setStatus(OrganizationStatus.ACTIVE);
        Wallet persisted = new Wallet();

        when(organizationPortOut.findById(organizationId)).thenReturn(Optional.of(organization));
        when(walletPortOut.findOrganizationWallet(
                        organizationId, "VND", WalletPurpose.ORGANIZATION_SETTLEMENT))
                .thenReturn(Optional.empty(), Optional.of(persisted));
        when(walletPortOut.createIfAbsent(org.mockito.ArgumentMatchers.any(Wallet.class)))
                .thenReturn(persisted);

        assertSame(persisted, useCase.provisionPartnerWallet(organizationId));
        assertSame(persisted, useCase.provisionPartnerWallet(organizationId));
        verify(walletPortOut, times(1)).createIfAbsent(org.mockito.ArgumentMatchers.any(Wallet.class));
    }
}
