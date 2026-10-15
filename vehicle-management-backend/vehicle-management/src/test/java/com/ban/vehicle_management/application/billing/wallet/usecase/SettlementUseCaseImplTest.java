package com.ban.vehicle_management.application.billing.wallet.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut;
import com.ban.vehicle_management.application.billing.payment.port.out.PaymentPortOut;
import com.ban.vehicle_management.application.billing.platformfee.port.in.PlatformFeeResolverPortIn;
import com.ban.vehicle_management.application.billing.wallet.authorization.WalletAccessGuard;
import com.ban.vehicle_management.application.billing.wallet.port.out.LedgerPortOut;
import com.ban.vehicle_management.application.billing.wallet.port.out.SettlementPortOut;
import com.ban.vehicle_management.application.billing.wallet.port.out.WalletPortOut;
import com.ban.vehicle_management.application.catalog.voucher.port.out.VoucherPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.billing.payment.model.Payment;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeLine;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeResolution;
import com.ban.vehicle_management.domain.billing.voucherredemption.model.VoucherRedemption;
import com.ban.vehicle_management.domain.billing.wallet.model.LedgerAccount;
import com.ban.vehicle_management.domain.billing.wallet.model.RevenueAllocation;
import com.ban.vehicle_management.domain.billing.wallet.model.VoucherFinancialTerm;
import com.ban.vehicle_management.shared.enumeration.billing.CommissionBasis;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeBasis;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeChargedTo;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
import com.ban.vehicle_management.shared.enumeration.billing.VoucherFundingSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SettlementUseCaseImplTest {

    @Mock
    private WalletPortOut walletPortOut;
    @Mock
    private InvoicePortOut invoicePortOut;
    @Mock
    private PaymentPortOut paymentPortOut;
    @Mock
    private LedgerPortOut ledgerPortOut;
    @Mock
    private SettlementPortOut settlementPortOut;
    @Mock
    private OrganizationPortOut organizationPortOut;
    @Mock
    private WalletAccessGuard walletAccessGuard;
    @Mock
    private CurrentAccountPortIn currentAccountPortIn;
    @Mock
    private PlatformFeeResolverPortIn platformFeeResolverPortIn;
    @Mock
    private VoucherPortOut voucherPortOut;

    private SettlementUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new SettlementUseCaseImpl(
                walletPortOut, invoicePortOut, paymentPortOut, ledgerPortOut, settlementPortOut,
                organizationPortOut, walletAccessGuard, currentAccountPortIn, platformFeeResolverPortIn,
                voucherPortOut, new BigDecimal("0.05"), 7);
    }

    @Test
    void subscriptionSettlementUsesResolvedPartnerFeeInsteadOfLegacyRate() {
        UUID paymentId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        UUID voucherId = UUID.randomUUID();
        Instant paidAt = Instant.parse("2026-10-15T08:00:00Z");

        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setInvoiceId(invoiceId);
        payment.setPaidAt(paidAt);

        Invoice invoice = new Invoice();
        invoice.setInvoiceId(invoiceId);
        invoice.setSubscriptionId(subscriptionId);
        invoice.setAmount(new BigDecimal("200000"));
        invoice.setDiscountAmount(new BigDecimal("20000"));
        invoice.setFinalAmount(new BigDecimal("180000"));

        PlatformFeeLine partnerFee = new PlatformFeeLine(
                UUID.randomUUID(), UUID.randomUUID(), "PLATFORM_FEE", "Phí nền tảng", 1,
                new BigDecimal("0.06"), PlatformFeeBasis.GROSS_AMOUNT,
                PlatformFeeChargedTo.PARTNER, 200000L, 12000L);
        PlatformFeeLine platformFundedFee = new PlatformFeeLine(
                UUID.randomUUID(), UUID.randomUUID(), "INTERNAL_FEE", "Phí nội bộ", 1,
                new BigDecimal("0.01"), PlatformFeeBasis.GROSS_AMOUNT,
                PlatformFeeChargedTo.PLATFORM, 200000L, 2000L);
        PlatformFeeResolution resolution = new PlatformFeeResolution(
                PlatformFeeTransactionType.SUBSCRIPTION_PURCHASE, paidAt,
                200000L, 180000L, List.of(partnerFee, platformFundedFee),
                14000L, new BigDecimal("0.07"));

        LedgerAccount ledgerAccount = new LedgerAccount();
        ledgerAccount.setLedgerAccountId(UUID.randomUUID());
        when(paymentPortOut.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));
        when(settlementPortOut.findAllocationByPayment(paymentId)).thenReturn(Optional.empty());
        when(invoicePortOut.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(platformFeeResolverPortIn.resolveFees(
                PlatformFeeTransactionType.SUBSCRIPTION_PURCHASE, paidAt, 200000L, 180000L))
                .thenReturn(resolution);
        VoucherRedemption redemption = new VoucherRedemption();
        redemption.setVoucherId(voucherId);
        redemption.setSubscriptionId(subscriptionId);
        when(voucherPortOut.findRedemptionBySubscriptionId(subscriptionId)).thenReturn(Optional.of(redemption));
        VoucherFinancialTerm term = new VoucherFinancialTerm();
        term.setFundingSource(VoucherFundingSource.PLATFORM);
        term.setCommissionBasis(CommissionBasis.GROSS);
        term.setCommissionRate(new BigDecimal("0.10"));
        term.setSettlementDelayDays(7);
        when(settlementPortOut.findActiveTerm(voucherId, paidAt)).thenReturn(Optional.of(term));
        when(ledgerPortOut.findAccountByCode(anyString())).thenReturn(Optional.of(ledgerAccount));
        when(settlementPortOut.saveAllocation(any(RevenueAllocation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RevenueAllocation allocation = useCase.allocateForCompletedPayment(paymentId);

        assertEquals(new BigDecimal("12000.00"), allocation.getPlatformFeeAmount());
        assertEquals(new BigDecimal("20000.00"), allocation.getVoucherCommissionAmount());
        assertEquals(new BigDecimal("188000.00"), allocation.getPartnerPayableAmount());
        assertEquals(voucherId, allocation.getVoucherId());
        verify(platformFeeResolverPortIn).resolveFees(
                PlatformFeeTransactionType.SUBSCRIPTION_PURCHASE, paidAt, 200000L, 180000L);
        verify(ledgerPortOut).findAccountByCode("PLATFORM_CASH_CLEARING");
        verify(ledgerPortOut, never()).findAccountByCode("CUSTOMER_WALLET_LIABILITY");
        verify(ledgerPortOut).findAccountByCode("PLATFORM_VOUCHER_EXPENSE");
    }
}
