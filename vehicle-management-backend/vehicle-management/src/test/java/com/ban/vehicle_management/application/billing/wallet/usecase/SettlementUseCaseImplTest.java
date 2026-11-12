package com.ban.vehicle_management.application.billing.wallet.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut;
import com.ban.vehicle_management.application.billing.payment.port.out.PaymentPortOut;
import com.ban.vehicle_management.application.billing.platformfee.port.in.PlatformFeeResolverPortIn;
import com.ban.vehicle_management.application.billing.platformfee.port.out.PlatformFeeAssessmentPortOut;
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
import com.ban.vehicle_management.domain.billing.wallet.model.RevenueAllocationReversal;
import com.ban.vehicle_management.domain.billing.wallet.model.RevenueAllocationReversalTotals;
import com.ban.vehicle_management.domain.billing.wallet.model.VoucherFinancialTerm;
import com.ban.vehicle_management.domain.billing.wallet.model.Wallet;
import com.ban.vehicle_management.shared.enumeration.billing.CommissionBasis;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeBasis;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeChargedTo;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
import com.ban.vehicle_management.shared.enumeration.billing.RevenueAllocationStatus;
import com.ban.vehicle_management.shared.enumeration.billing.WalletPurpose;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentMethod;
import com.ban.vehicle_management.shared.enumeration.billing.FundCustody;
import com.ban.vehicle_management.shared.enumeration.billing.LedgerEntrySide;
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
import org.mockito.ArgumentCaptor;
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
    private PlatformFeeAssessmentPortOut platformFeeAssessmentPortOut;
    @Mock
    private VoucherPortOut voucherPortOut;

    private SettlementUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new SettlementUseCaseImpl(
                walletPortOut, invoicePortOut, paymentPortOut, ledgerPortOut, settlementPortOut,
                organizationPortOut, walletAccessGuard, currentAccountPortIn, platformFeeResolverPortIn,
                platformFeeAssessmentPortOut, voucherPortOut, 7);
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
        payment.setPaymentMethod(PaymentMethod.VNPAY);

        Invoice invoice = new Invoice();
        invoice.setInvoiceId(invoiceId);
        invoice.setSubscriptionId(subscriptionId);
        invoice.setPurpose(com.ban.vehicle_management.shared.enumeration.billing.InvoicePurpose.SUBSCRIPTION_PURCHASE);
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
        when(platformFeeAssessmentPortOut.findByPaymentId(paymentId)).thenReturn(Optional.empty());
        when(platformFeeAssessmentPortOut.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        VoucherRedemption redemption = new VoucherRedemption();
        redemption.setVoucherId(voucherId);
        redemption.setSubscriptionId(subscriptionId);
        when(voucherPortOut.findRedemptionByInvoiceId(invoiceId)).thenReturn(Optional.of(redemption));
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

    @Test
    void partnerCustodyRecordsOnlyPlatformFeeReceivableAndDoesNotCreatePartnerPayable() {
        UUID paymentId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        UUID parkingLotId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        Instant paidAt = Instant.parse("2026-10-15T08:00:00Z");

        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setInvoiceId(invoiceId);
        payment.setPaidAt(paidAt);
        payment.setPaymentMethod(PaymentMethod.CASH);

        Invoice invoice = new Invoice();
        invoice.setInvoiceId(invoiceId);
        invoice.setParkingLotId(parkingLotId);
        invoice.setPurpose(com.ban.vehicle_management.shared.enumeration.billing.InvoicePurpose.LOST_CARD);
        invoice.setAmount(new BigDecimal("100000"));
        invoice.setDiscountAmount(BigDecimal.ZERO);
        invoice.setFinalAmount(new BigDecimal("100000"));

        PlatformFeeLine partnerFee = new PlatformFeeLine(
                UUID.randomUUID(), UUID.randomUUID(), "PLATFORM_FEE", "Phí nền tảng", 1,
                new BigDecimal("0.05"), PlatformFeeBasis.GROSS_AMOUNT,
                PlatformFeeChargedTo.PARTNER, 100000L, 5000L);
        PlatformFeeResolution resolution = new PlatformFeeResolution(
                PlatformFeeTransactionType.LOST_CARD, paidAt,
                100000L, 100000L, List.of(partnerFee),
                5000L, new BigDecimal("0.05"));

        LedgerAccount ledgerAccount = new LedgerAccount();
        ledgerAccount.setLedgerAccountId(UUID.randomUUID());
        when(paymentPortOut.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));
        when(settlementPortOut.findAllocationByPayment(paymentId)).thenReturn(Optional.empty());
        when(invoicePortOut.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(organizationPortOut.findOrganizationIdByParkingLotId(parkingLotId))
                .thenReturn(Optional.of(organizationId));
        when(platformFeeResolverPortIn.resolveFees(
                PlatformFeeTransactionType.LOST_CARD, paidAt, 100000L, 100000L))
                .thenReturn(resolution);
        when(platformFeeAssessmentPortOut.findByPaymentId(paymentId)).thenReturn(Optional.empty());
        when(platformFeeAssessmentPortOut.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(voucherPortOut.findRedemptionByInvoiceId(invoiceId)).thenReturn(Optional.empty());
        when(ledgerPortOut.findAccountByCode(anyString())).thenReturn(Optional.of(ledgerAccount));
        when(settlementPortOut.saveAllocation(any(RevenueAllocation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RevenueAllocation allocation = useCase.allocateForCompletedPayment(paymentId);

        assertEquals(new BigDecimal("5000.00"), allocation.getPlatformFeeAmount());
        assertEquals(new BigDecimal("0.00"), allocation.getPartnerPayableAmount());
        verify(ledgerPortOut).findAccountByCode("PARTNER_RECEIVABLE");
        verify(ledgerPortOut).findAccountByCode("PLATFORM_REVENUE");
        verify(ledgerPortOut, never()).findAccountByCode("PLATFORM_CASH_CLEARING");
        verify(walletPortOut, never()).findOrganizationWalletForUpdate(
                organizationId, "VND", WalletPurpose.ORGANIZATION_SETTLEMENT);
    }

    @Test
    @SuppressWarnings("unchecked")
    void partialRefundCreatesBalancedAllocationReversalAndReducesPendingPartnerBalance() {
        UUID originalPaymentId = UUID.randomUUID();
        UUID refundPaymentId = UUID.randomUUID();
        UUID allocationId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        RevenueAllocation allocation = new RevenueAllocation();
        allocation.setRevenueAllocationId(allocationId);
        allocation.setPaymentId(originalPaymentId);
        allocation.setOrganizationId(organizationId);
        allocation.setCustomerPaidAmount(new BigDecimal("100000.00"));
        allocation.setPlatformFeeAmount(new BigDecimal("5000.00"));
        allocation.setVoucherCommissionAmount(BigDecimal.ZERO);
        allocation.setPartnerPayableAmount(new BigDecimal("95000.00"));
        allocation.setGatewayFeeAmount(BigDecimal.ZERO);
        allocation.setDiscountAmount(BigDecimal.ZERO);
        allocation.setFundCustody(FundCustody.PLATFORM);
        allocation.setFundingSourceSnapshot("PARTNER");

        Wallet partnerWallet = new Wallet();
        partnerWallet.setPendingBalance(new BigDecimal("95000.00"));
        partnerWallet.setAvailableBalance(BigDecimal.ZERO);
        LedgerAccount ledgerAccount = new LedgerAccount();
        ledgerAccount.setLedgerAccountId(UUID.randomUUID());
        when(settlementPortOut.findReversalByRefundPayment(refundPaymentId))
                .thenReturn(Optional.empty());
        when(settlementPortOut.findAllocationByPayment(originalPaymentId))
                .thenReturn(Optional.of(allocation));
        when(settlementPortOut.findAllocationByIdForUpdate(allocationId))
                .thenReturn(Optional.of(allocation));
        when(settlementPortOut.sumReversals(allocationId))
                .thenReturn(RevenueAllocationReversalTotals.zero());
        when(walletPortOut.findOrganizationWalletForUpdate(
                organizationId, "VND", WalletPurpose.ORGANIZATION_SETTLEMENT))
                .thenReturn(Optional.of(partnerWallet));
        when(ledgerPortOut.findAccountByCode(anyString())).thenReturn(Optional.of(ledgerAccount));
        when(settlementPortOut.saveReversal(any(RevenueAllocationReversal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(platformFeeAssessmentPortOut.findByPaymentId(originalPaymentId))
                .thenReturn(Optional.empty());

        RevenueAllocationReversal reversal = useCase.reverseForRefund(
                originalPaymentId, refundPaymentId, new BigDecimal("50000"));

        assertEquals(new BigDecimal("2500.00"), reversal.getPlatformFeeAmount());
        assertEquals(new BigDecimal("47500.00"), reversal.getPartnerPayableAmount());
        assertEquals(new BigDecimal("47500.00"), partnerWallet.getPendingBalance());
        ArgumentCaptor<List<com.ban.vehicle_management.domain.billing.wallet.model.LedgerEntry>> entries =
                ArgumentCaptor.forClass(List.class);
        verify(ledgerPortOut).postBalancedTransaction(any(), entries.capture());
        BigDecimal debit = entries.getValue().stream()
                .filter(entry -> LedgerEntrySide.DEBIT.equals(entry.getEntrySide()))
                .map(com.ban.vehicle_management.domain.billing.wallet.model.LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = entries.getValue().stream()
                .filter(entry -> LedgerEntrySide.CREDIT.equals(entry.getEntrySide()))
                .map(com.ban.vehicle_management.domain.billing.wallet.model.LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(debit, credit);
    }

    @Test
    void releaseDueSettlementMarksPlatformAllocationSettled() {
        UUID allocationId = UUID.randomUUID();
        RevenueAllocation allocation = new RevenueAllocation();
        allocation.setRevenueAllocationId(allocationId);
        allocation.setStatus(RevenueAllocationStatus.POSTED);

        when(settlementPortOut.claimDueAllocations(anyInt(), any(Instant.class)))
                .thenReturn(List.of(allocationId));
        when(settlementPortOut.findAllocationByIdForUpdate(allocationId))
                .thenReturn(Optional.of(allocation));
        when(settlementPortOut.saveAllocation(allocation)).thenReturn(allocation);

        assertEquals(1, useCase.releaseDueSettlements(10));
        assertNotNull(allocation.getSettledAt());
        verify(settlementPortOut).saveAllocation(allocation);
    }

    @Test
    void releaseDueSettlementPropagatesFailureSoFinancialChangesRollback() {
        UUID allocationId = UUID.randomUUID();
        when(settlementPortOut.claimDueAllocations(anyInt(), any(Instant.class)))
                .thenReturn(List.of(allocationId));
        when(settlementPortOut.findAllocationByIdForUpdate(allocationId))
                .thenReturn(Optional.empty());

        assertThrows(com.ban.vehicle_management.shared.exception.NotFoundException.class,
                () -> useCase.releaseDueSettlements(10));
    }

    @Test
    void allocationFailsClosedWhenParkingLotOrganizationCannotBeResolved() {
        UUID paymentId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        UUID parkingLotId = UUID.randomUUID();
        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setInvoiceId(invoiceId);
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(invoiceId);
        invoice.setParkingLotId(parkingLotId);

        when(paymentPortOut.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));
        when(settlementPortOut.findAllocationByPayment(paymentId)).thenReturn(Optional.empty());
        when(invoicePortOut.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(organizationPortOut.findOrganizationIdByParkingLotId(parkingLotId))
                .thenReturn(Optional.empty());

        assertThrows(com.ban.vehicle_management.shared.exception.NotFoundException.class,
                () -> useCase.allocateForCompletedPayment(paymentId));
        verify(platformFeeResolverPortIn, never()).resolveFees(any(), any(), anyLong(), anyLong());
    }
}
