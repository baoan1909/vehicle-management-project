package com.ban.vehicle_management.application.billing.wallet.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.accesscontrol.subscription.port.in.SubscriptionPortIn;
import com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut;
import com.ban.vehicle_management.application.billing.payment.port.out.PaymentPortOut;
import com.ban.vehicle_management.application.billing.wallet.authorization.WalletAccessGuard;
import com.ban.vehicle_management.application.billing.wallet.port.in.SettlementPortIn;
import com.ban.vehicle_management.application.billing.wallet.port.out.LedgerPortOut;
import com.ban.vehicle_management.application.billing.wallet.port.out.WalletPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.parking.parkingsession.port.in.ParkingCheckoutCompletionPortIn;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.billing.payment.model.Payment;
import com.ban.vehicle_management.domain.billing.wallet.model.LedgerAccount;
import com.ban.vehicle_management.domain.billing.wallet.model.Wallet;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import com.ban.vehicle_management.shared.enumeration.billing.WalletOwnerType;
import com.ban.vehicle_management.shared.enumeration.billing.WalletPurpose;
import com.ban.vehicle_management.shared.enumeration.billing.WalletStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WalletInvoicePaymentUseCaseImplTest {

    @Mock
    private WalletPortOut walletPortOut;
    @Mock
    private InvoicePortOut invoicePortOut;
    @Mock
    private PaymentPortOut paymentPortOut;
    @Mock
    private LedgerPortOut ledgerPortOut;
    @Mock
    private WalletAccessGuard walletAccessGuard;
    @Mock
    private CurrentAccountPortIn currentAccountPortIn;
    @Mock
    private SubscriptionPortIn subscriptionPortIn;
    @Mock
    private ParkingCheckoutCompletionPortIn parkingCheckoutCompletionPortIn;
    @Mock
    private SettlementPortIn settlementPortIn;

    private WalletInvoicePaymentUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new WalletInvoicePaymentUseCaseImpl(
                walletPortOut, invoicePortOut, paymentPortOut, ledgerPortOut, walletAccessGuard,
                currentAccountPortIn, subscriptionPortIn, parkingCheckoutCompletionPortIn, settlementPortIn);
    }

    @Test
    void successfulSubscriptionPaymentAllocatesRevenueBeforeCompletingCheckout() {
        UUID customerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();

        Wallet wallet = new Wallet();
        wallet.setWalletId(UUID.randomUUID());
        wallet.setOwnerType(WalletOwnerType.CUSTOMER);
        wallet.setCustomerId(customerId);
        wallet.setWalletPurpose(WalletPurpose.PERSONAL);
        wallet.setCurrency("VND");
        wallet.setAvailableBalance(new BigDecimal("500000.00"));
        wallet.setPendingBalance(BigDecimal.ZERO);
        wallet.setHeldBalance(BigDecimal.ZERO);
        wallet.setStatus(WalletStatus.ACTIVE);

        Invoice invoice = new Invoice();
        invoice.setInvoiceId(invoiceId);
        invoice.setInvoiceNo("INV-TEST-001");
        invoice.setCustomerId(customerId);
        invoice.setSubscriptionId(subscriptionId);
        invoice.setAmount(new BigDecimal("200000.00"));
        invoice.setDiscountAmount(new BigDecimal("20000.00"));
        invoice.setFinalAmount(new BigDecimal("180000.00"));
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setIssuedAt(Instant.parse("2026-10-15T07:00:00Z"));

        LedgerAccount ledgerAccount = new LedgerAccount();
        ledgerAccount.setLedgerAccountId(UUID.randomUUID());
        when(paymentPortOut.findByIdempotencyKey("wallet-payment-001")).thenReturn(Optional.empty());
        when(walletAccessGuard.resolveCurrentApprovedCustomerId()).thenReturn(customerId);
        when(walletPortOut.findCustomerWalletForUpdate(customerId, "VND", WalletPurpose.PERSONAL))
                .thenReturn(Optional.of(wallet));
        when(invoicePortOut.findByIdForUpdate(invoiceId)).thenReturn(Optional.of(invoice));
        when(paymentPortOut.existsByInvoiceIdAndStatus(any(), any())).thenReturn(false);
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(ledgerPortOut.findAccountByCode(anyString())).thenReturn(Optional.of(ledgerAccount));
        when(paymentPortOut.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = useCase.payInvoice(invoiceId, "wallet-payment-001");

        assertEquals(new BigDecimal("320000.00"), wallet.getAvailableBalance());
        verify(settlementPortIn).allocateForCompletedPayment(payment.getPaymentId());
        InOrder order = inOrder(subscriptionPortIn, settlementPortIn, parkingCheckoutCompletionPortIn);
        order.verify(subscriptionPortIn).markSubscriptionPaymentCompleted(subscriptionId);
        order.verify(settlementPortIn).allocateForCompletedPayment(payment.getPaymentId());
        order.verify(parkingCheckoutCompletionPortIn).completePaidCheckout(invoiceId);
    }
}
